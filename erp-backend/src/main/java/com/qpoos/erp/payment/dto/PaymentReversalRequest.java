package com.qpoos.erp.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PaymentReversalRequest(@NotNull LocalDate reversalDate, @NotBlank @Size(max = 1000) String reason) {}
