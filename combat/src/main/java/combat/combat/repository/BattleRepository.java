package combat.combat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import combat.combat.model.Battle;

@Repository
public interface BattleRepository extends MongoRepository<Battle, String> {
}