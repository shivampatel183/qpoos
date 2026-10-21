package com.qpoos.erp.product.api;

import com.qpoos.erp.product.application.ProductService;
import com.qpoos.erp.auth.dto.MessageResponse;
import com.qpoos.erp.product.dto.ProductRequest;
import com.qpoos.erp.product.dto.ProductResponse;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> list() {
        return ResponseEntity.ok(productService.list());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> get(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(productService.get(productId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.update(productId, request));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable Long productId
    ) {
        productService.delete(productId);
        return ResponseEntity.ok(new MessageResponse("Product deleted successfully."));
    }
}
