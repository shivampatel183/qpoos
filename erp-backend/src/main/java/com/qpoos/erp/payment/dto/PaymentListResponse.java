package com.qpoos.erp.payment.dto;

import com.qpoos.erp.payment.domain.PaymentMethod;
import com.qpoos.erp.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentListResponse(Long id, String paymentNumber, LocalDate paymentDate, Long vendorId,
                                  String vendorName, PaymentMethod paymentMethod, BigDecimal amount,
                                  PaymentStatus status, Long allocationCount) {}
