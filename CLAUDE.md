# RichmediaEditor Android - Project Instructions

## What This Is

An Android library (Jetpack Compose) for creating and viewing richmedia documents with an Instagram/TikTok-style editor. Users compose rich posts by adding text layers, animations, and Lottie overlays on top of photos and videos, with pinch-to-zoom/pan media positioning, then export as JSON. Documents are cross-platform (iOS/Android/web) — the canonical format spec lives at `../loxation-sw/docs/guide_to_richmedia_posts.md`.

## Build & Test

```bash
./gradlew :richmedia-editor:assembleDebug       # Build the library
./gradlew :richmedia-editor:testDebugUnitTest    # Run tests
./gradlew :richmedia-editor:compileDebugKotlin   # Fast compile check
```

Requires JDK 17+, Android SDK 35.

## Architecture

**MVVM** with Jetpack Compose. All models use kotlinx.serialization.

```
richmedia-editor/src/main/java/com/loxation/richmedia/
├── model/       # Serializable data types (RichPostContent, TextLayer, etc.)
├── ui/          # Compose views (editor, canvas, player, overlays)
├── viewmodel/   # AnimatedPostEditorViewModel (StateFlow-based)
├── service/     # AnimationRenderer, LottieImporter
└── util/        # Color extensions
```

### Key Entry Points

- `AnimatedPostEditorScreen` — main public editor composable. Takes `MediaInput` list, returns `RichPostContent` JSON via `onComplete` callback.
- `GalleryPlayerView` — read-only TikTok-style viewer for displaying animated posts. Takes `RichPostContent` and optional `localImages`.

### Document Format

`RichPostContent` → JSON with `blocks[]`, each block has media reference + `textLayers[]` with normalized position (0-1), style, animation preset, path, optional Lottie overlay, and optional `mediaTransform` (zoom/pan).

Content type: `application/vnd.loxation.richmedia+json`

## iOS Parity

iOS counterpart: `../richmedia-editor` (Swift Package). Keep models JSON-compatible. Check iOS implementation when adding features.

## Code Conventions

- All models are data classes with `@Serializable`
- Positions use normalized coordinates (0.0–1.0) for device independence
- 9:16 aspect ratio (Instagram Stories format)
- Max 10 text layers per block
- Material 3 for UI components
- Color stored as hex strings (`#RRGGBB`)

## Do NOT

- Add HTML export (server-side rendering handles it)
- Add media picker/upload logic (host app responsibility)
- Break the public API surface of `AnimatedPostEditorScreen` or `GalleryPlayerView`
- Modify model fields without updating the format spec at `../loxation-sw/docs/guide_to_richmedia_posts.md`
