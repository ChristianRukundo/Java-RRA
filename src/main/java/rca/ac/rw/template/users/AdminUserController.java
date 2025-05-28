package rca.ac.rw.template.users; // Or rca.ac.rw.template.admin

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Add SLF4J
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rca.ac.rw.template.users.dtos.AdminUserUpdateRequestDto;
import rca.ac.rw.template.users.dtos.UserProfileResponseDto;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminUserController {

    private final UserService userService;
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * GET /api/v1/admin/users : Get all users with pagination, sorting, and filtering.
     *
     * @param pageable      Pagination and sorting information.
     * @param search        Optional search term.
     * @param role          Optional role to filter by.
     * @param status        Optional status to filter by.
     * @param enabled       Optional enabled status to filter by.
     * @return Page of UserProfileResponseDto.
     */
    @GetMapping
    public ResponseEntity<Page<UserProfileResponseDto>> getAllUsers(
//
            @PageableDefault(
                    size = DEFAULT_PAGE_SIZE,
                    page = 0,
                    sort = "email",
                    direction = Sort.Direction.ASC
            ) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Boolean enabled) {
        log.info("Admin request to get all users with search: {}, role: {}, status: {}, enabled: {}", search, role, status, enabled);
        // Corrected method call:
        Page<UserProfileResponseDto> users = userService.getAllUsersForAdmin(pageable, search, role, status, enabled);
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/v1/admin/users/{userId} : Get a specific user by ID.
     *
     * @param userId The ID of the user.
     * @return UserProfileResponseDto for the found user.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponseDto> getUserById(@PathVariable UUID userId) {
        log.info("Admin request to get user by ID: {}", userId);
        UserProfileResponseDto user = userService.getUserByIdForAdmin(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * PUT /api/v1/admin/users/{userId} : Admin updates a user's details.
     *
     * @param userId    The ID of the user to update.
     * @param updateDto DTO with fields to update.
     * @return Updated UserProfileResponseDto.
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponseDto> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserUpdateRequestDto updateDto) {
        log.info("Admin request to update user ID: {}", userId);
        UserProfileResponseDto updatedUser = userService.updateUserByAdmin(userId, updateDto);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * DELETE /api/v1/admin/users/{userId} : Admin soft deletes a user.
     *
     * @param userId The ID of the user to soft delete.
     * @return HTTP 204 No Content.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> softDeleteUser(@PathVariable UUID userId) {
        log.info("Admin request to soft delete user ID: {}", userId);
        userService.softDeleteUserByAdmin(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/v1/admin/users/{userId}/activate : Admin activates a user account.
     *
     * @param userId The ID of the user to activate.
     * @return HTTP 200 OK with a success message.
     */
    @PatchMapping("/{userId}/activate")
    public ResponseEntity<String> activateUserAccount(@PathVariable UUID userId) {
        log.info("Admin request to activate account for user ID: {}", userId);
        userService.activateUserAccountByAdmin(userId);
        return ResponseEntity.ok("User account activated successfully.");
    }

    /**
     * PATCH /api/v1/admin/users/{userId}/status : Admin updates user status.
     *
     * @param userId    The ID of the user.
     * @param newStatus The new status to set (passed as request parameter).
     * @return HTTP 200 OK with a success message.
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam Status newStatus) {
        log.info("Admin request to update status for user ID: {} to {}", userId, newStatus);
        userService.updateUserStatusByAdmin(userId, newStatus);
        return ResponseEntity.ok("User status updated to " + newStatus + " successfully.");
    }
}