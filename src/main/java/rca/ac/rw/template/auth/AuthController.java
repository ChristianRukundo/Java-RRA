package rca.ac.rw.template.auth;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Add SLF4J
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import rca.ac.rw.template.auth.dtos.*;
import rca.ac.rw.template.commons.exceptions.BadRequestException;
import rca.ac.rw.template.email.EmailService;
import rca.ac.rw.template.users.Status;
import rca.ac.rw.template.users.User; // For fetching User entity
import rca.ac.rw.template.users.UserService;
import rca.ac.rw.template.users.dtos.UserProfileResponseDto; // For profile endpoint
import rca.ac.rw.template.users.dtos.UserResponseDto; // For register endpoint if it returns this

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/auth")
@Slf4j // Add SLF4J
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final OtpService otpService;
    private final EmailService emailService;

    /**
     * Registers a new user, sends an OTP for account verification.
     *
     * @param registerDto The user registration details.
     * @param uriBuilder  For building the created URI.
     * @return ResponseEntity with the created UserResponseDto.
     */
    @PostMapping("/register")
    @RateLimiter(name = "auth-rate-limiter")
    public ResponseEntity<UserResponseDto> registerUser(
            @Valid @RequestBody RegisterRequestDto registerDto,
            UriComponentsBuilder uriBuilder) {
        log.info("Registering user with email: {}", registerDto.email());

        UserResponseDto userResponse = userService.createUser(registerDto);
        var uri = uriBuilder.path("/users/{id}").buildAndExpand(userResponse.getId()).toUri();
        var otpToSend = otpService.generateAndStoreOtp(userResponse.getEmail(), OtpType.VERIFY_ACCOUNT);

        emailService.sendAccountVerificationEmail(userResponse.getEmail(), userResponse.getFirstName(), otpToSend);
        return ResponseEntity.created(uri).body(userResponse);
    }

    /**
     * Verifies a user account using an OTP.
     *
     * @param verifyAccountRequest DTO with email and OTP.
     * @return ResponseEntity with success message.
     */
    @PatchMapping("/verify-account")
    @RateLimiter(name = "otp-rate-limiter")
    ResponseEntity<?> verifyAccount(@Valid @RequestBody VerifyAccountDto verifyAccountRequest) {
        log.info("Verifying account for email: {}", verifyAccountRequest.email());
        if (!otpService.verifyOtp(verifyAccountRequest.email(), verifyAccountRequest.otp(), OtpType.VERIFY_ACCOUNT)) {
            throw new BadRequestException("Invalid email or OTP");
        }

        userService.activateUserAccount(verifyAccountRequest.email()); // This activates and sets status to ACTIVE
        // userService.updateUserStatus(verifyAccountRequest.email(), Status.ACTIVE); // activateUserAccount should ideally handle this

        // Fetch user to get their name for the email
        // We need a public method in UserService to get User entity by email if not using getAuthenticatedUser
        User user = userService.findUserByActualEmail(verifyAccountRequest.email()); // Use the new public method
        emailService.sendVerificationSuccessEmail(user.getEmail(), user.getFirstName());

        return ResponseEntity.ok("Account Activated successfully");
    }

    /**
     * Initiates the password reset process for a user.
     *
     * @param initiateRequest DTO with user's email.
     * @return ResponseEntity with a message.
     */
    @PostMapping("/initiate-password-reset")
    @RateLimiter(name = "auth-rate-limiter") // Added RateLimiter
    ResponseEntity<?> initiatePasswordReset(@Valid @RequestBody InitiatePasswordResetDto initiateRequest) {
        log.info("Initiating password reset for email: {}", initiateRequest.email());
        // Ensure user exists before generating OTP and sending email
        User user = userService.findUserByActualEmail(initiateRequest.email()); // Use the new public method

        var otpToSend = otpService.generateAndStoreOtp(user.getEmail(), OtpType.FORGOT_PASSWORD);
        userService.updateUserStatus(user.getEmail(), Status.RESET); // Set status to RESET
        emailService.sendResetPasswordOtp(user.getEmail(), user.getFirstName(), otpToSend);
        return ResponseEntity.ok("If your email is registered, you will receive an email with instructions to reset your password.");
    }

    /**
     * Retrieves the profile of the currently logged-in user.
     * Uses UserService.getMyProfile which returns UserProfileResponseDto.
     *
     * @return ResponseEntity with UserProfileResponseDto.
     */
    @GetMapping("/profile")
    @RateLimiter(name = "auth-rate-limiter") // Added RateLimiter
    public ResponseEntity<UserProfileResponseDto> getLoggedInUserProfile() {
        log.info("Fetching profile for logged-in user.");
        // This now correctly calls getMyProfile which returns UserProfileResponseDto
        UserProfileResponseDto userProfile = userService.getMyProfile();
        return ResponseEntity.ok(userProfile);
    }

    /**
     * Resets the user's password using an OTP and a new password.
     *
     * @param resetPasswordRequest DTO with email, OTP, and new password.
     * @return ResponseEntity with success message.
     */
    @PatchMapping("/reset-password")
    @RateLimiter(name = "auth-rate-limiter")
    ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDto resetPasswordRequest) {
        log.info("Resetting password for email: {}", resetPasswordRequest.email());
        if (!otpService.verifyOtp(resetPasswordRequest.email(), resetPasswordRequest.otp(), OtpType.FORGOT_PASSWORD)) {
            throw new BadRequestException("Invalid email or OTP");
        }

        userService.changeUserPassword(resetPasswordRequest.email(), resetPasswordRequest.newPassword());
        // changeUserPassword should ideally set status to ACTIVE. If not:
        // userService.updateUserStatus(resetPasswordRequest.email(), Status.ACTIVE);

        User user = userService.findUserByActualEmail(resetPasswordRequest.email()); // Use new public method
        emailService.sendResetPasswordSuccessEmail(user.getEmail(), user.getFirstName());

        return ResponseEntity.ok("Password reset successfully. You can now login with your new password.");
    }

    /**
     * Logs in a user.
     *
     * @param loginRequestDto DTO with email and password.
     * @param response        HttpServletResponse to add refresh token cookie.
     * @return ResponseEntity with LoginResponse (access token).
     */
    @PostMapping("/login")
    @RateLimiter(name = "auth-rate-limiter")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequestDto loginRequestDto,
            HttpServletResponse response) {
        log.info("Login attempt for email: {}", loginRequestDto.email());
        LoginResponse loginResult = authService.login(loginRequestDto, response);
        return ResponseEntity.ok(loginResult); // loginResult directly contains the token
    }
}