---
name: DeviceInfo update() behavior
description: How DeviceInfo.update() handles null fields and the null-wiping second loop
type: project
---

**`DeviceInfo.update(deviceInfo)`** (`server/objects/DeviceInfo.js:120-143`):

Loop 1 (lines 125-132): For each key in new `deviceInfoJson`, if value differs from existing, copy it in. Skips `id` and `deviceId`.

Loop 2 (lines 134-141): For each key in existing `existingDeviceInfoJson`, if existing has a value but new json does NOT have that key, set `this[key] = null`.

**Critical: `toJSON()` strips null/undefined fields** (`DeviceInfo.js:61-66`). So if new DeviceInfo has `browserName: null`, it won't appear in `toJSON()` output at all.

**Consequence**: If new device info has field X as null, and existing device has field X as non-null:
- Field X is absent from new `toJSON()` output
- Loop 2 fires: existing[X] is truthy, new[X] is falsy (missing) → `this[X] = null` → field is WIPED

**For Campfire Android**: Fields like `browserName`, `browserVersion`, `deviceType` are null in payload → they get stripped from toJSON → Loop 2 will null them out on existing records. This is correct behavior (they shouldn't have values for Android anyway).

**The ipAddress update case**: `ipAddress` IS in toJSON output (non-null on server side since server reads from request). So it correctly updates if user's IP changes.
