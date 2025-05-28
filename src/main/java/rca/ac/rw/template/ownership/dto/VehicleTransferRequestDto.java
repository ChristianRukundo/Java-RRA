package rca.ac.rw.template.ownership.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rca.ac.rw.template.commons.validation.ValidPlateNumber;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTransferRequestDto {

    @NotNull(message = "Vehicle ID to be transferred is required")
    private UUID vehicleId;

    @NotNull(message = "Current owner ID is required for verification") // Or could be derived if admin is performing
    private UUID currentOwnerId;

    @NotNull(message = "New owner ID is required")
    private UUID newOwnerId;

    @NotNull(message = "Purchase amount by new owner is required")
    @Positive(message = "Purchase amount must be positive")
    private BigDecimal transferAmount;

    // Option 1: New owner provides one of THEIR available plate numbers
    @NotBlank(message = "Plate number from new owner's list is required")
    @ValidPlateNumber
    private String newPlateNumberStringForNewOwner;

    // Option 2: System issues a completely new plate (if "new Owner platenumbers list" means this)
    // If Option 2, then 'newPlateNumberStringForNewOwner' might be system generated or a different DTO field.
    // The requirement "from the new Owner platenumbers list" implies the new owner has available plates.
}