"""
AI总结模块 - 使用 Ollama 调用 qwen3-8b 模型，
从文本中提取结构化的 Q&A 知识条目（性能优化版）
"""
import json
import re
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading

import ollama

from config import (
    OLLAMA_BASE_URL, OLLAMA_MODEL, OLLAMA_TIMEOUT, CHUNK_SIZE, CHUNK_OVERLAP,
)

_stop_requested = False

SUMMARY_PROMPT = """从以下校园文本提取Q&A知识条目，输出JSON数组：

格式：[{"question":"问题","answer":"答案","category":"分类","keywords":"关键词"}]

要求：
- question模拟学生提问（简洁明了，不超过30字）
- answer必须完整详细：
  ⚠️ 保留所有关键信息：时间、地点、人物、事件、数字、URL等
  ⚠️ 答案长度建议100-500字，确保信息完整
  ⚠️ 如果包含[链接: URL]，将URL整合到答案中
  ⚠️ 遇到"点击跳转"、"待开放"等，从上下文获取实际信息或保留完整URL
  ⚠️ 绝对不要截断答案内容，必须完整输出
- category归类（选课/宿舍/图书馆/奖学金/招生/就业/校园服务等）
- keywords选3-5个关键词
- 无问答内容输出[]
- 只输出JSON，不要其他文字

文本：
{chunk}
"""


def _preprocess_text(text: str) -> str:
    lines = text.split('\n')
    meaningful_lines = []
    for line in lines:
        stripped = line.strip()
        if len(stripped) < 10:
            continue
        if stripped.isdigit():
            continue
        if re.match(r'^[\s\W\d]+$', stripped):
            continue
        meaningful_lines.append(stripped)
    result = '\n'.join(meaningful_lines)
    while '\n\n\n' in result:
        result = result.replace('\n\n\n', '\n\n')
    return result


def _init_ollama_client():
    client = ollama.Client(host=OLLAMA_BASE_URL, timeout=OLLAMA_TIMEOUT)
    return client


def _chunk_text(text: str, chunk_size: int = None, overlap: int = CHUNK_OVERLAP) -> list[str]:
    if chunk_size is None:
        chunk_size = CHUNK_SIZE
    
    processed_text = _preprocess_text(text)
    if len(processed_text) <= chunk_size:
        return [processed_text] if len(processed_text.strip()) > 50 else []

    chunks = []
    start = 0
    while start < len(processed_text):
        end = start + chunk_size
        if end < len(processed_text):
            last_period = processed_text.rfind('。', start, end)
            last_newline = processed_text.rfind('\n', start, end)
            split_pos = max(last_period, last_newline)
            if split_pos > start + chunk_size // 2:
                end = split_pos + 1
        
        chunk = processed_text[start:end].strip()
        if len(chunk) > 50:
            chunks.append(chunk)
        
        start = end - overlap
        if start < 0:
            start = 0
    
    return chunks


def _parse_ai_response(response: str) -> list[dict]:
    json_str = response.strip()

    match = re.search(r'\[\s*\{.*\}\s*\]', json_str, re.DOTALL)
    if match:
        json_str = match.group(0)

    try:
        data = json.loads(json_str)
        if isinstance(data, list):
            return data
    except json.JSONDecodeError:
        pass

    try:
        cleaned = json_str.replace("\n", "").replace("\r", "")
        data = json.loads(cleaned)
        if isinstance(data, list):
            return data
    except json.JSONDecodeError:
        pass

    print(f"  [警告] AI返回的内容无法解析为JSON，原始响应前500字符: {response[:500]}")
    return []


def _validate_entry(entry: dict) -> dict | None:
    question = entry.get("question", "").strip()
    answer = entry.get("answer", "").strip()
    if not question or not answer:
        return None
    if len(question) < 2 or len(answer) < 5:
        return None

    return {
        "question": question,
        "answer": answer,
        "category": entry.get("category", "通用").strip() or "通用",
        "keywords": entry.get("keywords", "").strip(),
    }


def check_ollama_connection() -> bool:
    try:
        client = _init_ollama_client()
        client.list()
        return True
    except Exception as e:
        print(f"[错误] 无法连接到 Ollama: {e}")
        print(f"请确保 Ollama 正在运行，且地址为 {OLLAMA_BASE_URL}")
        return False


def check_model_available() -> bool:
    try:
        client = _init_ollama_client()
        response = client.list()
        if hasattr(response, 'models'):
            model_names = [m.model for m in response.models]
        else:
            model_names = [m.get('model', m.get('name', '')) if isinstance(m, dict) else getattr(m, 'model', getattr(m, 'name', '')) for m in response]
        if any(OLLAMA_MODEL in name for name in model_names):
            return True
        print(f"[信息] 模型 {OLLAMA_MODEL} 未找到，尝试拉取...")
        client.pull(OLLAMA_MODEL)
        return True
    except Exception as e:
        print(f"[错误] 检查模型失败: {e}")
        return False


def _process_single_chunk(client, chunk: str, chunk_index: int, total_chunks: int) -> tuple[int, list[dict]]:
    prompt = SUMMARY_PROMPT.replace("{chunk}", chunk)
    
    try:
        response_text = ""
        for part in client.generate(
            model=OLLAMA_MODEL,
            prompt=prompt,
            options={"temperature": 0.2, "num_predict": 4096},
            stream=False,
        ):
            if isinstance(part, dict):
                response_text = part.get("response", "")
            else:
                response_text = getattr(part, "response", "")
        
        entries = _parse_ai_response(response_text)
        validated_entries = []
        for entry in entries:
            validated = _validate_entry(entry)
            if validated:
                validated_entries.append(validated)
        
        return (chunk_index, validated_entries)
    except Exception as e:
        print(f"    [错误] 片段 {chunk_index + 1} 处理失败: {e}")
        return (chunk_index, [])


def summarize_text(text: str, source: str = "") -> list[dict]:
    global _stop_requested
    
    client = _init_ollama_client()
    chunks = _chunk_text(text)
    
    if not chunks:
        print(f"  文本无有效内容，跳过")
        return []
    
    all_entries: list[dict] = []
    
    original_len = len(text) if text else 0
    processed_len = sum(len(c) for c in chunks)
    
    print(f"  原始文本: {original_len} 字符 → 预处理后: {processed_len} 字符")
    print(f"  切分为 {len(chunks)} 个有效片段")
    print(f"  正在调用 {OLLAMA_MODEL} 进行知识提取（并发模式）...")
    
    max_workers = min(4, len(chunks))
    
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = {
            executor.submit(_process_single_chunk, client, chunk, i, len(chunks)): i
            for i, chunk in enumerate(chunks)
        }
        
        completed = 0
        for future in as_completed(futures):
            if _stop_requested:
                break
            
            chunk_index, entries = future.result()
            completed += 1
            all_entries.extend(entries)
            
            print(f"    [{completed}/{len(chunks)}] 片段 {chunk_index + 1} 完成 → 提取 {len(entries)} 条知识")
    
    seen = set()
    unique_entries = []
    for entry in all_entries:
        key = entry["question"]
        if key not in seen:
            seen.add(key)
            unique_entries.append(entry)

    print(f"  共提取 {len(unique_entries)} 条不重复知识\n")
    return unique_entries


def batch_summarize(sources: list[dict]) -> list[dict]:
    if not sources:
        print("[警告] 没有文本需要总结")
        return []

    all_knowledge: list[dict] = []
    total = len(sources)

    for idx, src in enumerate(sources):
        print(f"\n[进度 {idx + 1}/{total}] 正在处理: {src['source']}")
        entries = summarize_text(src["content"], src["source"])
        all_knowledge.extend(entries)

    seen = set()
    unique = []
    for entry in all_knowledge:
        key = entry["question"]
        if key not in seen:
            seen.add(key)
            unique.append(entry)

    print(f"\n{'=' * 50}")
    print(f"全部处理完成！共提取 {len(unique)} 条不重复知识")
    print(f"{'=' * 50}\n")
    return unique
