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
        log.info("Initiating vehicle transfer for vehicle ID: {} from owner ID: {} to owner ID: {}",
                dto.getVehicleId(), dto.getCurrentOwnerId(), dto.getNewOwnerId());

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "ID", dto.getVehicleId()));

        Owner currentOwner = ownerRepository.findById(dto.getCurrentOwnerId())
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Current Owner", "ID", dto.getCurrentOwnerId()));

        Owner newOwner = ownerRepository.findById(dto.getNewOwnerId())
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("New Owner", "ID", dto.getNewOwnerId()));

        if (currentOwner.getId().equals(newOwner.getId())) {
            throw new ValidationException("Cannot transfer vehicle to the same owner.");
        }

        OwnerShip currentOwnerShipRecord = ownerShipRepository.findFirstByVehicleAndOwnerAndEndDateIsNullOrderByStartDateDesc(vehicle, currentOwner)
                .orElseThrow(() -> new ValidationException(
                        String.format("Owner ID %s does not currently own vehicle ID %s or ownership record is inconsistent.",
                                currentOwner.getId(), vehicle.getId())));

        // --- Plate Number Logic ---
        PlateNumber existingPlate = vehicle.getPlateNumbers().stream()
                .filter(pn -> pn.getStatus() == PlateStatus.IN_USE && pn.getOwner().getId().equals(currentOwner.getId()))
                .findFirst()
                .orElseThrow(() -> new ValidationException("No active IN_USE plate found for the vehicle under the current owner."));

        log.info("Marking existing plate {} as TRANSFERRED_OUT for vehicle ID {}.", existingPlate.getPlateNumber(), vehicle.getId());
        existingPlate.setStatus(PlateStatus.TRANSFERRED_OUT); // Or AVAILABLE based on policy
        // Detach from current vehicle and owner if the plate truly becomes "available" in a general pool
        // For now, status change is key. Reassignment happens when a new vehicle gets this plate.
        plateNumberRepository.save(existingPlate);

        // Handle the new plate for the new owner
        Optional<PlateNumber> newPlateOpt = plateNumberRepository.findByPlateNumber(dto.getNewPlateNumberStringForNewOwner());
        if (newPlateOpt.isPresent()) {
            PlateNumber potentialNewPlate = newPlateOpt.get();
            if (potentialNewPlate.getStatus() == PlateStatus.IN_USE) {
                // Allow reassigning if it's to the *same vehicle* (e.g., correcting an error)
                // but not if it's IN_USE on a *different* vehicle.
                if (potentialNewPlate.getVehicle() != null && !potentialNewPlate.getVehicle().getId().equals(vehicle.getId())) {
                    throw new ValidationException("The provided new plate number '" + dto.getNewPlateNumberStringForNewOwner() + "' is already in use on a different vehicle.");
                }
            }
            // If the plate exists, is AVAILABLE, and belongs to the new owner, re-use it.
            // Or if it's TRANSFERRED_OUT and belongs to the new owner (meaning they previously had it on another car)
            if ((potentialNewPlate.getStatus() == PlateStatus.AVAILABLE || potentialNewPlate.getStatus() == PlateStatus.TRANSFERRED_OUT)
                    && potentialNewPlate.getOwner().getId().equals(newOwner.getId())) {
                log.info("Re-assigning existing {} plate {} to vehicle {} for new owner {}.",
                        potentialNewPlate.getStatus(), potentialNewPlate.getPlateNumber(), vehicle.getId(), newOwner.getId());
                potentialNewPlate.setVehicle(vehicle); // Assign to this vehicle
                potentialNewPlate.setStatus(PlateStatus.IN_USE);
                plateNumberRepository.save(potentialNewPlate);
            } else {
                throw new ValidationException(
                        String.format("Plate number '%s' exists but is not in an assignable status (AVAILABLE/TRANSFERRED_OUT) or not associated with the new owner ID %s.",
                                dto.getNewPlateNumberStringForNewOwner(), newOwner.getId()));
            }
        } else {
            // Plate does not exist, issue it as a brand new plate for the new owner and this vehicle
            log.info("Issuing new plate {} for new owner {} and vehicle {}.",
                    dto.getNewPlateNumberStringForNewOwner(), newOwner.getId(), vehicle.getId());
            PlateNumber brandNewPlate = new PlateNumber();
            brandNewPlate.setPlateNumber(dto.getNewPlateNumberStringForNewOwner());
            brandNewPlate.setOwner(newOwner);
            brandNewPlate.setVehicle(vehicle);
            brandNewPlate.setStatus(PlateStatus.IN_USE);
            plateNumberRepository.save(brandNewPlate);
        }

        // --- Update Ownership Records ---
        log.info("Ending current ownership (ID: {}) for vehicle ID {} by owner ID {}",
                currentOwnerShipRecord.getId(), vehicle.getId(), currentOwner.getId());
        currentOwnerShipRecord.setEndDate(Instant.now());
        ownerShipRepository.save(currentOwnerShipRecord);

        OwnerShip newOwnerShipRecord = new OwnerShip();
        newOwnerShipRecord.setVehicle(vehicle);
        newOwnerShipRecord.setOwner(newOwner);
        newOwnerShipRecord.setStartDate(Instant.now());
        newOwnerShipRecord.setEndDate(null);
        newOwnerShipRecord.setTransferAmount(dto.getTransferAmount());
        ownerShipRepository.save(newOwnerShipRecord);
        log.info("Created new ownership record for vehicle ID {} by new owner ID {}", vehicle.getId(), newOwner.getId());

        // --- Send Notifications ---
        sendTransferNotifications(currentOwner, newOwner, vehicle, existingPlate.getPlateNumber(), dto.getNewPlateNumberStringForNewOwner(), dto.getTransferAmount());

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