package rca.ac.rw.template.plateNumber;

public enum PlateStatus {
    IN_USE,         // Currently assigned and active on a vehicle
    AVAILABLE,      // Not assigned to any vehicle, can be reassigned (e.g., after transfer if policy requires new plate)
    TRANSFERRED_OUT,// Plate's association with previous owner ended due to vehicle transfer
    DAMAGED,        // Plate is damaged
    RETIRED         // Plate is permanently out of commission
}