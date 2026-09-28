package com.example.jura.Service;

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
        if (user == null || !Objects.equals(user.getPasswordHash(), request.getPassword())) return null;
        return new LoginResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public int addUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return 2; // Email already exists
        }

        userRepository.save(user);
        return 0; // User added successfully
    }

    public int updateUser(Integer id, User user) {
        User oldUser = userRepository.findUserById(id);
        if (oldUser == null) {
            return 1; // User ID not found
        }

        if (!oldUser.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(user.getEmail())) {
            return 2; // Email already belongs to another user
        }

        oldUser.setName(user.getName());
        oldUser.setEmail(user.getEmail());
        oldUser.setMedicalConditions(user.getMedicalConditions());
        oldUser.setRole(user.getRole());
        oldUser.setPasswordHash(user.getPasswordHash());
        userRepository.save(oldUser);
        return 0; // User updated successfully
    }

    @Transactional
    public boolean deleteUser(Integer id) {
        User user = userRepository.findUserById(id);
        if (user == null) {
            return false;
        }

        dataCleanupService.cleanupUser(id);
        userRepository.delete(user);
        return true;
    }

    public User getUserById(Integer id) {
        return userRepository.findUserById(id);
    }



}
