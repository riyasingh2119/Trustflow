#!/usr/bin/env python3
"""Re-embed lambda/notify.py into cloudformation/alert-stack*.yaml (the ZipFile block)."""
import pathlib
import re
import sys

root = pathlib.Path(__file__).resolve().parent.parent
code = (root / "lambda" / "notify.py").read_text().rstrip("\n")
block = "\n".join(("          " + l if l else "") for l in code.splitlines())
pattern = re.compile(r"(        ZipFile: \|\n)(?:          .*\n|\n)+")
for name in ("alert-stack.yaml", "alert-stack-v2.yaml", "alert-stack-v3-broken.yaml"):
    path = root / "cloudformation" / name
    text = path.read_text()
    new, n = pattern.subn(lambda m: m.group(1) + block + "\n", text, count=1)
    if n != 1:
        sys.exit(f"ZipFile block not found in {name}")
    path.write_text(new)
    print("updated", name)
