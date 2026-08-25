"""The hard boundary from ARCHITECTURE.md, made a guarantee: no Java source
outside client/ may import a client-only type. Exits non-zero on a leak.

This is the dedicated-server crash class caught at commit time instead of at a
server's class-load. The pattern list is conservative and grows as real client
packages get used; a miss here is a bug report against THIS file.
"""
import os, re, sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOTS = [os.path.join(REPO, "src/main/java"), os.path.join(REPO, "core/src")]
CLIENT_MARKER = f"{os.sep}client{os.sep}"
BANNED = re.compile(
    r"^import\s+(net\.minecraft\.client\.|com\.mojang\.blaze3d\."
    r"|net\.neoforged\.neoforge\.client\.|net\.neoforged\.api\.distmarker\.OnlyIn)"
)

fails = []
for root in ROOTS:
    for dirpath, _, files in os.walk(root):
        for f in files:
            if not f.endswith(".java"):
                continue
            p = os.path.join(dirpath, f)
            if CLIENT_MARKER in p:
                continue
            for i, line in enumerate(open(p), 1):
                if BANNED.match(line.strip()):
                    fails.append(f"{os.path.relpath(p, REPO)}:{i}: {line.strip()}")

if fails:
    print(f"FAIL: {len(fails)} client-only import(s) outside client/:")
    for f in fails:
        print("  -", f)
    sys.exit(1)
print("OK: no client-only imports outside client/")
