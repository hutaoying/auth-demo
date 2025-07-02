package com.hangyue.auth.service;

import com.hangyue.auth.dto.LoginRequest;
import com.hangyue.auth.dto.RegisterRequest;
import com.hangyue.auth.entity.UserEntity;
import com.hangyue.auth.exception.EmailAlreadyExistsException;
import com.hangyue.auth.exception.InvalidCredentialsException;
import com.hangyue.auth.exception.UserNotFoundException;
import com.hangyue.auth.mapper.UserMapper;
import com.hangyue.auth.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Service
public class UserService implements UserDetailsService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       RedisService redisService, KafkaTemplate<String, String> kafkaTemplate) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisService = redisService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userMapper.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + email);
        }
        return new User(user.getEmail(), user.getPasswordHash(), new ArrayList<>());
    }

    public UserEntity registerUser(RegisterRequest request) {
        // 检查邮箱是否已存在
        if (userMapper.findByEmail(request.getEmail()) != null) {
            throw new EmailAlreadyExistsException("邮箱已注册");
        }

        // 创建新用户
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getUsername());
        user.setCreatedAt(LocalDateTime.now());

        // 保存用户
        userMapper.insert(user);
        // 发送验证邮件（异步）
        try {
            kafkaTemplate.send("email-notifications", "verification",
                    "To:" + user.getEmail() + ",Subject:账号验证,Body:请验证您的账号");
        } catch (Exception e) {
            log.error("email send error",e);
        }

        return user;
    }

    public String authenticate(LoginRequest request) {
        // 查找用户
        UserEntity user = userMapper.findByEmail(request.getEmail());
        if (user == null) {
            throw new UserNotFoundException("用户不存在");
        }
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("密码不正确");
        }
        // 生成JWT令牌
        try {
            UserDetails userDetails = loadUserByUsername(user.getEmail());
            return jwtUtil.generateToken(userDetails);
        } catch (Exception e) {
            log.error("error:{}",e);
        }
        return null;
    }

    public void logout(String token) {
        // 从令牌中提取过期时间
        long expirationTime = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
        // 将令牌加入黑名单
        redisService.addToBlacklist(token, expirationTime);
    }

    public UserEntity updateUserInfo(Long userId, String username) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new UserNotFoundException("用户不存在");
        }

        user.setUsername(username);
        userMapper.update(user);
        return user;
    }
}