package rca.ac.rw.template.vehicle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import rca.ac.rw.template.audits.InitiatorAudit;
import rca.ac.rw.template.ownership.OwnerShip;
import rca.ac.rw.template.plateNumber.PlateNumber;


import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE vehicles SET deleted = true, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted = false")
public class Vehicle extends InitiatorAudit {


    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, name = "chassis_number", unique = true )
    private String chassisNumber;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(nullable = true, name = "manufacturer_company")
    private String manufacturerCompany;

    @Column(nullable = false, name = "manufacturer_year")
    private Year manufacturedYear;

    @Column(nullable = false)
    private BigDecimal price;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlateNumber> plateNumbers;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OwnerShip> ownerships;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false; // Field for soft delete

}