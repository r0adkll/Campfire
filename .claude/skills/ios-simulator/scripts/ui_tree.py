#!/usr/bin/env python3
# Copyright 2026, Drew Heavner and the Campfire project contributors
# SPDX-License-Identifier: GPL-3.0-only
"""Flatten `axe describe-ui` JSON into one line per labelled or interactive node, with its centre point.

usage: axe describe-ui --udid <udid> | python3 -I ui_tree.py [--all]

Nodes whose centre lies outside the screen are dropped: the tree includes the hidden software
keyboard's keys below the bottom edge. --all also keeps unlabelled nodes.
"""
import json
import sys

SHOW_ALL = "--all" in sys.argv
INTERACTIVE = {"Button", "TextField", "TextArea", "SecureTextField", "Switch", "Slider"}


def walk(node, screen, depth=0):
    frame = node.get("frame") or {}
    cx = frame.get("x", 0) + frame.get("width", 0) / 2
    cy = frame.get("y", 0) + frame.get("height", 0) / 2
    label, value, ident = node.get("AXLabel"), node.get("AXValue"), node.get("AXUniqueId")
    kind = node.get("type") or node.get("role")
    on_screen = screen is None or (0 <= cx <= screen[0] and 0 <= cy <= screen[1])
    if on_screen and (SHOW_ALL or label or value or ident or kind in INTERACTIVE):
        bits = [f"{'  ' * depth}{kind}"]
        if label:
            bits.append(f"label={label!r}")
        if value:
            bits.append(f"value={value!r}")
        if ident:
            bits.append(f"id={ident!r}")
        bits.append(f"center=({cx:.0f},{cy:.0f})")
        print(" ".join(bits))
    for child in node.get("children") or []:
        walk(child, screen, depth + 1)


data = json.load(sys.stdin)
for root in data if isinstance(data, list) else [data]:
    f = root.get("frame") or {}
    walk(root, (f["width"], f["height"]) if f.get("width") else None)
