package rca.ac.rw.template.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import rca.ac.rw.template.auth.OtpType; // Assuming this is still used

import java.math.BigDecimal; // Import BigDecimal

@Service
@AllArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine; // Injected by Spring if configured

    // --- Existing OTP and Success Email Methods (Assumed to be correct and used elsewhere) ---
    @Async
    public void sendAccountVerificationEmail(String to, String name, String otp) {
        sendOtpEmail(to, name, otp, OtpType.VERIFY_ACCOUNT);
    }

    @Async
    public void sendResetPasswordOtp(String to, String name, String otp) {
        sendOtpEmail(to, name, otp, OtpType.FORGOT_PASSWORD);
    }

    @Async
    public void sendVerificationSuccessEmail(String to, String name) {
        sendSuccessEmail(to, name, "verify_success", "Account Verified Successfully");
    }

    @Async
    public void sendResetPasswordSuccessEmail(String to, String name) {
        sendSuccessEmail(to, name, "reset_success", "Password Reset Successfully");
    }

    // --- Existing Generic, Plate, and Inspection Emails (Review if context variables need update) ---
    @Async
    public void sendUserRegisteredEmail(String to, String name, String optionalPlateNumber) {
        // Assuming owner_registered template might use plateNumber if provided
        Context context = new Context();
        context.setVariable("name", name);
        if (optionalPlateNumber != null && !optionalPlateNumber.isEmpty()) {
            context.setVariable("plateNumber", optionalPlateNumber);
        }
        context.setVariable("companyName", "Rwanda Revenue Authority");
        sendEmail("owner_registered", context, to, "Welcome to RRA Vehicle Management");
    }

    @Async
    public void sendPlateCreatedEmail(String to, String name, String plateNumber) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("plateNumber", plateNumber);
        context.setVariable("companyName", "Rwanda Revenue Authority");
        sendEmail("plate_created", context, to, "Your Plate Number Has Been Created");
    }

    @Async
    public void sendPlateAssignedEmail(String to, String name, String plateNumber, String vehicleDetails) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("plateNumber", plateNumber);
        context.setVariable("vehicleDetails", vehicleDetails); // e.g., "Toyota RAV4 (Chassis: XXXXX)"
        context.setVariable("companyName", "Rwanda Revenue Authority");
        sendEmail("plate_assigned", context, to, "Your Plate Has Been Assigned to a Vehicle");
    }

    @Async
    public void sendPostInspectionNotification(String to, String name, String plateNumber, String vehicleIdentifier) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("plateNumber", plateNumber); // Current plate related to inspection
        context.setVariable("vehicleIdentifier", vehicleIdentifier); // e.g., Chassis number
        context.setVariable("companyName", "Rwanda Revenue Authority");
        sendEmail("post_inspection_notification", context, to, "Vehicle Inspection Update");
    }


    // --- UPDATED Ownership Transfer Emails ---

    /**
     * Sends an email notification to the sender (previous owner) about a vehicle ownership transfer.
     *
     * @param to                     The email address of the sender.
     * @param senderName             The full name of the sender.
     * @param vehicleIdentifier      A unique identifier for the vehicle (e.g., chassis number).
     * @param oldPlateNumber         The plate number previously associated with the vehicle by the sender.
     * @param transferAmount         The amount for which the vehicle was transferred.
     * @param newOwnerFullName       The full name of the new owner.
     */
    @Async
    public void sendOwnershipTransferEmailToSender(String to,
                                                   String senderName,
                                                   String vehicleIdentifier,
                                                   String oldPlateNumber,
                                                   BigDecimal transferAmount,
                                                   String newOwnerFullName) {
        Context context = new Context();
        context.setVariable("name", senderName); // Recipient's name (the sender of the vehicle)
        context.setVariable("vehicleIdentifier", vehicleIdentifier);
        context.setVariable("plateNumber", oldPlateNumber); // The plate they had
        context.setVariable("amount", transferAmount); // Use BigDecimal
        context.setVariable("newOwner", newOwnerFullName);
        context.setVariable("companyName", "Rwanda Revenue Authority");
        sendEmail("ownership_transferred_sender", context, to, "Vehicle Ownership Transferred");
    }

    /**
     * Sends an email notification to the receiver (new owner) about a vehicle ownership transfer.
     *
     * @param to                       The email address of the receiver.
     * @param receiverName             The full name of the receiver.
     * @param vehicleIdentifier        A unique identifier for the vehicle (e.g., chassis number).
     * @param newPlateNumber           The new plate number assigned to the vehicle for the receiver.
     * @param transferAmount           The amount for which the vehicle was acquired.
     * @param previousOwnerFullName    The full name of the previous owner.
     */
    @Async
    public void sendOwnershipTransferEmailToReceiver(String to, String receiverName,
                                                     String vehicleIdentifier, String newPlateNumber,
                                                     BigDecimal transferAmount,
                                                     String previousOwnerFullName) {
        Context context = new Context();
        context.setVariable("name", receiverName); // Recipient's name (the new owner)
        context.setVariable("vehicleIdentifier", vehicleIdentifier);
        context.setVariable("plateNumber", newPlateNumber); // The plate they are getting
        context.setVariable("amount", transferAmount); // Use BigDecimal
        context.setVariable("previousOwner", previousOwnerFullName);
        context.setVariable("companyName", "Rwanda Revenue Authority");
        sendEmail("ownership_transferred_receiver", context, to, "You Have Received Vehicle Ownership");
    }


    // --- Private Helper Methods ---

    private void sendOtpEmail(String to, String name, String otp, OtpType otpType) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("otp", otp);
            context.setVariable("companyName", "Rwanda Revenue Authority");
            context.setVariable("expirationTimeMinutes", "10"); // Consistent variable name

            String subject;
            String templateName;

            switch (otpType) {
                case VERIFY_ACCOUNT:
                    templateName = "verify_account"; // Ensure this Thymeleaf template exists
                    subject = "Verify your account - One Time Password (OTP)";
                    break;
                case FORGOT_PASSWORD:
                    templateName = "forgot_password"; // Ensure this Thymeleaf template exists
                    subject = "Reset your password - One Time Password (OTP)";
                    break;
                default:
                    log.error("Invalid OtpType detected for email sending: {}", otpType);
                    return; // Do not proceed if type is unknown
            }
            String htmlContent = templateEngine.process(templateName, context);
            sendHtmlEmail(to, subject, htmlContent);

        } catch (Exception e) { // Catch broader Exception for template processing errors
            log.error("Unable to prepare or send OTP email to {} for type {}: {}", to, otpType, e.getMessage(), e);
        }
    }

    private void sendSuccessEmail(String to, String name, String templateName, String subject) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("companyName", "Rwanda Revenue Authority");
            // Add any other common variables for success emails

            String htmlContent = templateEngine.process(templateName, context); // Ensure template exists
            sendHtmlEmail(to, subject, htmlContent);

        } catch (Exception e) {
            log.error("Unable to prepare or send success email '{}' to {}: {}", subject, to, e.getMessage(), e);
        }
    }

    private void sendEmail(String templateName, Context context, String to, String subject) {
        try {
            String htmlContent = templateEngine.process(templateName, context);
            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            log.error("Failed to process template '{}' or send email [{}] to {}: {}", templateName, subject, to, e.getMessage(), e);
        }
    }

    /**
     * Core method to send an HTML email.
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // Use true for multipart message, true for HTML
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("noreply@rra.com"); // Consider making this configurable
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true indicates html content
            mailSender.send(mimeMessage);
            log.info("Successfully sent email '{}' to {}", subject, to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email '{}' to {}: {}", subject, to, e.getMessage(), e);
            // Consider how to handle this failure, e.g., retry mechanism, DLQ, or just log.
        }
    }
}