package com.example.demo.dto.response;

import com.example.demo.entity.enums.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String location;
    private String cccd;
    private CustomerStatus status;
    private LocalDateTime createdAt;
}
