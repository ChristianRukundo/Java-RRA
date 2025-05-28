package rca.ac.rw.template.owner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rca.ac.rw.template.commons.exceptions.ValidationException;
import rca.ac.rw.template.owner.dto.OwnerResponseDto;
import rca.ac.rw.template.owner.dto.RegisterOwnerRequestDto;
import rca.ac.rw.template.users.Role;
import rca.ac.rw.template.users.Status;
import rca.ac.rw.template.users.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing Owner specific operations.
 * Owners are a specialized type of User.
 */
@Service
public class OwnerService {

    private static final Logger log = LoggerFactory.getLogger(OwnerService.class);
    private final OwnerRepository ownerRepository;
    private final UserRepository userRepository; // For checking existing users
    private final PasswordEncoder passwordEncoder;

    public OwnerService(OwnerRepository ownerRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.ownerRepository = ownerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new car owner in the system.
     * Owners are created with ROLE_CUSTOMER, disabled, and PENDING status initially.
     *
     * @param requestDto The DTO containing the new owner's details.
     * @return The Response DTO of the registered owner.
     * @throws ValidationException if a user with the same email, phone number, or national ID already exists.
     */
    @Transactional
    public OwnerResponseDto registerOwner(RegisterOwnerRequestDto requestDto) {
        log.info("Attempting to register new owner with email: {}", requestDto.getEmail());
        // Check for duplicates in the general user table
        if (userRepository.existsByEmailOrPhoneNumberOrNationalId(
                requestDto.getEmail(), requestDto.getPhoneNumber(), requestDto.getNationalId())) {
            throw new ValidationException("A user with this email, phone number, or national ID already exists.");
        }

        Owner owner = OwnerConverter.toEntity(requestDto); // Use converter

        owner.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        owner.setRole(Role.ROLE_CUSTOMER); // Owners are customers/standard users
        owner.setEnabled(false); // Requires account activation (e.g., OTP)
        owner.setStatus(Status.PENDING);

        Owner savedOwner = ownerRepository.save(owner);
        log.info("Owner registered successfully with ID: {}", savedOwner.getId());

        return OwnerConverter.toDto(savedOwner); // Use converter
    }


    /**
     * Retrieves all registered car owners with pagination, sorting, and optional search.
     * This method is kept for potential specific Owner views, though admins typically use UserService.getAllUsersForAdmin.
     *
     * @param pageable Spring Data Pageable for pagination and sorting information.
     * @param search   Optional search term to filter owners.
     * @return A Page of OwnerResponseDto containing the requested owners.
     */
    @Transactional(readOnly = true)
    public Page<OwnerResponseDto> getAllOwners(Pageable pageable, String search) {
        log.debug("Fetching all owners. Search: '{}'", search);
        Specification<Owner> spec = OwnerSpecifications.searchOwners(search);

        Page<Owner> ownerPage = ownerRepository.findAll(spec, pageable);

        List<OwnerResponseDto> ownerResponseDtos = ownerPage.getContent().stream()
                .map(OwnerConverter::toDto) // Use converter
                .collect(Collectors.toList());

        return new PageImpl<>(ownerResponseDtos, pageable, ownerPage.getTotalElements());
    }
}