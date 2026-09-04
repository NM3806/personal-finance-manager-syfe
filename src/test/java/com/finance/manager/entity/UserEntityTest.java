package com.finance.manager.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserEntityTest {

    @Test
    void testUserEntityGettersAndSetters() {
        User user = new User();
        user.setId(10L);
        user.setUsername("test@example.com");
        user.setPassword("secret");
        user.setFullName("John Doe");
        user.setPhoneNumber("+1234567890");
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);

        assertEquals(10L, user.getId());
        assertEquals("test@example.com", user.getUsername());
        assertEquals("secret", user.getPassword());
        assertEquals("John Doe", user.getFullName());
        assertEquals("+1234567890", user.getPhoneNumber());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    void testUserEntityConstructor() {
        User user = new User("test@example.com", "secret", "John Doe", "+1234567890");
        assertEquals("test@example.com", user.getUsername());
        assertEquals("secret", user.getPassword());
        assertEquals("John Doe", user.getFullName());
        assertEquals("+1234567890", user.getPhoneNumber());
        assertNotNull(user.getCreatedAt());
    }
}
