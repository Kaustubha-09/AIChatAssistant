package com.example.aichatassistant.data.api;

import com.example.aichatassistant.data.model.ChatRequest;
import com.example.aichatassistant.data.model.ChatResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit interface — used only for the non-streaming (single-shot) request path.
 * Streaming is handled directly via raw OkHttp in RemoteChatDataSource to allow
 * line-by-line SSE parsing without a separate okhttp-sse artifact.
 */
public interface ChatApiService {

    @POST("v1/chat/completions")
    Call<ChatResponse> sendMessage(@Body ChatRequest request);
}
