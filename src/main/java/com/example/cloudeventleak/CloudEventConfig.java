package com.example.cloudeventleak;

import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class CloudEventConfig {

    @Bean
    @Order(0)
    public CodecCustomizer cloudEventCustomizer() {
        return configurer -> {
            final var customCodecs = configurer.customCodecs();
            customCodecs.register(new CloudEventHttpMessageReader());
            customCodecs.register(new CloudEventHttpMessageWriter());
        };
    }

}
