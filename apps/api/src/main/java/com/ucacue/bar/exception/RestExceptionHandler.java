package com.ucacue.bar.exception;

import com.ucacue.bar.config.RequestTraceFilter;
import com.ucacue.bar.erp.shared.exception.ServiceUnavailableException;
import jakarta.validation.ConstraintViolationException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request, null, "BAD_REQUEST");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request, null, "UNAUTHORIZED");
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request, null, "NOT_FOUND");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "Los datos enviados no son validos",
                request,
                errors,
                "VALIDATION_ERROR");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Constraint Violation",
                "La solicitud contiene restricciones invalidas",
                request,
                errors,
                "CONSTRAINT_VIOLATION");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed Payload",
                "El cuerpo de la solicitud no tiene un formato valido",
                request,
                null,
                "MALFORMED_PAYLOAD");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        Map<String, String> details = Map.of(ex.getName(), "Tipo de dato invalido");
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Type Mismatch",
                "Uno o mas parametros tienen un tipo invalido",
                request,
                details,
                "TYPE_MISMATCH");
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex, WebRequest request) {
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                ex.getMessage(),
                request,
                null,
                "SERVICE_UNAVAILABLE");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Access Denied",
                "No tienes permisos para acceder a este recurso",
                request,
                null,
                "ACCESS_DENIED");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Authentication Failed",
                "Autenticacion requerida o invalida",
                request,
                null,
                "AUTHENTICATION_FAILED");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Bad Credentials",
                "Credenciales invalidas",
                request,
                null,
                "BAD_CREDENTIALS");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                "El metodo HTTP usado no coincide con el contrato del endpoint",
                request,
                null,
                "METHOD_NOT_ALLOWED");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Ha ocurrido un error inesperado. Por favor, intente mas tarde.",
                request,
                null,
                "INTERNAL_ERROR");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status,
                                                        String error,
                                                        String message,
                                                        WebRequest request,
                                                        Map<String, String> validationErrors,
                                                        String code) {
        ErrorResponse payload = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .code(code)
                .traceId(resolveTraceId())
                .path(request.getDescription(false).replace("uri=", ""))
                .validationErrors(validationErrors)
                .build();
        return new ResponseEntity<>(payload, status);
    }

    private String resolveTraceId() {
        String traceId = MDC.get(RequestTraceFilter.TRACE_ID_KEY);
        return traceId != null ? traceId : "n/a";
    }

    @Builder
    @Data
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String code;
        private String traceId;
        private String path;
        private Map<String, String> validationErrors;
    }
}
