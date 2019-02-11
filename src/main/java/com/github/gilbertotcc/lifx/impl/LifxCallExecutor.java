package com.github.gilbertotcc.lifx.impl;

import java.io.IOException;
import java.util.Optional;

import com.github.gilbertotcc.lifx.exception.LifxErrorException;
import com.github.gilbertotcc.lifx.exception.LifxErrorType;
import com.github.gilbertotcc.lifx.exception.LifxCallException;
import com.github.gilbertotcc.lifx.models.Error;
import com.github.gilbertotcc.lifx.util.JacksonUtils;
import lombok.Value;
import retrofit2.Call;
import retrofit2.Response;

@Value(staticConstructor = "of")
class LifxCallExecutor<T> {

  private Call<T> call;

  T getResponse() {
    try {
      final Response<T> response = call.execute();
      if (response.isSuccessful()) {
        return response.body();
      }
      throw lifxErrorExceptionFrom(response);
    } catch (IOException e) {
      throw new LifxCallException(call, e);
    }
  }

  private static LifxErrorException lifxErrorExceptionFrom(final Response<?> response) {
    return Optional.ofNullable(response.errorBody())
      .<Error>map(errorBody -> {
        try {
          return JacksonUtils.OBJECT_MAPPER.readerFor(Error.class).readValue(errorBody.string());
        } catch (IOException e) {
          return null;
        }
      })
      .flatMap(error ->
        LifxErrorType.byHttpCode(response.code())
          .map(errorType -> new LifxErrorException(errorType, error)))
      .orElse(LifxErrorException.GENERIC_LIFX_ERROR);
  }
}
