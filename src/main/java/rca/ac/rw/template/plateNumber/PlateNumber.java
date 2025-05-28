package rca.ac.rw.template.plateNumber;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import rca.ac.rw.template.audits.InitiatorAudit;
import rca.ac.rw.template.owner.Owner;
import rca.ac.rw.template.users.Status;
import rca.ac.rw.template.vehicle.Vehicle;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plate_number")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlateNumber extends InitiatorAudit {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, name = "plate_number")
    private String plateNumber;

    @CreationTimestamp// Automatically set the timestamp when the entity is created
    @Column(name = "issued_date", nullable = false, updatable = false)
    private Instant issuedDate;

    // Many PlateNumbers -> One Owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    // Many PlateNumbers -> One Vehicle
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "plate_status", nullable = false)
    private PlateStatus status = PlateStatus.AVAILABLE;
}
