---
name: Android sdkVersion + clientName Bug
description: Root causes for why Android sessions from Campfire don't show correct device info on server
type: project
---

**Confirmed bugs as of 2026-04-22, server v2.33.2:**

**Bug 1 (CRITICAL): `toOldJSON`/`getOldDevice` sdkVersion reconstruction is keyed on `clientName === 'Abs Android'`**
- `Device.js:125`: `if (this.clientName === 'Abs Android') { sdkVersion = this.deviceVersion }`
- Campfire sends `clientName: "Campfire"` → server stores `"Campfire"` in DB
- When the Device row is later read back via `getOldDevice()`, it hits the `else` branch → `sdkVersion = null`, `browserVersion = deviceVersion`
- The `deviceVersion` column (which correctly stores the SDK int as string) is misrouted to `browserVersion` instead of `sdkVersion` on read-back
- **Impact**: Any code that reads the stored Device and uses `sdkVersion` will get null; `browserVersion` will incorrectly have the SDK value

**Bug 2 (POTENTIAL): `stripAllTags` destroys integer sdkVersion**
- `DeviceInfo.js:99`: `this.sdkVersion = stripAllTags(clientDeviceInfo?.sdkVersion) || null`
- `htmlSanitizer.js`: `if (typeof html !== 'string') return ''`
- `stripAllTags(34)` → `""` → `"" || null` → `null`
- Campfire currently sends `sdkVersion` as a STRING ("34") via `applicationInfo.sdkVersion?.toString()` in `DefaultNetworkSessionMapper.kt:108` so this is NOT currently triggered
- **Risk**: If any future code path sends sdkVersion as Int (core model has `sdkVersion: Int?`), it will silently become null

**Bug 3: osName/osVersion from UA, not clientDeviceInfo**
- `DeviceInfo.setData` lines 92-93: `this.osName = ua?.os.name || null; this.osVersion = ua?.os.version || null`
- Client-provided `osName`/`osVersion` in the JSON body are IGNORED; UA parse result wins
- Campfire UA: `Campfire Alpha/1.2.3 (Android 34; Mobile)` → ua-parser-js parses OS as Android/34 correctly, so this works in practice

**Campfire file locations:**
- `ApplicationInfo.kt`: `app/android/src/main/java/app/campfire/android/di/AndroidAppComponent.kt:43` — `sdkVersion = Build.VERSION.SDK_INT` (Int)
- `NetworkDeviceInfo.kt`: `data/network/api/src/commonMain/kotlin/app/campfire/network/models/DeviceInfo.kt:18` — `sdkVersion: String?`
- `DefaultNetworkSessionMapper.kt:108`: `sdkVersion = applicationInfo.sdkVersion?.toString()` — converts Int→String correctly
- `PlaybackSessionMapping.kt:54`: `sdkVersion = sdkVersion?.toIntOrNull()` — parses back String→Int for domain model
