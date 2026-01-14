package com.gatcha.auth.repository;

import com.gatcha.auth.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    // Spring implémente cette méthode automatiquement grâce au nom
    Optional<User> findByUsername(String username);
    Optional<User> findByToken(String token);
}
