package dm.dracolich.forge.to;

import dm.dracolich.forge.Dice;

import java.util.List;

public enum DiceEnum {
    D4(Dice.d4()),
    D8(Dice.d8()),
    D10(Dice.d10()),
    D12(Dice.d12()),
    D20(Dice.d20()),
    D100(Dice.d100());

    private final List<Value> dice;

    DiceEnum(List<Value> dice) {
        this.dice = dice;
    }

    public List<Value> getDiceValues(DiceEnum dice) {
        return dice.dice;
    }
}
