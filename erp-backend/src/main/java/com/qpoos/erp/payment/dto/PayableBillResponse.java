package com.qpoos.erp.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayableBillResponse(Long id, String billNumber, LocalDate billDate, LocalDate dueDate,
                                  Long vendorId, String vendorName, BigDecimal totalAmount,
                                  BigDecimal settledAmount, BigDecimal outstandingAmount) {}
