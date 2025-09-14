package it.bentzen.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bentzen.client.ExternalSpotprisAPI;
import it.bentzen.resources.SpotprisResource;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SpotprisScheduler {
    private final Map<String, Double> data = new ConcurrentHashMap<>();

    @Inject
    @RestClient
    ExternalSpotprisAPI externalAPI;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SpotprisResource spotprisResource;

    @Scheduled(cron = "0 0 14 * * ?")
    void updateDaily() throws JsonProcessingException {
        String latest = externalAPI.getSpotPrices();
        data.putAll(objectMapper.readValue(latest, new TypeReference<Map<String, Double>>() {}));
        spotprisResource.receiveJson(data);
    }
}

