package com.hangyue.auth.mapper;

import com.hangyue.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    UserEntity findByEmail(@Param("email") String email);
    
    int insert(UserEntity user);
    
    int update(UserEntity user);
    
    UserEntity findById(@Param("id") Long id);
}