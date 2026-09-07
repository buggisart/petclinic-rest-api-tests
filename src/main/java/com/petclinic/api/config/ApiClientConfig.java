package com.petclinic.api.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiClientConfig {

    @Bean
    RequestSpecification requestSpec(PetclinicProperties properties, ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        RestAssured.config = RestAssuredConfig.config()
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> mapper));

        AllureRestAssured allureFilter = new AllureRestAssured()
                .setRequestAttachmentName("Request")
                .setResponseAttachmentName("Response");

        return new RequestSpecBuilder()
                .setBaseUri(properties.baseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new HttpLoggingFilter())
                .addFilter(allureFilter)
                .build();
    }
}
