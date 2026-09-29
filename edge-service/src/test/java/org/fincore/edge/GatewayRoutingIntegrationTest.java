package org.fincore.edge;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.gateway.routes[0].id=test-route",
                "spring.cloud.gateway.routes[0].uri=${test.downstream.uri}",
                "spring.cloud.gateway.routes[0].predicates[0]=Path=/test/**"
        }
)
class GatewayRoutingIntegrationTest {

    private static MockWebServer downstream;

    @LocalServerPort
    private int gatewayPort;

    @BeforeAll
    static void startDownstream() throws IOException {
        downstream = new MockWebServer();
        downstream.start();
    }

    @AfterAll
    static void stopDownstream() throws IOException {
        downstream.shutdown();
    }

    @DynamicPropertySource
    static void configureDownstream(DynamicPropertyRegistry registry) {
        registry.add(
                "test.downstream.uri",
                () -> downstream.url("/").toString()
        );
    }

    @Test
    void shouldRouteRequestToDownstreamService() {
        downstream.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("downstream-ok"));

        String response = WebClient.create()
                .get()
                .uri("http://localhost:" + gatewayPort + "/test/resource")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertEquals("downstream-ok", response);
    }
}