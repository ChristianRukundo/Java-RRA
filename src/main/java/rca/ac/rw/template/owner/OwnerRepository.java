package rca.ac.rw.template.owner;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, UUID> , JpaSpecificationExecutor<Owner> {
    Optional<Owner> findByNationalId(String nationalId);
    Optional<Owner> findByPhoneNumber(String phoneNumber);
    Optional<Owner> findByEmail(String email);

//    Page<Owner> findAll(Specification<Owner> spec, Pageable pageable);
}
