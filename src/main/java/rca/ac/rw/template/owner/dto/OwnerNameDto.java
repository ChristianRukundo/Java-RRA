package rca.ac.rw.template.owner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerNameDto {
    private UUID ownerId;
    private String firstName;
    private String lastName;
}