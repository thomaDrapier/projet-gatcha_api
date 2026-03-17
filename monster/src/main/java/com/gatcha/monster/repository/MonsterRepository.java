package com.gatcha.monster.repository;

import org.springframework.data.mongodb.repository.MongoRepository; // Assurez-vous que c'est le bon import
import org.springframework.stereotype.Repository;

import com.gatcha.monster.model.MonsterInstance;

@Repository
// Le premier type doit être MonsterInstance, pas Monster
public interface MonsterRepository extends MongoRepository<MonsterInstance, String> {
}