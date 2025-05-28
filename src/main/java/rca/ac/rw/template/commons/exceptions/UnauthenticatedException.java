package rca.ac.rw.template.commons.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.core.AuthenticationException; // Important to extend Spring's one if used with security entry points

/**
 * Exception thrown when an operation requiring authentication is attempted
 * without proper authentication credentials.
 * Maps to HTTP 401 Unauthorized.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthenticatedException extends AuthenticationException { // Or RuntimeException if not directly used by Spring Security internals

    public UnauthenticatedException(String message) {
        super(message);
    }

    public UnauthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }
}