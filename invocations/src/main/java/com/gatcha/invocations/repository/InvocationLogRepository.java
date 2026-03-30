package com.gatcha.invocations.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.gatcha.invocations.model.InvocationLog;

@Repository
public interface InvocationLogRepository extends MongoRepository<InvocationLog, String> {
}