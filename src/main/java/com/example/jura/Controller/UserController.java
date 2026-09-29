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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = userService.login(request);
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
        return ResponseEntity.status(200).body(user);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addUser(@RequestBody @Valid User user) {
        userService.addUser(user);
        return ResponseEntity.status(201).body(new ApiResponse("User added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody @Valid User user) {
        userService.updateUser(id, user);
        return ResponseEntity.status(200).body(new ApiResponse("User updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.status(200).body(new ApiResponse("User deleted successfully"));
    }
}
