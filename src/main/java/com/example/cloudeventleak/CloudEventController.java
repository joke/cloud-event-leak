package com.example.cloudeventleak;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

import static io.cloudevents.core.data.PojoCloudEventData.wrap;
import static io.cloudevents.jackson.PojoCloudEventDataMapper.from;
import static java.net.URI.create;
import static java.util.UUID.randomUUID;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.just;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CloudEventController {

    public static final String TYPE = Person.class.getCanonicalName();
    public static final URI SOURCE = create("urn:person");

    private final ObjectMapper objectMapper;

    @PostMapping("/")
    public Mono<CloudEvent> event(@RequestBody final Mono<CloudEvent> cloudEvent) {
        return cloudEvent
                .mapNotNull(CloudEvent::getData)
                .map(data -> from(objectMapper, Person.class).map(data))
                .map(PojoCloudEventData::getValue)
                .flatMap(this::buildResponseCloudEvent);
    }

    private Mono<CloudEvent> buildResponseCloudEvent(final Person person) {
        return defer(() -> just(
                CloudEventBuilder.v1()
                        .withId(randomUUID().toString())
                        .withSource(SOURCE)
                        .withType(TYPE)
                        .withData(wrap(person, objectMapper::writeValueAsBytes))
                        .build()));
    }

    @Value
    @Builder
    @Jacksonized
    static class Person {
        String firstName;
        String lastName;
    }

}
