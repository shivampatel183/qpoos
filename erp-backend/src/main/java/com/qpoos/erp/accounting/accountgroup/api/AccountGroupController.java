package com.qpoos.erp.accounting.accountgroup.api;

import com.qpoos.erp.accounting.accountgroup.application.AccountGroupService;
import com.qpoos.erp.accounting.accountgroup.dto.AccountGroupRequest;
import com.qpoos.erp.accounting.accountgroup.dto.AccountGroupResponse;
import com.qpoos.erp.auth.dto.MessageResponse;
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
@RequestMapping("/api/accounting/account-groups")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AccountGroupController {

    private final AccountGroupService accountGroupService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountGroupResponse> create(
            @Valid @RequestBody AccountGroupRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountGroupService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<AccountGroupResponse>> list() {
        return ResponseEntity.ok(accountGroupService.list());
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<AccountGroupResponse> get(
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(accountGroupService.get(groupId));
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountGroupResponse> update(
            @PathVariable Long groupId,
            @Valid @RequestBody AccountGroupRequest request
    ) {
        return ResponseEntity.ok(accountGroupService.update(groupId, request));
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deactivate(
            @PathVariable Long groupId
    ) {
        accountGroupService.deactivate(groupId);
        return ResponseEntity.ok(new MessageResponse("Account group deactivated successfully."));
    }
}
