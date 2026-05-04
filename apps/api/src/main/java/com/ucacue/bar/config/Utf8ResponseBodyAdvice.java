package com.ucacue.bar.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class Utf8ResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        boolean isAttachment = response.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION);
        boolean isBinaryBody = body instanceof byte[];
        boolean isJsonLike = selectedContentType == null
                || MediaType.APPLICATION_JSON.includes(selectedContentType)
                || (selectedContentType.getSubtype() != null && selectedContentType.getSubtype().contains("json"));

        if (isAttachment || isBinaryBody || !isJsonLike) {
            return body;
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Content-Type", "application/json; charset=UTF-8");
        return body;
    }
}
