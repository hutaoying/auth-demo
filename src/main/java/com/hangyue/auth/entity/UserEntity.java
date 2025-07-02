package com.hangyue.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserEntity {
    private Long id;
    private String email;
    private String passwordHash;
    private String username;
    private LocalDateTime createdAt;
}