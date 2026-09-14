#!/bin/sh
# Copyright 2026, Drew Heavner and the Campfire project contributors
# SPDX-License-Identifier: GPL-3.0-only
#
# Regenerates app/desktop/src/main/resources/icon-macos.png from the shared app artwork.
#
# macOS app icons are not edge-to-edge. They sit in a 824x824 rounded square centred on a 1024x1024
# canvas, leaving a 100px margin, with a soft shadow beneath — an icon that fills its canvas reads
# as noticeably larger than everything around it in the Dock. Windows and Linux want the full-bleed
# square instead, which is what icon.png stays.
#
# The silhouette in macos-squircle.svg is Apple's, and Apple's corner is a continuous curve rather
# than a circular arc — a superellipse fitted to it needs an exponent that drifts from about 5.0 to
# 6.3 along the curve, so no single formula reproduces it. It was instead traced from the alpha
# channel of a stock system icon (/System/Applications/Calculator.app), at the 50% crossing with
# sub-pixel interpolation, and matches that shape to a mean error of 0.4%. Committing the trace
# means regenerating needs nothing but the two tools below.
#
# Requires: ImageMagick, librsvg  (brew install imagemagick librsvg)

set -eu

here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)

src="$root/app/ios/iosApp/Assets.xcassets/AppIcon.appiconset/icon_1024x1024.png"
squircle="$here/macos-squircle.svg"
out="$root/app/desktop/src/main/resources/icon-macos.png"

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

rsvg-convert -w 1024 -h 1024 "$squircle" -o "$work/mask.png"
magick "$work/mask.png" -alpha extract "$work/mask-a.png"

# Scale the whole artwork into the body, so the glyph keeps the proportion it has on iOS/Android.
magick "$src" -resize 824x824 -background none -gravity center -extent 1024x1024 "$work/art.png"
magick "$work/art.png" "$work/mask-a.png" -alpha off -compose CopyOpacity -composite "$work/body.png"

magick "$work/mask-a.png" -blur 0x11 -background black -alpha shape \
  -channel A -evaluate multiply 0.28 +channel "$work/shadow.png"

magick -size 1024x1024 xc:none \
  \( "$work/shadow.png" -repage +0+10 \) -composite \
  "$work/body.png" -composite \
  -depth 8 "$out"

echo "wrote $out"
magick identify "$out"
