package com.qpoos.erp.common.address;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddressSnapshot {

    @Column(name = "delivery_address_line_1", length = 255)
    private String line1;

    @Column(name = "delivery_address_line_2", length = 255)
    private String line2;

    @Column(name = "delivery_address_city", length = 100)
    private String city;

    @Column(name = "delivery_address_state", length = 100)
    private String state;

    @Column(name = "delivery_address_country", length = 100)
    private String country;

    @Column(name = "delivery_address_postal_code", length = 20)
    private String postalCode;
}