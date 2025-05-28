package rca.ac.rw.template.plateNumber;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rca.ac.rw.template.commons.exceptions.ResourceNotFoundException;
import rca.ac.rw.template.commons.exceptions.ValidationException;
import rca.ac.rw.template.owner.Owner;
import rca.ac.rw.template.owner.OwnerRepository;
import rca.ac.rw.template.ownership.OwnerShip;
import rca.ac.rw.template.ownership.OwnerShipRepository;
import rca.ac.rw.template.plateNumber.dto.IssueNewPlateRequestDto;
import rca.ac.rw.template.plateNumber.dto.PlateNumberResponseDto;
import rca.ac.rw.template.vehicle.Vehicle;
import rca.ac.rw.template.vehicle.VehicleRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class PlateNumberService {

    private final PlateNumberRepository plateNumberRepository;
    private final OwnerRepository ownerRepository;
    public  final OwnerShipRepository ownerShipRepository;
    private final VehicleRepository vehicleRepository;


    /**
     * Issues a new plate number to an existing vehicle for a specific owner.
     * Optionally marks an old plate as RETIRED if provided.
     *
     * @param dto Request containing vehicleId, ownerId, new plate string, and optional old plate string.
     * @return The DTO of the newly issued plate.
     */
    @Transactional

    public PlateNumberResponseDto issueNewPlateForVehicle(IssueNewPlateRequestDto dto) {
        log.info("Attempting to issue new plate {} for vehicle {} by alleged owner {}",
                dto.getPlateNumberString(), dto.getVehicleId(), dto.getOwnerId());

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "ID", dto.getVehicleId()));

        Owner specifiedOwnerInDto = ownerRepository.findById(dto.getOwnerId())
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Owner specified in DTO", "ID", dto.getOwnerId()));

        // --- CRUCIAL VALIDATION ---
        // Verify that the specifiedOwnerInDto is indeed the current owner of the vehicle.
        OwnerShip currentActualOwnerShip = ownerShipRepository.findFirstByVehicleAndEndDateIsNullOrderByStartDateDesc(vehicle)
                .orElseThrow(() -> new ValidationException("Vehicle ID " + vehicle.getId() + " has no current ownership record."));

        if (!currentActualOwnerShip.getOwner().getId().equals(specifiedOwnerInDto.getId())) {
            throw new ValidationException(String.format(
                    "Owner ID %s specified in request is not the current legal owner (ID %s) of vehicle ID %s.",
                    specifiedOwnerInDto.getId(), currentActualOwnerShip.getOwner().getId(), vehicle.getId()
            ));
        }
        // Now, 'specifiedOwnerInDto' is confirmed to be the current legal owner. Let's use this owner.
        Owner currentLegalOwner = specifiedOwnerInDto; // or currentActualOwnerShip.getOwner();

        // Validate new plate number uniqueness
        if (plateNumberRepository.findByPlateNumber(dto.getPlateNumberString())
                .filter(existingPlate -> existingPlate.getStatus() == PlateStatus.IN_USE && (existingPlate.getVehicle() == null || !existingPlate.getVehicle().getId().equals(vehicle.getId())) ) // Check if IN_USE on a DIFFERENT vehicle
                .isPresent()) {
            throw new ValidationException("Plate number '" + dto.getPlateNumberString() + "' is already IN_USE on another vehicle.");
        }

        // Deactivate any currently IN_USE plates ON THIS VEHICLE
        vehicle.getPlateNumbers().stream()
                .filter(pn -> pn.getStatus() == PlateStatus.IN_USE)
                .forEach(activePlate -> {
                    log.info("Marking existing active plate {} (ID: {}) as TRANSFERRED_OUT for vehicle {}",
                            activePlate.getPlateNumber(), activePlate.getId(), vehicle.getId());
                    activePlate.setStatus(PlateStatus.TRANSFERRED_OUT); // Or AVAILABLE, depending on precise rules
                    plateNumberRepository.save(activePlate);
                });

        PlateNumber newPlate;
        Optional<PlateNumber> existingPlateOpt = plateNumberRepository.findByPlateNumber(dto.getPlateNumberString());
        if (existingPlateOpt.isPresent()) {
            // Plate string exists. Can it be used?
            newPlate = existingPlateOpt.get();
            if (!newPlate.getOwner().getId().equals(currentLegalOwner.getId())) {
                throw new ValidationException(String.format("Plate number '%s' exists but is registered to a different owner (Owner ID %s), not the current vehicle owner (Owner ID %s).",
                        dto.getPlateNumberString(), newPlate.getOwner().getId(), currentLegalOwner.getId()));
            }
            if (newPlate.getStatus() != PlateStatus.AVAILABLE && newPlate.getStatus() != PlateStatus.TRANSFERRED_OUT) {
                throw new ValidationException(String.format("Plate number '%s' exists for this owner but is not in an assignable status (current: %s).",
                        dto.getPlateNumberString(), newPlate.getStatus()));
            }
            log.info("Re-activating plate {} for vehicle {} and owner {}", dto.getPlateNumberString(), vehicle.getId(), currentLegalOwner.getId());
        } else {
            // Plate string does not exist, create a new one
            newPlate = new PlateNumber();
            newPlate.setPlateNumber(dto.getPlateNumberString());
            log.info("Issuing brand new plate {} for vehicle {} and owner {}", dto.getPlateNumberString(), vehicle.getId(), currentLegalOwner.getId());
        }

        newPlate.setVehicle(vehicle);
        newPlate.setOwner(currentLegalOwner); // Plate is associated with the current legal owner of the vehicle
        newPlate.setStatus(PlateStatus.IN_USE);
        PlateNumber savedPlate = plateNumberRepository.save(newPlate);
        log.info("Plate {} successfully set to IN_USE for vehicle {} with owner {}", savedPlate.getPlateNumber(), vehicle.getId(), currentLegalOwner.getId());

        return PlateNumberConverter.toDto(savedPlate);
    }

    /**
     * Retrieves a plate number by its ID.
     * @param plateId The UUID of the plate number.
     * @return PlateNumberResponseDto.
     */
    @Transactional(readOnly = true)
    public PlateNumberResponseDto getPlateNumberById(UUID plateId) {
        PlateNumber plateNumber = plateNumberRepository.findById(plateId)
                .orElseThrow(() -> new ResourceNotFoundException("PlateNumber", "ID", plateId));
        return PlateNumberConverter.toDto(plateNumber);
    }

    /**
     * Retrieves all plate numbers with pagination and optional filtering.
     *
     * @param pageable Pagination information.
     * @param plateString Optional filter by plate number string (exact match or partial).
     * @param status Optional filter by plate status.
     * @param vehicleId Optional filter by vehicle ID.
     * @param ownerId Optional filter by owner ID.
     * @return Page of PlateNumberResponseDto.
     */
    @Transactional(readOnly = true)
    public Page<PlateNumberResponseDto> getAllPlateNumbers(Pageable pageable, String plateString, PlateStatus status, UUID vehicleId, UUID ownerId) {
        log.debug("Fetching all plate numbers. PlateString: '{}', Status: {}, VehicleId: {}, OwnerId: {}", plateString, status, vehicleId, ownerId);
        Specification<PlateNumber> spec = PlateNumberSpecifications.filterPlates(plateString, status, vehicleId, ownerId);
        Page<PlateNumber> platePage = plateNumberRepository.findAll(spec, pageable);

        List<PlateNumberResponseDto> dtos = platePage.getContent().stream()
                .map(PlateNumberConverter::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, platePage.getTotalElements());
    }

    /**
     * Retrieves all plate numbers associated with a specific owner.
     *
     * @param ownerId The UUID of the owner.
     * @param pageable Pagination information.
     * @return Page of PlateNumberResponseDto.
     */
    @Transactional(readOnly = true)
    public Page<PlateNumberResponseDto> getPlatesByOwner(UUID ownerId, Pageable pageable) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "ID", ownerId));

        Specification<PlateNumber> spec = PlateNumberSpecifications.filterPlates(null, null, null, ownerId);
        Page<PlateNumber> platePage = plateNumberRepository.findAll(spec, pageable);

        List<PlateNumberResponseDto> dtos = platePage.getContent().stream()
                .map(PlateNumberConverter::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, platePage.getTotalElements());
    }


    /**
     * Retrieves all plate numbers associated with a specific vehicle.
     * @param vehicleId The UUID of the vehicle.
     * @param pageable Pagination information.
     * @return Page of PlateNumberResponseDto.
     */
    @Transactional(readOnly = true)
    public Page<PlateNumberResponseDto> getPlatesByVehicle(UUID vehicleId, Pageable pageable) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "ID", vehicleId));
        // Similar to getPlatesByOwner, using Specification for consistency
        Specification<PlateNumber> spec = PlateNumberSpecifications.filterPlates(null, null, vehicleId, null);
        Page<PlateNumber> platePage = plateNumberRepository.findAll(spec, pageable);

        List<PlateNumberResponseDto> dtos = platePage.getContent().stream()
                .map(PlateNumberConverter::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, platePage.getTotalElements());
    }


    /**
     * Updates the status of a specific plate number.
     * @param plateId The ID of the plate number to update.
     * @param newStatus The new status to set for the plate.
     * @return The updated PlateNumberResponseDto.
     */
    @Transactional
    public PlateNumberResponseDto updatePlateStatus(UUID plateId, PlateStatus newStatus) {
        log.info("Attempting to update status of plate {} to {}", plateId, newStatus);
        PlateNumber plateNumber = plateNumberRepository.findById(plateId)
                .orElseThrow(() -> new ResourceNotFoundException("PlateNumber", "ID", plateId));

        // Add any business logic here, e.g., cannot change status from RETIRED, etc.
        if (plateNumber.getStatus() == PlateStatus.RETIRED && newStatus != PlateStatus.RETIRED) {
            throw new ValidationException("Cannot change the status of a RETIRED plate.");
        }

        plateNumber.setStatus(newStatus);
        PlateNumber updatedPlate = plateNumberRepository.save(plateNumber);
        log.info("Plate {} status updated to {}", updatedPlate.getId(), updatedPlate.getStatus());
        return PlateNumberConverter.toDto(updatedPlate);
    }
}