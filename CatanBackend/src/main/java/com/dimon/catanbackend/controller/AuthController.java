package com.dimon.catanbackend.controller;
import com.dimon.catanbackend.dtos.JwtRequest;
import com.dimon.catanbackend.dtos.RegistrationUserDto;
import com.dimon.catanbackend.entities.User;
import com.dimon.catanbackend.service.AuthService;
import com.dimon.catanbackend.service.FileStorageService;
import com.dimon.catanbackend.service.UserService;
import com.dimon.catanbackend.utils.JwtTokenUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;
    private final FileStorageService fileStorageService;


    @PostMapping("/login")
    public ResponseEntity<?> createAuthToken(@RequestBody @Valid JwtRequest authRequest) {
        return authService.createAuthToken(authRequest);
    }

    @PostMapping("/registration")
    public ResponseEntity<?> createNewUser(@RequestBody RegistrationUserDto userDto) {
        System.out.println("I am in registration");
        System.out.println(userDto.getEmail());
        return authService.createNewUser(userDto);
    }

    @PostMapping("/activation")
    public ResponseEntity<?> activateAccount(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        return authService.activateAccount(token);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user);
    }

    @PostMapping("verify")
    public ResponseEntity<?> verifyToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body("Token is missing");
        }

        try {
            String username = jwtTokenUtils.getUsername(token);
            UserDetails userDetails = userService.loadUserByUsername(username);
            if (jwtTokenUtils.validateToken(token, userDetails)) {
                return ResponseEntity.ok("Token is valid");
            } else {
                return ResponseEntity.status(401).body("Invalid token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid token");
        }
    }


    @GetMapping("/profile-photo/{userId}")
    public ResponseEntity<Resource> getUserProfilePhoto(@PathVariable Long userId) {
        User user = userService.findById(userId).orElseThrow(() -> new RuntimeException("User not found with id " + userId));

        Resource resource;
        String profilePhotoFileName = user.getProfilePhotoFileName();

        if(profilePhotoFileName == null || profilePhotoFileName.isEmpty()) {
            // If no profile photo is set, load the default image
            resource = fileStorageService.loadFileAsResource("default-photo.png");
        } else {
            try {
                resource = fileStorageService.loadFileAsResource(profilePhotoFileName);
            } catch (RuntimeException e) {
                resource = fileStorageService.loadFileAsResource("default-photo.png");
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}



//    @PostMapping("/registration")
//    public ResponseEntity<?> createNewUser(@RequestParam("username") String username,
//                                           @RequestParam("email") String email,
//                                           @RequestParam("password") String password,
//                                           @RequestParam("re_password") String re_password,
//                                           @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto) {
//
//        // Construct the RegistrationUserDto
//        RegistrationUserDto userDto = new RegistrationUserDto();
//        userDto.setUsername(username);
//        userDto.setEmail(email);
//        userDto.setPassword(password);
//        userDto.setRe_password(re_password);
//
//        // Validate password and re_password
//        if (!password.equals(re_password)) {
//            return new ResponseEntity<>(new AppError(HttpStatus.BAD_REQUEST.value(), "Passwords do not match"), HttpStatus.BAD_REQUEST);
//        }
//
//        // Validate email uniqueness
//        if (userService.findByEmail(email).isPresent()) {
//            throw new EmailAlreadyInUseException("Email is already in use");
//        }
//
//        // Validate and store profile photo if provided
//        if (profilePhoto != null && !profilePhoto.isEmpty()) {
//            if (profilePhoto.getSize() > 2 * 1024 * 1024) { // 2 MB
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds the 2 MB limit");
//            }
//
//            String contentType = profilePhoto.getContentType();
//            if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) || MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PNG and JPG images are allowed");
//            }
//
//            String fileName = fileStorageService.storeFile(profilePhoto);
//            userDto.setPhotoPath(fileName);  // Set the photo path in the DTO
//        }
//
//        // Use authService to create the user
//        ResponseEntity<?> response = authService.createNewUser(userDto);
//
//        return response;
//    }
