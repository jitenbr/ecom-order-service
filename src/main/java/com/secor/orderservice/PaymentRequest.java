package com.secor.orderservice;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentRequest {

    private String payment_id;
    private String order_id;
    private Integer amount;
}
