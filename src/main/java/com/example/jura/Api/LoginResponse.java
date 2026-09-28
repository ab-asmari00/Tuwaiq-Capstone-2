package com.example.jura.Api;
import lombok.*;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private Integer id;
    private String name;
    private String email;
    private String role;
}
