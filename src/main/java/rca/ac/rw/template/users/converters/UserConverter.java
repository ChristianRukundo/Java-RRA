package rca.ac.rw.template.users.converters;

import rca.ac.rw.template.auth.dtos.RegisterRequestDto;
import rca.ac.rw.template.owner.Owner;
import rca.ac.rw.template.ownership.OwnerShip;

import rca.ac.rw.template.users.Address;
import rca.ac.rw.template.users.User;
import rca.ac.rw.template.users.dtos.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import rca.ac.rw.template.plateNumber.PlateNumberConverter;
import rca.ac.rw.template.vehicle.VehicleConverter;

/**
 * Utility class for converting between User entities and User DTOs.
 */
public class UserConverter {

    /**
     * Converts a {@link RegisterRequestDto} to a {@link User} entity.
     * Password, role, status, and enabled are set by service logic.
     *
     * @param dto The registration DTO.
     * @return The mapped {@link User} entity.
     */
    public static User toEntity(RegisterRequestDto dto) {
        if (dto == null) return null;
        User user = new User(); // Could be new Owner() if registration is always for owners
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setEmail(dto.email());
        user.setPhoneNumber(dto.phoneNumber());
        user.setNationalId(dto.nationalId());
        // Address might be part of RegisterRequestDto in some scenarios
//         if (dto.address() != null) {
//             user.setAddress(AddressConverter.toEntity(dto.address()));
//         }
        return user;
    }

    /**
     * Converts a {@link User} entity to a basic {@link UserResponseDto}.
     *
     * @param user The user entity.
     * @return The mapped {@link UserResponseDto}.
     */
    public static UserResponseDto toUserResponseDto(User user) {
        if (user == null) return null;
        return new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
//                user.getUpdatedAt(),
//                user.getCreatedAt()
        );
    }

    /**
     * Converts a {@link User} entity to a detailed {@link UserProfileResponseDto}.
     * Includes address and, if the user is an Owner, their plate numbers and currently owned vehicles.
     *
     * @param user The user entity.
     * @return The mapped {@link UserProfileResponseDto}.
     */
    public static UserProfileResponseDto toUserProfileResponseDto(User user) {
        if (user == null) return null;
        UserProfileResponseDto dto = new UserProfileResponseDto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setNationalId(user.getNationalId());
        dto.setStatus(user.getStatus());
        if (user.getAddress() != null) {
            // Create new Address instance for DTO to avoid sharing reference if Address is mutable
            dto.setAddress(new Address(
                    user.getAddress().getProvince(),
                    user.getAddress().getDistrict(),
                    user.getAddress().getSector()
            ));
        }
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        // Optionally map audit fields if they are in UserProfileResponseDto
         dto.setCreatedAt(user.getCreatedAt());
         dto.setUpdatedAt(user.getUpdatedAt());

        if (user instanceof Owner) {
            Owner owner = (Owner) user;
            // Map Plate Numbers
            if (owner.getPlateNumbers() != null && !owner.getPlateNumbers().isEmpty()) {
                dto.setPlateNumbers(owner.getPlateNumbers().stream()
                        .map(PlateNumberConverter::toDto) // Uses PlateNumberConverter
                        .collect(Collectors.toList()));
            } else {
                dto.setPlateNumbers(Collections.emptyList());
            }

            // Map Currently Owned Vehicles
            if (owner.getOwnerShips() != null && !owner.getOwnerShips().isEmpty()) {
                dto.setVehiclesOwned(owner.getOwnerShips().stream()
                        .filter(os -> os.getVehicle() != null && os.getEndDate() == null) // Check vehicle not null
                        .map(OwnerShip::getVehicle)
                        .map(VehicleConverter::toDto) // Uses VehicleConverter
                        .collect(Collectors.toList()));
            } else {
                dto.setVehiclesOwned(Collections.emptyList());
            }
        } else {
            dto.setPlateNumbers(Collections.emptyList());
            dto.setVehiclesOwned(Collections.emptyList());
        }
        return dto;
    }

    /**
     * Updates an existing {@link User} entity from an {@link UpdateUserProfileRequestDto}.
     * Only updates fields present and non-null in the DTO.
     *
     * @param dto  The DTO containing updates.
     * @param user The user entity to update.
     */
    public static void updateUserFromDto(UpdateUserProfileRequestDto dto, User user) {
        if (dto == null || user == null) return;

        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getAddress() != null) {
            if (user.getAddress() == null) {
                user.setAddress(new Address());
            }
            AddressConverter.updateAddressFromDto(dto.getAddress(), user.getAddress());
        }
    }

    /**
     * Updates an existing {@link User} entity from an {@link AdminUserUpdateRequestDto}.
     * Allows admin to update more fields like role, status, enabled.
     *
     * @param dto  The DTO from an admin.
     * @param user The user entity to update.
     */
    public static void updateUserFromAdminDto(AdminUserUpdateRequestDto dto, User user) {
        if (dto == null || user == null) return;

        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        if (dto.getEnabled() != null) {
            user.setEnabled(dto.getEnabled());
        }
        if (dto.getAddress() != null) {
            if (user.getAddress() == null) {
                user.setAddress(new Address());
            }
            AddressConverter.updateAddressFromDto(dto.getAddress(), user.getAddress());
        }
    }
}