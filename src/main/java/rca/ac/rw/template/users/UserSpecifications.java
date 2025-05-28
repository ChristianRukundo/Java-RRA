package rca.ac.rw.template.users;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class UserSpecifications {

    /**
     * Creates a Specification for searching and filtering Users for an admin view.
     *
     * @param searchTerm    A general term to search across firstName, lastName, email, nationalId, phoneNumber. Case-insensitive.
     * @param roleFilter    Optional Role to filter by.
     * @param statusFilter  Optional Status to filter by.
     * @param enabledFilter Optional Boolean to filter by enabled status.
     * @return A Specification<User> for querying.
     */
    public static Specification<User> adminSearchUsers(String searchTerm, Role roleFilter, Status statusFilter, Boolean enabledFilter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search Term Predicate (searches multiple fields with OR)
            if (StringUtils.hasText(searchTerm)) {
                String lowerCaseSearchTerm = searchTerm.toLowerCase().trim();
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + lowerCaseSearchTerm + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%" + lowerCaseSearchTerm + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + lowerCaseSearchTerm + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nationalId")), "%" + lowerCaseSearchTerm + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), "%" + lowerCaseSearchTerm + "%")
                );
                predicates.add(searchPredicate);
            }

            // Role Filter Predicate
            if (roleFilter != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), roleFilter));
            }

            // Status Filter Predicate
            if (statusFilter != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), statusFilter));
            }

            // Enabled Filter Predicate
            if (enabledFilter != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabledFilter));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}