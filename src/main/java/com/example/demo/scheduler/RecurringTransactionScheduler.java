package com.example.demo.scheduler;

import com.example.demo.entity.RecurringTransaction;
import com.example.demo.entity.enums.RecurringTransactionStatus;
import com.example.demo.repository.RecurringTransactionRepository;
import com.example.demo.service.RecurringTransactionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionScheduler {

    private final RecurringTransactionRepository recurringRepository;
    private final RecurringTransactionExecutor executor;

    @Value("${app.scheduler.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${app.scheduler.poll-interval-ms:30000}")
    @Transactional
    public void processDueRecurringTransactions() {
        if (!enabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<RecurringTransaction> due = recurringRepository.findDueRecurringTransactions(now);

        if (due.isEmpty()) {
            log.debug("No due recurring transactions at {}", now);
            return;
        }

        log.info("Found {} due recurring transactions at {}", due.size(), now);

        for (RecurringTransaction rt : due) {
            try {
                executor.execute(rt);
            } catch (Exception e) {
                log.error("Unexpected error executing recurring transaction {}", rt.getId(), e);
            }
        }
    }
}