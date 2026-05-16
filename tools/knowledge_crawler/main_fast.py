"""
============================================================
校园智能客服 - 知识库爬虫工具 (主入口) - 快速版
============================================================

优化策略：
  1. 只处理 MANUAL_URLS 中的核心页面，不自动跟踪子链接
  2. 并发调用 Ollama（多线程）
  3. 减少每个页面的 AI 输出量（num_predict 限制）
  4. 短文本跳过 AI 调用
  5. 断点续传 + 随时停止

使用方式：
  python main_fast.py              # 快速模式（推荐）
  python main_fast.py --resume     # 恢复上次进度
  python main_fast.py --check      # 检测环境
============================================================
"""
import argparse
import signal
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

from config import OLLAMA_BASE_URL, OLLAMA_MODEL, OLLAMA_TIMEOUT
from web_crawler import fetch_page, extract_text_with_links, extract_text
from ai_summarizer import (
    check_ollama_connection, check_model_available,
    _validate_entry, _parse_ai_response, _init_ollama_client, SUMMARY_PROMPT,
    _chunk_text,
)
from excel_exporter import export_to_excel, preview_entries
import state_manager as sm

BANNER = """
========================================================
       Campus AI - Fast Knowledge Crawler v2.0
       Concurrent | Fast | Resume Support
========================================================
"""

_stop_requested = False


def _signal_handler(sig, frame):
    global _stop_requested
    print("\n\n*** Stop signal received, saving progress... ***")
    _stop_requested = True


def _process_one_page(client, source: dict) -> list[dict]:
    text = source["content"]
    src_url = source["source"]

    chunks = _chunk_text(text)
    if not chunks:
        return []

    all_entries = []

    for chunk in chunks:
        if _stop_requested:
            break

        prompt = SUMMARY_PROMPT.replace("{chunk}", chunk)

        try:
            response = client.generate(
                model=OLLAMA_MODEL,
                prompt=prompt,
                options={"temperature": 0.3, "num_predict": 2048},
                stream=False,
            )

            response_text = ""
            if isinstance(response, dict):
                response_text = response.get("response", "")
            else:
                response_text = getattr(response, "response", "")

            entries = _parse_ai_response(response_text)
            for entry in entries:
                validated = _validate_entry(entry)
                if validated:
                    all_entries.append(validated)
        except Exception as e:
            print(f"    [ERR] {src_url[:50]}: {e}")
            continue

    return all_entries


def _crawl_manual_fast(urls: list[str]) -> list[dict]:
    results = []
    total = len(urls)
    print(f"\nCrawling {total} pages (no sub-link following)...\n")

    for i, url in enumerate(urls):
        if _stop_requested:
            break
        print(f"  [{i+1}/{total}] {url}")
        html = fetch_page(url)
        if not html:
            continue

        text_with_links = extract_text_with_links(html, url)
        plain_text = extract_text(html, url)
        combined = f"{text_with_links}\n\n---\n{plain_text}"

        if combined and len(combined.strip()) > 50:
            results.append({"source": url, "content": combined})

    print(f"\nCrawled {len(results)} valid pages\n")
    return results


def run_fast_pipeline(resume: bool = False, workers: int = 3):
    from config import MANUAL_URLS

    global _stop_requested
    _stop_requested = False
    signal.signal(signal.SIGINT, _signal_handler)

    processed_urls = []
    knowledge_entries = []

    if resume and sm.has_saved_state():
        saved = sm.load_state()
        processed_urls = saved.get("processed_urls", [])
        knowledge_entries = saved.get("knowledge_entries", [])
        print(f"\n[Resume] {len(processed_urls)} pages done, {len(knowledge_entries)} entries\n")
    else:
        sm.clear_state()

    already_processed = set(u["source"] for u in processed_urls)

    print("[Step 1/3] Crawling pages...")
    all_sources = _crawl_manual_fast(MANUAL_URLS)

    pending = [s for s in all_sources if s["source"] not in already_processed]
    total = len(pending)
    print(f"\n[Step 2/3] AI extraction ({total} pages, {workers} workers)...\n")

    client = _init_ollama_client()
    seen_questions = set(e["question"] for e in knowledge_entries)
    completed = 0

    with ThreadPoolExecutor(max_workers=workers) as executor:
        future_map = {}
        for source in pending:
            if _stop_requested:
                break
            future = executor.submit(_process_one_page, client, source)
            future_map[future] = source

        for future in as_completed(future_map):
            if _stop_requested:
                break

            source = future_map[future]
            completed += 1

            try:
                entries = future.result()
                new_count = 0
                for entry in entries:
                    if entry["question"] not in seen_questions:
                        seen_questions.add(entry["question"])
                        knowledge_entries.append(entry)
                        new_count += 1

                print(f"  [{completed}/{total}] {source['source'][:60]} -> {new_count} new entries (total: {len(knowledge_entries)})")
            except Exception as e:
                print(f"  [{completed}/{total}] FAILED {source['source'][:60]}: {e}")

            processed_urls.append(source)
            if completed % 3 == 0:
                sm.save_state(processed_urls, knowledge_entries)

    sm.save_state(processed_urls, knowledge_entries)

    print(f"\n[Step 3/3] Exporting {len(knowledge_entries)} entries...")
    if knowledge_entries:
        preview_entries(knowledge_entries)
        export_path = export_to_excel(knowledge_entries)

        if not _stop_requested:
            sm.clear_state()
            print(f"\n[DONE] All finished! {len(knowledge_entries)} entries exported.")
        else:
            print(f"\n[PAUSED] {len(knowledge_entries)} entries saved. Run --resume to continue.")

    return knowledge_entries


def check_environment():
    print("\n[Check] Environment...")
    print(f"  1. Ollama ({OLLAMA_BASE_URL})... ", end="")
    if check_ollama_connection():
        print("[OK]")
    else:
        print("[FAIL]")
        return False

    print(f"  2. Model {OLLAMA_MODEL}... ", end="")
    if check_model_available():
        print("[OK]")
    else:
        print("[FAIL]")
        return False

    print("\nEnvironment OK!\n")
    return True


def main():
    parser = argparse.ArgumentParser(description="Campus AI Fast Knowledge Crawler")
    parser.add_argument("--resume", action="store_true", help="Resume last task")
    parser.add_argument("--check", action="store_true", help="Check environment only")
    parser.add_argument("--workers", type=int, default=3, help="Concurrent workers (default: 3)")
    args = parser.parse_args()

    print(BANNER)

    if args.check:
        check_environment()
        return

    if not check_environment():
        return

    run_fast_pipeline(resume=args.resume, workers=args.workers)


if __name__ == "__main__":
    main()