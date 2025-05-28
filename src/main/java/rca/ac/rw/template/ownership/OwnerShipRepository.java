package rca.ac.rw.template.ownership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Optional for more complex queries
import org.springframework.stereotype.Repository;
import rca.ac.rw.template.owner.Owner; // Import Owner
import rca.ac.rw.template.vehicle.Vehicle; // Import Vehicle

import java.util.List; // If you need list finders
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerShipRepository extends JpaRepository<OwnerShip, UUID> {


    Optional<OwnerShip> findFirstByVehicleAndEndDateIsNullOrderByStartDateDesc(Vehicle vehicle);


    Optional<OwnerShip> findFirstByVehicleAndOwnerAndEndDateIsNullOrderByStartDateDesc(Vehicle vehicle, Owner owner);


    List<OwnerShip> findByVehicleOrderByStartDateDesc(Vehicle vehicle);


}