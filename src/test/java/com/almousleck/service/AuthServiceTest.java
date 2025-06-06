package com.almousleck.service;

import com.almousleck.config.AppProperties;
import com.almousleck.dto.auth.*;
import com.almousleck.exception.BadRequestException;
import com.almousleck.model.OtpVerification;
import com.almousleck.model.Role;
import com.almousleck.model.User;
import com.almousleck.repository.OtpVerificationRepository;
import com.almousleck.repository.RoleRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.security.JwtTokenProvider;
import com.almousleck.dto.auth.JwtAuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "app.jwt.expiration=86400000",
        "app.jwt.refresh-expiration=604800000",
        "app.otp.expiration=300"
})
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private EmailService emailService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.Jwt jwtProperties;

    @Mock
    private AppProperties.Otp otpProperties;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role userRole;
    private LoginRequest loginRequest;
    private SignupRequest signupRequest;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .id(1L)
                .name(Role.RoleName.ROLE_USER)
                .build();

        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .username("johndoe")
                .email("john@example.com")
                .password("encodedPassword")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("johndoe");
        loginRequest.setPassword("password123");

        signupRequest = new SignupRequest();
        signupRequest.setName("Jane Doe");
        signupRequest.setUsername("janedoe");
        signupRequest.setEmail("jane@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setPhoneNumber("+1234567890");
    }

//    @Test
//    void whenAuthenticateUser_withValidCredentials_thenReturnJwtResponse() {
//        // Given
//        Authentication authentication = mock(Authentication.class);
//        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                .thenReturn(authentication);
//        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");
//        when(tokenProvider.generateRefreshToken(authentication)).thenReturn("refresh-token");
//
//        // When
//        JwtAuthResponse response = authService.authenticateUser(loginRequest);
//
//        // Then
//        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
//        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
//        assertThat(response.getTokenType()).isEqualTo("Bearer");
//        assertThat(response.getExpiresIn()).isEqualTo(86400000L);
//
//        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
//        verify(tokenProvider).generateToken(authentication);
//        verify(tokenProvider).generateRefreshToken(authentication);
//    }

    @Test
    void whenRegisterUser_withValidData_thenCreateUser() {
        // Given
        when(userRepository.existsByUsername("janedoe")).thenReturn(false);
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(appProperties.getOtp()).thenReturn(otpProperties);
        when(otpProperties.getExpiration()).thenReturn(900); // Changed from 900L to 900 (int)

        // When
        authService.registerUser(signupRequest);

        // Then
        verify(userRepository).existsByUsername("janedoe");
        verify(userRepository).existsByEmail("jane@example.com");
        verify(userRepository).save(any(User.class));
        verify(otpVerificationRepository).save(any(OtpVerification.class));
        verify(emailService).sendRegistrationOtp(eq("jane@example.com"), eq("Jane Doe"), anyString());
    }

    @Test
    void whenRegisterUser_withExistingUsername_thenThrowException() {
        // Given
        when(userRepository.existsByUsername("janedoe")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(signupRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username is already taken");

        verify(userRepository).existsByUsername("janedoe");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenVerifyRegistrationOtp_withInvalidOtp_thenThrowException() {
        // Given
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("wrong-otp");

        when(otpVerificationRepository.findByEmailAndOtpAndTypeAndUsedFalse(
                "john@example.com", "wrong-otp", OtpVerification.OtpType.REGISTRATION))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.verifyRegistrationOtp(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    void whenVerifyRegistrationOtp_withExpiredOtp_thenThrowException() {
        // Given
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        OtpVerification expiredOtp = OtpVerification.builder()
                .email("john@example.com")
                .otp("123456")
                .type(OtpVerification.OtpType.REGISTRATION)
                .expiryDate(LocalDateTime.now().minusMinutes(1)) // Expired
                .build();

        when(otpVerificationRepository.findByEmailAndOtpAndTypeAndUsedFalse(
                "john@example.com", "123456", OtpVerification.OtpType.REGISTRATION))
                .thenReturn(Optional.of(expiredOtp));

        // When & Then
        assertThatThrownBy(() -> authService.verifyRegistrationOtp(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OTP has expired");
    }
}