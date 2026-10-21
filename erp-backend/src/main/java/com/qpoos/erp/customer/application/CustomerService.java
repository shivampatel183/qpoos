package com.qpoos.erp.customer.application;

import com.qpoos.erp.customer.domain.CustomerEntity;
import com.qpoos.erp.customer.infrastructure.CustomerRepository;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.customer.dto.CustomerRequest;
import com.qpoos.erp.customer.dto.CustomerResponse;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final EntityManager entityManager;

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        CompanyEntity company = entityManager.getReference(CompanyEntity.class, companyId);
        String displayName = normalizeDisplayName(request.displayName());

        if (customerRepository.existsByCompany_IdAndDisplayNameIgnoreCase(companyId, displayName)) {
            throw new IllegalArgumentException("Customer display name already exists");
        }

        CustomerEntity customer = CustomerEntity.builder()
                .company(company)
                .active(true)
                .build();
        applyRequest(customer, request, displayName);

        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public List<CustomerResponse> list() {
        return customerRepository
                .findAllByCompany_IdAndActiveTrueOrderByDisplayNameAsc(SecurityUtils.getCompanyId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CustomerResponse get(Long customerId) {
        return toResponse(getCustomer(SecurityUtils.getCompanyId(), customerId));
    }

    @Transactional
    public CustomerResponse update(Long customerId, CustomerRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        CustomerEntity customer = getCustomer(companyId, customerId);
        String displayName = normalizeDisplayName(request.displayName());

        if (customerRepository.existsByCompany_IdAndDisplayNameIgnoreCaseAndIdNot(
                companyId,
                displayName,
                customerId
        )) {
            throw new IllegalArgumentException("Customer display name already exists");
        }

        applyRequest(customer, request, displayName);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long customerId) {
        CustomerEntity customer = getCustomer(SecurityUtils.getCompanyId(), customerId);
        customer.setActive(false);
        customerRepository.save(customer);
    }

    private CustomerEntity getCustomer(UUID companyId, Long customerId) {
        return customerRepository.findByIdAndCompany_IdAndActiveTrue(customerId, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    private void applyRequest(
            CustomerEntity customer,
            CustomerRequest request,
            String displayName
    ) {
        customer.setCompanyName(blankToNull(request.companyName()));
        customer.setDisplayName(displayName);
        customer.setGstNo(uppercaseOrNull(request.gstNo()));
        customer.setFirstName(blankToNull(request.firstName()));
        customer.setLastName(blankToNull(request.lastName()));
        customer.setEmail(lowercaseOrNull(request.email()));
        customer.setMobileNo(blankToNull(request.mobileNo()));
        customer.setStreetAddress1(blankToNull(request.streetAddress1()));
        customer.setStreetAddress2(blankToNull(request.streetAddress2()));
        customer.setCity(blankToNull(request.city()));
        customer.setState(blankToNull(request.state()));
        customer.setCountry(blankToNull(request.country()));
        customer.setPinCode(blankToNull(request.pinCode()));
        customer.setNotes(blankToNull(request.notes()));
        customer.setAccountHolderName(blankToNull(request.accountHolderName()));
        customer.setAccountNumber(blankToNull(request.accountNumber()));
        customer.setIfscCode(uppercaseOrNull(request.ifscCode()));
        customer.setOpeningBalance(
                request.openingBalance() == null ? BigDecimal.ZERO : request.openingBalance()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeDisplayName(String displayName) {
        return displayName.trim();
    }

    private String lowercaseOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String uppercaseOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private CustomerResponse toResponse(CustomerEntity customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCompany().getId(),
                customer.getCompanyName(),
                customer.getDisplayName(),
                customer.getGstNo(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getMobileNo(),
                customer.getStreetAddress1(),
                customer.getStreetAddress2(),
                customer.getCity(),
                customer.getState(),
                customer.getCountry(),
                customer.getPinCode(),
                customer.getNotes(),
                customer.getAccountHolderName(),
                customer.getAccountNumber(),
                customer.getIfscCode(),
                customer.getOpeningBalance(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
