package com.lory.testapp;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;

public class RequestController {
    private static final String BASE_URL = "https://gist.githubusercontent.com";
    private static RequestController mRequestController;

    private Retrofit mRetrofit;

    private RequestController() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.level(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .build();

        mRetrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .build();
    }

    public static RequestController getInstance() {
        if (mRequestController == null) {
            mRequestController = new RequestController();
        }
        return mRequestController;
    }

    public GeoRequest getGeoRequest() {
        return mRetrofit.create(GeoRequest.class);
    }
}

interface GeoRequest {
    @GET("/NazarKacharaba/6dfe9ca20ce95f73c6796384d2553a88/raw/25894bd730700f173147aaa26e947e0371ab7bff/geocoordinates")
    Call<ResponseBody> getCoordinates();
}
