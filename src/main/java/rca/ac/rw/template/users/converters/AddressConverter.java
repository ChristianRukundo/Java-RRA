package rca.ac.rw.template.users.converters;

import rca.ac.rw.template.users.Address;
import rca.ac.rw.template.users.dtos.AddressDto;

/**
 * Utility class for converting between Address entities/embeddables and Address DTOs.
 */
public class AddressConverter {

    /**
     * Converts an {@link AddressDto} to an {@link Address} entity/embeddable.
     *
     * @param dto The Address DTO.
     * @return The mapped {@link Address} object.
     */
    public static Address toEntity(AddressDto dto) {
        if (dto == null) return null;
        Address address = new Address();
        address.setProvince(dto.getProvince());
        address.setDistrict(dto.getDistrict());
        address.setSector(dto.getSector());
        return address;
    }

    /**
     * Converts an {@link Address} entity/embeddable to an {@link AddressDto}.
     *
     * @param entity The Address object.
     * @return The mapped {@link AddressDto}.
     */
    public static AddressDto toDto(Address entity) {
        if (entity == null) return null;
        AddressDto dto = new AddressDto();
        dto.setProvince(entity.getProvince());
        dto.setDistrict(entity.getDistrict());
        dto.setSector(entity.getSector());
        return dto;
    }

    /**
     * Updates an existing {@link Address} object from an {@link AddressDto}.
     *
     * @param dto    The DTO containing updates.
     * @param entity The Address object to update.
     */
    public static void updateAddressFromDto(AddressDto dto, Address entity) {
        if (dto == null || entity == null) return;

        if (dto.getProvince() != null) {
            entity.setProvince(dto.getProvince());
        }
        if (dto.getDistrict() != null) {
            entity.setDistrict(dto.getDistrict());
        }
        if (dto.getSector() != null) {
            entity.setSector(dto.getSector());
        }
    }
}