package rca.ac.rw.template.users.dtos; // or a sub-package

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for address information in requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    @Size(max = 100, message = "Province cannot exceed 100 characters")
    private String province;

    @Size(max = 100, message = "District cannot exceed 100 characters")
    private String district;

    @Size(max = 100, message = "Sector cannot exceed 100 characters")
    private String sector;
}