package rca.ac.rw.template.ownership;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rca.ac.rw.template.ownership.OwnerShipService;
import rca.ac.rw.template.ownership.dto.OwnershipRecordDto;
import rca.ac.rw.template.ownership.dto.VehicleTransferRequestDto;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ownership")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminOwnershipController {

    private final OwnerShipService ownershipService;

    /**
     * POST /api/v1/admin/ownership/transfer : Admin transfers a vehicle's ownership.
     * (Task 4: Vehicle transfer - Parts 1, 2, 4)
     */
    @PostMapping("/transfer")
    public ResponseEntity<String> transferVehicleOwnership(
            @Valid @RequestBody VehicleTransferRequestDto transferRequestDto) {
        log.info("Admin API request to transfer vehicle ID: {}", transferRequestDto.getVehicleId());
        ownershipService.transferVehicleOwnership(transferRequestDto);
        return ResponseEntity.ok("Vehicle ownership transferred successfully.");
    }

    /**
     * GET /api/v1/admin/ownership/history/by-vehicle/{vehicleId} : Admin views ownership history of a vehicle by Vehicle ID.
     * (Task 5: History of Vehicle ownership)
     */
    @GetMapping("/history/by-vehicle/{vehicleId}")
    public ResponseEntity<List<OwnershipRecordDto>> getVehicleOwnershipHistoryByVehicleId(@PathVariable UUID vehicleId) {
        log.info("Admin API request for ownership history of vehicle ID: {}", vehicleId);
        List<OwnershipRecordDto> history = ownershipService.getVehicleOwnershipHistoryByVehicleId(vehicleId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/v1/admin/ownership/history/by-chassis?chassisNumber=...
     * Admin views ownership history by vehicle's chassis number.
     */
    @GetMapping("/history/by-chassis")
    public ResponseEntity<List<OwnershipRecordDto>> getVehicleOwnershipHistoryByChassis(@RequestParam String chassisNumber) {
        log.info("Admin API request for ownership history by chassis number: {}", chassisNumber);
        List<OwnershipRecordDto> history = ownershipService.getVehicleOwnershipHistoryByChassisNumber(chassisNumber);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/v1/admin/ownership/history/by-plate?plateNumber=...
     * Admin views ownership history by vehicle's plate number.
     */
    @GetMapping("/history/by-plate")
    public ResponseEntity<List<OwnershipRecordDto>> getVehicleOwnershipHistoryByPlate(@RequestParam String plateNumber) {
        log.info("Admin API request for ownership history by plate number: {}", plateNumber);
        List<OwnershipRecordDto> history = ownershipService.getVehicleOwnershipHistoryByPlateNumber(plateNumber);
        return ResponseEntity.ok(history);
    }
}