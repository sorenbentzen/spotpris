package it.bentzen.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bentzen.client.ExternalSpotprisAPI;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

@Path("/api/spotpris")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SpotprisResource {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @RestClient
    ExternalSpotprisAPI externalAPI;

    private final Map<String, Double> data = new ConcurrentHashMap<>();

    @POST
    public Response receiveJson(Map<String, Double> incoming) {
        data.clear();
        data.putAll(incoming);
        return Response.ok().build();
    }

    @GET
    public Map<String, Double> getData() {
        return data;
    }

    @GET
    @Path("/minimum")
    public Map<String, Double> findMinimum() throws JsonProcessingException {
        if (data.isEmpty()) {
            getSpotprisData();
        }
        String minKey = data.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new NotFoundException("No minimum found."));
        Double minValue = data.get(minKey);
        return Map.of(minKey, minValue);
    }

    @POST
    @Path("/around-minimum/{timer}")
    public Map<String, Double> findAPriserOmkringMinimum(Map<String, Double> priser, @PathParam("timer") int timer) {
        if (data.isEmpty()) {
            try {
                getSpotprisData();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        String minKey = priser.keySet().iterator().next();
        OffsetDateTime minTime = OffsetDateTime.parse(minKey + "Z");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00:00X");
        Map<String, Double> result = new LinkedHashMap<>();
        for (int i = -timer; i <= timer; i++) {
            OffsetDateTime candidate = minTime.plusHours(i);
            String key = candidate.format(formatter);
            if (key.endsWith("Z")) {
                key = key.substring(0, key.length() - 1);
            }
            if (data.containsKey(key)) {
                result.put(key, data.get(key));
            }
        }
        return result;
    }

    @GET
    @Path("/laveste-sum/{heleTimer}")
    public Entry<Double, List<String>> findLavestePrisSumAfNPriserMedTid(Map<String, Double> priser, @PathParam("heleTimer") int heleTimer) {
        List<Entry<String, Double>> entries = new ArrayList<>(priser.entrySet());
        entries.sort(Comparator.comparing(Entry::getValue));

        // Vælg de 'n' laveste priser og deres tidsstempler
        List<Entry<String, Double>> lowestNEntries = entries.subList(0, heleTimer);

        // Beregn summen
        double sum = lowestNEntries.stream().mapToDouble(Entry::getValue).sum();

        // Hent tidsstemplerne og sorter dem kronologisk
        List<String> timestamps = lowestNEntries.stream()
                .map(Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        SimpleEntry<Double, List<String>> doubleListSimpleEntry = new SimpleEntry<>(sum, timestamps);
        System.out.println("Laveste sum af " + heleTimer + " priser: " + doubleListSimpleEntry.getKey());
        System.out.println("Tilsvarende klokkeslæt (sorteret kronologisk): " + doubleListSimpleEntry.getValue());
        return doubleListSimpleEntry;
    }


    @Path("/data")
    @GET
    public Map<String, Double> getSpotprisData() throws JsonProcessingException {
        String latest = externalAPI.getSpotPrices();
        data.clear();
        data.putAll(objectMapper.readValue(latest, new TypeReference<Map<String, Double>>() {
        }));
        return data;
    }

}

