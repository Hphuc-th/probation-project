package com.example.demo.recurringTransaction.service;

import com.example.demo.account.entity.Account;
import com.example.demo.account.repository.AccountRepository;
import com.example.demo.authentication.service.SecurityUtils;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.recurringTransaction.dto.request.ChangeRecurringStatusRequest;
import com.example.demo.recurringTransaction.dto.request.CreateRecurringTransactionRequest;
import com.example.demo.recurringTransaction.dto.request.UpdateRecurringTransactionRequest;
import com.example.demo.recurringTransaction.dto.response.RecurringTransactionResponse;
import com.example.demo.recurringTransaction.entity.RecurringTransaction;
import com.example.demo.recurringTransaction.entity.enums.RecurringTransactionStatus;
import com.example.demo.recurringTransaction.mapper.RecurringTransactionMapper;
import com.example.demo.recurringTransaction.repository.RecurringTransactionRepository;
import com.example.demo.transaction.entity.enums.TransactionType;

import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final AccountRepository accountRepository;
    private final RecurringTransactionMapper mapper;
    private final RecurringTransactionExecutor executor;
    private final Scheduler scheduler;

    private static final TimeZone SYSTEM_TIMEZONE = TimeZone.getTimeZone(ZoneId.systemDefault());

    private String getJobName(Long id) {
        return "recurringTx-" + id;
    }

    private String getGroupName() {
        return "recurringTransactions";
    }

    private JobKey getJobKey(Long id) {
        return new JobKey(getJobName(id), getGroupName());
    }

    private TriggerKey getTriggerKey(Long id) {
        return new TriggerKey(getJobName(id), getGroupName());
    }

    private void scheduleJob(RecurringTransaction rt) {
        try {
            if (!scheduler.isStarted()) {
                return;
            }

            JobKey jobKey = getJobKey(rt.getId());
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("recurringTransactionId", rt.getId());

            JobDetail jobDetail = JobBuilder.newJob(RecurringTransactionJob.class)
                    .withIdentity(jobKey)
                    .setJobData(jobDataMap)
                    .storeDurably()
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(getTriggerKey(rt.getId()))
                    .withSchedule(CronScheduleBuilder.cronSchedule(rt.getCronExpression())
                            .inTimeZone(SYSTEM_TIMEZONE)
                            .withMisfireHandlingInstructionFireAndProceed())
                    .startAt(Date.from(rt.getNextRunAt().atZone(ZoneId.systemDefault()).toInstant()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.debug("Scheduled recurring transaction {} with cron: {}", rt.getId(), rt.getCronExpression());
        } catch (SchedulerException e) {
            log.error("Failed to schedule recurring transaction {}", rt.getId(), e);
            throw new BusinessException("Failed to schedule recurring transaction: " + e.getMessage());
        }
    }

    private void unscheduleJob(Long id) {
        try {
            JobKey jobKey = getJobKey(id);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.debug("Unscheduled recurring transaction {}", id);
            }
        } catch (SchedulerException e) {
            log.error("Failed to unschedule recurring transaction {}", id, e);
            throw new BusinessException("Failed to unschedule recurring transaction: " + e.getMessage());
        }
    }

    private void rescheduleJob(RecurringTransaction rt) {
        try {
            TriggerKey triggerKey = getTriggerKey(rt.getId());
            if (scheduler.checkExists(triggerKey)) {
                CronTrigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerKey)
                        .withSchedule(CronScheduleBuilder.cronSchedule(rt.getCronExpression())
                                .inTimeZone(SYSTEM_TIMEZONE)
                                .withMisfireHandlingInstructionFireAndProceed())
                        .startAt(Date.from(rt.getNextRunAt().atZone(ZoneId.systemDefault()).toInstant()))
                        .build();
                scheduler.rescheduleJob(triggerKey, trigger);
                log.debug("Rescheduled recurring transaction {} with cron: {}", rt.getId(), rt.getCronExpression());
            } else {
                scheduleJob(rt);
            }
        } catch (SchedulerException e) {
            log.error("Failed to reschedule recurring transaction {}", rt.getId(), e);
            throw new BusinessException("Failed to reschedule recurring transaction: " + e.getMessage());
        }
    }

    @Transactional
    public RecurringTransactionResponse create(CreateRecurringTransactionRequest request) {
        Account account = getAccountAndCheckOwnership(request.getAccountId());

        if (account.getStatus() != com.example.demo.account.entity.enums.AccountStatus.ACTIVE) {
            throw new BusinessException("Source account is not active");
        }

        Account toAccount = null;
        if (request.getTransactionType() == TransactionType.TRANSFER) {
            if (request.getToAccountId() == null) {
                throw new BusinessException("toAccountId is required for TRANSFER");
            }
            if (request.getToAccountId().equals(request.getAccountId())) {
                throw new BusinessException("Source and target account cannot be the same");
            }
            toAccount = accountRepository.findById(request.getToAccountId())
                    .orElseThrow(() -> new NotFoundException("Target account not found: " + request.getToAccountId()));
            if (toAccount.getStatus() != com.example.demo.account.entity.enums.AccountStatus.ACTIVE) {
                throw new BusinessException("Target account is not active");
            }
        } else if (request.getTransactionType() == TransactionType.DEPOSIT) {
            if (request.getToAccountId() != null) {
                throw new BusinessException("toAccountId must be null for DEPOSIT");
            }
        } else {
            throw new BusinessException("Only DEPOSIT and TRANSFER transaction types are supported for recurring transactions");
        }

        if (!org.springframework.scheduling.support.CronExpression.isValidExpression(request.getCronExpression())) {
            throw new BusinessException("Invalid cron expression: " + request.getCronExpression());
        }

        if (request.getAmount().compareTo(account.getTransactionLimit()) > 0) {
            throw new BusinessException("Amount exceeds account transaction limit: " + account.getTransactionLimit());
        }

        RecurringTransaction rt = mapper.toEntity(request, account, toAccount);
        rt.computeNextRun();
        RecurringTransaction saved = recurringRepository.save(rt);
        scheduleJob(saved);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<RecurringTransactionResponse> search(String accountNumber, String transactionType,
                                                      RecurringTransactionStatus status, Pageable pageable) {
        Specification<RecurringTransaction> spec = (root, query, cb) -> cb.conjunction();

        if (SecurityUtils.isCustomer()) {
            Long customerId = SecurityUtils.getCurrentCustomerId();
            if (customerId != null) {
                spec = spec.and((root, q, cb) ->
                        cb.equal(root.get("account").get("customer").get("id"), customerId));
            }
        }

        if (StringUtils.hasText(accountNumber)) {
            String pattern = accountNumber.trim() + "%";
            spec = spec.and((root, q, cb) ->
                    cb.like(root.get("account").get("accountNumber"), pattern));
        }

        if (StringUtils.hasText(transactionType)) {
            try {
                TransactionType type = TransactionType.valueOf(transactionType.toUpperCase());
                spec = spec.and((root, q, cb) -> cb.equal(root.get("transactionType"), type));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid transaction type: " + transactionType);
            }
        }

        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }

        return recurringRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponse getById(Long id) {
        RecurringTransaction rt = getEntity(id);
        checkOwnershipOrAdmin(rt.getAccount().getCustomer().getId());
        return mapper.toResponse(rt);
    }

    @Transactional
    public RecurringTransactionResponse update(Long id, UpdateRecurringTransactionRequest request) {
        RecurringTransaction rt = getEntity(id);
        checkOwnershipOrAdmin(rt.getAccount().getCustomer().getId());

        Account toAccount = null;
        if (request.getToAccountId() != null) {
            toAccount = accountRepository.findById(request.getToAccountId())
                    .orElseThrow(() -> new NotFoundException("Target account not found: " + request.getToAccountId()));
        }

        if (request.getCronExpression() != null && !org.springframework.scheduling.support.CronExpression.isValidExpression(request.getCronExpression())) {
            throw new BusinessException("Invalid cron expression: " + request.getCronExpression());
        }

        if (request.getAmount() != null && request.getAmount().compareTo(rt.getAccount().getTransactionLimit()) > 0) {
            throw new BusinessException("Amount exceeds account transaction limit: " + rt.getAccount().getTransactionLimit());
        }

        String oldCron = rt.getCronExpression();
        mapper.updateEntity(request, rt, toAccount);
        RecurringTransaction saved = recurringRepository.save(rt);

        if (!oldCron.equals(saved.getCronExpression())) {
            saved.computeNextRun();
            recurringRepository.save(saved);
            rescheduleJob(saved);
        }

        return mapper.toResponse(saved);
    }

    @Transactional
    public RecurringTransactionResponse changeStatus(Long id, ChangeRecurringStatusRequest request) {
        RecurringTransaction rt = getEntity(id);
        checkOwnershipOrAdmin(rt.getAccount().getCustomer().getId());

        RecurringTransactionStatus oldStatus = rt.getStatus();
        rt.setStatus(request.getStatus());
        if (request.getStatus() == RecurringTransactionStatus.ACTIVE && rt.getNextRunAt() == null) {
            rt.computeNextRun();
        }
        RecurringTransaction saved = recurringRepository.save(rt);

        if (oldStatus != RecurringTransactionStatus.ACTIVE && request.getStatus() == RecurringTransactionStatus.ACTIVE) {
            scheduleJob(saved);
        } else if (oldStatus == RecurringTransactionStatus.ACTIVE && request.getStatus() != RecurringTransactionStatus.ACTIVE) {
            unscheduleJob(id);
        }

        return mapper.toResponse(saved);
    }

    @Transactional
    public RecurringTransactionResponse runNow(Long id) {
        RecurringTransaction rt = getEntity(id);
        checkOwnershipOrAdmin(rt.getAccount().getCustomer().getId());

        if (rt.getStatus() != RecurringTransactionStatus.ACTIVE) {
            throw new BusinessException("Cannot run recurring transaction with status: " + rt.getStatus());
        }

        executor.execute(rt);
        return mapper.toResponse(rt);
    }

    @Transactional
    public void delete(Long id) {
        RecurringTransaction rt = getEntity(id);
        checkOwnershipOrAdmin(rt.getAccount().getCustomer().getId());
        unscheduleJob(id);
        recurringRepository.delete(rt);
    }

    @Transactional
    public void scheduleExistingTransactions() {
        List<RecurringTransaction> activeTransactions = recurringRepository.findByStatus(RecurringTransactionStatus.ACTIVE);
        for (RecurringTransaction rt : activeTransactions) {
            if (rt.getNextRunAt() != null) {
                scheduleJob(rt);
            }
        }
        log.info("Scheduled {} existing recurring transactions", activeTransactions.size());
    }

    private RecurringTransaction getEntity(Long id) {
        return recurringRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recurring transaction not found: " + id));
    }

    private Account getAccountAndCheckOwnership(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        SecurityUtils.checkOwnershipOrAdmin(account.getCustomer().getId());
        return account;
    }

    private void checkOwnershipOrAdmin(Long customerId) {
        SecurityUtils.checkOwnershipOrAdmin(customerId);
    }
}