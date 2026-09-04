package com.finance.manager.exception;

import com.finance.manager.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationExceptions() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "must not be blank");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("must not be blank", response.getBody().getMessage());
    }

    @Test
    void handleValidationExceptions_emptyErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation error", response.getBody().getMessage());
    }

    @Test
    void handleConstraintViolation() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Invalid value");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid value", response.getBody().getMessage());
    }

    @Test
    void handleConstraintViolation_empty() {
        ConstraintViolationException ex = new ConstraintViolationException(Collections.emptySet());

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation error", response.getBody().getMessage());
    }

    @Test
    void handleBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(new BadRequestException("Bad request error"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad request error", response.getBody().getMessage());

        ResponseEntity<ErrorResponse> methodNotSupported = handler.handleBadRequest(new HttpRequestMethodNotSupportedException("POST"));
        assertEquals(HttpStatus.BAD_REQUEST, methodNotSupported.getStatusCode());
    }

    @Test
    void handleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Malformed JSON");
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request payload or invalid data format", response.getBody().getMessage());

        HttpMessageNotReadableException exDeserialize = new HttpMessageNotReadableException("Cannot deserialize value of type");
        ResponseEntity<ErrorResponse> responseDeserialize = handler.handleHttpMessageNotReadable(exDeserialize);
        assertEquals(HttpStatus.BAD_REQUEST, responseDeserialize.getStatusCode());
        assertEquals("Invalid value provided for field in request body", responseDeserialize.getBody().getMessage());
    }

    @Test
    void handleUnauthorized() {
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(new BadCredentialsException("Bad creds"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Bad creds", response.getBody().getMessage());
    }

    @Test
    void handleForbidden() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(new AccessDeniedException("Access denied"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access denied", response.getBody().getMessage());
    }

    @Test
    void handleNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(new ResourceNotFoundException("Not found"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not found", response.getBody().getMessage());
    }

    @Test
    void handleConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(new UserAlreadyExistsException("User exists"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("User exists", response.getBody().getMessage());
    }
}
