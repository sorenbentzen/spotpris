package it.bentzen.client;

import jakarta.ws.rs.GET;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


@RegisterRestClient(configKey="external-api")
public interface ExternalSpotprisAPI {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getSpotPrices();
}
