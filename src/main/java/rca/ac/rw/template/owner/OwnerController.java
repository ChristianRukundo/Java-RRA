package rca.ac.rw.template.owner; // Consistent controller package naming

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rca.ac.rw.template.auth.OtpService;
import rca.ac.rw.template.auth.OtpType;
import rca.ac.rw.template.email.EmailService;
import rca.ac.rw.template.owner.dto.OwnerResponseDto;
import rca.ac.rw.template.owner.dto.RegisterOwnerRequestDto;

import java.util.List;


@RestController
@RequestMapping("/api/v1/owners")
public class OwnerController {

    private final OwnerService ownerService;
    private final OtpService otpService;
    private final EmailService emailService;

    @Autowired
    public OwnerController(OwnerService ownerService, OtpService otpService, EmailService emailService) {
        this.ownerService = ownerService;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    /**
     * Endpoint for ADMIN to register a new car owner.
     * Requires the authenticated user to have the 'ADMIN' role.
     *
     * @param requestDto The DTO containing the new owner's details.
     * @return ResponseEntity with the created OwnerResponseDto and HttpStatus.CREATED.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OwnerResponseDto> registerOwner(@Valid @RequestBody RegisterOwnerRequestDto requestDto) {
        OwnerResponseDto registeredOwner = ownerService.registerOwner(requestDto);

        var otpToSend = otpService.generateAndStoreOtp(registeredOwner.getEmail(), OtpType.VERIFY_ACCOUNT);
        emailService.sendAccountVerificationEmail(registeredOwner.getEmail(), registeredOwner.getFirstName(), otpToSend);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredOwner);
    }

    /**
     * Endpoint for ADMIN to retrieve all car owners with pagination, sorting, and search.
     * Requires the authenticated user to have the 'ADMIN' role.
     * Parameters are passed as query parameters:
     * - page (default 0)
     * - size (default 10)
     * - sort (e.g., firstName,asc or email,desc)
     * - search (optional term to filter results)
     *
     * @param pageable Spring Data Pageable automatically resolved from request parameters.
     * @param search   Optional search term from query parameter.
     * @return ResponseEntity with a Page of OwnerResponseDto and HttpStatus.OK.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OwnerResponseDto>> getAllOwners(
            @PageableDefault(size = 10, sort = "firstName") Pageable pageable,
            @RequestParam(required = false) String search
    ) {

        Page<OwnerResponseDto> ownersPage = ownerService.getAllOwners(pageable, search);


        return ResponseEntity.ok(ownersPage);
    }

}
