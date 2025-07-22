[简体中文](./CHANGELOG.md) | [English](./CHANGELOG.en.md)

# Changelog

All notable changes to this project will be documented in this file.

### v0.10.0 - A Leap in Performance & Stability (July 2025)
- ✨ **Advanced Image Loading**: Implemented advanced image loading features, including multiple loading strategies, memory and disk caching, a Bitmap recycling pool, and memory pressure management.
- ⚡️ **Ultimate Compose Performance Optimization**:
    - Replaced `List` with `ImmutableList` across the board to enhance data stability.
    - Added the `@Stable` annotation to data classes, significantly improving recomposition performance.
    - Widely used `derivedStateOf` to optimize state calculations and avoid unnecessary recompositions.
    - Minimized Lambda instance creation through `remember` memoization for callbacks.
- ♻️ **MVI State Access Refactoring**:
    - Refactored state access in all core pages (`Home`, `Login`, `Book`, `Read`, `Search`) to enable finer-grained subscriptions, significantly increasing the `skippable` ratio of Composable functions.
    - Deprecated the old `collectAsState` pattern.
- 🏗️ **Underlying Architecture Fortification**:
    - Introduced `StableThrowable` to optimize the exception handling flow.
    - Refactored the network layer to natively support `ImmutableList` serialization and deserialization.
- 🚀 **Introduced Baseline Profiles**: Added `Baseline Profiles` for app startup and critical user paths (Home, Reader) to significantly boost performance.

### v0.9.0 - Fully Embracing MVI Architecture (July 2025)
- 🏗️ **MVI Foundation Components**: Implemented core components like `BaseMviViewModel`, `MviIntent`, `MviState`, and `MviReducer`.
- ♻️ **Core Module Refactoring**:
    - Completely refactored the Book Detail, Home, Search, and Login modules to the MVI architecture.
    - The UI layer now has zero business logic, solely responsible for state display and intent forwarding.
- ✨ **StateAdapter Pattern**: Introduced `StateAdapter` to provide more granular state flows and optimize Compose recomposition.
- ✅ **Comprehensive Test Coverage**: Added complete unit, component, integration, and performance tests for the refactored modules.
- 🌉 **RN Bridge Refactoring**: Refactored the original RN bridge module into functional domains (`Settings` and `Navigation`) to improve maintainability.
- 🔧 **Reader Code Refactoring**: Extracted duplicate models into `ReaderModels.kt` and refactored all related use cases.

### v0.8.0 - Settings & Cross-Platform Sync (June 2025)
- 🛠️ **Hybrid Architecture Settings Page**: Implemented a hybrid architecture settings page with Android Compose for navigation and React Native for content.
- 💾 **Smart Cache Management**: Provided cache size calculation, formatted display, and a one-click clear function.
- 🎨 **Complete Theme Switching**: Supported light/dark/system-default modes and implemented scheduled automatic switching.
- ✨ **Profile Page (RN)**: Completed the profile page using `Zustand` for state management.
- 🔄 **Cross-Platform Theme Sync**: Implemented bidirectional theme state synchronization between Android and RN.
- ⚡️ **ReactRootView Reuse**: Introduced a caching and reuse mechanism for `ReactRootView` instances to improve RN page loading performance.
- 📝 **New Pages**: Added Privacy Policy and Help & Support pages.

### v0.7.0 - Search & Smart Caching (June 2025)
- 🔍 **Search Functionality Enhancement**:
    - Implemented a search page with search history and popular rankings.
    - Implemented a search results page with advanced category filtering.
    - Implemented a full ranking page with optimized scrolling behavior.
- 💾 **Universal Smart Caching System**:
    - `NetworkCacheManager`: Implemented a universal memory + disk dual-layer network cache manager.
    - `Cache-First` Strategy: Adopted an offline-first strategy that prioritizes the cache.
    - `Repository` Pattern: Encapsulated caching logic and state management by implementing `CachedBookRepository`.
    - `SMART_FALLBACK` Strategy: Added a smart fallback to automatically attempt a network request when the cache is invalid.
- 🎨 **Skeleton Screen Experience**: Added `Shimmer` skeleton screen animations to the page loading process to improve user experience.

### v0.6.0 - Reader Architecture Refactoring (June 2025)
- 📚 **Full-Book Architecture**: Refactored the reader to a full-book management model, fetching and managing all chapters at once.
- 📊 **Smart Page-Count Statistics**: Implemented page count calculation and caching based on the full book content, supporting progressive calculation and absolute page number navigation.
- 💾 **Chapter Preloading & Progress Saving**:
    - Implemented adjacent page preloading.
    - Implemented precise saving and restoration of reading progress (chapter, page number).
- ✨ **Seamless Reading Experience**: Integrated the book detail page as page 0 of the reader for a seamless transition into reading.
- 🐛 **Gesture Conflict Fix**: Fixed gesture conflicts and page jumping issues in the reader's translation mode.

### v0.5.0 - Core Novel Reader Functionality (June 2025)
- ✨ **Dynamic Text Pagination**: Implemented `PageSplitter` to dynamically paginate chapter content based on screen size and font settings.
- 🎨 **Multiple Paging Effects**:
    - Implemented various paging modes: page curl, cover flip, slide, vertical scroll, and no animation.
    - Implemented realistic page curl shadows, thickness, and highlight effects.
- ⚙️ **Reader Settings Panel**:
    - Supports brightness adjustment, font size adjustment, background color switching, and paging effect switching.
- 📖 **Chapter List**: Implemented a chapter list panel for users to quickly jump between chapters.

### v0.4.0 - Book Detail & 3D Animation (May 2025)
- ✨ **Book Detail Page**: Implemented the book detail page, including modules for the cover, author, description, statistics, and reviews.
- 🎨 **Half-Screen Description Bottom Sheet**: Clicking "more" on the description displays a half-screen bottom sheet that can be dismissed by dragging.
- 🔄 **iOS-Style Swipe-Back**: Implemented an `iosSwipeBack` gesture for a smooth swipe-to-go-back experience.
- 🦋 **3D Book Flip Animation**:
    - Implemented `FlipBookAnimation` to support a 3D book flip transition from the home page to the book detail page.
    - Added a "scale-fade" animation effect.

### v0.3.0 - Home Page & Core Services (May 2025)
- ✨ **Brand-New Home Page**:
    - Implemented the home page layout, including a top search bar, category filters, rankings, and a recommended books waterfall stream.
    - Supports pull-to-refresh and pull-up-to-load-more.
- 💾 **Data Layer**: Added `Room` database caching for various home page modules.
- 🔧 **Core Services**:
    - Added multiple network service interfaces, including `HomeService`, `BookService`, `AuthorService`, `SearchService`, `NewsService`, and `AiService`.
- ⚡️ **Performance Optimization**: Implemented image height caching for the home page waterfall stream to avoid recalculations during recomposition.

### v0.2.0 - Login/Register & Basic Architecture (May 2025)
- ✨ **Login/Register**:
    - Implemented a complete login and registration flow, including UI, state management, and animations.
    - Supports graphical CAPTCHA functionality.
- 🔐 **Security**: Implemented `CryptoManager` to encrypt sensitive data using `AndroidKeyStore`.
- 🏗️ **Basic Architecture**:
    - Set up the `Retrofit` + `OkHttp` networking framework, with support for `Hilt` dependency injection.
    - Implemented `BaseViewModel` and a universal loading state component (`LoadingStateComponent`).
    - Established the `Room` database foundation.
- 💾 **User Data Persistence**: Implemented `UserRepository` to handle local caching and management of user information.

### v0.1.0 - Project Initialization & RN/Compose Integration (May 2025)
- 🎉 **Project Creation**: Initialized the project using `react-native`.
- 🏗️ **RN/Compose Integration**: Successfully integrated and displayed a React Native page within `ComposeMainActivity`.
- ⚙️ **Basic Configuration**: Completed basic frontend configuration for `babel`, `typescript`, and `eslint`.
- 📄 **Documentation Initialization**: Created `README.md` to record basic project information. 