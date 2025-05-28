package rca.ac.rw.template.owner;

import rca.ac.rw.template.owner.dto.OwnerResponseDto;
import rca.ac.rw.template.owner.dto.RegisterOwnerRequestDto;

import rca.ac.rw.template.plateNumber.PlateNumberConverter;
import rca.ac.rw.template.users.Address;
import rca.ac.rw.template.vehicle.VehicleConverter;


import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Utility class for converting between Owner entities and Owner DTOs.
 */
public class OwnerConverter {

    /**
     * Converts a {@link RegisterOwnerRequestDto} to an {@link Owner} entity.
     * This assumes the DTO contains all necessary fields for an Owner that are not
     * also on the base User (if any), or it sets user fields directly.
     *
     * @param dto The registration DTO for an owner.
     * @return The mapped {@link Owner} entity.
     */
    public static Owner toEntity(RegisterOwnerRequestDto dto) {
        if (dto == null) return null;
        Owner owner = new Owner();
        // Set fields from User part
        owner.setFirstName(dto.getFirstName());
        owner.setLastName(dto.getLastName());
        owner.setEmail(dto.getEmail());
        owner.setPhoneNumber(dto.getPhoneNumber());
        owner.setNationalId(dto.getNationalId());
        // If RegisterOwnerRequestDto includes address:
        // if (dto.getAddress() != null) { // Assuming RegisterOwnerRequestDto has getAddress()
        //     owner.setAddress(AddressConverter.toEntity(dto.getAddress()));
        // }
        // Owner specific fields, if any, would be set here.
        // Password, role, status, enabled are set by service logic.
        return owner;
    }

    /**
     * Converts an {@link Owner} entity to an {@link OwnerResponseDto}.
     *
     * @param owner The owner entity.
     * @return The mapped {@link OwnerResponseDto}.
     */
    public static OwnerResponseDto toDto(Owner owner) {
        if (owner == null) return null;
        OwnerResponseDto dto = new OwnerResponseDto();
        dto.setId(owner.getId());
        dto.setFirstName(owner.getFirstName());
        dto.setLastName(owner.getLastName());
        dto.setEmail(owner.getEmail());
        dto.setPhoneNumber(owner.getPhoneNumber());
        dto.setNationalId(owner.getNationalId());
        dto.setStatus(owner.getStatus());
        if (owner.getAddress() != null) {
            dto.setAddress(new Address( // Create new instance
                    owner.getAddress().getProvince(),
                    owner.getAddress().getDistrict(),
                    owner.getAddress().getSector()
            ));
        }
        dto.setRole(owner.getRole());
        dto.setEnabled(owner.isEnabled());

        // Map Plate Numbers
        if (owner.getPlateNumbers() != null && !owner.getPlateNumbers().isEmpty()) {
            dto.setPlateNumbers(owner.getPlateNumbers().stream()
                    .map(PlateNumberConverter::toDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setPlateNumbers(Collections.emptyList());
        }

        // Map Currently Owned Vehicles
        if (owner.getOwnerShips() != null && !owner.getOwnerShips().isEmpty()) {
            dto.setVehiclesOwned(owner.getOwnerShips().stream()
                    .filter(os -> os.getVehicle() != null && os.getEndDate() == null)
                    .map(os -> VehicleConverter.toDto(os.getVehicle()))
                    .collect(Collectors.toList()));
        } else {
            dto.setVehiclesOwned(Collections.emptyList());
        }
        return dto;
    }
}