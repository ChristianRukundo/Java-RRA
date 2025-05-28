package rca.ac.rw.template.vehicle.dto;

import lombok.*;
import rca.ac.rw.template.owner.dto.OwnerNameDto;
import rca.ac.rw.template.plateNumber.dto.PlateNumberResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class VehicleResponseDto {
    private UUID id;
    private String chassisNumber;
    private String modelName;
    private String manufacturerCompany;
    private Year manufacturedYear;
    private BigDecimal price;

    private PlateNumberResponseDto currentPlate;
    private OwnerNameDto currentOwner;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public VehicleResponseDto(UUID id, String chassisNumber, String modelName, String manufacturerCompany, Year manufacturedYear, BigDecimal price) {
        this.id = id;
        this.chassisNumber = chassisNumber;
        this.modelName = modelName;
        this.manufacturerCompany = manufacturerCompany;
        this.manufacturedYear = manufacturedYear;
        this.price = price;
    }


}