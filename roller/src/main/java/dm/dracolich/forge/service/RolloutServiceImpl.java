package dm.dracolich.forge.service;

import dm.dracolich.forge.Roll;
import dm.dracolich.forge.to.DiceEnum;
import dm.dracolich.forge.to.Rollout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RolloutServiceImpl implements RolloutService {
    @Override
    public Rollout rollDice(DiceEnum dice, String serverSeed, String clientSeed, Long nonce) {
        Roll.FairRoll roll = Roll.fairRoll(serverSeed, clientSeed,
                nonce, dice.getDiceValues(), true);

        return roll.result();
    }
}
