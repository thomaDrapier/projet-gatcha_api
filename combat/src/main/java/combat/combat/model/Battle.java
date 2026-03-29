package combat.combat.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "battles")
public class Battle {
    @Id
    private String id;
    
    private String monster1Id;
    private String monster2Id;
    private String winnerMonsterId;
    private LocalDateTime battleDate;
    
    private List<BattleStep> replayLogs;

    public Battle() {
        this.battleDate = LocalDateTime.now();
        this.replayLogs = new ArrayList<>();
    }
}