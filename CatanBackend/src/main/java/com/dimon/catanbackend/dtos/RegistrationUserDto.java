package com.dimon.catanbackend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationUserDto {
    @NotEmpty(message = "Username is required")
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    private String username;

    @NotEmpty(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotEmpty(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotEmpty(message = "Re-enter Password is required")
    private String re_password;

    private MultipartFile profilePhoto;


    public RegistrationUserDto(String username, String email, String password, String re_password) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.re_password = re_password;
    }
}
