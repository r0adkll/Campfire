---
status: accepted
---

# Store screenshots keep native device resolution, not Play's 9:16

Google Play recommends 9:16 (1080×1920) phone screenshots and gates some promotion placements on that ratio, but our phone Shots are captured at the emulator's native 1080×2424 and committed to Store Metadata as-is. Cropping to 9:16 would cut the bottom of every shot — exactly where the mini player and navigation live — and the same files are consumed by F-Droid/IzzyOnDroid, which have no ratio requirement. The screenshot tool only normalizes PNGs; a 9:16 crop is an opt-in flag for the day promotion eligibility matters more than showing the full UI.
