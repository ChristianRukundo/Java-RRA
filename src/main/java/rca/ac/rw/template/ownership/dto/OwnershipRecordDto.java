package rca.ac.rw.template.ownership.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rca.ac.rw.template.owner.dto.OwnerNameDto; // Re-use for owner details
import rca.ac.rw.template.vehicle.dto.VehicleSummaryDto; // New DTO for vehicle summary

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnershipRecordDto {
    private UUID ownershipId;
    private VehicleSummaryDto vehicleSummary; // Summary of the vehicle
    private OwnerNameDto ownerDetails;      // Details of the owner during this period
    private Instant startDate;
    private Instant endDate; // Null for current owner
    private BigDecimal transferAmount;
    // private String plateNumberActiveDuringOwnership; // Could be added later
}