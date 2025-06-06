package com.almousleck.controller;

import com.almousleck.dto.auth.*;
import com.almousleck.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private SignupRequest signupRequest;
    private JwtAuthResponse jwtAuthResponse;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");

        signupRequest = new SignupRequest();
        signupRequest.setName("Test User");
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setPhoneNumber("+1234567890");

        jwtAuthResponse = JwtAuthResponse.builder()
                .accessToken("jwt-access-token")
                .refreshToken("jwt-refresh-token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .build();
    }

    @Test
    void whenLogin_withValidCredentials_thenReturnJwtResponse() throws Exception {
        // Given
        when(authService.authenticateUser(any(LoginRequest.class))).thenReturn(jwtAuthResponse);

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("jwt-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400));

        verify(authService).authenticateUser(any(LoginRequest.class));
    }

    @Test
    void whenLogin_withInvalidData_thenReturnBadRequest() throws Exception {
        // Given
        LoginRequest invalidRequest = new LoginRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).authenticateUser(any());
    }

    @Test
    void whenSignup_withValidData_thenReturnCreated() throws Exception {
        // Given
        doNothing().when(authService).registerUser(any(SignupRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully. Please verify your email."));

        verify(authService).registerUser(any(SignupRequest.class));
    }

    @Test
    void whenSignup_withInvalidEmail_thenReturnBadRequest() throws Exception {
        // Given
        signupRequest.setEmail("invalid-email");

        // When & Then
        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void whenVerifyOtp_withValidData_thenReturnSuccess() throws Exception {
        // Given
        VerifyOtpRequest verifyRequest = new VerifyOtpRequest();
        verifyRequest.setEmail("test@example.com");
        verifyRequest.setOtp("123456");

        doNothing().when(authService).verifyRegistrationOtp(any(VerifyOtpRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/verify-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        verify(authService).verifyRegistrationOtp(any(VerifyOtpRequest.class));
    }

    @Test
    void whenForgotPassword_withValidEmail_thenReturnSuccess() throws Exception {
        // Given
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest();
        forgotRequest.setEmail("test@example.com");

        doNothing().when(authService).requestPasswordReset(any(ForgotPasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset OTP sent to your email"));

        verify(authService).requestPasswordReset(any(ForgotPasswordRequest.class));
    }
}