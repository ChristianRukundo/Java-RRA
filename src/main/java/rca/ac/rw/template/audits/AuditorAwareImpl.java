package rca.ac.rw.template.audits;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of AuditorAware to provide the current auditor's ID (UUID).
 * This class is used by Spring Data JPA to automatically populate createdBy and lastModifiedBy fields
 * in auditable entities.
 */
@Component("auditorAware") // You can give it a specific bean name if needed
public class AuditorAwareImpl implements AuditorAware<UUID> {

    /**
     * Retrieves the current auditor's ID from the Spring Security context.
     *
     * @return An {@link Optional} containing the {@link UUID} of the current auditor if authenticated,
     *         or an empty {@link Optional} otherwise (e.g., for system processes or unauthenticated actions).
     */
    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {

            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();

        if (principal instanceof UUID) {
            return Optional.of((UUID) principal);
        }
        return Optional.empty();
    }
}