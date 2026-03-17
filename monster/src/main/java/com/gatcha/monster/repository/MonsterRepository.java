package com.gatcha.monster.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.gatcha.monster.model.Monster;

@Repository
public interface MonsterRepository extends MongoRepository<Monster, String> {
}
