# Launcher3 - Modern Compose Launcher

A complete rewrite of AOSP Launcher3 using modern Jetpack Compose, [Compose Cupertino](https://github.com/alexzhirkevich/compose-cupertino) for iOS-inspired UI, and [Haze](https://github.com/chrisbanes/haze) for blur effects.

## Features

- **Workspace** - Multi-page home screen grid with app icons, folders, and widgets
- **Hotseat** - iOS-style dock with blurred background
- **All Apps** - Scrollable app drawer with search and alphabetical grouping
- **Folders** - Animated folder popups with grid layout
- **Widget Picker** - Browse and search available widgets
- **Settings** - Configure grid rows/columns, hotseat count, notification dots, and more

## Tech Stack

- **100% Jetpack Compose** - No XML layouts, fully declarative UI
- **Compose Cupertino** - iOS-inspired search fields, navigation patterns
- **Haze** - Material blur effects for dock and overlays
- **Material 3** - Dynamic theming with light/dark support
- **Coroutines + StateFlow** - Efficient state management
- **Coil** - Image loading for icons

## Memory Management

- `LazyColumn`/`LazyRow` for virtualized lists (only visible items in memory)
- `StateFlow` with `collectAsStateWithLifecycle` for lifecycle-aware collection
- `remember`/`rememberSaveable` for efficient recomposition
- ViewModel-based state surviving configuration changes
- Proper coroutine scope management with `viewModelScope`

## Build

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## GitHub Actions

The project includes a CI workflow that:
- Builds both debug and release APKs
- Runs lint checks
- Uploads APK artifacts
- Supports keystore-based signing via secrets
