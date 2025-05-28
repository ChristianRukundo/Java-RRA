package rca.ac.rw.template.auth;

import lombok.RequiredArgsConstructor; // Use this instead of @AllArgsConstructor for final fields
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor // Lombok for constructor injection of final fields
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_EXPIRATION_MINUTES = 10; // Default OTP expiration

    /**
     * Generates a new OTP, stores it in the database, and returns the OTP.
     * Any previous active OTPs of the same type for the user will be marked as used.
     *
     * @param userEmail The email of the user.
     * @param otpType   The type of OTP.
     * @return The generated 6-digit OTP string.
     */
    @Transactional
    public String generateAndStoreOtp(String userEmail, OtpType otpType) {
        log.info("Generating OTP for UserEmail: {}, Type: {}", userEmail, otpType);

        // Step 1: Mark any existing active OTPs for this user and type as used
        otpRepository.markOldOtpsAsUsed(userEmail, otpType, LocalDateTime.now());

        // Step 2: Generate new OTP
        String otpValue = generateNumericOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES);

        // Step 3: Create and save the new OTP entity
        Otp otpEntity = new Otp(otpValue, userEmail, otpType, expiresAt);
        otpRepository.save(otpEntity);

        log.info("Stored new OTP: {} for UserEmail: {}, Type: {}, ExpiresAt: {}",
                otpValue, userEmail, otpType, expiresAt);
        return otpValue;
    }

    /**
     * Verifies the provided OTP against the one stored in the database.
     * If verification is successful, the OTP is marked as used.
     *
     * @param userEmail The email of the user.
     * @param clientOtp The OTP string provided by the client.
     * @param otpType   The type of OTP being verified.
     * @return True if the OTP is valid, active, and matches, false otherwise.
     */
    @Transactional
    public boolean verifyOtp(String userEmail, String clientOtp, OtpType otpType) {
        log.info("Verifying OTP. UserEmail: {}, ClientOTP: {}, Type: {}", userEmail, clientOtp, otpType);

        if (!isValidOtpFormat(clientOtp)) {
            log.warn("Invalid OTP format received for verification: '{}'", clientOtp);
            return false;
        }

        // Find an OTP that matches the user, value, type, is not used, and has not expired.
        Optional<Otp> otpOptional = otpRepository.findByUserEmailAndOtpValueAndOtpTypeAndUsedFalseAndExpiresAtAfter(
                userEmail, clientOtp, otpType, LocalDateTime.now()
        );

        if (otpOptional.isPresent()) {
            Otp otpEntity = otpOptional.get();
            otpEntity.setUsed(true); // Mark as used
            otpRepository.save(otpEntity);
            log.info("OTP MATCHED and verified for UserEmail: {}, Type: {}. OTP marked as used.", userEmail, otpType);
            return true;
        } else {
            log.warn("OTP verification FAILED for UserEmail: {}, ClientOTP: {}, Type: {}. OTP not found, expired, or already used.",
                    userEmail, clientOtp, otpType);
            return false;
        }
    }

    /**
     * Generates a 6-digit numeric OTP.
     */
    private String generateNumericOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            otp.append(SECURE_RANDOM.nextInt(10));
        }
        String generatedOtp = otp.toString();
        log.debug("Generated new numeric OTP value: {}", generatedOtp);
        return generatedOtp;
    }

    /**
     * Validates the format of the OTP (e.g., 6 digits).
     */
    private boolean isValidOtpFormat(String otp) {
        return otp != null && otp.matches("\\d{6}");
    }

    /**
     * Scheduled task or admin utility to clean up old, expired, or used OTPs.
     * This helps keep the 'otps' table from growing indefinitely.
     */
    @Transactional
    public void cleanupOldOtps() {

        LocalDateTime cleanupThreshold = LocalDateTime.now().minusDays(1); // Example: clean OTPs expired more than a day ago
        log.info("Performing cleanup of old OTPs expired or used before: {}", cleanupThreshold);
        int deletedCount = otpRepository.deleteExpiredAndUsedOtps(cleanupThreshold);
        log.info("Cleaned up {} old OTP records.", deletedCount);
    }
}