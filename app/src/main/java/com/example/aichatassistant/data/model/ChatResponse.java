package com.example.aichatassistant.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Non-streaming response from /v1/chat/completions. */
public class ChatResponse {

    @SerializedName("id")
    public String id;

    @SerializedName("choices")
    public List<Choice> choices;

    public static class Choice {
        @SerializedName("message")
        public ApiMessage message;

        @SerializedName("finish_reason")
        public String finishReason;
    }

    /** Convenience accessor for the assistant's reply text. */
    public String getContent() {
        if (choices != null && !choices.isEmpty() && choices.get(0).message != null) {
            return choices.get(0).message.content;
        }
        return null;
    }
}
