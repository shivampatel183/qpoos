package com.qpoos.erp.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayableDocumentResponse(String documentType, Long id, String documentNumber,
                                      LocalDate documentDate, LocalDate dueDate, Long vendorId,
                                      String vendorName, BigDecimal totalAmount, BigDecimal settledAmount,
                                      BigDecimal outstandingAmount) {}
