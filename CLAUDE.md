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

## Lessons Learned

### Material Icons

The default `material3` dependency only includes a small set of icons (`Icons.Default.*`). Icons like `Pause`, `TextFields`, `TouchApp`, `AutoAwesome`, `Palette`, `Videocam` require `androidx.compose.material:material-icons-extended`. This dependency is already added to `build.gradle`.

### Text Outline Rendering

Android Compose has no built-in text stroke/outline. Use a **two-pass approach**: render `Text` with `drawStyle = Stroke(width)` for the outline behind, then render normal `Text` on top for the fill. Both must share identical `TextStyle` (font, size, alignment) to align perfectly.

### Compose Gesture Stacking

Multiple gesture detectors can coexist on the same composable via chained `.pointerInput` blocks — e.g., `detectTapGestures` (tap + long press) in one block and `detectDragGestures` in another, plus `.transformable` for pinch/rotate. Order matters: earlier modifiers get priority.

### Column/Row Don't Have a `spacing` Parameter

Unlike SwiftUI's `VStack(spacing:)`, Compose `Column` and `Row` use `verticalArrangement = Arrangement.spacedBy(X.dp)`. Writing `Column(spacing = 0.dp)` won't compile.

### ExoPlayer Lifecycle

Always call `player.release()` in a `DisposableEffect(Unit) { onDispose { ... } }` block. For editing mode, set `volume = 0f` (muted) and control playback via `LaunchedEffect(isPlaying)`.

### Lottie Compose Integration

Use `rememberLottieComposition(LottieCompositionSpec.JsonString(jsonData))` for inline JSON. Pair with `animateLottieCompositionAsState(composition, isPlaying, iterations)`. The `iterations` param uses `LottieConstants.IterateForever` for looping.

### AnimationRenderer Completeness

All 30 `AnimationPreset` enum values must have explicit `when` branches — avoid `else -> Modifier` catch-alls that silently swallow new presets. Use explicit `AnimationPreset.motionPath, AnimationPreset.curvePath -> Modifier` for unimplemented path animations.

### GalleryCanvasView Pager Navigation

Use `rememberCoroutineScope()` + `pagerState.animateScrollToPage()` for programmatic page changes (prev/next buttons). `HorizontalPager` doesn't expose a direct `currentPage` setter.

### Editor Bottom Toolbar Clipping — BLOCKING FAILURE

The `EditorBottomToolbar` in `AnimatedPostEditorScreen` gets half-hidden at the bottom of the screen. This is a **blocking issue** — no further toolbar-related work should proceed until it is resolved.

**Host app context**: The editor is launched inside a Compose `Dialog` with `decorFitsSystemWindows = false` (in `PublicPostComposerSheet.kt`). The host app targets SDK 35 (Android 15 enforces edge-to-edge). The XML layout uses `fitsSystemWindows="true"` on `DrawerLayout` and `CoordinatorLayout`.

**All attempted fixes FAILED on device:**
1. **Scaffold `bottomBar`** — Toolbar in Scaffold's `bottomBar` slot is still clipped.
2. **Box overlay with `navigationBarsPadding()` inside Scaffold** — No effect; Scaffold consumes the insets.
3. **Box overlay with `navigationBarsPadding()` outside Scaffold** — Toolbar still clipped; insets appear to be 0 for siblings.
4. **Toolbar inside Scaffold content Column** (below `GalleryCanvasView` with `weight(1f)`) — Still clipped. Scaffold's own content padding doesn't provide enough bottom space.

**Root cause is unknown.** Likely related to how the Compose `Dialog` window interacts with the host app's inset handling. Needs on-device investigation with Layout Inspector to determine what inset values are actually reported and where space is being lost.

## Do NOT

- Add HTML export (server-side rendering handles it)
- Add media picker/upload logic (host app responsibility)
- Break the public API surface of `AnimatedPostEditorScreen` or `GalleryPlayerView`
- Modify model fields without updating the format spec at `../loxation-sw/docs/guide_to_richmedia_posts.md`
