package rca.ac.rw.template.commons.response; // Or a more general DTOs package

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A standardized DTO for API error responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Only include fields that are not null in the JSON response
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error; // Short description of the HTTP status (e.g., "Bad Request", "Not Found")
    private String message; // More specific error message
    private String path; // The request path that caused the error

    // For validation errors, to include details about specific field errors
    private Map<String, List<String>> fieldErrors;

    public ErrorResponse(HttpStatus httpStatus, String message, String path) {
        this.timestamp = Instant.now();
        this.status = httpStatus.value();
        this.error = httpStatus.getReasonPhrase();
        this.message = message;
        this.path = path;
    }

    public ErrorResponse(HttpStatus httpStatus, String message, String path, Map<String, List<String>> fieldErrors) {
        this(httpStatus, message, path);
        this.fieldErrors = fieldErrors;
    }
}