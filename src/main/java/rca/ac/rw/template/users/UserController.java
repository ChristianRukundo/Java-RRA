package rca.ac.rw.template.users; // Or rca.ac.rw.template.profile

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rca.ac.rw.template.users.dtos.UpdateUserProfileRequestDto;
import rca.ac.rw.template.users.dtos.UserProfileResponseDto;


@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @return ResponseEntity containing the UserProfileResponseDto.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponseDto> getMyProfile() {
        UserProfileResponseDto profile = userService.getMyProfile();
        return ResponseEntity.ok(profile);
    }

    /**
     * Updates the profile of the currently authenticated user.
     *
     * @param updateUserProfileRequestDto DTO containing the profile information to update.
     * @return ResponseEntity containing the updated UserProfileResponseDto.
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponseDto> updateMyProfile(
            @Valid @RequestBody UpdateUserProfileRequestDto updateUserProfileRequestDto) {
        UserProfileResponseDto updatedProfile = userService.updateMyProfile(updateUserProfileRequestDto);
        return ResponseEntity.ok(updatedProfile);
    }
}