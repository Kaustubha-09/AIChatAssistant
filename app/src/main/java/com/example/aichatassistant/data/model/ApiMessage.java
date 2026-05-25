package com.example.aichatassistant.data.model;

import com.google.gson.annotations.SerializedName;

/** DTO for a single message entry in the OpenAI messages array. */
public class ApiMessage {

    @SerializedName("role")
    public String role;

    @SerializedName("content")
    public String content;

    public ApiMessage(String role, String content) {
        this.role    = role;
        this.content = content;
    }

    public static ApiMessage system(String content)    { return new ApiMessage("system",    content); }
    public static ApiMessage user(String content)      { return new ApiMessage("user",      content); }
    public static ApiMessage assistant(String content) { return new ApiMessage("assistant", content); }
}
