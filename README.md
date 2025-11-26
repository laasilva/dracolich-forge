# Dracolich Forge

**Dracolich Forge** is a Java library that provides tools and helpers for online RPG applications, with a focus on provably-fair random number generation for dice rolls, loot drops, and weighted item selection.

## Features

- **Provably-Fair RNG**: HMAC-SHA256-based deterministic random selection that can be independently verified
- **Flexible Weighted Selection**: Generic weighted choice algorithm for any type of item
- **Dice Rolling**: Simple API for rolling dice (D20, D6, etc.) with cryptographic guarantees
- **Seed Chain Advancement**: SHA-256-based seed progression for sequential rolls
- **Debug Information**: Complete audit trail with HMAC values, messages, and intermediate computations
- **Generic Primitives**: Reusable building blocks (`Prf`, `Select`, `RollContext`) for custom use cases

## Installation

### Maven

```xml
<dependency>
    <groupId>dm.dracolich.forge</groupId>
    <artifactId>roller</artifactId>
    <version>0.0.1</version>
</dependency>
```

### Requirements

- Java 25+
- Lombok (annotation processing enabled)

## Quick Start

### Rolling a D20

```java
import dm.dracolich.forge.Roll;

String serverSeed = "secret-server-seed";
String clientSeed = "player-chosen-seed";
long nonce = 1;

// Roll a D20 (returns index 0-19)
int result = Roll.drawIndex(serverSeed, clientSeed, nonce, "dice", 20);
System.out.println("D20 roll: " + (result + 1)); // Convert to 1-20
```

### Weighted Item Selection

```java
import dm.dracolich.forge.Roll;
import dm.dracolich.forge.to.Value;
import java.util.List;

Value common = Value.builder().id("Common").weight(70).count(10).build();
Value rare = Value.builder().id("Rare").weight(25).count(5).build();
Value legendary = Value.builder().id("Legendary").weight(5).count(2).build();

List<Value> rarities = List.of(common, rare, legendary);

Value chosen = Roll.drawWeighted(
    serverSeed, 
    clientSeed, 
    nonce, 
    "rarity", 
    rarities, 
    v -> v.getWeight()
);

System.out.println("Selected rarity: " + chosen.getId());
```

### Full Provably-Fair Roll with Debug Info

```java
import dm.dracolich.forge.Roll;
import dm.dracolich.forge.to.Value;
import java.util.List;
import java.util.Map;

Value common = Value.builder().id("Common").weight(70).count(10).build();
Value rare = Value.builder().id("Rare").weight(25).count(5).build();
Value legendary = Value.builder().id("Legendary").weight(5).count(2).build();

Roll.FairRoll result = Roll.fairRoll(
    serverSeed,
    clientSeed,
    nonce,
    List.of(common, rare, legendary),
    true  // advance server seed
);

Map<String, Object> rollResult = result.result();
System.out.println("Rarity: " + rollResult.get("value"));
System.out.println("Item index: " + rollResult.get("item"));
System.out.println("Next server seed: " + result.nextServerSeed());

// Access debug information for verification
Map<String, Object> debug = (Map<String, Object>) rollResult.get("debug");
System.out.println("Value HMAC: " + debug.get("value_hmac_hex"));
System.out.println("Item HMAC: " + debug.get("item_hmac_hex"));
```

## API Reference

### Core Classes

#### `Roll`

Main entry point for all rolling operations.

**Static Methods:**

- `drawIndex(serverSeed, clientSeed, nonce, category, size)` - Select an index from 0 to size-1
- `drawWeighted(serverSeed, clientSeed, nonce, category, items, weightFn)` - Select item by weight
- `fairRoll(serverSeed, clientSeed, nonce, values, advanceServerSeed)` - Full two-stage roll with debug info
- `seedChainAdvance(serverSeed)` - Compute SHA-256 of seed for next roll
- `validateHmac(secretKey, message, expectedHmacHex)` - Verify HMAC (constant-time)
- `buildMessage(clientSeed, nonce, category)` - Build message for HMAC computation

#### `Roll.RollContext`

Record holding roll parameters:

```java
public record RollContext(String serverSeed, String clientSeed, long nonce) { }
```

#### `Roll.Prf`

Pseudo-random function primitives:

- `drawInt(RollContext ctx, String category)` - Get deterministic int from HMAC
- `drawHex(RollContext ctx, String category)` - Get HMAC as hex string

#### `Roll.Select`

Generic selection algorithms:

- `weightedChoice(items, weightFn, draw)` - Pick item by weight
- `indexChoice(size, draw)` - Map draw to index (returns -1 if size ≤ 0)

#### `Value`

Represents a rollable value (dice face, item rarity, etc.):

```java
public class Value {
    private String id;       // Identifier (e.g., "Common", "D20")
    private Integer weight;  // Weight for selection (null/0 = not chosen)
    private Integer count;   // Number of items in this value
}
```

### `FairRoll` Result Structure

```java
public record FairRoll(Map<String, Object> result, String nextServerSeed) { }
```

**Result Map Keys:**

- `"value"` - Selected value ID
- `"item"` - Selected item index (or null if count is 0/null)
- `"debug"` - Map with verification data:
  - `"server_seed_used"`, `"client_seed"`, `"nonce"`
  - `"value_msg"`, `"value_hmac_hex"`, `"value_value"`, `"value_roll"`
  - `"item_msg"`, `"item_hmac_hex"`, `"item_value"`, `"item_index"`
  - `"selected_value_weight"`, `"items_in_value"`, `"total_weight"`

## How Provably-Fair Works

1. **Server Seed**: Secret value known only to the server
2. **Client Seed**: Public value chosen by the player
3. **Nonce**: Counter incremented for each roll
4. **Category**: Distinguishes different draws ("value", "item", "dice")

### Algorithm

```
HMAC = HMAC-SHA256(serverSeed, "clientSeed:nonce:category")
value = first 4 bytes of HMAC as unsigned int
result = value % poolSize
```

### Verification

Players can verify rolls by:

1. Obtaining the server seed (revealed after roll or session end)
2. Computing HMAC with their client seed and nonce
3. Comparing with the published HMAC hex in debug info
4. Verifying the selection matches the computed value

### Seed Chain

Server seeds advance via SHA-256 hashing:

```
nextServerSeed = SHA-256(currentServerSeed)
```

This allows pre-commitment: server publishes future seed hashes, then reveals seeds later for verification.

## Testing

```bash
# Run all tests
mvn test

# Run only roller module tests
mvn test -pl roller

# Run with coverage
mvn clean verify
```

### Example Test

```java
@Test
void d20_deterministic_roll() {
    Value d20 = Value.builder().id("D20").weight(1).count(20).build();
    
    Roll.FairRoll result = Roll.fairRoll(
        "server-seed",
        "client-seed",
        42L,
        List.of(d20),
        true
    );
    
    Integer itemIndex = (Integer) result.result().get("item");
    assertTrue(itemIndex >= 0 && itemIndex < 20);
}
```

## Use Cases

- **Online Casinos**: Provably-fair dice games, slot machines
- **RPG Loot Systems**: Weighted rarity selection with item drops
- **Gacha Games**: Transparent pull mechanics
- **Tabletop RPG Tools**: Verifiable dice rolling for online play
- **Randomized Rewards**: Any system requiring transparent RNG

## Building from Source

```bash
git clone https://github.com/laasilva/dracolich-forge.git
cd dracolich-forge
mvn clean install
```

## Project Structure

```
forge/
├── pom.xml                 # Parent POM
└── roller/                 # Roller module
    ├── pom.xml
    ├── src/
    │   ├── main/java/dm/dracolich/forge/
    │   │   ├── Roll.java           # Main API
    │   │   └── to/
    │   │       └── Value.java      # Value DTO
    │   └── test/java/dm/dracolich/forge/
    │       └── RollTest.java       # Unit tests
```

