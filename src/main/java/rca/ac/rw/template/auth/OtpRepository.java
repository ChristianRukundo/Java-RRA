package rca.ac.rw.template.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {

    /**
     * Finds an active (not used, not expired) OTP for a given user email, OTP value, and type.
     */
    Optional<Otp> findByUserEmailAndOtpValueAndOtpTypeAndUsedFalseAndExpiresAtAfter(
            String userEmail, String otpValue, OtpType otpType, LocalDateTime currentTime);

    /**
     * Finds the latest active (not used, not expired) OTP for a given user email and type.
     * Useful if you want to invalidate previous OTPs of the same type for a user.
     */
    Optional<Otp> findFirstByUserEmailAndOtpTypeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String userEmail, OtpType otpType, LocalDateTime currentTime);


    /**
     * Marks all active OTPs for a given user email and OTP type as used.
     * Useful for invalidating previous OTPs when a new one is generated.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Otp o SET o.used = true WHERE o.userEmail = :userEmail AND o.otpType = :otpType AND o.used = false AND o.expiresAt > :currentTime")
    void markOldOtpsAsUsed(String userEmail, OtpType otpType, LocalDateTime currentTime);

    /**
     * Deletes all OTPs (used or expired) older than a certain timestamp.
     * This can be called by a scheduled task for cleanup.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Otp o WHERE o.expiresAt < :cleanupTime OR o.used = true AND o.updatedAt < :cleanupTime")
    int deleteExpiredAndUsedOtps(LocalDateTime cleanupTime);
}