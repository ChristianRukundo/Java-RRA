package rca.ac.rw.template.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rca.ac.rw.template.commons.validation.ValidPlateNumber;

import java.math.BigDecimal;
import java.time.Year;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterVehicleAndIssuePlateRequestDto {

    // Vehicle Details
    @NotBlank(message = "Chassis number is required")
    @Size(min = 5, max = 50, message = "Chassis number must be between 5 and 50 characters")
    private String chassisNumber;

    @NotBlank(message = "Model name is required")
    @Size(min = 2, max = 100, message = "Model name must be between 2 and 100 characters")
    private String modelName;

    @Size(max = 100, message = "Manufacturer company cannot exceed 100 characters")
    private String manufacturerCompany;

    @NotNull(message = "Manufactured year is required")
    private Year manufacturedYear;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    // Owner Information (to link the vehicle and plate)
    @NotNull(message = "Owner ID is required for registration")
    private UUID ownerId;


    @NotBlank(message = "Plate number string is required")
    @ValidPlateNumber
    private String plateNumberString;

}