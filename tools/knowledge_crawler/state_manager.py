import json
import os
from datetime import datetime

STATE_FILE = "knowledge_crawler_state.json"


def _get_state_path():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    return os.path.join(script_dir, STATE_FILE)


def save_state(processed_urls: list[dict], knowledge_entries: list[dict], pending_urls: list[str] = None):
    state = {
        "last_updated": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "processed_count": len(processed_urls),
        "knowledge_count": len(knowledge_entries),
        "processed_urls": processed_urls,
        "knowledge_entries": knowledge_entries,
        "pending_urls": pending_urls or [],
    }
    path = _get_state_path()
    with open(path, "w", encoding="utf-8") as f:
        json.dump(state, f, ensure_ascii=False, indent=2)


def load_state() -> dict | None:
    path = _get_state_path()
    if not os.path.exists(path):
        return None
    try:
        with open(path, "r", encoding="utf-8") as f:
            state = json.load(f)
        return state
    except Exception:
        return None


def has_saved_state():
    return load_state() is not None


def clear_state():
    path = _get_state_path()
    if os.path.exists(path):
        os.remove(path)


def print_state_summary():
    state = load_state()
    if not state:
        print("  没有找到已保存的进度")
        return
    print(f"  上次保存时间: {state['last_updated']}")
    print(f"  已处理页面: {state['processed_count']} 个")
    print(f"  已提取知识: {state['knowledge_count']} 条")
    if state.get("pending_urls"):
        print(f"  待处理页面: {len(state['pending_urls'])} 个")
