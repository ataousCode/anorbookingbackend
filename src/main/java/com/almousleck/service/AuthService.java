package com.almousleck.service;

import com.almousleck.config.AppProperties;
import com.almousleck.dto.auth.*;
import com.almousleck.exception.BadRequestException;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.OtpVerification;
import com.almousleck.model.Role;
import com.almousleck.model.User;
import com.almousleck.repository.OtpVerificationRepository;
import com.almousleck.repository.RoleRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final AppProperties appProperties;
    private final Random random = new Random();

    public JwtAuthResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        return new JwtAuthResponse(accessToken, refreshToken, "Bearer");
    }

    @Transactional
    public void registerUser(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }

        // Create user account but set enabled to false until OTP verification
        User user = User.builder()
                .name(signupRequest.getName())
                .username(signupRequest.getUsername())
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .phoneNumber(signupRequest.getPhoneNumber())
                .enabled(false)
                .build();

        // check if a role is here or give error
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.setRoles(Collections.singleton(userRole));

        userRepository.save(user);

        // Generate and send OTP
        String otp = generateOtp();
        saveOtp(user.getEmail(), otp, OtpVerification.OtpType.REGISTRATION);
        emailService.sendRegistrationOtp(user.getEmail(), user.getName(), otp);
    }

    @Transactional
    public void verifyRegistrationOtp(VerifyOtpRequest request) {
        OtpVerification otpVerification = otpVerificationRepository
                .findByEmailAndOtpAndTypeAndUsedFalse(
                        request.getEmail(),
                        request.getOtp(),
                        OtpVerification.OtpType.REGISTRATION)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (otpVerification.isExpired()) {
            throw new BadRequestException("OTP has expired");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        user.setEnabled(true);
        userRepository.save(user);

        otpVerification.setUsed(true);
        otpVerificationRepository.save(otpVerification);
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Check if there's a recent OTP that hasn't expired yet
        otpVerificationRepository.findTopByEmailAndTypeOrderByCreatedAtDesc(
                        request.getEmail(), OtpVerification.OtpType.valueOf(request.getType()))
                .ifPresent(otpVerification -> {
                    if (!otpVerification.isExpired() && !otpVerification.isUsed()) {
                        throw new BadRequestException("Please wait before requesting a new OTP");
                    }
                });

        String otp = generateOtp();
        OtpVerification.OtpType otpType = OtpVerification.OtpType.valueOf(request.getType());
        saveOtp(user.getEmail(), otp, otpType);

        switch (otpType) {
            case REGISTRATION:
                emailService.sendRegistrationOtp(user.getEmail(), user.getName(), otp);
                break;
            case PASSWORD_RESET:
                emailService.sendPasswordResetOtp(user.getEmail(), user.getName(), otp);
                break;
            case EMAIL_CHANGE:
                emailService.sendEmailChangeOtp(user.getEmail(), user.getName(), otp);
                break;
        }
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        String otp = generateOtp();
        saveOtp(user.getEmail(), otp, OtpVerification.OtpType.PASSWORD_RESET);
        emailService.sendPasswordResetOtp(user.getEmail(), user.getName(), otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        OtpVerification otpVerification = otpVerificationRepository
                .findByEmailAndOtpAndTypeAndUsedFalse(
                        request.getEmail(),
                        request.getOtp(),
                        OtpVerification.OtpType.PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (otpVerification.isExpired()) {
            throw new BadRequestException("OTP has expired");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpVerification.setUsed(true);
        otpVerificationRepository.save(otpVerification);
    }

    public JwtAuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        if (!tokenProvider.validateToken(refreshTokenRequest.getRefreshToken())) {
            throw new BadRequestException("Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshTokenRequest.getRefreshToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Set<Role> roles = user.getRoles();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username, null, roles.stream()
                .map(role -> (role.getName().name()))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));

        String accessToken = tokenProvider.generateToken(authentication);

        return new JwtAuthResponse(accessToken, refreshTokenRequest.getRefreshToken(), "Bearer");
    }

    private String generateOtp() {
        // Generate a 6-digit OTP
        return String.format("%06d", random.nextInt(1000000));
    }

    private void saveOtp(String email, String otp, OtpVerification.OtpType type) {
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(appProperties.getOtp().getExpiration());

        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .otp(otp)
                .type(type)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        otpVerificationRepository.save(otpVerification);
    }
}

