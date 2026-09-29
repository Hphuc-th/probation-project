package com.example.demo.account.dto.request;

import com.example.demo.account.entity.enums.AccountStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeAccountStatusRequest {

    @NotNull(message = "new status is required")
    private AccountStatus newStatus;

    private String reason;
}
