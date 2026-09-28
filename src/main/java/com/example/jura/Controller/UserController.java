package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Api.LoginRequest;
import com.example.jura.Api.LoginResponse;
import com.example.jura.Model.User;
import com.example.jura.Service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request, Errors errors) {
        if (errors.hasErrors()) return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        LoginResponse response = userService.login(request);
        if (response == null) return ResponseEntity.status(401).body(new ApiResponse("Invalid email or password"));
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userService.getAllUsers();

        if (users.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Users list is empty"));
        }

        return ResponseEntity.status(200).body(users);
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.status(404).body(new ApiResponse("User ID not found"));
        }
        return ResponseEntity.status(200).body(user);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addUser(@RequestBody @Valid User user, Errors errors) {
        if (errors.hasErrors()) {
            String message = errors.getFieldError().getDefaultMessage();
            return ResponseEntity.status(400).body(new ApiResponse(message));
        }

        int result = userService.addUser(user);
        if (result == 2) {
            return ResponseEntity.status(409).body(new ApiResponse("Email already exists"));
        }
        return ResponseEntity.status(201).body(new ApiResponse("User added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody @Valid User user, Errors errors) {
        if (errors.hasErrors()) {
            String message = errors.getFieldError().getDefaultMessage();
            return ResponseEntity.status(400).body(new ApiResponse(message));
        }

        int result = userService.updateUser(id, user);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("User ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(409).body(new ApiResponse("Email already exists"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("User updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        boolean isFound = userService.deleteUser(id);
        if (!isFound) {
            return ResponseEntity.status(404).body(new ApiResponse("User ID not found"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("User deleted successfully"));
    }
}
