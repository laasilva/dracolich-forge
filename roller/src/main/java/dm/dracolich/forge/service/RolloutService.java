package dm.dracolich.forge.service;

import dm.dracolich.forge.to.DiceEnum;
import dm.dracolich.forge.to.Rollout;

public interface RolloutService {
    Rollout rollDice(DiceEnum dice, String serverSeed, String clientSeed, Long nonce);
}
