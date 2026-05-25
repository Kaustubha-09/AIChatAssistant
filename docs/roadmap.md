# Roadmap

Phased plan. Each phase is shippable on its own.

## Phase 1 — Conversation list (1–2 weeks)

- Add a `chats` table in Room: `(id, title, created_at, model)`.
- New `ConversationListFragment` with a navigation drawer (Material 3 `ModalNavigationDrawer`).
- Move from single-Activity / single-Fragment to single-Activity / Jetpack Navigation with two fragments.
- Chat title auto-generated from first user message (truncated at 40 chars).

## Phase 2 — Markdown rendering (3–5 days)

- Add Markwon (`io.noties.markwon`) for assistant bubbles.
- Render fenced code blocks with monospace font + horizontal-scroll if long.
- Inline code, bold, italic, links.
- Selective rendering: only assistant bubbles; user bubbles stay plain text.

## Phase 3 — Encrypted API key storage (2–3 days)

- Replace `SharedPreferences` for the API key with `EncryptedSharedPreferences` (AndroidX Security).
- Master key in the Android Keystore.
- Migration path: read from plain `SharedPreferences` once, write to encrypted, delete plain.

## Phase 4 — Vision input (1 week)

- Image picker (Photos / Camera) for user-side multimodal turn.
- Send image as base64 in the multimodal `messages[].content` array.
- Switch model to `gpt-4o` automatically for image turns (the default `gpt-4o-mini` is text-only at lower cost).

## Phase 5 — Tablet layout (1 week)

- `sw600dp` resource bucket for the dual-pane layout: ConversationList on the left, ChatFragment on the right.
- Navigation drawer becomes a permanent side panel above the breakpoint.

## Phase 6 — Light + dark themes (3–5 days)

- `Theme.Material3.Dark.NoActionBar` with mirrored color palette.
- System-follow / light / dark setting in Settings bottom sheet.
- Persist via `AppCompatDelegate.setDefaultNightMode`.

## Phase 7 — Resilience hardening (1 week)

- Request cancellation on `ViewModel.onCleared()` — stop in-flight SSE stream when Fragment is destroyed.
- Retry on transient failures (timeout, 503) with exponential backoff + jitter.
- `Retry-After`-aware backoff on 429.

## Phase 8 — Tests + CI

- Unit-test coverage for `PromptBuilder`, `Resource<T>` mapping, `MockStreamingDataSource` token emission.
- Espresso UI tests for: empty state → send → streaming indicator → final bubble; clear chat; settings bottom sheet.
- The GitHub Actions workflow added in this commit will run them on every push.

## Phase 9 — Export / share (3 days)

- Export conversation as Markdown (`*.md`).
- Share intent into other apps.

## Long-term

- **Conversation search.** Index Room with FTS for free-text search across chats.
- **Voice input.** `RecognizerIntent` for speech-to-text on the input field.
- **Plugins / tool use.** Wire up OpenAI function-calling so the assistant can invoke calculator, weather, search.

## Out of scope

- **Building a competitive consumer chat app.** This is a developer-tool / portfolio surface, not a ChatGPT alternative.
- **Multi-provider switching in one chat.** The model swap is global, not per-message. Mixing GPT-4o + Claude responses in one thread is a different UX.
