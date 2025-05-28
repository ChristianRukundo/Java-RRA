package rca.ac.rw.template.users.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rca.ac.rw.template.commons.validation.ValidRwandanPhoneNumber;


/**
 * Data Transfer Object for updating a user's profile.
 * Fields like email and nationalId are typically not updatable by the user via this DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequestDto {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    @ValidRwandanPhoneNumber
    private String phoneNumber;

    @Valid
    private AddressDto address;


}