package com.example.demo.repository;

import com.example.demo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    @Query("""
            SELECT c.location, COUNT(c.id)
            FROM Customer c
            GROUP BY c.location
            ORDER BY COUNT(c.id) DESC
            """)
    List<Object[]> countCustomersByLocation();

    Optional<Customer> findByCccd(String cccd);
    boolean existsByEmail(String email);
    boolean existsByCccd(String cccd);
}
