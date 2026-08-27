"""Every link in `docs/` goes somewhere, including the part after the `#`.

The old version of this check lived inline in `tools/check_all.sh` and validated only
the FILE half of a link. It was green. Meanwhile `docs/LESSONS.md` had accumulated five
cross-references pointing at headings that no longer existed:

    [#15](#15-a-check-that-cannot-fail-is-a-comment)

...whose heading had long since been rewritten to "Assert the setup, or the test proves
whatever absence implies". The link still rendered, still looked like a citation, and
still landed the reader at the top of a two-thousand-line file with no idea which lesson
had been meant. Nothing failed, because nothing was looking.

A fragment is a claim -- *this section exists and says this* -- and an unchecked claim
rots. That is the whole doctrine of this repository applied to its own footnotes.

WHAT COUNTS AS AN ANCHOR is GitHub's rule, not a guess: the heading text, lowercased,
with backticks and punctuation dropped, spaces turned to hyphens, and a `-1` suffix on
each duplicate. Implemented below and verified against the links that were already
correct -- `#4-measure-the-process-you-mean-not-the-pipelines-tail` resolves, which
exercises the apostrophe and comma cases, and `#11-a-feature-that-cannot-be-placed-...`
exercises backtick-and-slash removal.

    python3 tools/doclink_check.py
"""
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(REPO, "docs")

LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)\)")
HEADING = re.compile(r"^#{1,6}\s+(.+?)\s*$", re.M)


def slug(text):
    """GitHub's heading-to-anchor rule."""
    text = re.sub(r"`([^`]*)`", r"\1", text)          # code spans contribute their text
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)  # links contribute their label
    text = re.sub(r"[*_]", "", text)                   # emphasis markers vanish
    text = text.lower()
    text = re.sub(r"[^\w\s-]", "", text)               # all other punctuation vanishes
    return re.sub(r"\s+", "-", text.strip())


def anchors(path):
    """Every fragment `path` offers, duplicates suffixed the way GitHub suffixes them."""
    seen = {}
    out = set()
    for m in HEADING.finditer(open(path).read()):
        s = slug(m.group(1))
        n = seen.get(s, 0)
        seen[s] = n + 1
        out.add(s if n == 0 else f"{s}-{n}")
    return out


cache = {}
fails = []
links = 0
fragments = 0

for root, _, files in os.walk(DOCS):
    for f in sorted(files):
        if not f.endswith(".md"):
            continue
        src = os.path.join(root, f)
        rel = os.path.relpath(src, REPO)
        for m in LINK.finditer(open(src).read()):
            target = m.group(1)
            if "://" in target or target.startswith("mailto:"):
                continue
            links += 1
            filepart, _, frag = target.partition("#")
            dest = os.path.normpath(os.path.join(root, filepart)) if filepart else src
            if not os.path.exists(dest):
                fails.append(f"{rel}: [{target}] -- no such file")
                continue
            if not frag:
                continue
            fragments += 1
            if dest not in cache:
                cache[dest] = anchors(dest)
            if frag not in cache[dest]:
                where = "in itself" if dest == src else f"in {os.path.relpath(dest, REPO)}"
                # A renamed heading is the usual cause, so offer the near misses: the
                # reader almost always wants the section that got renamed, and finding it
                # by hand in a file this size is the annoying part.
                head = frag.split("-")[0]
                near = sorted(a for a in cache[dest] if a.startswith(head + "-"))
                hint = f"  Did you mean: {near[0]}" if near else ""
                fails.append(f"{rel}: [{target}] -- no heading {where} anchors to "
                             f"'{frag}'.{hint}")

if fails:
    print()
    for f in fails:
        print("  - " + f)
    print(f"\nFAIL: {len(fails)} dead link(s). A cross-reference is a claim that a "
          f"section exists and says a particular thing; when the heading is rewritten "
          f"the link still renders and stops meaning anything.")
    sys.exit(1)

print(f"\nOK: {links} link(s) in docs/, {fragments} of them into a heading, all resolve")
