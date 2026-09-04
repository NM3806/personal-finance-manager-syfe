package com.finance.manager.service;

import com.finance.manager.dto.LoginRequest;
import com.finance.manager.dto.MessageResponse;
import com.finance.manager.dto.RegisterRequest;
import com.finance.manager.dto.RegisterResponse;
import com.finance.manager.entity.User;
import com.finance.manager.exception.InvalidCredentialsException;
import com.finance.manager.exception.UserAlreadyExistsException;
import com.finance.manager.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User", "+1234567890");

        when(userRepository.existsByUsername("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        User savedUser = new User("test@example.com", "hashedPassword", "Test User", "+1234567890");
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        assertEquals(1L, response.getUserId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User", "+1234567890");

        when(userRepository.existsByUsername("test@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        MessageResponse response = authService.login(request, httpRequest, httpResponse);

        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        verify(securityContextRepository).saveContext(any(), any(), any());
    }

    @Test
    void login_invalidCredentials_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpass");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, httpRequest, httpResponse));
    }

    @Test
    void logout_success() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        when(httpRequest.getSession(false)).thenReturn(session);

        MessageResponse response = authService.logout(httpRequest, httpResponse);

        assertNotNull(response);
        assertEquals("Logout successful", response.getMessage());
        verify(session).invalidate();
    }

    @Test
    void getCurrentUser_unauthenticated_throwsInvalidCredentials() {
        assertThrows(InvalidCredentialsException.class, () -> authService.getCurrentUser());
    }

    @Test
    void getCurrentUser_authenticated_returnsUser() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("test@example.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User("test@example.com", "hashed", "Test User", "123456");
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(user));

        User result = authService.getCurrentUser();

        assertNotNull(result);
        assertEquals("test@example.com", result.getUsername());
    }
}
