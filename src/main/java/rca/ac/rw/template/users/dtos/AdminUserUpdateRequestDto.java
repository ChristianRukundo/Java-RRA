package rca.ac.rw.template.users.dtos;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rca.ac.rw.template.commons.validation.ValidRwandanPhoneNumber;
import rca.ac.rw.template.users.Role;
import rca.ac.rw.template.users.Status;

/**
 * DTO for an admin to update user details.
 * Allows modification of fields like role, status, enabled status.
 * Password changes for other users by admin should be a separate, secure flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserUpdateRequestDto {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName; // Optional: Admin can update

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName; // Optional: Admin can update


    @ValidRwandanPhoneNumber
    private String phoneNumber; // Optional: Admin can update


    private Role role; // Admin can change user role

    private Status status; // Admin can change user status

    private Boolean enabled; // Admin can enable/disable account

    @Valid
    private AddressDto address; // Admin can update address (using AddressDto for request)


}