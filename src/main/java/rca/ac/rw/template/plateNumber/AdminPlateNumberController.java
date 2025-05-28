package rca.ac.rw.template.admin;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rca.ac.rw.template.plateNumber.PlateNumberService;
import rca.ac.rw.template.plateNumber.PlateStatus;
import rca.ac.rw.template.plateNumber.dto.IssueNewPlateRequestDto;
import rca.ac.rw.template.plateNumber.dto.PlateNumberResponseDto;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/plates")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminPlateNumberController {

    private final PlateNumberService plateNumberService;

    /**
     * POST /api/v1/admin/plates/issue : Admin issues a new plate number to an existing vehicle for an owner.
     * This covers "Register plate number under a specific Owner" from Task 2.
     */
    @PostMapping("/issue")
    public ResponseEntity<PlateNumberResponseDto> issueNewPlate(
            @Valid @RequestBody IssueNewPlateRequestDto requestDto) {
        log.info("Admin request to issue new plate: {}", requestDto.getPlateNumberString());
        PlateNumberResponseDto issuedPlate = plateNumberService.issueNewPlateForVehicle(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(issuedPlate);
    }

    /**
     * GET /api/v1/admin/plates : Get all plate numbers with filtering and pagination.
     */
    @GetMapping
    public ResponseEntity<Page<PlateNumberResponseDto>> getAllPlateNumbers(
            @PageableDefault(size = 10, sort = "issuedDate") Pageable pageable,
            @RequestParam(required = false) String plateString,
            @RequestParam(required = false) PlateStatus status,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) UUID ownerId) {
        log.info("Admin request to get all plates. Filters - plateString: {}, status: {}, vehicleId: {}, ownerId: {}", plateString, status, vehicleId, ownerId);
        Page<PlateNumberResponseDto> plates = plateNumberService.getAllPlateNumbers(pageable, plateString, status, vehicleId, ownerId);
        return ResponseEntity.ok(plates);
    }

    /**
     * GET /api/v1/admin/plates/{plateId} : Get a specific plate number by its ID.
     */
    @GetMapping("/{plateId}")
    public ResponseEntity<PlateNumberResponseDto> getPlateNumberById(@PathVariable UUID plateId) {
        log.info("Admin request to get plate by ID: {}", plateId);
        PlateNumberResponseDto plate = plateNumberService.getPlateNumberById(plateId);
        return ResponseEntity.ok(plate);
    }

    /**
     * GET /api/v1/admin/plates/by-owner/{ownerId} : Display plate numbers associated with a given Owner.
     * This covers "Display plate numbers associated with a given Owner" from Task 2.
     */
    @GetMapping("/by-owner/{ownerId}")
    public ResponseEntity<Page<PlateNumberResponseDto>> getPlatesByOwner(
            @PathVariable UUID ownerId,
            @PageableDefault(size = 10, sort = "issuedDate,desc") Pageable pageable) {
        log.info("Admin request to get plates for owner ID: {}", ownerId);
        Page<PlateNumberResponseDto> plates = plateNumberService.getPlatesByOwner(ownerId, pageable);
        return ResponseEntity.ok(plates);
    }

    /**
     * GET /api/v1/admin/plates/by-vehicle/{vehicleId} : Display plate numbers associated with a given Vehicle.
     */
    @GetMapping("/by-vehicle/{vehicleId}")
    public ResponseEntity<Page<PlateNumberResponseDto>> getPlatesByVehicle(
            @PathVariable UUID vehicleId,
            @PageableDefault(size = 10, sort = "issuedDate,desc") Pageable pageable) {
        log.info("Admin request to get plates for vehicle ID: {}", vehicleId);
        Page<PlateNumberResponseDto> plates = plateNumberService.getPlatesByVehicle(vehicleId, pageable);
        return ResponseEntity.ok(plates);
    }

    /**
     * PATCH /api/v1/admin/plates/{plateId}/status : Admin updates the status of a plate.
     */
    @PatchMapping("/{plateId}/status")
    public ResponseEntity<PlateNumberResponseDto> updatePlateStatus(
            @PathVariable UUID plateId,
            @RequestParam PlateStatus newStatus) {
        log.info("Admin request to update status for plate ID: {} to {}", plateId, newStatus);
        PlateNumberResponseDto updatedPlate = plateNumberService.updatePlateStatus(plateId, newStatus);
        return ResponseEntity.ok(updatedPlate);
    }
}