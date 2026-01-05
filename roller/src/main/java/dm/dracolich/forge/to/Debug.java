package dm.dracolich.forge.to;

import lombok.*;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
public class Debug {
    private String serverSeedUsed; // UUID - The server seed used for this roll (kept secret until revealed for verification)
    private String clientSeed; // UserId - The client-provided seed (ensures server can't predict outcome alone)
    private Long nonce; // Counter that increments per roll (ensures unique outcomes with same seeds)
    private String valueMsg; // The message hashed for value selection: clientSeed:nonce:value (for logging purposes)
    private String valueHmacHex; // Full HMAC-SHA256 output (hex) of value_msg using server seed as key
    private Long hmacValueNumericPrefix; // First 4 bytes of value_hmac_hex interpreted as an unsigned integer
    private Integer rollValue; // value_value % total_weight → determines which value is selected
    private Integer selectedValueWeight; // The weight of the chosen Value object
    private String itemMsg; // The message hashed for item selection: clientSeed:nonce:item (for logging purposes)
    private String itemHmacHex; // Full HMAC-SHA256 output (hex) of item_msg using server seed as key
    private Long hmacItemNumericPrefix; // First 4 bytes of item_hmac_hex as unsigned integer (714466818)
    private Integer itemIndex; // item_value % items_in_value → index within the selected value (714466818 % 4 = 2)
    private Integer itemsInValue; // Number of items in the selected Value (from Value.getCount())
    private Integer totalWeight; // Sum of all weights across all Value objects in the input list
}
