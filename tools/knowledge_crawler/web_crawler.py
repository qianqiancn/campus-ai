"""
网页爬取模块 - 从URL抓取网页内容并提取正文文本
"""
import time
from urllib.parse import urljoin, urlparse

import requests
from bs4 import BeautifulSoup

from config import (
    TARGET_URLS, MANUAL_URLS, USER_AGENT, REQUEST_TIMEOUT, REQUEST_DELAY, CHUNK_SIZE, MAX_CRAWL_PAGES
)

SKIP_TAGS = {"script", "style", "nav", "footer", "header", "aside", "noscript", "iframe"}
SKIP_CLASS_KEYWORDS = {"nav", "footer", "sidebar", "menu", "comment", "ad", "breadcrumb"}

SKIP_EXTENSIONS = {
    ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".svg", ".webp", ".ico", ".icon",
    ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
    ".zip", ".rar", ".7z", ".tar", ".gz",
    ".mp3", ".mp4", ".avi", ".mov", ".wav", ".flv", ".mkv",
    ".exe", ".msi", ".dmg", ".apk",
    ".css", ".js", ".json", ".xml", ".rss", ".atom"
}

SKIP_KEYWORDS = {
    "login", "logout", "register", "signup", "signin",
    "verify", "captcha", "code", "validate",
    "admin", "manage", "backend",
    "print", "download", "export",
    "search", "query", "result"
}

ALLOWED_DOMAINS = {
    "sctbc.net",
    "zsjy.sctbc.edu.cn",
}


def is_allowed_domain(url: str) -> bool:
    parsed = urlparse(url)
    domain = parsed.netloc.lower()
    
    if domain in ALLOWED_DOMAINS:
        return True
    
    for allowed in ALLOWED_DOMAINS:
        if domain.endswith("." + allowed) or domain == allowed:
            return True
    
    return False


def should_skip_url(url: str) -> bool:
    parsed = urlparse(url)
    path = parsed.path.lower()
    for ext in SKIP_EXTENSIONS:
        if path.endswith(ext):
            return True
    if "?" in url:
        base_path = path.split("?")[0]
        for ext in SKIP_EXTENSIONS:
            if base_path.endswith(ext):
                return True
    
    url_lower = url.lower()
    for keyword in SKIP_KEYWORDS:
        if keyword in url_lower:
            return True
    
    return False


def _is_skip_tag(tag):
    if tag.name in SKIP_TAGS:
        return True
    classes = tag.get("class", [])
    for cls in classes:
        for kw in SKIP_CLASS_KEYWORDS:
            if kw in cls.lower():
                return True
    return False


def fetch_page(url: str) -> str | None:
    headers = {"User-Agent": USER_AGENT}
    try:
        resp = requests.get(url, headers=headers, timeout=REQUEST_TIMEOUT)
        resp.raise_for_status()
        resp.encoding = resp.apparent_encoding or "utf-8"
        return resp.text
    except requests.RequestException as e:
        print(f"  [警告] 无法访问 {url}: {e}")
        return None


def extract_text(html: str, base_url: str = "") -> str:
    soup = BeautifulSoup(html, "lxml")
    for tag in soup.find_all(_is_skip_tag):
        tag.decompose()

    body = soup.find("body")
    if not body:
        body = soup

    lines = []
    for el in body.descendants:
        if isinstance(el, str):
            text = el.strip()
            if text:
                lines.append(text)
        elif el.name in ("p", "div", "section", "article", "li", "td", "th", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre"):
            pass
    full_text = "\n".join(lines)
    while "\n\n\n" in full_text:
        full_text = full_text.replace("\n\n\n", "\n\n")
    return full_text


def extract_text_with_links(html: str, base_url: str = "") -> str:
    soup = BeautifulSoup(html, "lxml")
    for tag in soup.find_all(_is_skip_tag):
        tag.decompose()

    body = soup.find("body")
    if not body:
        body = soup

    lines = []
    for el in body.descendants:
        if isinstance(el, str):
            text = el.strip()
            if text:
                parent = el.parent
                if parent and parent.name == "a" and parent.get("href"):
                    href = parent["href"]
                    absolute_url = urljoin(base_url, href)
                    link_text = f"{text} [链接: {absolute_url}]"
                    lines.append(link_text)
                else:
                    lines.append(text)
        elif el.name in ("p", "div", "section", "article", "li", "td", "th", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre"):
            pass
    full_text = "\n".join(lines)
    while "\n\n\n" in full_text:
        full_text = full_text.replace("\n\n\n", "\n\n")
    return full_text


ACTION_KEYWORDS = {
    "点击", "跳转", "查看", "详情", "下载", "更多",
    "进入", "访问", "获取", "申请", "填报",
    "开放", "公布", "查询", "搜索",
    "click", "jump", "view", "detail", "download", "more"
}


def extract_action_links(html: str, base_url: str) -> list[dict]:
    soup = BeautifulSoup(html, "lxml")
    action_links = []
    
    for a in soup.find_all("a", href=True):
        link_text = a.get_text(strip=True)
        href = a["href"]
        
        if not link_text or len(link_text) < 2:
            continue
        
        text_lower = link_text.lower()
        is_action = any(kw in text_lower for kw in ACTION_KEYWORDS)
        
        has_parentheses = any(char in link_text for char in ("（", ")", "(", ")"))
        is_short = len(link_text) <= 20
        
        if (is_action or has_parentheses or is_short) and link_text not in ["首页", "返回", "关闭", "取消"]:
            absolute_url = urljoin(base_url, href)
            parsed = urlparse(absolute_url)
            
            if parsed.scheme not in ("http", "https"):
                continue
            if not is_allowed_domain(absolute_url):
                continue
            if should_skip_url(absolute_url):
                continue
            
            action_links.append({
                "text": link_text,
                "url": absolute_url,
                "source": base_url
            })
    
    seen_urls = set()
    unique_links = []
    for link in action_links:
        if link["url"] not in seen_urls:
            seen_urls.add(link["url"])
            unique_links.append(link)
    
    return unique_links


def follow_action_links(action_links: list[dict], max_depth: int = 1) -> list[dict]:
    results = []
    
    if max_depth <= 0:
        return results
    
    print(f"\n  发现 {len(action_links)} 个操作链接，正在跟踪...")
    
    for i, link_info in enumerate(action_links):
        url = link_info["url"]
        link_text = link_info["text"]
        
        print(f"  [{i + 1}/{len(action_links)}] 跟踪: {link_text}")
        print(f"      URL: {url}")
        
        html = fetch_page(url)
        if not html:
            print(f"      [跳过] 无法访问")
            continue
        
        text = extract_text(html, url)
        if text and len(text.strip()) > 50:
            enhanced_content = f"[来源: {link_text}]\n{text}"
            results.append({
                "source": url,
                "content": enhanced_content,
                "parent_source": link_info["source"],
                "link_text": link_text
            })
            print(f"      [OK] 获取内容: {len(text)} 字符")
        else:
            print(f"      [跳过] 内容过短")
        
        time.sleep(REQUEST_DELAY * 0.5)
    
    print(f"\n  跟踪完成，新增 {len(results)} 个页面\n")
    return results


def extract_links(html: str, base_url: str) -> list[str]:
    soup = BeautifulSoup(html, "lxml")
    links = []
    for a in soup.find_all("a", href=True):
        href = a["href"]
        absolute = urljoin(base_url, href)
        parsed = urlparse(absolute)
        if parsed.scheme not in ("http", "https"):
            continue
        if not is_allowed_domain(absolute):
            continue
        if should_skip_url(absolute):
            continue
        if absolute not in links:
            links.append(absolute)
    return links


def crawl_site(start_url: str, max_pages: int = None) -> list[dict]:
    if max_pages is None:
        max_pages = MAX_CRAWL_PAGES
    visited: set[str] = set()
    queue: list[str] = [start_url]
    results: list[dict] = []

    print(f"\n开始爬取: {start_url}")
    print(f"最多爬取 {max_pages} 个页面\n")

    while queue and len(visited) < max_pages:
        url = queue.pop(0)
        if url in visited:
            continue
        if should_skip_url(url):
            print(f"  [跳过] 非文本资源: {url}")
            visited.add(url)
            continue
        visited.add(url)
        print(f"  [{len(visited)}/{max_pages}] 正在爬取: {url}")

        html = fetch_page(url)
        if not html:
            continue

        text = extract_text(html, url)
        if text and len(text.strip()) > 100:
            results.append({"source": url, "content": text})

        if len(visited) < max_pages:
            links = extract_links(html, url)
            for link in links:
                if link not in visited and link not in queue:
                    queue.append(link)

        time.sleep(REQUEST_DELAY)

    print(f"\n爬取完成，共获取 {len(results)} 个有效页面\n")
    return results


def crawl_manual_urls(urls: list[str], follow_links: bool = True) -> list[dict]:
    results = []
    total = len(urls)
    print(f"\n开始手动爬取 {total} 个指定页面...\n")

    for i, url in enumerate(urls):
        print(f"  [{i + 1}/{total}] 正在爬取: {url}")
        if should_skip_url(url):
            print(f"  [跳过] 非文本资源: {url}")
            continue
        html = fetch_page(url)
        if not html:
            continue
        
        text_with_links = extract_text_with_links(html, url)
        plain_text = extract_text(html, url)
        
        combined_text = f"{text_with_links}\n\n--- 页面正文内容 ---\n{plain_text}"
        
        if combined_text and len(combined_text.strip()) > 50:
            results.append({"source": url, "content": combined_text})
        
        if follow_links and html:
            action_links = extract_action_links(html, url)
            if action_links:
                sub_results = follow_action_links(action_links, max_depth=1)
                results.extend(sub_results)
        
        time.sleep(REQUEST_DELAY)

    print(f"\n手动爬取完成，共获取 {len(results)} 个有效页面\n")
    return results
