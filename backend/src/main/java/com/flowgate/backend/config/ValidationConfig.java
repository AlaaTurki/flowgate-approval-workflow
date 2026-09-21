package com.flowgate.backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Register a Jackson module that trims all incoming JSON string values and converts blank -> null.
 * This ensures "blank after trim counts as missing" behavior for @NotBlank and optional fields.
 */
@Configuration
public class ValidationConfig {

    public static class TrimStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String val = p.getValueAsString();
            if (val == null) return null;
            String t = val.trim();
            return t.isEmpty() ? null : t;
        }
    }

    @Bean
    public Module stringTrimmingModule() {
        SimpleModule m = new SimpleModule();
        m.addDeserializer(String.class, new TrimStringDeserializer());
        return m;
    }
}
