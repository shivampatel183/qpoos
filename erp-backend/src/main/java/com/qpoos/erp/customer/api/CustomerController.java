package com.qpoos.erp.customer.api;

import com.qpoos.erp.customer.application.CustomerService;
import com.qpoos.erp.auth.dto.MessageResponse;
import com.qpoos.erp.customer.dto.CustomerRequest;
import com.qpoos.erp.customer.dto.CustomerResponse;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CustomerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> list() {
        return ResponseEntity.ok(customerService.list());
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> get(
            @PathVariable Long customerId
    ) {
        return ResponseEntity.ok(customerService.get(customerId));
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerRequest request
    ) {
        return ResponseEntity.ok(customerService.update(customerId, request));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable Long customerId
    ) {
        customerService.delete(customerId);
        return ResponseEntity.ok(new MessageResponse("Customer deleted successfully."));
    }
}
