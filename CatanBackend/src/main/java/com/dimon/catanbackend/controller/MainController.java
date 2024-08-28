package com.dimon.catanbackend.controller;

import com.dimon.catanbackend.exceptions.GameNotFoundException;
import com.dimon.catanbackend.exceptions.UserNotFoundException;
import com.dimon.catanbackend.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class MainController {

    @GetMapping("/unsecured")
    public String unsecuredData() {
        return "Unsecured data";
    }

    @GetMapping("/secured")
    public String securedData() {
        return "Secured data";
    }

    @GetMapping("/admin")
    public String adminData(){
        return "Admin data";
    }

    @GetMapping("/info")
    public String userData(Principal principal){
        System.out.println(principal);
        return principal.getName();
    }

}
