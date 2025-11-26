package dm.dracolich.forge.to;

import lombok.*;

/**
 * Represents a rollable face of dices. Or Items. Or anything you'd want to roll.
 *
 * @implNote All fields are nullable because a Value may be created without any parameter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Value {
    /**
     * The identifier of the value (e.g.: "1" for a dice face, "Common" for an item rarity).
     */
    private String id;
    
    /**
     * The weight of the value. If null or zero, the value will not be chosen.
     */
    private Integer weight;
    
    /**
     * The count of occurrences of the value. If null, the value will be treated as having no instances.
     */
    private Integer count;
}
