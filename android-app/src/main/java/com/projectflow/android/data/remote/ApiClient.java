package com.projectflow.android.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static volatile ApiService INSTANCE;
    private static final String PREF_NAME = "projectflow_prefs";
    private static final String KEY_BASE_URL = "api_base_url";
    private static final String KEY_TOKEN = "auth_token";
    // Default to 127.0.0.1 for ADB reverse or 10.0.2.2 for emulator
    public static final String DEFAULT_URL = "http://127.0.0.1:8082/api/v1/";

    public static ApiService getService(Context context) {
        if (INSTANCE == null) {
            synchronized (ApiClient.class) {
                if (INSTANCE == null) {
                    INSTANCE = createService(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public static void resetService() {
        INSTANCE = null;
    }

    private static ApiService createService(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_URL);
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            String token = prefs.getString(KEY_TOKEN, "dev-admin-token");
            Request.Builder builder = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json");
            return chain.proceed(builder.build());
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(ApiService.class);
    }
}
