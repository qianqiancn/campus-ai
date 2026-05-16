"""
============================================
校园智能客服 - 知识库爬虫工具 配置文件
============================================
修改此文件中的配置项以适应你的需求
"""

TARGET_URLS = [
    "https://www.sctbc.net/",
]

MANUAL_URLS = [

    "https://www.sctbc.net/xygk/xyjj.htm",
    "https://www.sctbc.net/xygk/xrld.htm",
    "https://www.sctbc.net/xygk/xxxx.htm",
    "https://www.sctbc.net/xygk/xxjs.htm",
    "https://www.sctbc.net/xygk/xxxf.htm",
    "https://www.sctbc.net/xygk/xxjf.htm",
    "https://www.sctbc.net/xygk/xxxf1.htm",
    "https://www.sctbc.net/xygk/jzjs.htm",
    "https://www.sctbc.net/xygk/zlwh.htm",
    "https://www.sctbc.net/xygk/xxry.htm",

    "https://www.sctbc.net/szdw/szdwgk.htm",
    "https://www.sctbc.net/szdw/gcrc.htm",
    "https://www.sctbc.net/szdw/yxjs.htm",
    "https://www.sctbc.net/szdw/yxjs1.htm",

    "https://www.sctbc.net/zzjg1/jxjg.htm",
    "https://www.sctbc.net/zzjg1/glfwjg.htm",
    "https://www.sctbc.net/zzjg1/glfwjg/lddt/7.htm",
    "https://www.sctbc.net/zzjg1/glfwjg/lddt/6.htm",
    "https://www.sctbc.net/zzjg1/glfwjg/lddt/9.htm",
    "https://www.sctbc.net/zzjg1/glfwjg/tsqbg/zyjs.htm",

    "https://www.sctbc.net/zsjy1.htm",

    "https://www.sctbc.net/dwjlhzc/index.htm",

    "https://www.sctbc.net/xwgg/lddt.htm",
    "https://www.sctbc.net/xwgg/tzgg.htm",
    "https://www.sctbc.net/xmts/spzq.htm",
    "https://www.sctbc.net/xygk/xyly.htm",

    "https://zsjy.sctbc.edu.cn/info/1149/3223.htm",
    "https://zsjy.sctbc.edu.cn/info/1149/3233.htm",
    "https://zsjy.sctbc.edu.cn/info/1149/3213.htm",
    "https://zsjy.sctbc.edu.cn/",
    "https://zsjy.sctbc.edu.cn/zsxx.htm",
    "https://zsjy.sctbc.edu.cn/zsdt.htm",
]

WORD_DOC_DIR = r""

OLLAMA_BASE_URL = "http://localhost:11434"
OLLAMA_MODEL = "qwen3:8b"
OLLAMA_TIMEOUT = 120

CHUNK_SIZE = 6000
CHUNK_OVERLAP = 500

MAX_CRAWL_PAGES = 100

OUTPUT_EXCEL_PATH = r"knowledge_output.xlsx"
OUTPUT_EXCEL_SHEET = "知识库"

EXCEL_COLUMNS = ["question", "answer", "category", "keywords", "created_at"]
EXCEL_HEADERS = ["问题", "答案", "分类", "关键词", "创建时间"]

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

REQUEST_TIMEOUT = 30
REQUEST_DELAY = 1.0
