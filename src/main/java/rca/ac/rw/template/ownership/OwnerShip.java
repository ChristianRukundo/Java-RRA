package rca.ac.rw.template.ownership;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import rca.ac.rw.template.audits.InitiatorAudit;
import rca.ac.rw.template.owner.Owner;
import rca.ac.rw.template.vehicle.Vehicle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ownership")
public class OwnerShip extends InitiatorAudit {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Vehicle is mandatory for ownership history")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @NotNull(message = "Owner is mandatory for ownership history")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;


    @NotNull(message = "Ownership start date is mandatory")
    @PastOrPresent(message = "Ownership start date cannot be in the future")
    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @PastOrPresent(message = "Ownership end date cannot be in the future")
    @Column(name = "end_date")
    private Instant endDate;

    @DecimalMin(value = "0.0", inclusive = true, message = "Transfer amount must be positive or zero")
    @Column(name = "transfer_amount")
    private BigDecimal transferAmount;
}
