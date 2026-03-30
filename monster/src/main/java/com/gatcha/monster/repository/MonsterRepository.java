package com.gatcha.monster.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.gatcha.monster.model.Monster; // <--- Vérifie bien cet import

@Repository
public interface MonsterRepository extends MongoRepository<Monster, String> {
    // Spring va maintenant comprendre que findById renvoie un objet "Monster"
}