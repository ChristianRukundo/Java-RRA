package rca.ac.rw.template.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSummaryDto {
    private UUID vehicleId;
    private String chassisNumber;
    private String modelName;
}
