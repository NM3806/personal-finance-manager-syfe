package com.finance.manager.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthDtoTest {

    @Test
    void testRegisterResponse() {
        RegisterResponse response = new RegisterResponse();
        response.setMessage("Registered");
        response.setUserId(42L);

        assertEquals("Registered", response.getMessage());
        assertEquals(42L, response.getUserId());

        RegisterResponse parameterized = new RegisterResponse("Success", 100L);
        assertEquals("Success", parameterized.getMessage());
        assertEquals(100L, parameterized.getUserId());
    }

    @Test
    void testErrorResponse() {
        ErrorResponse response = new ErrorResponse();
        response.setMessage("Error occurred");

        assertEquals("Error occurred", response.getMessage());

        ErrorResponse parameterized = new ErrorResponse("Something failed");
        assertEquals("Something failed", parameterized.getMessage());
    }

    @Test
    void testMessageResponse() {
        MessageResponse response = new MessageResponse();
        response.setMessage("Success message");

        assertEquals("Success message", response.getMessage());

        MessageResponse parameterized = new MessageResponse("Done");
        assertEquals("Done", parameterized.getMessage());
    }

    @Test
    void testRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("test@example.com");
        request.setPassword("password123");
        request.setFullName("John Doe");
        request.setPhoneNumber("+1234567890");

        assertEquals("test@example.com", request.getUsername());
        assertEquals("password123", request.getPassword());
        assertEquals("John Doe", request.getFullName());
        assertEquals("+1234567890", request.getPhoneNumber());
    }

    @Test
    void testLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("test@example.com");
        request.setPassword("password123");

        assertEquals("test@example.com", request.getUsername());
        assertEquals("password123", request.getPassword());
    }
}
