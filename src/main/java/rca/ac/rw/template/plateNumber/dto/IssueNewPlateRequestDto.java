package rca.ac.rw.template.plateNumber.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rca.ac.rw.template.commons.validation.ValidPlateNumber;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueNewPlateRequestDto {

    @NotNull(message = "Vehicle ID is required")
    private UUID vehicleId;

    @NotNull(message = "Owner ID is required")
    private UUID ownerId; // The owner who will be associated with this new plate for this vehicle

    @NotBlank(message = "New plate number string is required")
    @ValidPlateNumber
    private String plateNumberString;

    // Old plate number (optional, if this is a replacement)
    private String oldPlateNumberString; // To mark the old one as 'RETIRED' or 'AVAILABLE'
}