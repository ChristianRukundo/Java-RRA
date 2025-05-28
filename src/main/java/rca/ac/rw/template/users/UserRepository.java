package rca.ac.rw.template.users;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    boolean existsByEmailOrPhoneNumberOrNationalId(String email, String phoneNumber, String nationalId);
    Optional<User> findByEmail(String email);

    Optional<User> findByNationalId(String nationalId);

    @Override
    Optional<User> findById(UUID userId);

    Optional<User> findByPhoneNumber(String phoneNumber);
}
