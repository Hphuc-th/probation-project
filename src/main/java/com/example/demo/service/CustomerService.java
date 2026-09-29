package com.example.demo.service;

import com.example.demo.dto.request.customer.CreateCustomerRequest;
import com.example.demo.dto.request.customer.UpdateCustomerRequest;
import com.example.demo.dto.response.LocationStatResponse;
import com.example.demo.dto.response.CustomerResponse;
import com.example.demo.entity.Customer;
import com.example.demo.entity.enums.CustomerStatus;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.mapper.CustomerMapper;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Cacheable(value = "customers", key = "#id")
    @Transactional(readOnly = true)
    //get customer by ID
    public CustomerResponse getCustomerById(Long id) {
        SecurityUtils.checkOwnershipOrAdmin(id);
        return customerMapper.toResponse(getEntity(id));
    }

    @Cacheable(value = "customers", key = "'search_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #fullName + '_' + #email + '_' + #location + '_' + #status")
    @Transactional(readOnly = true)
    //get all customers
    public Page<CustomerResponse> searchCustomers(String fullName, String email, String location,
                                                   CustomerStatus status, Pageable pageable) {
        Specification<Customer> spec = (root, query, cb) -> cb.conjunction();
        if (SecurityUtils.isCustomer()) {
            Long currentCustomerId = SecurityUtils.getCurrentCustomerId();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), currentCustomerId));
        }
        if (StringUtils.hasText(fullName)) {
            String pattern = fullName.trim() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("fullName"), pattern));
        }
        if (StringUtils.hasText(email)) {
            String pattern = email.trim() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("email"), pattern));
        }
        if (StringUtils.hasText(location)) {
            String pattern = location.trim() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("location"), pattern));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return customerRepository.findAll(spec, pageable).map(customerMapper::toResponse);
    }

    @CacheEvict(value = "customers", allEntries = true)
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        checkCccdUnique(request.getCccd(), null);
        Customer customer = customerMapper.toEntity(request);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    private void checkCccdUnique(String cccd, Long excludeId) {
    customerRepository.findByCccd(cccd)
        .filter(c -> excludeId == null || !c.getId().equals(excludeId))
        .ifPresent(c -> { throw new BusinessException("CCCD already exists: " + cccd); });
    }


    @CacheEvict(value = "customers", allEntries = true)
    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = getEntity(id);
        if (request.getCccd() != null) {
        checkCccdUnique(request.getCccd(), id);
    }
        customerMapper.updateEntity(request, customer);
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        }
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    private Customer getEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<LocationStatResponse> getCustomersByLocation() {
        return customerRepository.countCustomersByLocation().stream()
                .map(row -> LocationStatResponse.builder()
                        .location(row[0] != null ? (String) row[0] : "Unknown")
                        .customerCount((Long) row[1])
                        .build())
                .toList();
    }
}
