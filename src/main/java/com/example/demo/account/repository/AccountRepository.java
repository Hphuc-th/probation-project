package com.example.demo.account.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.account.entity.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    @Query("""
            SELECT a.accountType,
                   COUNT(DISTINCT a.id),
                   COUNT(t.id)
            FROM Account a
            LEFT JOIN a.transactions t
            GROUP BY a.accountType
            """)
    List<Object[]> countAccountsAndTransactionsByType();

    Optional<Account> findByAccountNumber(String accountNumber);
}
