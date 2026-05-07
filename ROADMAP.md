# ButtonBox Roadmap

This project is evolving from a single Aqua voice-button app into a data-driven
button-pack platform. Aqua and Mea are the first built-in packs; future packs can
include creators, games, meme sounds, user-made audio buttons, video buttons,
imports, and voice triggers.

## Target Product Shape

- Multiple switchable button packs, such as Aqua, Mea, game packs, and meme
  packs.
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

Status: completed for built-in packs.

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
- Aqua and Mea appear as selectable built-in packs.
- The old Aqua behavior still works.

## Phase 2: Local Persistence

Status: completed for built-in packs.

Goal: store imported and user-created packs locally.

- Add Room database.
- Store packs, categories, buttons, trigger phrases, and media references.
- Seed bundled Aqua pack on first launch.
- Keep bundled assets read-only and user/imported assets in app-private storage.

Done when:

- App loads packs from a repository layer instead of directly from assets.
- Built-in Aqua and database-backed packs share one UI path.

Current implementation:

- Added Room database entities for packs, categories, button items, and trigger
  phrases.
- Added a repository layer that seeds built-in Aqua and Mea packs into Room.
- Built-in packs are marked with `isBuiltIn = true` so bundled assets stay
  immutable while the UI can still hide deleted built-in packs and buttons as
  local user state.
- Playback supports asset paths, future local file paths, and remote fallback
  URLs from the same button item model.

## Phase 3: Import and Export

Status: foundation completed for audio packs.

Goal: support portable button packs.

- Implemented `.buttonpack.zip` import.
- Validates `pack.json` and rejects broken/missing media references.
- Copies imported media into app-private storage.
- Implements export for a selected pack.
- Adds simple import/export UI.
- Reserves manifest fields for future video media and trigger phrases.

Done when:

- A pack can be exported, deleted locally, imported again, and played.

Current implementation:

- Android system file picker is used for opening and creating pack zip files.
- Built-in packs can be exported as portable `.buttonpack.zip` files.
- Imported packs overwrite the same pack id, which makes iteration on custom
  packs straightforward during development.
- User-created/imported packs can now be deleted from the UI, including their
  database rows and app-private media directory.

## Phase 4: Custom Audio Buttons

Status: foundation completed for user packs, imported audio files, and deletion.

Goal: let users create their own audio buttons.

- Add create/edit button screens.
- Add category creation and editing.
- Import audio via Android file picker.
- Record audio with `MediaRecorder`.
- Store recordings and imported files in app-private storage.
- Support rename, delete, and move between categories.

Done when:

- A user can create a new pack/category/button and play it without rebuilding.

Current implementation:

- `New Pack` creates a user-owned pack with a first category.
- Built-in pack assets remain read-only; `Add Audio` is enabled only for user
  packs.
- `Add Audio` opens Android's file picker for `audio/*`, copies the selected
  file into app-private storage, and creates a playable button.
- User packs can be exported through the existing `.buttonpack.zip` path.
- User packs can be deleted with confirmation. Deleting a pack removes its
  categories, buttons, trigger phrases, and app-private media directory.
- Imported audio buttons in user packs can be deleted with confirmation.
  Deleting a button removes its database row, trigger phrases, and copied media
  file.
- Built-in Aqua and Mea assets remain read-only, but `Delete Pack` and
  per-button delete are available for built-in content. Deleting built-in
  content records local hidden-state preferences so startup reseeding skips it.
- `Add Audio` remains disabled for built-in packs because APK assets should not
  be mixed with user-owned media.
- Pack actions, pack chips, and category chips use wrapping rows so narrow
  screens avoid long one-line horizontal controls.
- Rename/edit flows, moving buttons, creating extra categories, and recording
  audio are still planned.

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

Continue polishing Phase 4: Custom Audio Buttons.

The immediate implementation should add rename/edit flows for user packs and
buttons, extra category management, then recording with `MediaRecorder`. After
that, the same pack editor can grow naturally into video buttons.
