package com.example.demo.accountStatusHistory.dto.response;

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
public class AccountStatusHistoryResponse {

    private Long id;
    private Long accountId;
    private String previousStatus;
    private String newStatus;
    private String reason;
    private LocalDateTime changedAt;
}
