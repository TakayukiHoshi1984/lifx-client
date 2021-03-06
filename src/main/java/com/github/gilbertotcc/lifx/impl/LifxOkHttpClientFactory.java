package com.github.gilbertotcc.lifx.impl;

import static java.lang.String.format;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class LifxOkHttpClientFactory {

  static final LifxOkHttpClientFactory INSTANCE = new LifxOkHttpClientFactory();

  // Shared OkHttpClient prevents OutOfMemoryError under high workloads
  private static final OkHttpClient OK_HTTP_CLIENT_INSTANCE = new OkHttpClient();

  OkHttpClient getOkHttpClient(final String accessToken, final HttpLoggingInterceptor.Logger logger) {
    return OK_HTTP_CLIENT_INSTANCE.newBuilder()
      .addInterceptor(accessTokenInterceptor(accessToken))
      .addInterceptor(loggingInterceptor(logger))
      .build();
  }

  // Define interceptors

  private static Interceptor accessTokenInterceptor(final String accessToken) {
    return chain -> {
      final Request authRequest = chain.request().newBuilder()
        .addHeader("Authorization", format("Bearer %s", accessToken))
        .build();
      return chain.proceed(authRequest);
    };
  }

  private static Interceptor loggingInterceptor(final HttpLoggingInterceptor.Logger logger) {
    HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(logger);
    httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
    return httpLoggingInterceptor;
  }
}
