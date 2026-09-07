package com.petclinic.api.config;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

public class HttpLoggingFilter implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        logRequest(requestSpec);
        Response response = ctx.next(requestSpec, responseSpec);
        logResponse(response);
        return response;
    }

    private static void logRequest(FilterableRequestSpecification request) {
        StringBuilder out = new StringBuilder();
        out.append("Request method:\t").append(request.getMethod()).append('\n');
        out.append("Request URI:\t").append(request.getURI()).append('\n');
        out.append("Headers:\t\t").append(request.getHeaders()).append('\n');
        out.append("Body:");
        Object body = request.getBody();
        if (body == null || String.valueOf(body).isBlank()) {
            out.append("\t\t\t<none>\n");
        } else {
            out.append('\n').append(body).append('\n');
        }
        System.out.print(out);
    }

    private static void logResponse(Response response) {
        StringBuilder out = new StringBuilder();
        out.append(response.getStatusLine()).append('\n');
        for (Header header : response.getHeaders()) {
            out.append(header.getName()).append(": ").append(header.getValue()).append('\n');
        }
        String body = readBody(response);
        out.append('\n');
        if (body.isBlank()) {
            out.append("Body:\t\t\t<none>\n");
        } else {
            out.append("Body:\n").append(body).append('\n');
        }
        System.out.print(out);
    }

    private static String readBody(Response response) {
        try {
            String body = response.getBody().asPrettyString();
            return body == null ? "" : body.trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
