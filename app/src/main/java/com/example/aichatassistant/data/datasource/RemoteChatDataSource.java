package com.example.aichatassistant.data.datasource;

import android.os.Handler;
import android.os.Looper;

import com.example.aichatassistant.common.AppConfig;
import com.example.aichatassistant.data.api.RetrofitClient;
import com.example.aichatassistant.data.model.ApiMessage;
import com.example.aichatassistant.data.model.ChatRequest;
import com.example.aichatassistant.data.model.StreamChunk;
import com.example.aichatassistant.domain.repository.ChatRepository;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

import java.io.IOException;
import java.util.List;

/**
 * Connects to a real OpenAI-compatible LLM API.
 *
 * Streaming implementation:
 *   POST /v1/chat/completions with "stream": true
 *   → response body is an SSE stream of lines starting with "data: "
 *   → read line-by-line via OkHttp BufferedSource (no extra library needed)
 *   → parse JSON delta from each chunk
 *   → deliver tokens to the callback on the main thread via Handler
 *
 * All callbacks are guaranteed to arrive on the Android main thread.
 */
public class RemoteChatDataSource {

    private final OkHttpClient httpClient;
    private final Gson         gson;
    private final Handler      mainHandler;
    private volatile Call      activeCall;

    public RemoteChatDataSource() {
        this.httpClient  = RetrofitClient.getInstance().getOkHttpClient();
        this.gson        = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void streamCompletion(List<ApiMessage> messages,
                                 ChatRepository.StreamingCallback callback) {

        ChatRequest body = new ChatRequest(
                AppConfig.DEFAULT_MODEL,
                messages,
                true,
                AppConfig.MAX_TOKENS,
                AppConfig.TEMPERATURE
        );

        Request request = new Request.Builder()
                .url(AppConfig.BASE_URL + "v1/chat/completions")
                .post(RequestBody.create(gson.toJson(body),
                        MediaType.parse("application/json")))
                .header("Accept", "text/event-stream")
                .build();

        activeCall = httpClient.newCall(request);
        activeCall.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) return;
                String msg = e.getMessage() != null ? e.getMessage() : "Network error";
                mainHandler.post(() -> callback.onError(msg));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (call.isCanceled()) return;

                if (!response.isSuccessful()) {
                    String msg = "Server error: HTTP " + response.code();
                    mainHandler.post(() -> callback.onError(msg));
                    return;
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    mainHandler.post(() -> callback.onError("Empty response body"));
                    return;
                }

                StringBuilder fullContent = new StringBuilder();
                try (BufferedSource source = responseBody.source()) {
                    while (!source.exhausted()) {
                        if (call.isCanceled()) break;

                        String line = source.readUtf8Line();
                        if (line == null) break;
                        if (line.isEmpty()) continue;

                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();

                            if ("[DONE]".equals(data)) {
                                String completed = fullContent.toString();
                                mainHandler.post(() -> callback.onComplete(completed));
                                return;
                            }

                            try {
                                StreamChunk chunk = gson.fromJson(data, StreamChunk.class);
                                String token = chunk.token();
                                if (token != null && !token.isEmpty()) {
                                    fullContent.append(token);
                                    final String t = token;
                                    mainHandler.post(() -> callback.onToken(t));
                                }
                            } catch (JsonSyntaxException ignored) {
                                // Malformed chunk — skip and continue reading
                            }
                        }
                    }
                } catch (IOException e) {
                    if (!call.isCanceled()) {
                        mainHandler.post(() ->
                                callback.onError("Stream interrupted: " + e.getMessage()));
                    }
                }
            }
        });
    }

    public void cancel() {
        if (activeCall != null) {
            activeCall.cancel();
        }
    }
}
