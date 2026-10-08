#!/usr/bin/env python3
# Copyright 2026, Drew Heavner and the Campfire project contributors
# SPDX-License-Identifier: GPL-3.0-only
"""Write a "Stack" section into every open PR of a stack.

A stack is a tree of open PRs: each PR's base branch is its parent PR's head branch, down to the
root PR, whose base is a branch no open PR owns (usually main). Every PR in the tree gets the same
nested list, with "[This PR]" marking itself, between hidden markers so reruns replace it in place.

    stack_section.py [PR]            # print each PR's section (dry run); PR defaults to this branch's
    stack_section.py [PR] --apply    # write the sections that changed

Run it again whenever the stack changes: a PR opened, merged, closed, or retargeted. A PR left
alone in its stack (the rest merged) has its section removed.
"""

import argparse
import json
import re
import subprocess
import sys
import tempfile

START = "<!-- pr-stack:start -->"
END = "<!-- pr-stack:end -->"
SECTION = re.compile(re.escape(START) + r".*?" + re.escape(END) + r"\n*", re.S)


def gh(*args):
    result = subprocess.run(["gh", *args], capture_output=True, text=True)
    if result.returncode != 0:
        sys.exit(f"gh {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout


def open_prs():
    prs = json.loads(gh("pr", "list", "--state", "open", "--limit", "500",
                        "--json", "number,headRefName,baseRefName,body"))
    return {pr["number"]: pr for pr in prs}


def stack_tree(prs, number):
    """The root of [number]'s stack, and each PR's children, ordered by PR number."""
    by_head = {pr["headRefName"]: pr["number"] for pr in prs.values()}
    root = number
    seen = {root}
    while prs[root]["baseRefName"] in by_head:
        root = by_head[prs[root]["baseRefName"]]
        if root in seen:
            sys.exit(f"PR #{root}'s base branches form a loop")
        seen.add(root)
    children = {}
    for pr in sorted(prs.values(), key=lambda it: it["number"]):
        parent = by_head.get(pr["baseRefName"])
        if parent is not None:
            children.setdefault(parent, []).append(pr["number"])
    return root, children


def members(root, children):
    yield root
    for child in children.get(root, []):
        yield from members(child, children)


def render(root, children, current):
    lines = ["## Stack", ""]

    def walk(number, depth):
        marker = "[This PR] " if number == current else ""
        lines.append(f"{'  ' * depth}- {marker}#{number}")
        for child in children.get(number, []):
            walk(child, depth + 1)

    walk(root, 0)
    return f"{START}\n" + "\n".join(lines) + f"\n{END}\n\n"


def with_section(body, section):
    body = (body or "").replace("\r\n", "\n")
    if SECTION.search(body):
        return SECTION.sub(lambda _: section, body, count=1)
    # A new section goes first, so the stack is the first thing a reviewer sees
    return section + body.lstrip("\n") if section else body


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("pr", nargs="?", type=int, help="any PR in the stack (default: this branch's PR)")
    parser.add_argument("--apply", action="store_true", help="write the changed sections to GitHub")
    args = parser.parse_args()

    number = args.pr or json.loads(gh("pr", "view", "--json", "number"))["number"]
    prs = open_prs()
    if number not in prs:
        sys.exit(f"#{number} isn't an open PR")

    root, children = stack_tree(prs, number)
    stack = list(members(root, children))
    repo = json.loads(gh("repo", "view", "--json", "nameWithOwner"))["nameWithOwner"]

    for pr in stack:
        section = render(root, children, pr) if len(stack) > 1 else ""
        body = (prs[pr]["body"] or "").replace("\r\n", "\n")
        updated = with_section(body, section)
        status = "unchanged" if updated == body else ("updated" if args.apply else "would update")
        print(f"#{pr}: {status}")
        if section:
            print("".join(f"    {line}\n" for line in section.strip().splitlines()[1:-1]))
        if args.apply and updated != body:
            with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False) as file:
                file.write(updated)
            # gh pr edit fails in this repo (deprecated projectCards query), so patch through the API
            gh("api", "-X", "PATCH", f"repos/{repo}/pulls/{pr}", "-F", f"body=@{file.name}", "--silent")


if __name__ == "__main__":
    main()
