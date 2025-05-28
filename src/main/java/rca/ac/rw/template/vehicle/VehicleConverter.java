package rca.ac.rw.template.vehicle;

import rca.ac.rw.template.vehicle.dto.VehicleResponseDto; // Corrected package

/**
 * Utility class for converting between Vehicle entities and DTOs.
 */
public class VehicleConverter {

    /**
     * Converts a {@link Vehicle} entity to a basic {@link VehicleResponseDto}.
     * More detailed DTO population (current owner, plate) should be handled by service layer
     * as it requires fetching related entities.
     *
     * @param entity The Vehicle entity.
     * @return The mapped {@link VehicleResponseDto}.
     */
    public static VehicleResponseDto toDto(Vehicle entity) {
        if (entity == null) return null;
        VehicleResponseDto dto = new VehicleResponseDto(
                entity.getId(),
                entity.getChassisNumber(),
                entity.getModelName(),
                entity.getManufacturerCompany(),
                entity.getManufacturedYear(),
                entity.getPrice()
        );
        // Populate audit fields
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        // CurrentPlate and CurrentOwner would be set by the service method that has access to all related data
        return dto;
    }
}