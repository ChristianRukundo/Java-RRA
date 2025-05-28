package rca.ac.rw.template.ownership.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerShipResponseDto {
    private UUID id;
    private UUID vehicleId; // Reference to the Vehicle by ID
    private UUID ownerId;   // Reference to the Owner by ID
    private Instant startDate;
    private Instant endDate;
    private BigDecimal transferAmount;
}