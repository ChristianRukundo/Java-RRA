package rca.ac.rw.template.users;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String province;
    private String district;
    private String sector;
}
