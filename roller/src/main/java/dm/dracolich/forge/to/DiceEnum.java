package dm.dracolich.forge.to;

import dm.dracolich.forge.Dice;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public enum DiceEnum {
    D4("d4", Dice.d4()),
    D8("d8", Dice.d8()),
    D10("d10", Dice.d10()),
    D12("d12", Dice.d12()),
    D20("d20", Dice.d20()),
    D100("d100", Dice.d100());

    private final String id;
    private final List<Value> dice;

    DiceEnum(String id, List<Value> dice) {
        this.id = id;
        this.dice = dice;
    }

    public List<Value> getDiceValues(DiceEnum dice) {
        return dice.dice;
    }

    public DiceEnum getDice(String dice) {
        if (dice.equals(DiceEnum.D4.id)) return DiceEnum.D4;
        if (dice.equals(DiceEnum.D8.id)) return DiceEnum.D8;
        if (dice.equals(DiceEnum.D10.id)) return DiceEnum.D10;
        if (dice.equals(DiceEnum.D12.id)) return DiceEnum.D12;
        if (dice.equals(DiceEnum.D20.id)) return DiceEnum.D20;
        if (dice.equals(DiceEnum.D100.id)) return DiceEnum.D100;

        log.error("Invalid dice [id: {}]", dice);
        throw new IllegalArgumentException(String.format("Invalid dice [id: %s]", dice));
    }
}
