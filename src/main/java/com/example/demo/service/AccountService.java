package com.example.demo.service;

import com.example.demo.dto.request.account.ChangeAccountStatusRequest;
import com.example.demo.dto.request.account.CreateAccountRequest;
import com.example.demo.dto.request.account.UpdateAccountRequest;
import com.example.demo.dto.response.AccountResponse;
import com.example.demo.dto.response.AccountStatusHistoryResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.AccountStatusHistory;
import com.example.demo.entity.Customer;
import com.example.demo.entity.enums.AccountStatus;
import com.example.demo.entity.enums.AccountType;
import com.example.demo.exception.NotFoundException;
import com.example.demo.mapper.AccountHistoryMapper;
import com.example.demo.mapper.AccountMapper;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.AccountHistoryRepository;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountHistoryRepository accountHistoryRepository;
    private final AccountMapper accountMapper;
    private final AccountHistoryMapper historyMapper;

    @Transactional(readOnly = true)
    public Page<AccountResponse> searchAccounts(String accountNumber, Long customerId,
                                                String cccd,
                                                AccountStatus status, AccountType accountType,
                                                Pageable pageable) {
        Specification<Account> spec = (root, query, cb) -> {
            var conjunction = cb.conjunction();
            if (SecurityUtils.isCustomer()) {
                Long currentCustomerId = SecurityUtils.getCurrentCustomerId();
                var customerJoin = root.join("customer");
                conjunction = cb.and(conjunction, cb.equal(customerJoin.get("id"), currentCustomerId));
            }
            if (StringUtils.hasText(accountNumber)) {
                String pattern = accountNumber.trim() + "%";
                conjunction = cb.and(conjunction, cb.like(root.get("accountNumber"), pattern));
            }
            if (customerId != null || StringUtils.hasText(cccd)) {
                var customerJoin = root.join("customer");
                if (customerId != null) {
                    conjunction = cb.and(conjunction, cb.equal(customerJoin.get("id"), customerId));
                }
                if (StringUtils.hasText(cccd)) {
                    String pattern = cccd.trim() + "%";
                    conjunction = cb.and(conjunction, cb.like(customerJoin.get("cccd"), pattern));
                }
            }
            if (status != null) {
                conjunction = cb.and(conjunction, cb.equal(root.get("status"), status));
            }
            if (accountType != null) {
                conjunction = cb.and(conjunction, cb.equal(root.get("accountType"), accountType));
            }
            return conjunction;
        };
        return accountRepository.findAll(spec, pageable).map(accountMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = getEntity(id);
        SecurityUtils.checkOwnershipOrAdmin(account.getCustomer().getId());
        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + request.getCustomerId()));
        Account account = accountMapper.toEntity(request);
        account.setCustomer(customer);
        Account saved = accountRepository.save(account);
        AccountStatusHistory history = historyMapper.toEntity(saved, null, saved.getStatus(), "Account opened");
        accountHistoryRepository.save(history);
        return accountMapper.toResponse(saved);
    }

    @Transactional
    public AccountResponse updateAccount(Long id, UpdateAccountRequest request) {
        Account account = getEntity(id);

        if (request.getAccountType() != null) {
            account.setAccountType(request.getAccountType());
        }
        if (request.getTransactionLimit() != null) {
            account.setTransactionLimit(request.getTransactionLimit());
        }
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + request.getCustomerId()));
            account.setCustomer(customer);
        }
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountStatusHistoryResponse changeStatus(Long id, ChangeAccountStatusRequest request) {
        Account account = getEntity(id);
        AccountStatus target = request.getNewStatus();
        AccountStatus current = account.getStatus();
        account.changeStatus(target);
        accountRepository.save(account);
        AccountStatusHistory history = historyMapper.toEntity(account, current, target, request.getReason());
        AccountStatusHistory saved = accountHistoryRepository.save(history);
        return historyMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountStatusHistoryResponse> getStatusHistory(Long accountId) {
        Account account = getEntity(accountId);
        SecurityUtils.checkOwnershipOrAdmin(account.getCustomer().getId());
        return accountHistoryRepository.findByAccountIdOrderByChangedAtDescIdDesc(account.getId())
                .stream()
                .map(historyMapper::toResponse)
                .toList();
    }

    private Account getEntity(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found: " + id));
    }
}
