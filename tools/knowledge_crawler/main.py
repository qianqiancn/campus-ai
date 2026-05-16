"""
============================================================
校园智能客服 - 知识库爬虫工具 (主入口)
============================================================

功能：
  1. 爬取学校网站网页内容
  2. 解析 Word 文档 (.docx)
  3. 使用 Ollama qwen3-8b 提取/总结 Q&A 知识条目
  4. 导出为 Excel，匹配 knowledge_base 表结构
  5. 断点续传 - 关闭后重新打开可继续上次进度
  6. 随时停止 - 按 Ctrl+C 可随时停止并导出当前结果

使用方式：
  python main.py                    # 交互式菜单
  python main.py --url URL          # 爬取指定网页并用AI提取
  python main.py --docx DIR         # 处理指定目录的Word文档
  python main.py --config           # 使用 config.py 中的配置运行
  python main.py --check            # 仅检测 Ollama 连接和模型状态
  python main.py --resume           # 恢复上次未完成的任务
============================================================
"""
import argparse
import signal
import sys
import threading

from config import OLLAMA_BASE_URL, OLLAMA_MODEL
from web_crawler import crawl_site, crawl_manual_urls, fetch_page, extract_text
from docx_parser import process_docx_files, scan_docx_files
from ai_summarizer import (
    batch_summarize, check_ollama_connection, check_model_available,
    summarize_text, _validate_entry, _parse_ai_response, _init_ollama_client, SUMMARY_PROMPT,
)
from excel_exporter import export_to_excel, preview_entries
import state_manager as sm

BANNER = """
========================================================
         Campus AI - Knowledge Crawler v1.1
     断点续传 | 随时停止 | 实时AI输出
========================================================
"""

_stop_requested = False


def _signal_handler(sig, frame):
    global _stop_requested
    print("\n\n*** 收到停止信号，正在保存当前进度并导出... ***")
    _stop_requested = True


def run_pipeline_with_resume(resume: bool = False):
    from config import TARGET_URLS, MANUAL_URLS, WORD_DOC_DIR

    global _stop_requested
    _stop_requested = False

    signal.signal(signal.SIGINT, _signal_handler)

    processed_urls = []
    knowledge_entries = []

    if resume and sm.has_saved_state():
        saved = sm.load_state()
        processed_urls = saved.get("processed_urls", [])
        knowledge_entries = saved.get("knowledge_entries", [])
        print(f"\n[恢复] 从上次进度继续...")
        print(f"       已处理 {len(processed_urls)} 页，已提取 {len(knowledge_entries)} 条知识\n")
    else:
        sm.clear_state()

    all_sources_to_process = []
    already_processed_set = set(u["source"] for u in processed_urls)

    if TARGET_URLS:
        print("[阶段 1/4] 网页自动爬取...")
        for url in TARGET_URLS:
            if _stop_requested:
                break
            pages = crawl_site(url)
            for p in pages:
                if p["source"] not in already_processed_set:
                    all_sources_to_process.append(p)

    if MANUAL_URLS and not _stop_requested:
        print("[阶段 1b/4] 手动指定页面爬取...")
        manual_pages = crawl_manual_urls(MANUAL_URLS)
        for p in manual_pages:
            if p["source"] not in already_processed_set:
                all_sources_to_process.append(p)

    if WORD_DOC_DIR and not _stop_requested:
        print("[阶段 2/4] Word文档解析...")
        docx_files = scan_docx_files()
        if docx_files:
            print(f"  发现 {len(docx_files)} 个 .docx 文件")
            docx_sources = process_docx_files(docx_files)
            for ds in docx_sources:
                if ds["source"] not in already_processed_set:
                    all_sources_to_process.append(ds)

    if not all_sources_to_process and not knowledge_entries:
        if not (TARGET_URLS or MANUAL_URLS or WORD_DOC_DIR):
            print("[提示] 未配置任何数据源，请先在 config.py 中设置 URL 或文档目录")
        else:
            print("[完成] 所有页面已处理完毕！")
        return knowledge_entries

    total_new = len(all_sources_to_process)
    print(f"\n[阶段 3/4] AI知识提取...")
    print(f"待处理: {total_new} 个文本源，已有: {len(knowledge_entries)} 条知识\n")

    client = _init_ollama_client()
    seen_questions = set(e["question"] for e in knowledge_entries)

    for idx, source in enumerate(all_sources_to_process):
        if _stop_requested:
            print(f"\n[停止] 用户请求停止，当前进度已保存")
            break

        src_url = source["source"]
        text = source["content"]

        print(f"[进度 {idx + 1}/{total_new}] 处理: {src_url}")

        try:
            entries = _summarize_one(client, text, src_url)
            for entry in entries:
                if entry["question"] not in seen_questions:
                    seen_questions.add(entry["question"])
                    knowledge_entries.append(entry)
                    print(f"  >> 新增知识: {entry['question'][:40]}...")
        except Exception as e:
            print(f"  [错误] 处理失败: {e}")
            continue

        processed_urls.append(source)

        sm.save_state(processed_urls, knowledge_entries)

    if _stop_requested or len(knowledge_entries) > 0:
        print(f"\n[阶段 4/4] 导出结果...")
        preview_entries(knowledge_entries)
        export_path = export_to_excel(knowledge_entries)

        if not _stop_requested:
            sm.clear_state()
            print("\n[完成] 全部处理完毕！进度文件已清理。")
        else:
            print(f"\n[已暂停] 已导出当前 {len(knowledge_entries)} 条知识到 Excel")
            print(f"下次启动选择'恢复任务'即可继续处理剩余页面。")

    return knowledge_entries


def _summarize_one(client, text: str, source: str) -> list[dict]:
    from ai_summarizer import _chunk_text, CHUNK_SIZE, CHUNK_OVERLAP, OLLAMA_MODEL

    chunks = _chunk_text(text)
    all_entries = []

    print(f"  文本长度: {len(text)} 字符, 切分为 {len(chunks)} 个片段")

    for i, chunk in enumerate(chunks):
        if _stop_requested:
            break

        print(f"\n  --- 片段 {i + 1}/{len(chunks)} ({len(chunk)} 字符) ---")
        prompt = SUMMARY_PROMPT.replace("{chunk}", chunk)

        try:
            print("  [AI回复]:", end="", flush=True)
            response_text = ""
            for part in client.generate(
                model=OLLAMA_MODEL,
                prompt=prompt,
                options={"temperature": 0.3, "num_predict": 4096},
                stream=True,
            ):
                token = part["response"]
                response_text += token
                print(token, end="", flush=True)
            print()
            entries = _parse_ai_response(response_text)
            for entry in entries:
                validated = _validate_entry(entry)
                if validated:
                    all_entries.append(validated)
            print(f"  >> 片段{i + 1} 提取出 {len(entries)} 条知识")
        except Exception as e:
            print(f"\n  [错误] AI调用失败 (片段 {i + 1}): {e}")
            continue

    print(f"  本页共提取 {len(all_entries)} 条知识")
    return all_entries


def check_environment():
    print("\n[检查] 环境检测...")
    print(f"  1. Ollama 连接({OLLAMA_BASE_URL})... ", end="")
    if check_ollama_connection():
        print("[OK] 正常")
    else:
        print("[X] 失败")
        return False

    print(f"  2. 模型 {OLLAMA_MODEL}... ", end="")
    if check_model_available():
        print("[OK] 就绪")
    else:
        print("[X] 不可用")
        return False

    print("\n环境检测通过，可以开始处理！\n")
    return True


def interactive_menu():
    print(BANNER)
    if not check_environment():
        return

    has_resume = sm.has_saved_state()

    while True:
        print("\n" + "=" * 55)
        print("请选择操作：")
        print("  1. 运行完整流程 (爬取网页 + 解析Word + AI提取)")
        if has_resume:
            print("  2. 恢复上次任务 (断点续传)")
            print("  3. 仅爬取指定网页")
            print("  4. 仅解析Word文档目录")
            print("  5. 手动粘贴文本让AI提取知识")
            print("  6. 清除保存的进度")
            print("  7. 检测环境")
            print("  0. 退出")
        else:
            print("  2. 仅爬取指定网页")
            print("  3. 仅解析Word文档目录")
            print("  4. 手动粘贴文本让AI提取知识")
            print("  5. 检测环境")
            print("  0. 退出")
        print("=" * 55)

        choice = input("请输入选项: ").strip()

        if choice == "1":
            run_pipeline_with_resume(resume=False)

        elif choice == "2" and has_resume:
            sm.print_state_summary()
            confirm = input("\n确认恢复上次任务？(y/n): ").strip().lower()
            if confirm == "y":
                run_pipeline_with_resume(resume=True)

        elif (choice == "2" and not has_resume) or (choice == "3" and has_resume):
            url = input("请输入网址URL: ").strip()
            if url:
                if not url.startswith("http"):
                    url = "https://" + url
                pages = crawl_site(url)
                if pages:
                    entries = batch_summarize(pages)
                    if entries:
                        preview_entries(entries)
                        save = input("\n是否导出到Excel？(y/n): ").strip().lower()
                        if save == "y":
                            export_to_excel(entries)

        elif (choice == "3" and not has_resume) or (choice == "4" and has_resume):
            directory = input("请输入Word文档所在目录路径: ").strip()
            if directory:
                docx_files = scan_docx_files(directory)
                if docx_files:
                    print(f"发现 {len(docx_files)} 个 .docx 文件")
                    sources = process_docx_files(docx_files)
                    if sources:
                        entries = batch_summarize(sources)
                        if entries:
                            preview_entries(entries)
                            save = input("\n是否导出到Excel？(y/n): ").strip().lower()
                            if save == "y":
                                export_to_excel(entries)
                else:
                    print("未发现 .docx 文件")

        elif (choice == "4" and not has_resume) or (choice == "5" and has_resume):
            print("\n请粘贴文本内容（输入 END 结束）：")
            lines = []
            while True:
                line = input()
                if line.strip() == "END":
                    break
                lines.append(line)
            text = "\n".join(lines)
            if text.strip():
                entries = summarize_text(text, "手动输入")
                if entries:
                    preview_entries(entries)
                    save = input("\n是否导出到Excel？(y/n): ").strip().lower()
                    if save == "y":
                        export_to_excel(entries)
            else:
                print("未输入任何文本")

        elif (choice == "5" and not has_resume) or (choice == "6" and has_resume):
            if choice == "6":
                sm.print_state_summary()
                confirm = input("\n确认清除保存的进度？(y/n): ").strip().lower()
                if confirm == "y":
                    sm.clear_state()
                    has_resume = False
                    print("已清除！")
            else:
                check_environment()

        elif (choice == "5" and has_resume) or ((choice == "7" and has_resume)):
            check_environment()

        elif choice == "0":
            print("再见！")
            break

        else:
            print("无效选项，请重新输入")


def main():
    parser = argparse.ArgumentParser(
        description="Campus AI Knowledge Crawler - Use Ollama to extract Q&A"
    )
    parser.add_argument("--url", type=str, help="Crawl specified URL")
    parser.add_argument("--docx", type=str, help="Process Word documents in directory")
    parser.add_argument("--config", action="store_true", help="Run full pipeline with config")
    parser.add_argument("--check", action="store_true", help="Check Ollama connection only")
    parser.add_argument("--resume", action="store_true", help="Resume last interrupted task")

    args = parser.parse_args()

    if args.check:
        print(BANNER)
        check_environment()
        return

    if args.resume:
        print(BANNER)
        if not check_environment():
            return
        if sm.has_saved_state():
            sm.print_state_summary()
            run_pipeline_with_resume(resume=True)
        else:
            print("没有找到可恢复的任务进度")
        return

    if args.url:
        print(BANNER)
        if not check_environment():
            return
        if not args.url.startswith("http"):
            args.url = "https://" + args.url
        pages = crawl_site(args.url)
        if pages:
            entries = batch_summarize(pages)
            if entries:
                preview_entries(entries)
                export_to_excel(entries)
        return

    if args.docx:
        print(BANNER)
        if not check_environment():
            return
        docx_files = scan_docx_files(args.docx)
        if docx_files:
            print(f"Found {len(docx_files)} .docx files")
            sources = process_docx_files(docx_files)
            if sources:
                entries = batch_summarize(sources)
                if entries:
                    preview_entries(entries)
                    export_to_excel(entries)
        else:
            print("No .docx files found")
        return

    if args.config:
        print(BANNER)
        if not check_environment():
            return
        run_pipeline_with_resume(resume=False)
        return

    interactive_menu()


if __name__ == "__main__":
    main()
