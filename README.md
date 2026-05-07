# ButtonBox

ButtonBox is a modern Android button-pack app rebuilt with Jetpack Compose. It
started from AquaButton, but the new architecture is intentionally generic:
voice buttons, meme soundboards, game clips, custom recordings, and future video
buttons can all live in the same pack system.

## Current Features

- Jetpack Compose and Material 3 UI.
- Android Studio Panda / AGP 9 / Gradle 9 toolchain.
- Built-in AquaButton pack with 260 local voice assets.
- Built-in MeaButton pack with 237 local voice assets.
- Room-backed local pack database foundation.
- `.buttonpack.zip` import/export foundation for portable audio packs.
- Custom pack creation plus single-audio import into user packs.
- Delete packs and buttons with confirmation, including bundled Aqua/Mea
  content as local hidden state.
- Add categories and imported audio to any selected pack, including bundled
  Aqua/Mea packs; imported media stays in app-private storage.
- Pack switching, category filtering, search, random play, stop, and local
  asset-first playback.
- Pack actions and category chips wrap across multiple rows on narrow screens.
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

- Manual import/export polish for `.buttonpack.zip` packs.
- Custom audio polish: rename/move buttons, extra categories, and recording.
- Video buttons.
- Foreground voice-recognition triggers.

The current app already stores the built-in packs through a Room database. This
keeps the UI path shared with future imported and user-created packs, including
non-VTuber packs, while still refreshing bundled Aqua and Mea assets from
read-only app assets.

## Button Pack Format

ButtonBox uses a zip-based `.buttonpack.zip` format. The current foundation
supports audio packs and reserves the same structure for future video packs.

```text
pack.json
assets/audio/*.mp3
assets/video/*.mp4
images/logo.png
```

The manifest stores pack metadata, categories, button titles, media type, media
path, and future trigger phrases. Imported media is copied into app-private
storage so packs stay portable after import.

## Custom Packs

Use `New Pack` to create a user-owned pack with an initial category. `New Pack`
uses the same chip style as regular pack tabs. `Add Category` appears at the
end of the category chips, and `Add Audio` appears as the last card in the
shown button list.

Built-in Aqua and Mea packs keep their bundled assets read-only, but you can
add categories and imported audio to them. Imported audio files are copied into
app-private storage, can be deleted, and can be exported again as part of a
`.buttonpack.zip`. `Delete Pack`, `Import`, and `Export` live in the top app
bar pack actions menu; deletion still asks for confirmation. Deleting built-in content
records a local hidden-state preference so the app stops reseeding it on
startup; it does not rewrite APK assets.

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
Hololive, Minato Aqua, Kagura Mea, game publishers, or any official agency.
