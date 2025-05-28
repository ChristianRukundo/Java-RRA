package rca.ac.rw.template.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rca.ac.rw.template.auth.OtpService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final OtpService otpService;

    /**
     * Cleans up old OTPs daily at 3 AM.
     * cron expression: second, minute, hour, day of month, month, day(s) of week
     * "0 0 3 * * ?" means "at 03:00:00 AM every day"
     */
    @Scheduled(cron = "0 0 3 * * ?") // Runs daily at 3 AM
    public void reportCurrentTime() {
        log.info("Executing scheduled task: Cleaning up old OTPs.");
        otpService.cleanupOldOtps();
    }
}