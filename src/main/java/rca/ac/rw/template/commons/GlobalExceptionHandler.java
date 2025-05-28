package rca.ac.rw.template.commons; // A common package for @ControllerAdvice classes

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import rca.ac.rw.template.commons.exceptions.BadRequestException;
import rca.ac.rw.template.commons.response.ErrorResponse;
import rca.ac.rw.template.commons.exceptions.ResourceNotFoundException;
import rca.ac.rw.template.commons.exceptions.UnauthenticatedException;
import rca.ac.rw.template.commons.exceptions.ValidationException;
import org.springframework.security.core.AuthenticationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> buildResponseEntity(HttpStatus status, String message, String path, Map<String, List<String>> fieldErrors) {
        ErrorResponse errorResponse;
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            errorResponse = new ErrorResponse(status, message, path, fieldErrors);
        } else {
            errorResponse = new ErrorResponse(status, message, path);
        }
        return new ResponseEntity<>(errorResponse, status);
    }

    private ResponseEntity<ErrorResponse> buildResponseEntity(HttpStatus status, String message, String path) {
        return buildResponseEntity(status, message, path, null);
    }

    // Handler for your custom ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("ResourceNotFoundException: {} on path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponseEntity(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    // Handler for your custom ValidationException
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, HttpServletRequest request) {
        log.warn("ValidationException: {} on path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponseEntity(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // Handler for your custom UnauthenticatedException
    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticatedException(UnauthenticatedException ex, HttpServletRequest request) {
        log.warn("UnauthenticatedException: {} on path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponseEntity(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    // Handler for Spring's MethodArgumentNotValidException (thrown by @Valid on @RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("MethodArgumentNotValidException: Validation failed for request on path: {}", request.getRequestURI(), ex);
        Map<String, List<String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));
        String message = "Validation failed. Check 'fieldErrors' for details.";
        return buildResponseEntity(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("AuthenticationException: {} on path: {}", ex.getMessage(), request.getRequestURI());
        // For "Bad credentials", HttpStatus.UNAUTHORIZED (401) is more appropriate than 403
        return buildResponseEntity(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }



    // Handler for ConstraintViolationException (thrown by @Valid on @PathVariable or @RequestParam)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("ConstraintViolationException: Validation failed for request on path: {}", request.getRequestURI(), ex);
        Map<String, List<String>> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.groupingBy(
                        violation -> getFieldNameFromPath(violation.getPropertyPath().toString()),
                        Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())
                ));
        String message = "Validation failed. Check 'fieldErrors' for details.";
        return buildResponseEntity(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), fieldErrors);
    }

    // Helper to extract field name from property path for ConstraintViolationException
    private String getFieldNameFromPath(String propertyPath) {
        if (propertyPath == null || propertyPath.isEmpty()) {
            return "unknownField";
        }
        // Example path: "methodName.argumentName.fieldName" or "methodName.argumentName"
        String[] parts = propertyPath.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : propertyPath;
    }


    // Handler for HttpMessageNotReadableException (e.g., malformed JSON request body)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("HttpMessageNotReadableException: Malformed request on path: {}. Details: {}", request.getRequestURI(), ex.getMessage());
        String message = "Malformed JSON request or invalid input format.";
        if (ex.getCause() instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ifx) {
            message = "Invalid value '" + ifx.getValue() + "' for field '" + ifx.getPath().get(ifx.getPath().size()-1).getFieldName() + "'. Expected type: " + ifx.getTargetType().getSimpleName();
        } else if (ex.getCause() instanceof com.fasterxml.jackson.core.JsonParseException jpe) {
            message = "Malformed JSON request: " + jpe.getOriginalMessage();
        }
        return buildResponseEntity(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    /**
     * Handles custom {@link BadRequestException}.
     * This is for general "bad request" scenarios not covered by more specific validation exceptions.
     *
     * @param ex      The BadRequestException.
     * @param request The HttpServletRequest.
     * @return A ResponseEntity with HTTP 400 Bad Request.
     */
    @ExceptionHandler(BadRequestException.class) // <<<--- ADDED THIS HANDLER
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        log.warn("BadRequestException: {} on path: {}", ex.getMessage(), request.getRequestURI(), ex); // Log with exception for context
        return buildResponseEntity(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // Handler for MissingServletRequestParameterException (e.g., required @RequestParam is missing)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("MissingServletRequestParameterException: Required parameter '{}' is missing on path: {}", ex.getParameterName(), request.getRequestURI());
        String message = String.format("Required request parameter '%s' of type %s is not present.", ex.getParameterName(), ex.getParameterType());
        return buildResponseEntity(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    // Handler for MethodArgumentTypeMismatchException (e.g., sending a string where an int is expected for a path variable or request param)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("MethodArgumentTypeMismatchException: Parameter '{}' has invalid value '{}' on path: {}", ex.getName(), ex.getValue(), request.getRequestURI());
        String message = String.format("Parameter '%s' should be of type '%s' but value was '%s'.",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "Unknown", ex.getValue());
        return buildResponseEntity(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }


    // Generic handler for other RuntimeExceptions (catch-all)
    // It's often better to handle specific exceptions, but this can be a fallback.
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Default to 500 for unexpected runtime exceptions
    public ResponseEntity<ErrorResponse> handleGenericRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("Unhandled RuntimeException: {} on path: {}", ex.getMessage(), request.getRequestURI(), ex); // Log full stack trace for unexpected errors
        return buildResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred. Please try again later.", request.getRequestURI());
    }

    // Generic handler for general Exceptions (very broad, use with caution)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled Exception: {} on path: {}", ex.getMessage(), request.getRequestURI(), ex);
        // Avoid exposing too much detail from generic exceptions
        String message = "An unexpected error occurred.";
        if (ex instanceof org.springframework.dao.DataIntegrityViolationException) {
            message = "Database integrity constraint violated. This could be due to duplicate data or invalid foreign key references.";
            return buildResponseEntity(HttpStatus.CONFLICT, message, request.getRequestURI()); // 409 Conflict is often suitable
        }
        return buildResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, request.getRequestURI());
    }
}