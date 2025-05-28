package rca.ac.rw.template.vehicle;

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
import rca.ac.rw.template.vehicle.VehicleRegistrationService;
import rca.ac.rw.template.vehicle.VehicleService; // Import new service
import rca.ac.rw.template.vehicle.dto.RegisterVehicleAndIssuePlateRequestDto;
import rca.ac.rw.template.vehicle.dto.UpdateVehicleRequestDto; // Import DTO
import rca.ac.rw.template.vehicle.dto.VehicleResponseDto;

import java.time.Year; // Import Year
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/vehicles")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminVehicleController {


    private final VehicleService vehicleService;


    /**
     * POST /api/v1/admin/vehicles/register : Admin registers a new vehicle and issues its first plate.
     * (Task 3: Vehicle registration - Part 1)
     */
    @PostMapping("/register")
    public ResponseEntity<VehicleResponseDto> registerVehicleAndIssuePlate(
            @Valid @RequestBody RegisterVehicleAndIssuePlateRequestDto requestDto) {
        log.info("Admin request to register new vehicle and issue plate: Chassis {}", requestDto.getChassisNumber());
        VehicleResponseDto registeredVehicle = vehicleService.registerVehicleAndIssuePlate(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredVehicle);
    }

    /**
     * GET /api/v1/admin/vehicles/{vehicleId} : Admin displays details of a specific vehicle.
     * (Task 3: Vehicle registration - Part 2: Allow display of the details)
     * This now uses the VehicleService method which enriches the DTO.
     */
    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponseDto> getVehicleDetails(@PathVariable UUID vehicleId) {
        log.info("Admin request to get vehicle details for ID: {}", vehicleId);
        // Use VehicleService for richer details if VehicleRegistrationService.getVehicleDetails was basic
        VehicleResponseDto vehicleDetails = vehicleService.getVehicleByIdAdmin(vehicleId);
        return ResponseEntity.ok(vehicleDetails);
    }

    /**
     * GET /api/v1/admin/vehicles : Get all vehicles with pagination, sorting, and filtering.
     */
    @GetMapping
    public ResponseEntity<Page<VehicleResponseDto>> getAllVehicles(
            @PageableDefault(size = 10, sort = "chassisNumber") Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Year manufacturedYear) {
        log.info("Admin request to get all vehicles. Search: {}, Manufactured Year: {}", search, manufacturedYear);
        Page<VehicleResponseDto> vehicles = vehicleService.getAllVehiclesAdmin(pageable, search, manufacturedYear);
        return ResponseEntity.ok(vehicles);
    }

    /**
     * PUT /api/v1/admin/vehicles/{vehicleId} : Admin updates an existing vehicle's details.
     */
    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponseDto> updateVehicle(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleRequestDto updateDto) {
        log.info("Admin request to update vehicle ID: {}", vehicleId);
        VehicleResponseDto updatedVehicle = vehicleService.updateVehicleAdmin(vehicleId, updateDto);
        return ResponseEntity.ok(updatedVehicle);
    }

    /**
     * DELETE /api/v1/admin/vehicles/{vehicleId} : Admin soft deletes a vehicle.
     */
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> softDeleteVehicle(@PathVariable UUID vehicleId) {
        log.info("Admin request to soft delete vehicle ID: {}", vehicleId);
        vehicleService.softDeleteVehicleAdmin(vehicleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/admin/vehicles/search/by-owner-national-id?nationalId=...
     * Admin searches for vehicles by owner's national ID.
     */
    @GetMapping("/search/by-owner-national-id")
    public ResponseEntity<List<VehicleResponseDto>> searchVehiclesByOwnerNationalId(@RequestParam String nationalId) {
        log.info("Admin API search for vehicles by owner national ID: {}", nationalId);
        List<VehicleResponseDto> vehicles = vehicleService.findVehiclesByOwnerNationalId(nationalId);
        if (vehicles.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vehicles);
    }

    /**
     * GET /api/v1/admin/vehicles/search/by-plate-number?plateNumber=...
     * Admin searches for a vehicle by its plate number.
     */
    @GetMapping("/search/by-plate-number")
    public ResponseEntity<VehicleResponseDto> searchVehicleByPlateNumber(@RequestParam String plateNumber) {
        log.info("Admin API search for vehicle by plate number: {}", plateNumber);
        Optional<VehicleResponseDto> vehicleOpt = vehicleService.findVehicleByPlateNumber(plateNumber);
        return vehicleOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/admin/vehicles/search/by-chassis-number?chassisNumber=...
     * Admin searches for a vehicle by its chassis number.
     */
    @GetMapping("/search/by-chassis-number")
    public ResponseEntity<VehicleResponseDto> searchVehicleByChassisNumber(@RequestParam String chassisNumber) {
        log.info("Admin API search for vehicle by chassis number: {}", chassisNumber);
        Optional<VehicleResponseDto> vehicleOpt = vehicleService.findVehicleByChassisNumber(chassisNumber);
        return vehicleOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


}