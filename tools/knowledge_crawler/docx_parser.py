"""
Word文档解析模块 - 解析 .docx 文件并提取文本内容
"""
import os

from docx import Document

from config import WORD_DOC_DIR, CHUNK_SIZE


def parse_docx(file_path: str) -> str | None:
    try:
        doc = Document(file_path)
        paragraphs = []
        for para in doc.paragraphs:
            text = para.text.strip()
            if text:
                paragraphs.append(text)
        content = "\n".join(paragraphs)
        return content if content.strip() else None
    except Exception as e:
        print(f"  [警告] 无法解析文件 {file_path}: {e}")
        return None


def scan_docx_files(directory: str = "") -> list[str]:
    if directory:
        search_dir = directory
    else:
        search_dir = WORD_DOC_DIR

    if not search_dir or not os.path.isdir(search_dir):
        print(f"  [警告] 目录不存在或未设置: {search_dir}")
        return []

    docx_files = []
    for root, dirs, files in os.walk(search_dir):
        for f in files:
            if f.lower().endswith(".docx") and not f.startswith("~$"):
                docx_files.append(os.path.join(root, f))
    return docx_files


def process_docx_files(file_paths: list[str]) -> list[dict]:
    results = []
    for fp in file_paths:
        print(f"  正在解析: {os.path.basename(fp)}")
        content = parse_docx(fp)
        if content and len(content.strip()) > 50:
            results.append({"source": fp, "content": content})
        else:
            print(f"  [警告] 文件内容为空或过短: {fp}")
    return results
