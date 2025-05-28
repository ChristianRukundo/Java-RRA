package rca.ac.rw.template.vehicle;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import rca.ac.rw.template.plateNumber.PlateStatus; // Ensure this enum exists
import rca.ac.rw.template.vehicle.dto.RegisterVehicleAndIssuePlateRequestDto; // Corrected package
import rca.ac.rw.template.vehicle.dto.VehicleResponseDto; // Corrected package

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class VehicleRegistrationService {

    private final VehicleRepository vehicleRepository;
    private final OwnerRepository ownerRepository;
    private final PlateNumberRepository plateNumberRepository;
    private final OwnerShipRepository ownerShipRepository;
    // Removed VehicleMapper, will use VehicleConverter for basic mapping
    // and assemble richer DTOs in service methods.

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


}