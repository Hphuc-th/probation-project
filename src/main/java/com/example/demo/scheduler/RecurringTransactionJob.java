package com.example.demo.scheduler;

import com.example.demo.entity.RecurringTransaction;
import com.example.demo.repository.RecurringTransactionRepository;
import com.example.demo.service.RecurringTransactionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@DisallowConcurrentExecution
@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionJob implements Job {

    private final RecurringTransactionRepository recurringRepository;
    private final RecurringTransactionExecutor executor;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        Long recurringTransactionId = dataMap.getLong("recurringTransactionId");

        if (recurringTransactionId == null) {
            log.error("recurringTransactionId not found in JobDataMap");
            return;
        }

        RecurringTransaction rt = recurringRepository.findById(recurringTransactionId).orElse(null);
        if (rt == null) {
            log.warn("RecurringTransaction {} not found, job will be deleted", recurringTransactionId);
            return;
        }

        if (rt.getStatus() != com.example.demo.entity.enums.RecurringTransactionStatus.ACTIVE) {
            log.debug("RecurringTransaction {} is not ACTIVE (status={}), skipping", recurringTransactionId, rt.getStatus());
            return;
        }

        if (!rt.isRunnable()) {
            log.debug("RecurringTransaction {} not yet due (nextRunAt={})", recurringTransactionId, rt.getNextRunAt());
            return;
        }

        try {
            log.info("Executing recurring transaction {} ({})", recurringTransactionId, rt.getTransactionType());
            executor.execute(rt);
            log.info("Successfully executed recurring transaction {}", recurringTransactionId);
        } catch (Exception e) {
            log.error("Error executing recurring transaction {}", recurringTransactionId, e);
            throw new JobExecutionException(e);
        }
    }
}