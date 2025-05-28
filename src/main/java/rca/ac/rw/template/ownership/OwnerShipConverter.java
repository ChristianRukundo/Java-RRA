package rca.ac.rw.template.ownership;

import rca.ac.rw.template.ownership.OwnerShip;
import rca.ac.rw.template.ownership.dto.OwnerShipResponseDto;

/**
 * Utility class for converting between OwnerShip entities and DTOs.
 */
public class OwnerShipConverter {

    /**
     * Converts an {@link OwnerShip} entity to an {@link OwnerShipResponseDto}.
     *
     * @param entity The OwnerShip entity.
     * @return The mapped {@link OwnerShipResponseDto}.
     */
    public static OwnerShipResponseDto toDto(OwnerShip entity) {
        if (entity == null) return null;
        return new OwnerShipResponseDto(
                entity.getId(),
                entity.getVehicle() != null ? entity.getVehicle().getId() : null,
                entity.getOwner() != null ? entity.getOwner().getId() : null,
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getTransferAmount()
        );
    }
}