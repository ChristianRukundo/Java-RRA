package rca.ac.rw.template.vehicle;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class VehicleSpecifications {

    public static Specification<Vehicle> adminSearchVehicles(String searchTerm, Year manufacturedYear) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(searchTerm)) {
                String lowerCaseSearchTerm = searchTerm.toLowerCase().trim();
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("chassisNumber")), "%" + lowerCaseSearchTerm + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("modelName")), "%" + lowerCaseSearchTerm + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("manufacturerCompany")), "%" + lowerCaseSearchTerm + "%")
                ));
            }

            if (manufacturedYear != null) {
                predicates.add(criteriaBuilder.equal(root.get("manufacturedYear"), manufacturedYear));
            }

            // @Where(clause = "deleted = false") on Vehicle entity handles soft delete filtering.
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}