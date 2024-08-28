package com.dimon.catanbackend.controller;

import com.dimon.catanbackend.dtos.FriendRequest;
import com.dimon.catanbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friend")
public class FriendController {

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    public ResponseEntity<String> addFriend(@RequestBody FriendRequest request) {
        userService.addFriend(request.getUsername(), request.getFriendUsername());
        return ResponseEntity.ok("Friend added successfully");
    }

    @PostMapping("/remove")
    public ResponseEntity<String> removeFriend(@RequestBody FriendRequest request) {
        userService.removeFriend(request.getUsername(), request.getFriendUsername());
        return ResponseEntity.ok("Friend removed successfully");
    }
}
