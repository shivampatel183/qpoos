package com.qpoos.erp.payment.dto;

import com.qpoos.erp.payment.domain.PaymentMethod;
import com.qpoos.erp.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementHistoryResponse(Long paymentId, String paymentNumber, LocalDate paymentDate,
                                        PaymentMethod paymentMethod, BigDecimal paymentAmount,
                                        BigDecimal allocatedAmount, PaymentStatus status,
                                        String referenceNumber, Long voucherId, Long reversalVoucherId) {}
