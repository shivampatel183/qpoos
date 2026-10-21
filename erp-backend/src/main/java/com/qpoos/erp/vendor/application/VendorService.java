package com.qpoos.erp.vendor.application;

import com.qpoos.erp.vendor.domain.VendorEntity;
import com.qpoos.erp.vendor.infrastructure.VendorRepository;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.vendor.dto.VendorRequest;
import com.qpoos.erp.vendor.dto.VendorResponse;
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
public class VendorService {

    private final VendorRepository vendorRepository;
    private final EntityManager entityManager;

    @Transactional
    public VendorResponse create(VendorRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        CompanyEntity company = entityManager.getReference(CompanyEntity.class, companyId);
        String displayName = normalizeDisplayName(request.displayName());

        if (vendorRepository.existsByCompany_IdAndDisplayNameIgnoreCase(companyId, displayName)) {
            throw new IllegalArgumentException("Vendor display name already exists");
        }

        VendorEntity vendor = VendorEntity.builder()
                .company(company)
                .active(true)
                .build();
        applyRequest(vendor, request, displayName);

        return toResponse(vendorRepository.save(vendor));
    }

    @Transactional
    public List<VendorResponse> list() {
        return vendorRepository
                .findAllByCompany_IdAndActiveTrueOrderByDisplayNameAsc(SecurityUtils.getCompanyId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VendorResponse get(Long vendorId) {
        return toResponse(getVendor(SecurityUtils.getCompanyId(), vendorId));
    }

    @Transactional
    public VendorResponse update(Long vendorId, VendorRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        VendorEntity vendor = getVendor(companyId, vendorId);
        String displayName = normalizeDisplayName(request.displayName());

        if (vendorRepository.existsByCompany_IdAndDisplayNameIgnoreCaseAndIdNot(
                companyId,
                displayName,
                vendorId
        )) {
            throw new IllegalArgumentException("Vendor display name already exists");
        }

        applyRequest(vendor, request, displayName);
        return toResponse(vendorRepository.save(vendor));
    }

    @Transactional
    public void delete(Long vendorId) {
        VendorEntity vendor = getVendor(SecurityUtils.getCompanyId(), vendorId);
        vendor.setActive(false);
        vendorRepository.save(vendor);
    }

    private VendorEntity getVendor(UUID companyId, Long vendorId) {
        return vendorRepository.findByIdAndCompany_IdAndActiveTrue(vendorId, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
    }

    private void applyRequest(
            VendorEntity vendor,
            VendorRequest request,
            String displayName
    ) {
        vendor.setCompanyName(blankToNull(request.companyName()));
        vendor.setDisplayName(displayName);
        vendor.setGstNo(uppercaseOrNull(request.gstNo()));
        vendor.setFirstName(blankToNull(request.firstName()));
        vendor.setLastName(blankToNull(request.lastName()));
        vendor.setEmail(lowercaseOrNull(request.email()));
        vendor.setMobileNo(blankToNull(request.mobileNo()));
        vendor.setStreetAddress1(blankToNull(request.streetAddress1()));
        vendor.setStreetAddress2(blankToNull(request.streetAddress2()));
        vendor.setCity(blankToNull(request.city()));
        vendor.setState(blankToNull(request.state()));
        vendor.setCountry(blankToNull(request.country()));
        vendor.setPinCode(blankToNull(request.pinCode()));
        vendor.setNotes(blankToNull(request.notes()));
        vendor.setAccountHolderName(blankToNull(request.accountHolderName()));
        vendor.setAccountNumber(blankToNull(request.accountNumber()));
        vendor.setIfscCode(uppercaseOrNull(request.ifscCode()));
        vendor.setOpeningBalance(
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

    private VendorResponse toResponse(VendorEntity vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getCompany().getId(),
                vendor.getCompanyName(),
                vendor.getDisplayName(),
                vendor.getGstNo(),
                vendor.getFirstName(),
                vendor.getLastName(),
                vendor.getEmail(),
                vendor.getMobileNo(),
                vendor.getStreetAddress1(),
                vendor.getStreetAddress2(),
                vendor.getCity(),
                vendor.getState(),
                vendor.getCountry(),
                vendor.getPinCode(),
                vendor.getNotes(),
                vendor.getAccountHolderName(),
                vendor.getAccountNumber(),
                vendor.getIfscCode(),
                vendor.getOpeningBalance(),
                vendor.getCreatedAt(),
                vendor.getUpdatedAt()
        );
    }
}
