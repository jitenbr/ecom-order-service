package com.secor.restroservice;

import org.springframework.data.mongodb.repository.MongoRepository;


public interface RestroRepository extends MongoRepository<Restro, String> {
}
