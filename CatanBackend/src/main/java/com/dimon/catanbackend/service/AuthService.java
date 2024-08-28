package com.dimon.catanbackend.service;

import com.dimon.catanbackend.dtos.JwtRequest;
import com.dimon.catanbackend.dtos.JwtResponse;
import com.dimon.catanbackend.dtos.RegistrationUserDto;
import com.dimon.catanbackend.dtos.UserDto;
import com.dimon.catanbackend.entities.User;
import com.dimon.catanbackend.exceptions.AppError;
import com.dimon.catanbackend.exceptions.EmailAlreadyInUseException;
import com.dimon.catanbackend.exceptions.InvalidTokenException;
import com.dimon.catanbackend.utils.JwtTokenUtils;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;
    private final AuthenticationManager authenticationManager;
    private final EmailSenderService emailSenderService;
    private final FileStorageService fileStorageService;

    public ResponseEntity<?> createAuthToken(JwtRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(), "Incorrect email or password"), HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = userService.loadUserByUsername(authRequest.getEmail());
        User user = userService.findByEmail(authRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AppError(HttpStatus.UNAUTHORIZED.value(), "User is not active"));
        }

        String token = jwtTokenUtils.generateToken(userDetails);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    public ResponseEntity<?> createNewUser(@RequestBody RegistrationUserDto userDto) {
        if (!userDto.getPassword().equals(userDto.getRe_password())) {
            return new ResponseEntity<>(new AppError(HttpStatus.BAD_REQUEST.value(), "Passwords do not match"), HttpStatus.BAD_REQUEST);
        }

        if (userService.findByEmail(userDto.getEmail()).isPresent()) {
            throw new EmailAlreadyInUseException("Email is already in use");
        }

        User user = userService.createNewUser(userDto);
        String activationToken = UUID.randomUUID().toString();
        System.out.println("Activation Token: " + activationToken);
        user.setActivationToken(activationToken);

        MultipartFile profilePhoto = userDto.getProfilePhoto();
        if(profilePhoto != null && !profilePhoto.isEmpty()) {
            try {
                String fileName = fileStorageService.storeFile(profilePhoto);
                user.setProfilePhotoFileName(fileName);
            } catch (Exception e) {
                return new ResponseEntity<>(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to save profile photo"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }


        System.out.println(user.getEmail());
        userService.save(user);

        // Send activation email
        String activationLink = "http://localhost:8080/activate?token=" + activationToken;
        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Email Verification</title>\n" +
                "</head>\n" +
                "<body style=\"font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;\">\n" +
                "<div style=\"max-width: 600px; margin: auto; background-color: #ffffff; padding: 20px; border-radius: 10px; text-align: center;\">\n" +
                "    <h2 style=\"color: #333;\">You're nearly there!</h2>\n" +
                "    <p style=\"color: #666;\">Verify your email address to log in and get started.</p>\n" +
                "    <a href=\"" + activationLink + "\" style=\"display: inline-block; padding: 10px 20px; color: #fff; background-color: #00bfa5; text-decoration: none; border-radius: 5px;\">Verify email</a>\n" +
                "</div>\n" +
                "<p style=\"text-align: center; color: #999; font-size: 12px;\">© Netlify, All rights reserved.</p>\n" +
                "</body>\n" +
                "</html>";

        try {
            emailSenderService.sendEmail(user.getEmail(), "Email Verification", htmlContent);
        } catch (MessagingException e) {
            e.printStackTrace();
            return new ResponseEntity<>(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to send email"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok(new UserDto(user.getId(), user.getUsername(), user.getEmail()));
    }

    public ResponseEntity<?> activateAccount(String token) {
        System.out.println("Token: "+ token);
        Optional<User> userOptional = userService.findByActivationToken(token);
        if (!userOptional.isPresent()) {
            System.out.println("Invalid");
            throw new InvalidTokenException("Invalid activation token");
        }
        User user = userOptional.get();
        System.out.println(user);
        user.setActive(true);
        user.setActivationToken(null);
        userService.save(user);
        return ResponseEntity.ok("Account activated successfully");
    }
}
