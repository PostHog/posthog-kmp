"""Generate the SDK reference Markdown from the KDoc comments with Dokka.

Dokka writes one HTML page per symbol. BeautifulSoup reads each page's signature, parameters and
description, and markdownify turns the description into Markdown. Our code groups PostHog's members by
the sdk-specs capability `docs/capabilities.json` maps their label to, in the PostHog/sdk-specs README's
order, and writes front matter. PostHog members the map lists under "utils" go to SDK utils. A PostHog
member in neither, an unknown slug, or a map entry with no member fails the run.

A declaration with a different signature per platform (expect/actual) shows the common one, then each
platform's that differs.

    pip install -r scripts/requirements.txt
    python3 scripts/generate_markdown.py --sdk-specs DIR

Set GRADLE_ARGS for extra Gradle arguments.
"""

from __future__ import annotations  # `str | None` on Python 3.9, macOS's built-in one

import argparse
import functools
import json
import os
import shlex
import subprocess
from pathlib import Path

from bs4 import BeautifulSoup
from markdownify import markdownify

ROOT = Path(__file__).resolve().parents[1]
MODULE = "posthog-kmp"
PACKAGE = "com.posthog.kmp"
FIRST = ["setup", "capture", "identify"]
# The object users call, as a Dokka folder. Its own type entry would repeat its members.
CLIENT = ("PostHog", "-post-hog")
HIDDEN = {"PostHog"}
# Members every class and enum has, which say nothing about the SDK.
NOISE = set("equals hashCode toString copy valueOf values entries name ordinal Companion".split())
MDX = str.maketrans({"{": "&#123;", "}": "&#125;"})  # MDX 1 reads braces as JSX


def run_dokka() -> None:
    cmd = ["./gradlew", "--no-daemon", *shlex.split(os.environ.get("GRADLE_ARGS", ""))]
    subprocess.run([*cmd, f":{MODULE}:dokkaGenerateHtml"], cwd=ROOT, check=True)


def package_dir() -> Path:
    return ROOT / MODULE / "build/dokka/html" / MODULE / PACKAGE


@functools.cache
def content(page: Path):
    return BeautifulSoup(page.read_text(), "html.parser").select_one("#content")


def title(page: Path) -> str:
    return content(page).select_one("h1").get_text().strip()


def signature(node) -> str:
    text = node.select_one(".symbol").get_text().strip()
    for word in ("expect ", "actual ", "abstract ", "open ", "override "):
        text = text.removeprefix(word)
    return text


def platforms(node) -> list[tuple[str, object]]:
    """Each source set's block, common first. A declaration in one source set has a single block."""
    blocks = [(b.get("data-togglable", "").rsplit("/", 1)[-1].removesuffix("Main"), b) for b in node.select(".sourceset-dependent-content")]
    return sorted(blocks, key=lambda b: b[0] != "common") or [("common", node)]


def lists(node) -> None:
    """Dokka's parameter and exception tables become `name: description` lists."""
    for table in node.select(".table"):
        rows = [r.select(".main-subrow > div") for r in table.select(".table-row")]
        items = "".join(f"<li><code>{k.get_text().strip()}</code>: {(v.p or v).decode_contents()}</li>" for k, v in rows)
        table.replace_with(BeautifulSoup(f"<ul>{items}</ul>", "html.parser"))


def markdown(node, level: int) -> str:
    lists(node)
    for el in node.select(".top-right-position, .copy-popup-wrapper"):  # Dokka's copy button
        el.decompose()
    for br in node.select("pre br"):
        br.replace_with("\n")
    for h in node.find_all(["h2", "h3", "h4"]):
        h.name = f"h{min(level, 6)}"
    for text in node.find_all(string=True):  # braces in prose only; code keeps them
        if not text.find_parent(["code", "pre"]):
            text.replace_with(str(text).translate(MDX))
    md = markdownify(
        str(node), strip=["a"], heading_style="ATX", bullets="-",
        code_language_callback=lambda el: "kotlin" if el.select_one(".lang-kotlin") else "",
    )
    return md.strip().replace("<br>", "<br />")


def declaration(blocks) -> list[str]:
    """The common signature and description, then each platform's that differs."""
    parts, common = [], None
    for platform, block in blocks:
        sig = signature(block)
        if common is not None and sig == common:
            continue
        for s in block.select(".symbol"):
            s.decompose()
        if common is not None:
            parts.append(f"On {platform}:")
        parts += [f"```kotlin\n{sig}\n```", markdown(block, 4)]
        common = sig if common is None else common
    return parts


def entry(page: Path, label: str) -> str:
    """A member's page as one H3 entry."""
    parts = [f"### `{label}`", *declaration(platforms(content(page)))]
    return "\n\n".join(p for p in parts if p)


def type_entry(page: Path, name: str) -> str:
    """A type's declaration, description and the members it declares itself, each with its brief."""
    node = content(page)
    parts = [f"### `{name}`", *declaration(platforms(node.select_one(".cover")))]
    for kind, heading in (("PROPERTY", "Properties"), ("FUNCTION", "Functions"), ("ENTRY", "Entries")):
        rows = {}
        for row in node.select(f"[data-togglable={kind}] .table-row"):
            link = row.select_one("a")
            # A member a supertype declares has no page here, only an anchor on this one.
            if not link or link["href"].startswith("index.html") or link.get_text().replace("​", "") in NOISE:
                continue
            brief = row.select_one(".brief")
            sig = signature(row) if row.select_one(".symbol") else link.get_text()
            rows.setdefault(sig, f"- `{sig}`" + (f": {markdown(brief, 5)}" if brief else ""))
        if rows:
            parts.append(f"#### {heading}\n\n" + "\n".join(rows.values()))
    return "\n\n".join(p for p in parts if p)


def capabilities(checkout: str) -> dict[str, str]:
    """Each sdk-specs capability's slug and title in sentence case, in the README's order."""
    acronyms = {"id": "ID", "ai": "AI", "http": "HTTP", "mcp": "MCP"}
    titles = {}
    for line in (Path(checkout) / "README.md").read_text().split("\n"):
        cells = line.split("|")
        if len(cells) > 2 and "openspec/specs/" in cells[2]:
            slug = cells[2].split("openspec/specs/")[1].split("/")[0]
            titles[slug] = " ".join(acronyms.get(w.lower(), w.lower()) for w in cells[1].split())
    order = [*FIRST, *(s for s in titles if s not in FIRST)]
    return {s: titles[s][0].upper() + titles[s][1:] for s in order if s in titles}


def reference(caps: dict[str, str]) -> list[str]:
    mapped = json.loads((ROOT / "docs/capabilities.json").read_text())
    listed = set(mapped.pop("utils"))
    if unknown := sorted(k for k, s in mapped.items() if s not in caps):
        raise SystemExit(f"docs/capabilities.json: unknown capability for {unknown}")
    tagged: dict[str, list[str]] = {}
    utils, labels = [], set()
    cls, path = CLIENT
    for page in sorted(p for p in (package_dir() / path).glob("*.html") if p.name != "index.html"):
        label = f"{cls}.{title(page)}"
        labels.add(label)
        found = mapped.get(label)
        if not found and label not in listed:
            raise SystemExit(f"{label} is not in docs/capabilities.json, map it or list it in utils")
        (tagged.setdefault(found, []) if found else utils).append(entry(page, label))
    for page in sorted(package_dir().glob("*.html")):
        if page.name != "index.html":  # Top-level functions, like the PostHogContext() factory.
            utils.append(entry(page, title(page)))
    if stale := sorted(set(mapped) - labels):
        raise SystemExit(f"docs/capabilities.json names no member: {stale}")
    out = []
    for slug, heading in caps.items():
        if slug in tagged:
            out += [f"## {heading}", *tagged[slug]]
    out += ["## SDK utils", *utils]
    pages = {title(p): p for p in package_dir().glob("-*/index.html")}
    types = [type_entry(page, name) for name, page in sorted(pages.items()) if name not in HIDDEN]
    return out + ["## Types", *types]


def anchor(heading: str) -> str:
    """The github-slugger slug the docs site gives a heading."""
    return "".join(c for c in heading.lower() if c.isalnum() or c in "-_ ").replace(" ", "-")


def front_matter(title: str, sections: list[str]) -> str:
    """The docs site's front matter. Its ToC lists the page's H2 sections only."""
    toc = []
    for heading in [s[3:] for s in sections if s.startswith("## ")]:
        toc.append(f'  - value: "{heading}"\n    url: {anchor(heading)}\n    depth: 0')
    head = [f'title: "{title}"', "sidebar: Docs", "showTitle: true"]
    head += ["contentMaxWidthClass: max-w-full", "platformLogo: kmp", "tableOfContents:", *toc]
    return "\n".join(["---", *head, "---"])


def nest(markdown: str) -> str:
    """Moves each heading down one level and drops the H1. Code stays as written."""
    out, fence = [], False
    for line in markdown.strip().split("\n"):
        fence ^= line.strip().startswith("```")
        if fence or not line.startswith("#"):
            out.append(line)
        elif not line.startswith("# "):
            out.append("#" + line)
    return "\n".join(out)


def migration_page(guide: str) -> list[str]:
    """The migration page. The guide's H1 becomes the page title, so the page shows one title."""
    title = "PostHog Kotlin Multiplatform SDK migration guides"
    if guide.startswith("# "):
        heading, _, rest = guide.partition("\n")
        title, guide = heading[2:].strip(), rest.strip()
    return [f"---\ntitle: {json.dumps(title)}\nsidebar: Docs\nshowTitle: true\n---", guide]


def write(name: str, sections: list[str]) -> None:
    (ROOT / "references" / name).write_text("\n\n".join(sections) + "\n")
    print(f"wrote references/{name}")


def main() -> None:
    args = argparse.ArgumentParser(description="Generate the SDK reference Markdown.")
    args.add_argument("--sdk-specs", required=True, help="PostHog/sdk-specs checkout")
    opts = args.parse_args()
    run_dokka()
    props = (ROOT / "version.properties").read_text().split("\n")
    major = int(next(l for l in props if l.startswith("VERSION_MAJOR=")).split("=")[1])
    body = ["## Getting started", nest((ROOT / "docs/getting-started.md").read_text()), *reference(capabilities(opts.sdk_specs))]
    (ROOT / "references").mkdir(exist_ok=True)
    write(f"posthog-kmp-{major}.md", [front_matter(f"PostHog Kotlin Multiplatform SDK {major}.x", body), *body])
    write("posthog-kmp-reference.md", [front_matter("PostHog Kotlin Multiplatform SDK", body), *body])
    if (guide := ROOT / "MIGRATION.md").exists():
        write("posthog-kmp-migration.md", migration_page(guide.read_text().strip()))


if __name__ == "__main__":
    main()
