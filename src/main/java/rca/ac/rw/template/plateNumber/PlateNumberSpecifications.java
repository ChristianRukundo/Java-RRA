package rca.ac.rw.template.plateNumber;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import rca.ac.rw.template.owner.Owner;
import rca.ac.rw.template.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlateNumberSpecifications {

    public static Specification<PlateNumber> filterPlates(String plateString, PlateStatus status, UUID vehicleId, UUID ownerId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(plateString)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("plateNumber")), "%" + plateString.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (vehicleId != null) {
                Join<PlateNumber, Vehicle> vehicleJoin = root.join("vehicle");
                predicates.add(criteriaBuilder.equal(vehicleJoin.get("id"), vehicleId));
            }
            if (ownerId != null) {
                Join<PlateNumber, Owner> ownerJoin = root.join("owner");
                predicates.add(criteriaBuilder.equal(ownerJoin.get("id"), ownerId));
            }
            // @Where on PlateNumber entity would handle soft delete filtering if implemented for PlateNumber
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}