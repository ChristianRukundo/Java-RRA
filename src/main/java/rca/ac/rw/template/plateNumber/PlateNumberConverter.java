package rca.ac.rw.template.plateNumber;

import rca.ac.rw.template.plateNumber.dto.PlateNumberResponseDto;

public class PlateNumberConverter {
    public static PlateNumberResponseDto toDto(PlateNumber entity) {
        if (entity == null) return null;
        return new PlateNumberResponseDto(
                entity.getId(),
                entity.getPlateNumber(),
                entity.getIssuedDate(),
                entity.getOwner() != null ? entity.getOwner().getId() : null,
                entity.getVehicle() != null ? entity.getVehicle().getId() : null,
                entity.getStatus()
        );
    }
}