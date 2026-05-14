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

Status: recording, management polish, and basic ordering controls completed.

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
- The launcher identity has been renamed to ButtonBox, with Android
  `applicationId = com.znbsf.buttonbox` and a custom launcher icon.
- Built-in pack assets remain read-only, but users can append app-private
  categories and imported audio to built-in packs.
- `Add Audio` opens Android's file picker for `audio/*`, copies the selected
  file into app-private storage, opens an edit dialog for title/category, and
  creates a playable button.
- `Add Audio` also supports microphone recording through `MediaRecorder`,
  saves AAC-in-MP4 `.m4a` files into app-private storage, and offers
  start/stop, preview, re-record, title/category selection, and save.
- `Add Audio` now appears as the last card in the shown button list instead of
  in the top action strip.
- `Add Category` appears as the last category chip for the selected pack.
- User packs can be exported through the existing `.buttonpack.zip` path.
- `.buttonpack.zip` import now checks `schemaVersion` and rejects packs created
  for a newer format.
- Import/export success messages include the affected button count.
- Import opens a preview dialog before writing data, including pack metadata,
  schema version, category/button/media counts, and same-id conflicts.
- Same-id imports can replace the existing pack or be imported as a copy.
- User packs can be deleted with confirmation. Deleting a pack removes its
  categories, buttons, trigger phrases, and app-private media directory.
- Packs can be renamed from the top app bar actions menu.
- Categories can be renamed or deleted from the top app bar actions menu.
- Buttons can be renamed and moved between categories from the list edit action.
- User-owned categories can be moved up/down from the top app bar actions menu.
- User-owned buttons can be moved up/down inside the selected category from the
  list controls; ordering is saved in Room and survives app restart.
- Per-button actions now live in a compact overflow menu, reducing right-side
  crowding on cards with long titles.
- Pack actions are grouped into pack, category, and danger sections, and import
  preview copy now explains schema support, media counts, and conflict choices.
- Imported audio buttons in user packs can be deleted with confirmation.
  Deleting a button removes its database row, trigger phrases, and copied media
  file.
- Built-in Aqua and Mea assets remain read-only, but `Delete Pack` and
  per-button delete are available for built-in content. Deleting built-in
  content records local hidden-state preferences so startup reseeding skips it.
- MyInstants Meme Pack is bundled as the third built-in pack with 37 local
  audio buttons after removing the two Hakimi entries requested for exclusion.
- The old action strip has been reduced: `New Pack` uses the same chip style as
  pack tabs, while import, export, and delete live in the top app bar pack
  actions menu.
- Pack actions, pack chips, and category chips use wrapping rows so narrow
  screens avoid long one-line horizontal controls.
- A full recording regression was verified on an emulator: record, preview,
  save, play, export the pack, clear app data, re-import the exported pack, and
  play the recorded button again.
- Ordering controls were smoke-tested on an imported pack in the emulator:
  button order persisted in Room and category chip order updated immediately.

## Phase 5: Video Buttons

Goal: support video media in the same button system.

Status: in-app flow completed for local imported videos.

- Added `mediaType = video` import through the same pack/category/button model.
- Imported videos are copied to app-private `assets/video/...` storage.
- Video buttons play inside ButtonBox through AndroidX Media3 / ExoPlayer.
- Playback uses an immersive fullscreen `PlayerView` surface with a visible
  close control.
- Playback returns to the button list automatically when the clip ends.
- `.buttonpack.zip` import/export includes audio and video files through the
  same manifest/media path flow.
- Schema v2 validates safe `assets/audio/...` and `assets/video/...` paths,
  rejects duplicate category/button ids, and persists trigger phrases.
- Add/edit dialogs support title, category, and trigger phrase editing for both
  audio and video buttons.
- The top-right pack menu can switch video playback between fill-screen crop
  and complete-frame fit.

Next polish:

- Add optional player controls for pause/seek when useful.
- Add thumbnails or video duration metadata to video buttons.
- Add size warnings for large imported videos.

Done when:

- A video button can be imported or created, edited, exported, re-imported,
  played in-app fullscreen, and return to the button list automatically.
  Completed for local imported videos in v2.19.

## Phase 6: Voice Recognition Triggers

Goal: trigger buttons by spoken phrases.

First version should be foreground-only:

- Add microphone action in the app.
- Use Android `SpeechRecognizer`.
- Match recognized text against `triggerPhrases`.
- Trigger audio or video playback on match.
- Show clear UI for listening, matched, and no-match states.

Status: foreground MVP completed in v2.20.

- Added a microphone floating action button for start/stop listening.
- Reuses `RECORD_AUDIO` permission without adding a background service.
- Matches normalized recognition results against saved trigger phrases.
- Audio matches play through the existing `MediaPlayer` path.
- Video matches open the existing fullscreen Media3 player.
- Non-matching results are surfaced as notices so trigger phrases can be tuned.

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

Move into Phase 6 polish: better match ranking, optional fuzzy matching,
language selection, and a testable voice-recognition diagnostics screen.
