package rca.ac.rw.template.vehicle.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Year;

/**
 * DTO for an admin to update existing vehicle details.
 * Chassis number is typically not updatable as it's a primary identifier.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVehicleRequestDto {

    @Size(min = 2, max = 100, message = "Model name must be between 2 and 100 characters")
    private String modelName; // Make fields optional for partial updates

    @Size(max = 100, message = "Manufacturer company cannot exceed 100 characters")
    private String manufacturerCompany;

    private Year manufacturedYear; // Nullable if admin doesn't want to change it

    @Positive(message = "Price must be positive")
    private BigDecimal price; // Nullable

}