package rca.ac.rw.template.vehicle;

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
import rca.ac.rw.template.owner.dto.OwnerNameDto;
import rca.ac.rw.template.ownership.OwnerShip;
import rca.ac.rw.template.ownership.OwnerShipRepository;
import rca.ac.rw.template.plateNumber.PlateNumber;
import rca.ac.rw.template.plateNumber.PlateNumberConverter;
import rca.ac.rw.template.plateNumber.PlateNumberRepository;
import rca.ac.rw.template.plateNumber.PlateStatus;
import rca.ac.rw.template.vehicle.dto.RegisterVehicleAndIssuePlateRequestDto;
import rca.ac.rw.template.vehicle.dto.UpdateVehicleRequestDto;
import rca.ac.rw.template.vehicle.dto.VehicleResponseDto;

import java.time.Instant;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final OwnerRepository ownerRepository; // Needed for search by owner's national ID
    private final PlateNumberRepository plateNumberRepository; // Needed for search by plate number
    private final OwnerShipRepository ownerShipRepository;



    /**
     * Registers a new vehicle, issues a plate for it, and creates the initial ownership record.
     *
     * @param dto The request DTO containing vehicle, owner, and plate details.
     * @return A {@link VehicleResponseDto} for the newly registered vehicle.
     * @throws ResourceNotFoundException if the specified owner does not exist.
     * @throws ValidationException if the chassis number or plate number already exists.
     */
    @Transactional
    public VehicleResponseDto registerVehicleAndIssuePlate(RegisterVehicleAndIssuePlateRequestDto dto) {
        log.info("Attempting to register vehicle with chassis {} and plate {}", dto.getChassisNumber(), dto.getPlateNumberString());

        Owner owner = ownerRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "ID", dto.getOwnerId()));

        if (vehicleRepository.findByChassisNumber(dto.getChassisNumber()).isPresent()) {
            throw new ValidationException("Vehicle with chassis number '" + dto.getChassisNumber() + "' already exists.");
        }
        if (plateNumberRepository.findByPlateNumber(dto.getPlateNumberString()).isPresent()) {
            throw new ValidationException("Plate number '" + dto.getPlateNumberString() + "' is already registered.");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setChassisNumber(dto.getChassisNumber());
        vehicle.setModelName(dto.getModelName());
        vehicle.setManufacturerCompany(dto.getManufacturerCompany());
        vehicle.setManufacturedYear(dto.getManufacturedYear());
        vehicle.setPrice(dto.getPrice());
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle created with ID: {}", savedVehicle.getId());

        PlateNumber plateNumber = new PlateNumber();
        plateNumber.setPlateNumber(dto.getPlateNumberString());
        plateNumber.setOwner(owner);
        plateNumber.setVehicle(savedVehicle);
        plateNumber.setStatus(PlateStatus.IN_USE); // Set initial status
        // issuedDate is handled by @CreationTimestamp on PlateNumber entity's 'createdAt' or a specific 'issuedDate' field
        // If 'issuedDate' is separate from 'createdAt' on PlateNumber, set it here: plateNumber.setIssuedDate(Instant.now());
        PlateNumber savedPlateNumber = plateNumberRepository.save(plateNumber);
        log.info("Plate number {} issued with ID: {} for vehicle {}", savedPlateNumber.getPlateNumber(), savedPlateNumber.getId(), savedVehicle.getId());


        OwnerShip ownerShip = new OwnerShip();
        ownerShip.setVehicle(savedVehicle);
        ownerShip.setOwner(owner);
        ownerShip.setStartDate(Instant.now());
        ownerShip.setEndDate(null);
        ownerShip.setTransferAmount(savedVehicle.getPrice());
        ownerShipRepository.save(ownerShip);
        log.info("Initial ownership record created for vehicle ID {} and owner ID {}", savedVehicle.getId(), owner.getId());

        // Construct a detailed response
        VehicleResponseDto responseDto = VehicleConverter.toDto(savedVehicle);
        responseDto.setCurrentPlate(PlateNumberConverter.toDto(savedPlateNumber));
        responseDto.setCurrentOwner(new OwnerNameDto(owner.getId(), owner.getFirstName(), owner.getLastName()));

        return responseDto;
    }


    /**
     * Retrieves a paginated list of all vehicles for admin purposes.
     * Soft-deleted vehicles are excluded by default due to @Where clause on Vehicle entity.
     *
     * @param pageable      Pagination and sorting information.
     * @param searchTerm    Optional term to search by chassis, model, or manufacturer.
     * @param manufacturedYear Optional year to filter by.
     * @return A Page of VehicleResponseDto.
     */
    @Transactional(readOnly = true)
    public Page<VehicleResponseDto> getAllVehiclesAdmin(Pageable pageable, String searchTerm, Year manufacturedYear) {
        log.debug("Fetching all vehicles for admin. Search: '{}', Manufactured Year: {}", searchTerm, manufacturedYear);
        Specification<Vehicle> spec = VehicleSpecifications.adminSearchVehicles(searchTerm, manufacturedYear);
        Page<Vehicle> vehiclePage = vehicleRepository.findAll(spec, pageable);

        List<VehicleResponseDto> dtos = vehiclePage.getContent().stream()
                .map(this::enrichVehicleResponseDto) // Use helper to enrich DTO
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, vehiclePage.getTotalElements());
    }

    /**
     * Retrieves a single vehicle by ID for admin purposes, enriched with current owner and plate.
     *
     * @param vehicleId The ID of the vehicle to retrieve.
     * @return Enriched VehicleResponseDto for the found vehicle.
     * @throws ResourceNotFoundException if vehicle with vehicleId is not found.
     */
    @Transactional(readOnly = true)
    public VehicleResponseDto getVehicleByIdAdmin(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "ID", vehicleId));
        return enrichVehicleResponseDto(vehicle);
    }

    /**
     * Updates an existing vehicle's details by an admin.
     *
     * @param vehicleId The ID of the vehicle to update.
     * @param updateDto DTO containing the fields to update.
     * @return VehicleResponseDto of the updated vehicle.
     */
    @Transactional
    public VehicleResponseDto updateVehicleAdmin(UUID vehicleId, UpdateVehicleRequestDto updateDto) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "ID", vehicleId));

        log.info("Admin updating vehicle ID: {}", vehicleId);

        if (updateDto.getModelName() != null) {
            vehicle.setModelName(updateDto.getModelName());
        }
        if (updateDto.getManufacturerCompany() != null) {
            vehicle.setManufacturerCompany(updateDto.getManufacturerCompany());
        }
        if (updateDto.getManufacturedYear() != null) {
            vehicle.setManufacturedYear(updateDto.getManufacturedYear());
        }
        if (updateDto.getPrice() != null) {
            vehicle.setPrice(updateDto.getPrice());
        }
        // Chassis number is not updated.

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return enrichVehicleResponseDto(savedVehicle);
    }

    /**
     * Soft deletes a vehicle by its ID.
     * This will trigger the @SQLDelete logic on the Vehicle entity.
     *
     * @param vehicleId The ID of the vehicle to soft delete.
     * @throws ResourceNotFoundException if vehicle with vehicleId is not found.
     */
    @Transactional
    public void softDeleteVehicleAdmin(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "ID", vehicleId));
        log.info("Admin soft deleting vehicle ID: {}", vehicleId);
        // Mark associated IN_USE plates as AVAILABLE or TRANSFERRED_OUT
        // This depends on business rules: if a vehicle is "deleted", what happens to its plate?
        vehicle.getPlateNumbers().stream()
                .filter(pn -> pn.getStatus() == PlateStatus.IN_USE)
                .forEach(plate -> {
                    plate.setStatus(PlateStatus.AVAILABLE); // Or another appropriate status
                    // plateNumberRepository.save(plate); // Assuming PlateNumberRepo is injected if needed here
                    // For now, we are not saving plate changes here to keep VehicleService focused.
                    // This might be better handled by an event or a higher-level service if plate status change is complex.
                    log.info("Plate {} for deleted vehicle {} marked as {}", plate.getPlateNumber(), vehicleId, plate.getStatus());
                });

        vehicleRepository.delete(vehicle); // Triggers @SQLDelete
    }


    /**
     * Helper method to convert a Vehicle entity to VehicleResponseDto and enrich it
     * with current owner and plate information.
     */
    private VehicleResponseDto enrichVehicleResponseDto(Vehicle vehicle) {
        VehicleResponseDto dto = VehicleConverter.toDto(vehicle); // Basic conversion

        // Find current active plate
        vehicle.getPlateNumbers().stream()
                .filter(pn -> pn.getStatus() == PlateStatus.IN_USE)
                .max(Comparator.comparing(PlateNumber::getCreatedAt))
                .ifPresent(plate -> dto.setCurrentPlate(PlateNumberConverter.toDto(plate)));

        // Find current owner
        vehicle.getOwnerships().stream()
                .filter(os -> os.getEndDate() == null)
                .findFirst()
                .ifPresent(currentOwnerShip -> {
                    Owner owner = currentOwnerShip.getOwner();
                    if (owner != null) {
                        dto.setCurrentOwner(new OwnerNameDto(owner.getId(), owner.getFirstName(), owner.getLastName()));
                    }
                });
        return dto;
    }


    /**
     * Searches for vehicles by the current owner's national ID.
     * Returns a list as an owner can have multiple vehicles.
     *
     * @param nationalId The national ID of the current owner.
     * @return A list of enriched VehicleResponseDto.
     */
    @Transactional(readOnly = true)
    public List<VehicleResponseDto> findVehiclesByOwnerNationalId(String nationalId) {
        log.debug("Admin searching for vehicles by owner's national ID: {}", nationalId);
        Optional<Owner> ownerOpt = ownerRepository.findByNationalId(nationalId); // Assuming OwnerRepository has this
        if (ownerOpt.isEmpty()) {
            log.warn("No owner found with national ID: {}", nationalId);
            return Collections.emptyList();
        }
        Owner owner = ownerOpt.get();

        // Find current ownerships for this owner
        // This assumes Owner entity has a 'ownerShips' collection properly mapped.
        // And that OwnerShip links to Vehicle.
        return owner.getOwnerShips().stream()
                .filter(os -> os.getEndDate() == null && os.getVehicle() != null) // Current ownership
                .map(OwnerShip::getVehicle)
                .filter(vehicle -> !vehicle.isDeleted()) // Ensure vehicle is not soft-deleted
                .map(this::enrichVehicleResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Finds a vehicle by its current plate number.
     *
     * @param plateNumberString The plate number string.
     * @return An Optional containing the enriched VehicleResponseDto if found.
     */
    @Transactional(readOnly = true)
    public Optional<VehicleResponseDto> findVehicleByPlateNumber(String plateNumberString) {
        log.debug("Admin searching for vehicle by plate number: {}", plateNumberString);
        Optional<PlateNumber> plateOpt = plateNumberRepository.findByPlateNumber(plateNumberString);
        if (plateOpt.isEmpty() || plateOpt.get().getVehicle() == null || plateOpt.get().getVehicle().isDeleted()) {
            log.warn("No active vehicle found for plate number: {}", plateNumberString);
            return Optional.empty();
        }
        // Ensure the plate is currently IN_USE for this vehicle
        if (plateOpt.get().getStatus() != PlateStatus.IN_USE) {
            log.warn("Plate {} found but is not currently IN_USE.", plateNumberString);
            return Optional.empty();
        }
        return Optional.of(enrichVehicleResponseDto(plateOpt.get().getVehicle()));
    }

    /**
     * Finds a vehicle by its chassis number.
     *
     * @param chassisNumber The chassis number.
     * @return An Optional containing the enriched VehicleResponseDto if found.
     */
    @Transactional(readOnly = true)
    public Optional<VehicleResponseDto> findVehicleByChassisNumber(String chassisNumber) {
        log.debug("Admin searching for vehicle by chassis number: {}", chassisNumber);
        Optional<Vehicle> vehicleOpt = vehicleRepository.findByChassisNumber(chassisNumber);
        if (vehicleOpt.isEmpty() || vehicleOpt.get().isDeleted()) {
            log.warn("No active vehicle found for chassis number: {}", chassisNumber);
            throw new ResourceNotFoundException("vehicle", "Chassis number", chassisNumber);
        }
        return vehicleOpt.map(this::enrichVehicleResponseDto);
    }
}