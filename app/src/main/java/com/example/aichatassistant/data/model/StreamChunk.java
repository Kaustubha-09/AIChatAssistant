package com.example.aichatassistant.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * A single SSE event payload from the streaming endpoint.
 *
 * Wire format (one line per event):
 *   data: {"choices":[{"delta":{"content":"Hello"},"finish_reason":null}]}
 */
public class StreamChunk {

    @SerializedName("choices")
    public List<Choice> choices;

    public static class Choice {
        @SerializedName("delta")
        public Delta delta;

        @SerializedName("finish_reason")
        public String finishReason;
    }

    public static class Delta {
        @SerializedName("content")
        public String content;
    }

    /** Extracts the token from this chunk, or null if there is none. */
    public String token() {
        if (choices == null || choices.isEmpty()) return null;
        Choice c = choices.get(0);
        if (c.delta == null) return null;
        return c.delta.content;
    }
}
