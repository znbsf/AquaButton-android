# AquaButton Modern Roadmap

This project is evolving from a single Aqua voice-button app into a data-driven
button-pack platform. Aqua is the first built-in pack; future packs can include
other creators, user-made audio buttons, video buttons, imports, and voice
triggers.

## Target Product Shape

- Multiple switchable button packs, such as Aqua and Mea.
- Import/export complete button packs, including metadata and media files.
- User-created buttons from recording or imported audio files.
- Video buttons that play a fullscreen clip and return automatically.
- Optional voice recognition triggers, such as saying a phrase to play a button.

## Core Data Model

```text
ButtonPack
- id
- name
- author
- description
- logoPath
- categories
- items

ButtonCategory
- id
- packId
- title
- sortOrder

ButtonItem
- id
- packId
- categoryId
- title
- mediaType: audio | video
- mediaPath
- triggerPhrases
- sortOrder
- metadata
```

## Pack File Format

Use a zip-based format, tentatively named `.buttonpack.zip`.

```text
pack.json
assets/audio/*.mp3
assets/video/*.mp4
images/logo.png
```

`pack.json` should describe the pack, categories, buttons, media type, asset
paths, and optional trigger phrases. Import should validate the manifest before
copying files into app storage.

## Phase 1: Button Pack Architecture

Goal: turn the current Aqua-only app into a multi-pack app.

- Extract the current Aqua data into an internal `ButtonPack`.
- Replace Aqua-specific names in the view model with pack/category/item models.
- Add a top-level pack switcher in the UI.
- Keep Aqua as the default built-in pack.
- Add a placeholder path for future built-in packs such as Mea.
- Preserve search, category filtering, random play, and stop behavior.

Done when:

- The app still builds and runs in Android Studio.
- Aqua appears as a selectable pack.
- Switching logic exists even if only one pack is bundled.
- The old Aqua behavior still works.

## Phase 2: Local Persistence

Goal: store imported and user-created packs locally.

- Add Room database.
- Store packs, categories, buttons, trigger phrases, and media references.
- Seed bundled Aqua pack on first launch.
- Keep bundled assets read-only and user/imported assets in app-private storage.

Done when:

- App loads packs from a repository layer instead of directly from assets.
- Built-in Aqua and database-backed packs share one UI path.

## Phase 3: Import and Export

Goal: support portable button packs.

- Implement `.buttonpack.zip` import.
- Validate `pack.json` and reject broken/missing media references.
- Copy imported media into app-private storage.
- Implement export for a selected pack.
- Add simple import/export UI.

Done when:

- A pack can be exported, deleted locally, imported again, and played.

## Phase 4: Custom Audio Buttons

Goal: let users create their own audio buttons.

- Add create/edit button screens.
- Add category creation and editing.
- Import audio via Android file picker.
- Record audio with `MediaRecorder`.
- Store recordings and imported files in app-private storage.
- Support rename, delete, and move between categories.

Done when:

- A user can create a new pack/category/button and play it without rebuilding.

## Phase 5: Video Buttons

Goal: support video media in the same button system.

- Add `mediaType = video`.
- Use AndroidX Media3 / ExoPlayer.
- Add fullscreen video playback screen.
- Auto-close when playback completes.
- Provide a visible close control.
- Support video import and export in pack files.

Done when:

- A video button can be imported or created, played fullscreen, and return to
  the button list automatically.

## Phase 6: Voice Recognition Triggers

Goal: trigger buttons by spoken phrases.

First version should be foreground-only:

- Add microphone action in the app.
- Use Android `SpeechRecognizer`.
- Match recognized text against `triggerPhrases`.
- Trigger audio or video playback on match.
- Show clear UI for listening, matched, and no-match states.

Later version can investigate background listening, but that has Android
permission, battery, privacy, and vendor-kill restrictions.

Done when:

- Saying a configured phrase while the app is listening triggers the matching
  button.

## Engineering Notes

- Keep the app data-driven; avoid hard-coding Aqua-specific logic into UI.
- Prefer app-private storage for imported media.
- Keep bundled packs immutable; copy only user/imported packs.
- Avoid implementing background voice listening until the foreground flow is
  stable.
- For large video packs, consider size warnings and future external storage or
  streaming options.

## Current Recommended Next Step

Start with Phase 1: Button Pack Architecture.

The immediate implementation should create the shared `ButtonPack`,
`ButtonCategory`, and `ButtonItem` model layer, then adapt the existing Compose
UI and player to use those models while preserving the current Aqua behavior.
