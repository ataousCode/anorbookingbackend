package com.almousleck.repository;

import com.almousleck.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByEmailAndOtpAndTypeAndUsedFalse(
            String email, String otp, OtpVerification.OtpType type);

    Optional<OtpVerification> findTopByEmailAndTypeOrderByCreatedAtDesc(
            String email, OtpVerification.OtpType type);
}