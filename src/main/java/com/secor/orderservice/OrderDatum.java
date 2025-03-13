package com.secor.orderservice;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderDatum {

    private String orderid;
    private String paymentid;
    private String type;
    private String description;
    private String status;
}
