package rca.ac.rw.template.ownership;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rca.ac.rw.template.commons.exceptions.ResourceNotFoundException;
import rca.ac.rw.template.commons.exceptions.ValidationException;
import rca.ac.rw.template.email.EmailService;
import rca.ac.rw.template.owner.Owner;
import rca.ac.rw.template.owner.OwnerRepository;
import rca.ac.rw.template.owner.dto.OwnerNameDto;
import rca.ac.rw.template.ownership.dto.OwnershipRecordDto;
import rca.ac.rw.template.ownership.dto.VehicleTransferRequestDto;
import rca.ac.rw.template.plateNumber.PlateNumber;
import rca.ac.rw.template.plateNumber.PlateNumberRepository;
import rca.ac.rw.template.plateNumber.PlateStatus;
import rca.ac.rw.template.vehicle.Vehicle;
import rca.ac.rw.template.vehicle.VehicleRepository;
import rca.ac.rw.template.vehicle.dto.VehicleSummaryDto;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class OwnerShipService {

    private final VehicleRepository vehicleRepository;
    private final OwnerRepository ownerRepository;
    private final OwnerShipRepository ownerShipRepository;
    private final PlateNumberRepository plateNumberRepository;
    private final EmailService emailService; // For notifications
    // No specific OwnershipConverter/Mapper needed if we construct DTOs directly in service

        /**
     * Transfers ownership of a vehicle from a current owner to a new owner.
     * Handles plate number changes as per requirements.
     *
     * @param dto The request DTO containing transfer details.
     * @throws ResourceNotFoundException if vehicle, current owner, or new owner is not found.
     * @throws ValidationException for various business rule violations.
     */
    @Transactional
    public void transferVehicleOwnership(VehicleTransferRequestDto dto) {
        log.info("Admin initiating vehicle transfer for vehicle ID: {} to new owner ID: {}",
                dto.getVehicleId(), dto.getNewOwnerId()); // Removed currentOwnerId from initial log as it needs verification

        // 1. Validate Entities
        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "ID", dto.getVehicleId()));

        // Determine the ACTUAL current owner from ownership records
        OwnerShip currentActualOwnerShipRecord = ownerShipRepository.findFirstByVehicleAndEndDateIsNullOrderByStartDateDesc(vehicle)
                .orElseThrow(() -> new ValidationException("Vehicle ID " + vehicle.getId() + " has no current active ownership record. Cannot transfer."));
        Owner actualCurrentOwner = currentActualOwnerShipRecord.getOwner();

        // Verify the currentOwnerId from DTO matches the actual current owner (important check)
        if (!actualCurrentOwner.getId().equals(dto.getCurrentOwnerId())) {
            throw new ValidationException(String.format(
                    "The provided currentOwnerId (%s) does not match the vehicle's actual current owner (ID: %s). Transfer request invalid.",
                    dto.getCurrentOwnerId(), actualCurrentOwner.getId()
            ));
        }
        // Now, 'actualCurrentOwner' is the verified current owner.

        Owner newOwner = ownerRepository.findById(dto.getNewOwnerId())
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("New Owner", "ID", dto.getNewOwnerId()));

        if (actualCurrentOwner.getId().equals(newOwner.getId())) {
            throw new ValidationException("Cannot transfer vehicle to the same owner.");
        }

        // --- Plate Number Logic ---
        // Find the plate IN_USE on THIS vehicle. Its owner *must* be actualCurrentOwner.
        PlateNumber existingPlateOnVehicle = vehicle.getPlateNumbers().stream()
                .filter(pn -> pn.getStatus() == PlateStatus.IN_USE)
                .findFirst() // A vehicle should only have one IN_USE plate.
                .orElseThrow(() -> new ValidationException("No active IN_USE plate found for vehicle ID " + vehicle.getId() + ". Data inconsistency."));

        // Double check if the owner of this IN_USE plate is indeed the actualCurrentOwner
        if (!existingPlateOnVehicle.getOwner().getId().equals(actualCurrentOwner.getId())) {
            throw new ValidationException(String.format(
                    "Data inconsistency: Active plate %s on vehicle %s is owned by %s, but current vehicle owner is %s.",
                    existingPlateOnVehicle.getPlateNumber(), vehicle.getId(), existingPlateOnVehicle.getOwner().getId(), actualCurrentOwner.getId()
            ));
        }

        log.info("Marking existing plate {} (ID: {}) as TRANSFERRED_OUT for vehicle ID {}.",
                existingPlateOnVehicle.getPlateNumber(), existingPlateOnVehicle.getId(), vehicle.getId());
        existingPlateOnVehicle.setStatus(PlateStatus.TRANSFERRED_OUT);
        plateNumberRepository.save(existingPlateOnVehicle);

        // Handle the new plate for the new owner (using dto.getNewPlateNumberStringForNewOwner)
        // This logic remains largely the same as before, ensuring the new plate is valid for the newOwner.
        Optional<PlateNumber> potentialNewPlateOpt = plateNumberRepository.findByPlateNumber(dto.getNewPlateNumberStringForNewOwner());
        PlateNumber plateToAssignToVehicle;

        if (potentialNewPlateOpt.isPresent()) {
            PlateNumber foundPlate = potentialNewPlateOpt.get();
            // Validations: Must belong to newOwner, must be AVAILABLE/TRANSFERRED_OUT, or if IN_USE then on NO vehicle or THIS vehicle
            if (!foundPlate.getOwner().getId().equals(newOwner.getId())) {
                throw new ValidationException(String.format("Plate number '%s' for new owner exists but is registered to a different owner (Owner ID %s).",
                        dto.getNewPlateNumberStringForNewOwner(), foundPlate.getOwner().getId()));
            }
            if (foundPlate.getStatus() == PlateStatus.IN_USE) {
                if (foundPlate.getVehicle() != null && !foundPlate.getVehicle().getId().equals(vehicle.getId())) { // If IN_USE on another vehicle
                    throw new ValidationException(String.format("Plate number '%s' is already IN_USE on a different vehicle (ID: %s).",
                            dto.getNewPlateNumberStringForNewOwner(), foundPlate.getVehicle().getId()));
                }
                // If IN_USE on this vehicle AND owner is newOwner, this is a strange state but means plate is already set.
                plateToAssignToVehicle = foundPlate; // Already correct
            } else if (foundPlate.getStatus() == PlateStatus.AVAILABLE || foundPlate.getStatus() == PlateStatus.TRANSFERRED_OUT) {
                log.info("Re-assigning existing {} plate {} to vehicle {} for new owner {}.",
                        foundPlate.getStatus(), foundPlate.getPlateNumber(), vehicle.getId(), newOwner.getId());
                foundPlate.setVehicle(vehicle);
                foundPlate.setStatus(PlateStatus.IN_USE);
                plateToAssignToVehicle = plateNumberRepository.save(foundPlate);
            } else {
                throw new ValidationException(String.format("Plate number '%s' (owned by new owner) is not in an assignable status (current: %s).",
                        dto.getNewPlateNumberStringForNewOwner(), foundPlate.getStatus()));
            }
        } else {
            log.info("Issuing new plate {} for new owner {} and vehicle {}.",
                    dto.getNewPlateNumberStringForNewOwner(), newOwner.getId(), vehicle.getId());
            PlateNumber brandNewPlate = new PlateNumber();
            brandNewPlate.setPlateNumber(dto.getNewPlateNumberStringForNewOwner());
            brandNewPlate.setOwner(newOwner);
            brandNewPlate.setVehicle(vehicle);
            brandNewPlate.setStatus(PlateStatus.IN_USE);
            plateToAssignToVehicle = plateNumberRepository.save(brandNewPlate);
        }

        // --- Update Ownership Records ---
        log.info("Ending current ownership (ID: {}) for vehicle ID {} by owner ID {}",
                currentActualOwnerShipRecord.getId(), vehicle.getId(), actualCurrentOwner.getId());
        currentActualOwnerShipRecord.setEndDate(Instant.now());
        ownerShipRepository.save(currentActualOwnerShipRecord);

        OwnerShip newOwnerShipRecord = new OwnerShip();
        newOwnerShipRecord.setVehicle(vehicle);
        newOwnerShipRecord.setOwner(newOwner);
        newOwnerShipRecord.setStartDate(Instant.now());
        newOwnerShipRecord.setEndDate(null);
        newOwnerShipRecord.setTransferAmount(dto.getTransferAmount());
        ownerShipRepository.save(newOwnerShipRecord);
        log.info("Created new ownership record for vehicle ID {} by new owner ID {}", vehicle.getId(), newOwner.getId());

        sendTransferNotifications(actualCurrentOwner, newOwner, vehicle,
                existingPlateOnVehicle.getPlateNumber(),
                plateToAssignToVehicle.getPlateNumber(),
                dto.getTransferAmount());

        log.info("Vehicle transfer completed successfully for vehicle ID: {}", vehicle.getId());
    }


    /**
     * Retrieves the ownership history for a given vehicle ID.
     *
     * @param vehicleId The UUID of the vehicle.
     * @return A list of OwnershipRecordDto, sorted by start date descending.
     */
    @Transactional(readOnly = true)
    public List<OwnershipRecordDto> getVehicleOwnershipHistoryByVehicleId(UUID vehicleId) {
        log.debug("Fetching ownership history for vehicle ID: {}", vehicleId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "ID", vehicleId));

        return vehicle.getOwnerships().stream()
                .sorted(Comparator.comparing(OwnerShip::getStartDate).reversed())
                .map(this::mapToOwnershipRecordDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the ownership history for a vehicle identified by its chassis number.
     */
    @Transactional(readOnly = true)
    public List<OwnershipRecordDto> getVehicleOwnershipHistoryByChassisNumber(String chassisNumber) {
        Vehicle vehicle = vehicleRepository.findByChassisNumber(chassisNumber)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "Chassis Number", chassisNumber));
        return getVehicleOwnershipHistoryByVehicleId(vehicle.getId());
    }

    /**
     * Retrieves the ownership history for a vehicle currently or previously associated with a plate number.
     */
    @Transactional(readOnly = true)
    public List<OwnershipRecordDto> getVehicleOwnershipHistoryByPlateNumber(String plateNumberString) {
        // This might return history for multiple vehicles if a plate was re-used over time,
        // but typically a plate is tied to one vehicle at a time.
        // We'll find the *latest* vehicle associated with this plate if it has moved.
        // Or, if a plate is unique to a vehicle throughout its life, this is simpler.
        // For now, let's assume we find the vehicle the plate is currently or was most recently on.
        PlateNumber plate = plateNumberRepository.findByPlateNumber(plateNumberString)
                // .filter(p -> p.getVehicle() != null) // Ensure it's associated with a vehicle
                .orElseThrow(() -> new ResourceNotFoundException("PlateNumber", "Number", plateNumberString));

        if(plate.getVehicle() == null){
            log.warn("Plate number {} is not currently associated with any vehicle.", plateNumberString);
            throw new ResourceNotFoundException("Vehicle", "associated with plate", plateNumberString + " (plate not assigned to a vehicle)");
        }
        if (plate.getVehicle().isDeleted()){
            log.warn("Vehicle associated with plate {} (Vehicle ID: {}) is soft-deleted.", plateNumberString, plate.getVehicle().getId());
            throw new ResourceNotFoundException("Vehicle", "associated with plate", plateNumberString + " (vehicle deleted)");
        }
        return getVehicleOwnershipHistoryByVehicleId(plate.getVehicle().getId());
    }


    // --- Helper Methods ---

    private OwnershipRecordDto mapToOwnershipRecordDto(OwnerShip os) {
        VehicleSummaryDto vehicleSummary = null;
        if (os.getVehicle() != null) {
            vehicleSummary = new VehicleSummaryDto(
                    os.getVehicle().getId(),
                    os.getVehicle().getChassisNumber(),
                    os.getVehicle().getModelName()
            );
        }
        OwnerNameDto ownerDetails = null;
        if (os.getOwner() != null) {
            ownerDetails = new OwnerNameDto(
                    os.getOwner().getId(),
                    os.getOwner().getFirstName(),
                    os.getOwner().getLastName()
            );
        }
        return new OwnershipRecordDto(
                os.getId(),
                vehicleSummary,
                ownerDetails,
                os.getStartDate(),
                os.getEndDate(),
                os.getTransferAmount()
        );
    }

    private void sendTransferNotifications(Owner fromOwner, Owner toOwner, Vehicle vehicle,
                                           String oldPlate, String newPlate, BigDecimal amount) {
        try {
            emailService.sendOwnershipTransferEmailToSender(
                    fromOwner.getEmail(),
                    fromOwner.getFirstName() + " " + fromOwner.getLastName(),
                    vehicle.getChassisNumber(),
                    oldPlate,
                    amount,
                    toOwner.getFirstName() + " " +toOwner.getLastName());

            emailService.sendOwnershipTransferEmailToReceiver(
                    toOwner.getEmail(),
                    toOwner.getFirstName() + " " + toOwner.getLastName(),
                    vehicle.getChassisNumber(),
                    newPlate,
                    amount,
                    fromOwner.getFirstName() + " "+ fromOwner.getLastName());
            log.info("Ownership transfer notification emails sent for vehicle chassis: {}", vehicle.getChassisNumber());
        } catch (Exception e) {
            log.error("Failed to send ownership transfer notification emails for vehicle chassis {}: {}",
                    vehicle.getChassisNumber(), e.getMessage(), e);
        }
    }
}