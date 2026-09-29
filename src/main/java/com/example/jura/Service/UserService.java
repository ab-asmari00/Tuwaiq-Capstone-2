package com.example.jura.Service;

import com.example.jura.Api.ApiException;
import com.example.jura.Api.LoginRequest;
import com.example.jura.Api.LoginResponse;
import com.example.jura.Model.User;
import com.example.jura.Repository.UserRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DataCleanupService dataCleanupService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findUserByEmail(request.getEmail());
        if (user == null || !Objects.equals(user.getPasswordHash(), request.getPassword())) {
            throw new ApiException("Invalid email or password");
        }
        return new LoginResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User ID not found"));
    }

    public void addUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ApiException("Email already exists");
        }
        userRepository.save(user);
    }

    public void updateUser(Integer id, User user) {
        User oldUser = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User ID not found"));

        if (!oldUser.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(user.getEmail())) {
            throw new ApiException("Email already exists");
        }

        oldUser.setName(user.getName());
        oldUser.setEmail(user.getEmail());
        oldUser.setMedicalConditions(user.getMedicalConditions());
        oldUser.setRole(user.getRole());
        oldUser.setPasswordHash(user.getPasswordHash());
        userRepository.save(oldUser);
    }

    @Transactional
    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User ID not found"));
        dataCleanupService.cleanupUser(id);
        userRepository.delete(user);
    }
}
