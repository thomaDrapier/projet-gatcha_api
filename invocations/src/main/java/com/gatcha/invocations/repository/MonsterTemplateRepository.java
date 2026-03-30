package com.gatcha.invocations.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.gatcha.invocations.model.MonsterTemplate;

@Repository
public interface MonsterTemplateRepository extends MongoRepository<MonsterTemplate, Integer> {
}
