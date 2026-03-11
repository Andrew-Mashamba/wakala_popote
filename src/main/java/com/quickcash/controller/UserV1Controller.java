package com.quickcash.controller;

import com.quickcash.auth.CurrentUser;
import com.quickcash.domain.User;
import com.quickcash.dto.UpdateLocationRequestV1;
import com.quickcash.dto.UpdateProfileRequest;
import com.quickcash.dto.UserProfileResponse;
import com.quickcash.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserV1Controller {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(@CurrentUser UUID userId) {
        User user = userService.getById(userId.toString());
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMe(@CurrentUser UUID userId,
                                                        @RequestBody @Valid UpdateProfileRequest request) {
        User user = userService.getById(userId.toString());
        if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getPhotoUrl() != null) user.setPhotoUrl(request.getPhotoUrl());
        user = userService.save(user);
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @PutMapping("/me/location")
    public ResponseEntity<UserProfileResponse> updateMyLocation(@CurrentUser UUID userId,
                                                                 @RequestBody @Valid UpdateLocationRequestV1 request) {
        User user = userService.getById(userId.toString());
        user.setLatitude(request.getLatitude());
        user.setLongitude(request.getLongitude());
        user.setAddress(request.getAddress());
        user.setSubLocality(request.getSubLocality());
        user.setLocality(request.getLocality());
        user.setAdministrativeArea(request.getAdministrativeArea());
        user.setPostalCode(request.getPostalCode());
        user.setCountry(request.getCountry());
        user = userService.updateLocation(user);
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @PutMapping("/me/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@CurrentUser UUID userId, @RequestBody MapBody body) {
        userService.updateToken(userId.toString(), body.getFcmToken());
        return ResponseEntity.ok().build();
    }

    private static UserProfileResponse toProfileResponse(User u) {
        return UserProfileResponse.builder()
                .id(u.getId())
                .uid(u.getUid())
                .displayName(u.getDisplayName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .photoUrl(u.getPhotoUrl())
                .latitude(u.getLatitude())
                .longitude(u.getLongitude())
                .address(u.getAddress())
                .userType(u.getUserType() != null ? u.getUserType().name() : null)
                .build();
    }

    @lombok.Data
    public static class MapBody {
        private String fcmToken;
    }
}
