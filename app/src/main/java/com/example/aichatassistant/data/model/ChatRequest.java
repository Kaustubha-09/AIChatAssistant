package com.example.aichatassistant.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Full POST body for /v1/chat/completions. */
public class ChatRequest {

    @SerializedName("model")
    public String model;

    @SerializedName("messages")
    public List<ApiMessage> messages;

    @SerializedName("stream")
    public boolean stream;

    @SerializedName("max_tokens")
    public int maxTokens;

    @SerializedName("temperature")
    public float temperature;

    public ChatRequest(String model, List<ApiMessage> messages,
                       boolean stream, int maxTokens, float temperature) {
        this.model       = model;
        this.messages    = messages;
        this.stream      = stream;
        this.maxTokens   = maxTokens;
        this.temperature = temperature;
    }
}
