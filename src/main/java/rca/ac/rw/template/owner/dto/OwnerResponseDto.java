package rca.ac.rw.template.owner.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rca.ac.rw.template.plateNumber.dto.PlateNumberResponseDto;
import rca.ac.rw.template.users.Address;
import rca.ac.rw.template.users.Role;
import rca.ac.rw.template.users.Status;
import rca.ac.rw.template.vehicle.dto.VehicleResponseDto;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerResponseDto {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String nationalId;
        private Status status;
        private Address address;
        private Role role;
        private boolean enabled;

        private List<PlateNumberResponseDto> plateNumbers;
        private List<VehicleResponseDto> vehiclesOwned;

}
