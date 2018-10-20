package com.github.gilbertotcc.lifx.impl;

import static java.util.function.Predicate.isEqual;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.gilbertotcc.lifx.LifxClient;
import com.github.gilbertotcc.lifx.api.LifxApi;
import com.github.gilbertotcc.lifx.exception.LifxRemoteException;
import com.github.gilbertotcc.lifx.models.Light;
import com.github.gilbertotcc.lifx.models.LightsSelector;
import com.github.gilbertotcc.lifx.models.LightsState;
import com.github.gilbertotcc.lifx.models.LightsStates;
import com.github.gilbertotcc.lifx.models.OperationResult;
import com.github.gilbertotcc.lifx.models.Result;
import com.github.gilbertotcc.lifx.models.Results;
import com.github.gilbertotcc.lifx.models.State;
import com.github.gilbertotcc.lifx.models.converter.LightsSelectorConverter;
import com.github.gilbertotcc.lifx.models.converter.StringConverterFactory;
import com.github.gilbertotcc.lifx.util.JacksonUtils;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class LifxClientImpl implements LifxClient {

    private static final String LIFX_BASE_URL = "https://api.lifx.com";

    private static final Logger LOG = Logger.getLogger(LifxClientImpl.class.getName());

    private final LifxApi lifxApi;

    // Mainly for testing purposes
    private LifxClientImpl(final LifxApi lifxApi) {
        this.lifxApi = lifxApi;
    }

    // Mainly for testing purposes
    static LifxClientImpl createNewClientFor(final String baseUrl, final String accessToken) {
        Objects.requireNonNull(baseUrl, "baseUrl == null");
        Objects.requireNonNull(accessToken, "accessToken == null");

        final OkHttpClient okHttpClient = LifxOkHttpClientFactory.INSTANCE
                .getOkHttpClient(accessToken, LOG::info, isLoggingVerbose());
        final LifxApi lifxApi = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(StringConverterFactory.of(LightsSelector.class, new LightsSelectorConverter()))
                .addConverterFactory(JacksonConverterFactory.create(JacksonUtils.OBJECT_MAPPER))
                .client(okHttpClient)
                .build()
                .create(LifxApi.class);
        return new LifxClientImpl(lifxApi);
    }

    public static LifxClientImpl createNewClientFor(final String accessToken) {
        return createNewClientFor(LIFX_BASE_URL, accessToken);
    }

    @Override
    public List<Light> listLights(final LightsSelector lightsSelector) {
        LOG.info(() -> String.format("List lights (selector: %s)", lightsSelector.getIdentifier()));
        return executeAndGetBody(lifxApi.listLights(lightsSelector));
    }

    @Override
    public List<Result> setLightsState(final LightsSelector lightsSelector, final State state) {
        LOG.info(() -> String.format("Set lights state of %s to %s", lightsSelector.getIdentifier(), ReflectionToStringBuilder.toString(state, ToStringStyle.JSON_STYLE)));
        final Results<Result> results = executeAndGetBody(lifxApi.setLightsState(lightsSelector, state));
        return results.getResults();
    }

    @Override
    public List<OperationResult> setLightsStates(final LightsStates lightsStates) {
        LOG.info(() -> String.format("Set lights states of %s", lightsSelectorListOf(lightsStates)));
        final Results<OperationResult> operationResults = executeAndGetBody(lifxApi.setLightStates(lightsStates));
        return operationResults.getResults();
    }

    private static <T> T executeAndGetBody(final Call<T> call) {
        try {
            final Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            }
            throw LifxRemoteException.of(response);
        } catch (IOException e) {
            throw new LifxRemoteException("Error occurred while calling LIFX HTTP API", e);
        }
    }

    private static boolean isLoggingVerbose() {
        return Stream.of(Level.FINE, Level.FINER, Level.FINEST, Level.ALL).anyMatch(isEqual(LOG.getLevel()));
    }

    private static String lightsSelectorListOf(final LightsStates lightsStates) {
        return lightsStates.getLightsStates().stream()
                .map(LightsState::getLightsSelector)
                .map(LightsSelector::getIdentifier)
                .collect(Collectors.joining(","));
    }
}