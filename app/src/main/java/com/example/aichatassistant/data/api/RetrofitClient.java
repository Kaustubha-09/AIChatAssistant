package com.example.aichatassistant.data.api;

import com.example.aichatassistant.common.AppConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * Provides a singleton OkHttpClient and Retrofit instance.
 * The OkHttpClient is reused by RemoteChatDataSource for raw streaming calls
 * so we don't create redundant connection pools.
 */
public class RetrofitClient {

    private static RetrofitClient instance;

    private final Retrofit       retrofit;
    private final OkHttpClient   okHttpClient;

    private RetrofitClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(AppConfig.READ_TIMEOUT_SECONDS,        TimeUnit.SECONDS)
                .writeTimeout(AppConfig.WRITE_TIMEOUT_SECONDS,      TimeUnit.SECONDS)
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .addHeader("Authorization", "Bearer " + AppConfig.API_KEY)
                                .addHeader("Content-Type",  "application/json")
                                .build()
                ))
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) instance = new RetrofitClient();
        return instance;
    }

    public OkHttpClient  getOkHttpClient() { return okHttpClient; }
    public ChatApiService getApiService()  { return retrofit.create(ChatApiService.class); }
}
