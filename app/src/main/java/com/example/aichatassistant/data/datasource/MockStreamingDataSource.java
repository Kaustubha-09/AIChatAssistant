package com.example.aichatassistant.data.datasource;

import android.os.Handler;
import android.os.Looper;

import com.example.aichatassistant.common.AppConfig;
import com.example.aichatassistant.domain.model.ChatMessage;
import com.example.aichatassistant.domain.repository.ChatRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Simulates a streaming AI response for development and demo use.
 *
 * Implementation:
 *   - Picks a contextually appropriate canned response based on keywords in the last user message.
 *   - Posts each word-group to the main thread with MOCK_TOKEN_DELAY_MS between them,
 *     creating the appearance of live token streaming.
 *   - Tracks all pending Runnables so streaming can be cancelled cleanly at any point.
 *
 * All callbacks are delivered on the main thread — identical contract to RemoteChatDataSource.
 */
public class MockStreamingDataSource {

    private final Handler      handler = new Handler(Looper.getMainLooper());
    private final List<Runnable> pending = new ArrayList<>();
    private volatile boolean   cancelled = false;

    // -----------------------------------------------------------------------
    // Canned responses — extend / localise as desired
    // -----------------------------------------------------------------------

    private static final String[] GREETING_RESPONSES = {
        "Hello! I'm your AI assistant. I can help you with questions, analysis, writing, code, math, and much more. What would you like to explore today?",
        "Hi there! Great to meet you. I'm here and ready to help with anything you'd like to discuss or work on. What's on your mind?",
    };

    private static final String[] CODE_RESPONSES = {
        "Sure! Here's a clean Java implementation:\n\n```java\npublic int fibonacci(int n) {\n    if (n <= 1) return n;\n    int a = 0, b = 1;\n    for (int i = 2; i <= n; i++) {\n        int tmp = a + b;\n        a = b;\n        b = tmp;\n    }\n    return b;\n}\n```\n\nThis iterative approach runs in O(n) time and O(1) space — much better than naive recursion. Would you like me to add memoization or extend it further?",
        "Great question! Here's how you'd approach this in Java:\n\n```java\n// HashMap for O(1) average-case lookups\nMap<String, Integer> cache = new HashMap<>();\ncache.put(\"key\", 42);\nint value = cache.getOrDefault(\"key\", 0);\n```\n\nFor thread-safe access, swap `HashMap` for `ConcurrentHashMap`. Would you like me to expand on any part of this?",
    };

    private static final String[] EXPLAIN_RESPONSES = {
        "Great question! Let me break it down.\n\nThe core idea is that complex problems become manageable when divided into smaller, well-defined sub-problems. This principle — decomposition — is fundamental to both software engineering and general problem-solving.\n\nAt a high level:\n\n1. **Identify** the inputs and desired outputs.\n2. **Decompose** the problem into independent sub-tasks.\n3. **Solve** each sub-task in isolation.\n4. **Compose** the results.\n\nIs there a specific aspect you'd like me to dive deeper into?",
        "Absolutely, happy to explain.\n\nThe most important thing to understand is the underlying mental model driving the behaviour. Once you have that model, the specific steps become much clearer and more memorable.\n\nHere are the three layers to think about:\n\n- **What** it does (observable behaviour)\n- **How** it does it (mechanism)\n- **Why** it does it that way (design intent)\n\nWould you like a concrete example?",
    };

    private static final String[] GENERAL_RESPONSES = {
        "That's an interesting question! There are a few angles worth exploring here.\n\nFrom a practical standpoint, the most important thing is to start with a working solution and iterate — perfect is the enemy of done. From a design standpoint, always ask: what are the trade-offs?\n\nEvery decision in software involves balancing speed, correctness, and maintainability. The right balance depends entirely on context. What's the use case you're working on?",
        "Good question. Let me think through this carefully.\n\nThere are really two ways to approach this problem:\n\n**Option A** — Simpler, works for most cases, easier to reason about.\n**Option B** — More flexible, handles edge cases, but adds complexity.\n\nFor most projects I'd start with Option A and only move to Option B when a concrete requirement forces it. Premature complexity is one of the most common engineering pitfalls.\n\nWhat constraints are you working within?",
        "I'd be happy to help with that!\n\nThe key insight here is that the best solution is almost always the simplest one that meets your actual requirements — not the theoretical maximum-flexibility design.\n\nStart by answering: what problem am I *actually* solving right now, versus what might I need in six months? Design for now, architect for extensibility.\n\nWould you like to walk through a concrete example together?",
        "That's a thoughtful question. Here's my perspective.\n\nThe challenge with this class of problems is that there's rarely one right answer — it depends on your constraints, your team's familiarity, and your operational environment.\n\nThat said, there are some heuristics that serve well:\n\n- **Correctness first** — a fast wrong answer is worse than a slow right one.\n- **Readability second** — code is read far more than it's written.\n- **Performance third** — optimise only where you have measured evidence of a bottleneck.\n\nDoes that framing help? Happy to go deeper on any of these.",
    };

    private static final String[] ERROR_RESPONSES = {
        "I'm sorry, I didn't quite catch that. Could you rephrase or give me a bit more context? I want to make sure I give you an accurate and useful response.",
        "Hmm, that's a nuanced one. Could you share a bit more about the specific scenario you have in mind? I'd rather give you a careful answer than a quick one.",
    };

    // -----------------------------------------------------------------------

    /**
     * Start streaming a mock response for the given conversation history.
     * Words are posted to the main thread at {@link AppConfig#MOCK_TOKEN_DELAY_MS} intervals.
     */
    public void streamResponse(List<ChatMessage> history,
                               ChatRepository.StreamingCallback callback) {
        cancelled = false;
        for (Runnable r : pending) handler.removeCallbacks(r);
        pending.clear();

        String response = pickResponse(history);
        // Split preserving spaces so the receiver sees natural word boundaries
        String[] tokens = response.split("(?<= )");
        StringBuilder accumulated = new StringBuilder();

        for (int i = 0; i < tokens.length; i++) {
            final int    idx   = i;
            final String token = tokens[i];

            Runnable r = () -> {
                if (cancelled) return;
                accumulated.append(token);
                callback.onToken(token);
                if (idx == tokens.length - 1) {
                    callback.onComplete(accumulated.toString());
                }
            };
            pending.add(r);
            handler.postDelayed(r, (long) idx * AppConfig.MOCK_TOKEN_DELAY_MS);
        }
    }

    /** Cancel all pending streaming Runnables immediately. */
    public void cancel() {
        cancelled = true;
        for (Runnable r : pending) handler.removeCallbacks(r);
        pending.clear();
    }

    // -----------------------------------------------------------------------
    // Keyword routing — lightweight but good enough for demo and unit testing
    // -----------------------------------------------------------------------

    private String pickResponse(List<ChatMessage> history) {
        if (history.isEmpty()) return GENERAL_RESPONSES[0];

        String lastUserMsg = "";
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).isUser()) {
                lastUserMsg = history.get(i).getContent().toLowerCase(Locale.getDefault());
                break;
            }
        }

        if (lastUserMsg.isEmpty()) return GENERAL_RESPONSES[0];

        int seed = history.size(); // vary the pick with conversation length

        if (containsAny(lastUserMsg, "hello", "hi ", "hey", "greet", "good morning", "good evening", "howdy")) {
            return pickFrom(GREETING_RESPONSES, seed);
        }
        if (containsAny(lastUserMsg, "code", "function", "java", "kotlin", "python", "algorithm",
                "implement", "write a", "snippet", "class ", "method", "loop", "array", "list")) {
            return pickFrom(CODE_RESPONSES, seed);
        }
        if (containsAny(lastUserMsg, "explain", "what is", "how does", "why does", "what are", "describe", "define")) {
            return pickFrom(EXPLAIN_RESPONSES, seed);
        }
        if (containsAny(lastUserMsg, "sorry", "wrong", "mistake", "didn't understand", "unclear")) {
            return pickFrom(ERROR_RESPONSES, seed);
        }
        return pickFrom(GENERAL_RESPONSES, seed);
    }

    private String pickFrom(String[] arr, int seed) {
        return arr[Math.abs(seed) % arr.length];
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
