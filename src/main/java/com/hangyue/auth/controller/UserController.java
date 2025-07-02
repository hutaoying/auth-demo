package com.hangyue.auth.controller;

import com.hangyue.auth.dto.ApiResponse;
import com.hangyue.auth.dto.LoginRequest;
import com.hangyue.auth.dto.RegisterRequest;
import com.hangyue.auth.entity.UserEntity;
import com.hangyue.auth.service.UserService;
import com.hangyue.auth.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    
    private final UserService userService;

    private final JwtUtil jwtUtil;

    @Autowired
    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
        UserEntity user = userService.registerUser(request);
        Map<String, Object> data = new HashMap<>();
        data.put("status", "success");
        data.put("user_id", user.getId());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.authenticate(request);
        Map<String, Object> data = new HashMap<>();
        data.put("status", "success");
        data.put("token", token);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        userService.logout(token);
        Map<String, String> data = new HashMap<>();
        data.put("status", "success");
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String username = request.get("username");
        userService.updateUserInfo(id, username);
        Map<String, String> data = new HashMap<>();
        data.put("status", "success");
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}