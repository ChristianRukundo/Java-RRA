package rca.ac.rw.template.ownership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rca.ac.rw.template.owner.Owner;
import rca.ac.rw.template.vehicle.Vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerShipRepository extends JpaRepository<OwnerShip, UUID> {
    Optional<OwnerShip> findFirstByVehicleAndOwnerAndEndDateIsNullOrderByStartDateDesc(Vehicle vehicle, Owner owner);

    List<OwnerShip> findByVehicleOrderByStartDateDesc(Vehicle vehicle);
}