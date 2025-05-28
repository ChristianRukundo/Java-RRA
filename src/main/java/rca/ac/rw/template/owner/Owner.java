package rca.ac.rw.template.owner;

import jakarta.persistence.*;
import lombok.*;
import rca.ac.rw.template.ownership.OwnerShip;
import rca.ac.rw.template.plateNumber.PlateNumber;
import rca.ac.rw.template.users.User;

import java.util.List;

@Entity
@Table(name = "owners")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Owner extends User {

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlateNumber> plateNumbers;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OwnerShip> ownerShips;
}
