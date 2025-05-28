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
public class VehicleOwnershipHistoryDto {
    private UUID ownershipId;
    private UUID ownerId;
    private String ownerFirstName;
    private String ownerLastName;
    private Instant startDate;
    private Instant endDate; // Null for current owner
    private BigDecimal purchaseAmount;
    // You might also want to include the plate number active during this ownership period
    // private String plateNumberDuringOwnership;
}