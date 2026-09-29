package com.example.demo.user.mapper;

import com.example.demo.authentication.dto.request.RegisterRequest;
import com.example.demo.authentication.dto.response.AuthResponse;
import com.example.demo.user.entity.User;
import com.example.demo.user.entity.enums.Role;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", constant = "CUSTOMER")
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    User toEntity(RegisterRequest request);

    @AfterMapping
    default void setDefaults(RegisterRequest request, @MappingTarget User user) {
        user.setPassword(null); 
    }

    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "token", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    AuthResponse toResponse(User user, String accessToken, String refreshToken);
}