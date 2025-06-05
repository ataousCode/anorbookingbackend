package com.almousleck.controller;

import com.almousleck.dto.auth.ApiResponse;
import com.almousleck.dto.user.ChangePasswordRequest;
import com.almousleck.dto.user.UpdateProfileRequest;
import com.almousleck.dto.user.UserProfileResponse;
import com.almousleck.security.CurrentUser;
import com.almousleck.security.UserPrincipal;
import com.almousleck.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    //$2a$10$fu0NfBbBoi5ApVqbxqC2senFmhosadiUcTTnR9LIGLnpf0utewRSe

    // 12345678

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(userService.getUserProfile(currentUser));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest updateRequest) {
        return ResponseEntity.ok(userService.updateProfile(currentUser, updateRequest));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        userService.changePassword(currentUser, changePasswordRequest);
        return ResponseEntity.ok(new ApiResponse(true, "Password changed successfully"));
    }
}
