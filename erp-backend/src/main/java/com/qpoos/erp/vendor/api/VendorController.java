package com.qpoos.erp.vendor.api;

import com.qpoos.erp.vendor.application.VendorService;
import com.qpoos.erp.auth.dto.MessageResponse;
import com.qpoos.erp.vendor.dto.VendorRequest;
import com.qpoos.erp.vendor.dto.VendorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    public ResponseEntity<VendorResponse> create(
            @Valid @RequestBody VendorRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vendorService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<VendorResponse>> list() {
        return ResponseEntity.ok(vendorService.list());
    }

    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> get(
            @PathVariable Long vendorId
    ) {
        return ResponseEntity.ok(vendorService.get(vendorId));
    }

    @PutMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> update(
            @PathVariable Long vendorId,
            @Valid @RequestBody VendorRequest request
    ) {
        return ResponseEntity.ok(vendorService.update(vendorId, request));
    }

    @DeleteMapping("/{vendorId}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable Long vendorId
    ) {
        vendorService.delete(vendorId);
        return ResponseEntity.ok(new MessageResponse("Vendor deleted successfully."));
    }
}
