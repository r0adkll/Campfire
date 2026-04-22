---
name: Device Info Pipeline
description: Complete server-side device info flow from request to DB storage; critical mapping of DeviceInfo fields to Device model columns
type: project
---

**Flow**: `getDeviceInfo(req, clientDeviceInfo)` in `PlaybackSessionManager.js:52`
  1. Parses UA with ua-parser-js → osName/osVersion come from UA if client doesn't override
  2. Calls `DeviceInfo.setData(ip, ua, clientDeviceInfo, serverVersion, userId)` (`server/objects/DeviceInfo.js:84`)
  3. In `setData`: `osName`/`osVersion` are set from UA parse, NOT from `clientDeviceInfo` — client values are ignored for these fields
  4. `sdkVersion`, `manufacturer`, `model`, `clientName`, `clientVersion` are taken from `clientDeviceInfo` via `stripAllTags()`
  5. If `sdkVersion` is truthy → `deviceName = "${manufacturer} ${model}"`, `clientName` defaults to `'Abs Android'` if not provided
  6. Looks up existing Device row by `clientDeviceInfo.deviceId` → if found, calls `existingDevice.update(newDeviceInfo)` then `updateFromOld`

**DB Schema** (`server/models/Device.js`): NO `sdkVersion`, `osName`, `osVersion`, `manufacturer`, `model`, `browserVersion`, `deviceType` columns directly.
  - `deviceVersion` STRING — stores `sdkVersion` (Android) OR `browserVersion` (web)
  - `extraData` JSON — stores `{ manufacturer, model, osName, osVersion, browserName }`
  - `deviceName` STRING — stores computed `"Google Pixel 9"` style string
  - `clientName` STRING — stores `"Campfire"` or `"Abs Android"` etc.
  - `clientVersion` STRING
  - `deviceId` STRING

**getFromOld mapping** (`Device.js:56`):
  - `oldDeviceInfo.sdkVersion || oldDeviceInfo.browserVersion` → `deviceVersion`
  - `{ manufacturer, model, osName, osVersion, browserName }` → `extraData` JSON
  - `deviceType` is NOT persisted to DB at all (lost after setData)

**toOldJSON/getOldDevice** (`Device.js:122,149`):
  - Reconstructs `sdkVersion` from `deviceVersion` only if `clientName === 'Abs Android'`
  - If `clientName === 'Campfire'`, `sdkVersion` will be NULL in the reconstructed object (wrong branch)
