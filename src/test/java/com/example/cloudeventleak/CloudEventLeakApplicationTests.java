package com.example.cloudeventleak;

import com.example.cloudeventleak.CloudEventController.Person;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.netty.util.ResourceLeakDetector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;

import static com.example.cloudeventleak.CloudEventController.SOURCE;
import static com.example.cloudeventleak.CloudEventController.TYPE;
import static io.cloudevents.core.data.PojoCloudEventData.wrap;
import static io.netty.util.ResourceLeakDetector.Level.PARANOID;
import static java.util.UUID.randomUUID;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Slf4j
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class CloudEventLeakApplicationTests {

    @Autowired
    WebClient.Builder webClientBuilder;

    @Autowired
    ObjectMapper objectMapper;

    @LocalServerPort
    int serverPort;

    @Test
    void callPostEndpoint(final CapturedOutput output) {
        ResourceLeakDetector.setLevel(PARANOID);
        final var webClient = webClientBuilder.baseUrl("http://127.0.0.1:" + serverPort + "/").build();

        while (true) {
            final var person = Person.builder()
                    .firstName(randomUUID().toString())
                    .lastName(randomUUID().toString())
                    .build();

            final var cloudEvent = CloudEventBuilder.v1()
                    .withId(randomUUID().toString())
                    .withSource(SOURCE)
                    .withType(TYPE)
                    .withData(wrap(person, objectMapper::writeValueAsBytes))
                    .build();

            webClient.post().uri("/")
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .header("ce-id", randomUUID().toString())
                    .header("ce-specversion", "1.0")
                    .header("ce-source", "/")
                    .header("ce-type", "urn:request")
                    .body(fromValue(person))
                    .retrieve()
                    .bodyToMono(CloudEvent.class)
                    .block();

            if (output.getOut().contains("LEAK: ByteBuf.release()")) {
                throw new RuntimeException("Buffer leak detected");
            }
        }
    }

}
