# AGENTS.md

Guidance for coding agents working in this repository.

## Scope

These instructions apply to the entire `chatbot-android` repository.

## Project Overview

This is a Kotlin Android chatbot app in the package `io.github.initrc.chatbot`.
The app uses Jetpack Compose Material 3 for UI, Hilt for dependency injection,
Room for chat history, DataStore Preferences for settings, and Ktor CIO with SSE
for OpenAI-compatible chat completion streaming. The default API base URL is the
Groq OpenAI-compatible endpoint.

Important areas:

- `app/src/main/java/io/github/initrc/chatbot/MainActivity.kt`: Compose host and
  app entry point.
- `app/src/main/java/io/github/initrc/chatbot/ui/chat`: chat screen, drawer,
  markdown rendering, and chat view models.
- `app/src/main/java/io/github/initrc/chatbot/ui/chat/ChatScreen.kt`: chat route
  state wiring, drawer coordination, settings sheet, and snackbar side effects.
- `app/src/main/java/io/github/initrc/chatbot/ui/chat/ChatScreenContent.kt`,
  `ModelHeader.kt`, `MessageList.kt`, and `SendView.kt`: chat UI rendering pieces.
- `app/src/main/java/io/github/initrc/chatbot/ui/settings`: settings view model.
- `app/src/main/java/io/github/initrc/chatbot/data`: repositories, settings,
  remote chat calls, and context compression.
- `app/src/main/java/io/github/initrc/chatbot/data/db`: Room database, DAO,
  entities, summaries, and Hilt database module.
- `app/src/test`: JVM unit tests.
- `app/src/androidTest`: instrumentation tests.

## Build And Test Commands

Use the checked-in Gradle wrapper.

- Build debug APK: `./gradlew :app:assembleDebug`
- Run JVM unit tests: `./gradlew :app:testDebugUnitTest`
- Run instrumentation tests, with a device or emulator connected:
  `./gradlew :app:connectedDebugAndroidTest`
- Install debug APK on a connected device: `./gradlew :app:installDebug`

Prefer the smallest relevant command while iterating. Before finishing changes
that touch production Kotlin, run at least `./gradlew :app:testDebugUnitTest` if
the local environment supports it.

## Architecture Rules

- Keep UI state ownership in `@HiltViewModel` classes and expose observable state
  with `StateFlow` or Compose state.
- Keep Compose files focused on rendering and UI events. Put persistence,
  networking, and request preparation in repositories or data sources.
- `ChatRepository` coordinates chat persistence, context compression, and remote
  streaming. Avoid duplicating that orchestration in UI code.
- Room is the durable source of truth for conversation history. The current
  in-memory streaming state in `ChatViewModel` is only for showing fresher chunks
  between throttled database writes.
- `ChatRemoteDataSource` should stay focused on HTTP/SSE request execution and
  response parsing. Do not add UI behavior or persistence there.
- `ChatContextCompressor` is pure logic and should remain easy to unit test.
- `SettingsRepository` owns API settings normalization and model-cache invalidation.
  Keep API keys out of logs, screenshots, test fixtures, and committed docs.
- Prefer constructor injection with Hilt. Add Hilt modules only when constructor
  injection is not enough, such as providing Room database instances.

## Kotlin And Compose Conventions

- Follow Kotlin official style; this repo sets `kotlin.code.style=official`.
- Keep package paths aligned with `io.github.initrc.chatbot`.
- Use the version catalog in `gradle/libs.versions.toml` for dependency and plugin
  changes.
- Use `suspend` functions for one-shot async work and `Flow` for observable data.
- Preserve coroutine cancellation by rethrowing `CancellationException`.
- Keep Compose components small enough to preview and test mentally. Add previews
  for meaningful standalone UI states when changing visual behavior.
- Keep chat UI split by responsibility: `ChatScreen.kt` wires ViewModels and
  route-level side effects, while content, model selection, messages, and composer
  controls live in sibling files under `ui/chat`.
- Prefer Material 3 components and the existing theme in `ui/theme`.
- Do not introduce broad navigation, styling, dependency, or schema refactors
  unless the task requires them.

## Data And Persistence Notes

- Room schema version is currently `1` and `exportSchema=false`.
- Conversation deletion should cascade to messages through the existing Room
  relationship.
- Store chat roles and message statuses as stable strings through the existing
  enum helpers.
- Do not persist the global system prompt as a visible message; it is injected
  when constructing the remote chat request.
- When changing chat history behavior, check both the DAO contract and
  `ChatHistoryLocalDataSource` summary/title update logic.

## Testing Guidance

- Add or update JVM tests for pure logic, especially context compression,
  request-shaping logic, mappers, and repository behavior that can be isolated.
- Add Room-focused tests when changing DAO queries, entity mappings, or persistence
  invariants.
- Use instrumentation tests only for Android framework or Compose behavior that
  cannot be covered by JVM tests.
- Avoid tests that require real API keys or external network calls.

## Working Safely

- Keep edits scoped to the requested behavior and nearby code.
- Do not rewrite generated Gradle wrapper files or Android project metadata unless
  the task explicitly needs it.
- Do not commit secrets. Treat stored API keys and base URLs as user data.
- If the worktree already has unrelated changes, leave them alone.
