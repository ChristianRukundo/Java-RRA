package rca.ac.rw.template.plateNumber;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Add this
import org.springframework.stereotype.Repository;
// import rca.ac.rw.template.owner.Owner; // Not needed if using Specifications for findByOwner

// import java.util.List; // Not needed for List<PlateNumber> findByOwner if using Page with Specs
import org.springframework.data.domain.Page; // For paginated results
import org.springframework.data.domain.Pageable; // For Pageable
import rca.ac.rw.template.owner.Owner;
import rca.ac.rw.template.vehicle.Vehicle;


import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlateNumberRepository extends JpaRepository<PlateNumber, UUID>, JpaSpecificationExecutor<PlateNumber> { // Extend here

    // List<PlateNumber> findByOwner(Owner owner); // Can be replaced by findAll(Specification, Pageable)
    Page<PlateNumber> findByOwner(Owner owner, Pageable pageable); // Keep if you prefer direct method
    Page<PlateNumber> findByVehicle(Vehicle vehicle, Pageable pageable); // Keep if you prefer direct method

    Optional<PlateNumber> findByPlateNumber(String plateNumber);
}