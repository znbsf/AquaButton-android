# AquaButton Modern

A modern Android voice-button app rebuilt with Jetpack Compose. The app started
from AquaButton and now uses a button-pack architecture so one APK can switch
between multiple voice packs.

## Current Features

- Jetpack Compose and Material 3 UI.
- Android Studio Panda / AGP 9 / Gradle 9 toolchain.
- Built-in AquaButton pack with 260 local voice assets.
- Built-in MeaButton pack with 237 local voice assets.
- Room-backed local pack database foundation.
- Pack switching, category filtering, search, random play, stop, and local
  asset-first playback.
- Fallback remote playback URL is retained for built-in packs.

## Downloads

Use the latest GitHub release:

https://github.com/znbsf/AquaButton-android/releases

## Build

Recommended environment:

- Android Studio Panda 2025.3.4 or newer
- JDK bundled with Android Studio
- Android SDK Platform 36
- Android SDK Build Tools 36

Build from the repository root:

```powershell
.\gradlew.bat assembleDebug
```

Install on a connected emulator or device:

```powershell
.\gradlew.bat installDebug
```

## Roadmap

Development is tracked in [ROADMAP.md](./ROADMAP.md). The next major phases are:

- `.buttonpack.zip` import/export.
- Custom audio buttons from recording or imported files.
- Video buttons.
- Foreground voice-recognition triggers.

The current app already stores the built-in packs through a Room database. This
keeps the UI path shared with future imported and user-created packs while still
refreshing bundled Aqua and Mea assets from read-only app assets.

## Source Credits

This repository is a modernized continuation of:

- [MinatoAquaCrew/AquaButton-android](https://github.com/MinatoAquaCrew/AquaButton-android)

Built-in voice data and assets are derived from:

- AquaButton web resources: [zyzsdy/aqua-button](https://github.com/zyzsdy/aqua-button)
- MeaButton web resources: [zyzsdy/meamea-button](https://github.com/zyzsdy/meamea-button)

## Licenses and Notes

The original AquaButton Android program is GPLv3. This project keeps that
license. Voice assets follow the relevant secondary-creation terms described by
their upstream projects. This is a fan project and is not affiliated with
Hololive, Minato Aqua, Kagura Mea, or any official agency.
