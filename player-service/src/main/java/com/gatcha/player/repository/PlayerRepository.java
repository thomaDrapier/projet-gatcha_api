package com.gatcha.player.repository;

import com.gatcha.player.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;import org.springframework.stereotype.Component;import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PlayerRepository extends MongoRepository<Player, String> {
    Optional<Player> findByUsername(String username);
}