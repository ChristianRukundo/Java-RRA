// Assuming this DTO exists in your project structure, e.g., in rca.ac.rw.template.plateNumber.dto
package rca.ac.rw.template.plateNumber.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rca.ac.rw.template.plateNumber.PlateStatus;


import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlateNumberResponseDto {
    private UUID id;
    private String plateNumber;
    private Instant issuedDate;
    private UUID ownerId;
    private UUID vehicleId;
    private PlateStatus status;

}