package com.example.demo.repository;

import com.example.demo.entity.RecurringTransaction;
import com.example.demo.entity.enums.RecurringTransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long>, JpaSpecificationExecutor<RecurringTransaction> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT rt FROM RecurringTransaction rt
        WHERE rt.status = com.example.demo.entity.enums.RecurringTransactionStatus.ACTIVE
        AND rt.nextRunAt IS NOT NULL
        AND rt.nextRunAt <= :now
        ORDER BY rt.nextRunAt ASC
        """)
    List<RecurringTransaction> findDueRecurringTransactions(LocalDateTime now);

    Page<RecurringTransaction> findAll(Specification<RecurringTransaction> spec, Pageable pageable);

    @Query("""
        SELECT rt FROM RecurringTransaction rt
        WHERE rt.account.customer.id = :customerId
        """)
    Page<RecurringTransaction> findByCustomerId(Long customerId, Pageable pageable);
}