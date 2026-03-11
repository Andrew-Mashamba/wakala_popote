package com.quickcash.controller;

import com.quickcash.dto.CreateUserRequest;
import com.quickcash.dto.UpdateLocationRequest;
import com.quickcash.dto.UpdateTokenRequest;
import com.quickcash.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User endpoints. Paths match existing Flutter app (visa_agent) for backward compatibility.
 * See PROJECT.md for /api/v1/... equivalents.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;

    /**
     * Register or update user. Returns our internal user ID (UUID string) - Flutter app stores this as userId.
     */
    @PostMapping(value = "/createUser", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createUser(@RequestBody @Valid CreateUserRequest request) {
        String userId = userService.createOrUpdateUser(request);
        return ResponseEntity.ok(userId);
    }

    @PostMapping(value = "/updateUserLocation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateUserLocation(@RequestBody UpdateLocationRequest request) {
        userService.updateLocation(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/updateToken", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateToken(@RequestBody UpdateTokenRequest request) {
        userService.updateToken(request);
        return ResponseEntity.ok().build();
    }
}
