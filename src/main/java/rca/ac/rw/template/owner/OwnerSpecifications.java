// Create a new file, e.g., rca.ac.rw.template.owner.OwnerSpecifications.java
package rca.ac.rw.template.owner;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils; // Use Spring's StringUtils for checking empty/null

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;


public class OwnerSpecifications {

    /**
     * Creates a Specification for searching Owners by a general search term
     * across multiple fields (firstName, lastName, email, nationalId).
     * The search is case-insensitive and uses 'LIKE' for partial matches.
     *
     * @param searchTerm The term to search for.
     * @return A Specification that filters Owners.
     */
    public static Specification<Owner> searchOwners(String searchTerm) {
        return (Root<Owner> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            // If search term is null or empty, apply no filtering
            if (!StringUtils.hasText(searchTerm)) {
                return criteriaBuilder.conjunction(); // Returns a predicate that is always true
            }


            String lowerCaseSearchTerm = searchTerm.toLowerCase();


            Predicate firstNameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + lowerCaseSearchTerm + "%");
            Predicate lastNameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%" + lowerCaseSearchTerm + "%");
            Predicate emailLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + lowerCaseSearchTerm + "%");
            Predicate nationalIdLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("nationalId")), "%" + lowerCaseSearchTerm + "%");
            // Add other fields to search if needed (e.g., phoneNumber)

            // Combine the predicates using OR - match if term is found in ANY of these fields
            return criteriaBuilder.or(firstNameLike, lastNameLike, emailLike, nationalIdLike);
        };
    }


}
