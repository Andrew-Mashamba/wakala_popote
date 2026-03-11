package com.quickcash.service;

import com.quickcash.domain.User;
import com.quickcash.dto.CreateUserRequest;
import com.quickcash.dto.UpdateLocationRequest;
import com.quickcash.dto.UpdateTokenRequest;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateLocation(User user) {
        user.setLastLocationUpdate(Instant.now());
        return userRepository.save(user);
    }

    @Transactional
    public void updateToken(String userId, String fcmToken) {
        User user = getById(userId);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Transactional
    public String createOrUpdateUser(CreateUserRequest req) {
        if (req.getUid() == null || req.getUid().isBlank()) {
            throw new IllegalArgumentException("uid is required");
        }
        User user = userRepository.findByUid(req.getUid())
                .map(existing -> {
                    existing.setDisplayName(req.getDisplayName());
                    existing.setEmail(req.getEmail());
                    existing.setEmailVerified(req.getEmailVerified());
                    existing.setPhoneNumber(req.getPhoneNumber());
                    existing.setPhotoUrl(req.getPhotoURL());
                    if (req.getToken() != null && !req.getToken().isBlank()) {
                        existing.setFcmToken(req.getToken());
                    }
                    return existing;
                })
                .orElseGet(() -> User.builder()
                        .uid(req.getUid())
                        .displayName(req.getDisplayName())
                        .email(req.getEmail())
                        .emailVerified(req.getEmailVerified())
                        .phoneNumber(req.getPhoneNumber())
                        .photoUrl(req.getPhotoURL())
                        .fcmToken(req.getToken())
                        .build());
        user = userRepository.save(user);
        return user.getId().toString();
    }

    @Transactional
    public void updateLocation(UpdateLocationRequest req) {
        User user = getById(req.getUserId());
        user.setLatitude(req.getLatitude());
        user.setLongitude(req.getLongitude());
        user.setAddress(req.getAddress());
        user.setSubLocality(req.getSubLocality());
        user.setLocality(req.getLocality());
        user.setAdministrativeArea(req.getAdministrativeArea());
        user.setPostalCode(req.getPostalCode());
        user.setCountry(req.getCountry());
        user.setLastLocationUpdate(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public void updateToken(UpdateTokenRequest req) {
        User user = getById(req.getUserId());
        user.setFcmToken(req.getFirebaseToken());
        userRepository.save(user);
    }

    public User getById(String userId) {
        UUID id = UUID.fromString(userId);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User getByUid(String uid) {
        return userRepository.findByUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User with uid", uid));
    }
}
