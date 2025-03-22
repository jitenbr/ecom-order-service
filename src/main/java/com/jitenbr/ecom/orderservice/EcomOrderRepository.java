package com.jitenbr.ecom.orderservice;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface EcomOrderRepository extends MongoRepository<EcomOrder, String> {
}
