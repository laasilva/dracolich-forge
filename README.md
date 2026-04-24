# Dracolich Forge

**Dracolich Forge** is a Java library that provides tools and helpers for online RPG applications, with a focus on provably-fair random number generation for dice rolls, loot drops, and weighted item selection.

## Features

### Common
- **Response Wrapping**: Automatic `DmdResponse` envelope for all controller returns
- **Error Framework**: Typed error codes, `ApiError`, and `ResponseException` with global `ControllerAdvice`
- **Security**: Unified `Principal` abstraction across `JwtAuthenticationWebFilter` (authenticated) and `AnonCookieFilter` (anonymous users with signed cookies), plus reactive helpers for reading the current identity and enforcing ownership

### Roller
- **Provably-Fair RNG**: HMAC-SHA256-based deterministic random selection that can be independently verified
- **Flexible Weighted Selection**: Generic weighted choice algorithm for any type of item
- **Dice Rolling**: Simple API for rolling dice (D20, D6, etc.) with cryptographic guarantees
- **Seed Chain Advancement**: SHA-256-based seed progression for sequential rolls
- **Debug Information**: Complete audit trail with HMAC values, messages, and intermediate computations
- **Generic Primitives**: Reusable building blocks (`Prf`, `Select`, `RollContext`) for custom use cases

## Modules

| Module | Description |
|---|---|
| `common` | Shared infrastructure — response wrappers, error handling, JWT security filter |
| `roller` | Provably-fair RNG — HMAC-SHA256 dice rolls, weighted selection |

## Requirements

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

## Common Module

The `common` module provides shared infrastructure for all Dracolich services.

### Response Handling

Standardized response wrapper used across all APIs:

```java
import dm.dracolich.forge.response.DmdResponse;

// Success
new DmdResponse<>(payload);
new DmdResponse<>(payload, "Custom message");

// Error
DmdResponse.of(errors, HttpStatus.BAD_REQUEST, "Validation failed");
```

**`DmdResponseWrapper`** automatically wraps all controller returns from `dm.dracolich.*` packages in `DmdResponse`. Controllers return raw `Mono<T>` / `Flux<T>` — the wrapper handles the envelope.

### Error Handling

```java
import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.error.ErrorCode;
import dm.dracolich.forge.exception.ResponseException;

// Throw from service layer — ControllerAdvice handles the rest
throw new ResponseException(
    "Something went wrong",
    List.of(new ApiError(myErrorCode)),
    HttpStatus.BAD_REQUEST
);
```

- **`ErrorCode`** — interface with `getCode()` and `getMessage()`. Each service implements its own enum.
- **`ApiError`** — wraps an `ErrorCode` with optional `severity` and `field`.
- **`ControllerAdvice`** — catches `ResponseException` and returns `ResponseEntity<DmdResponse<?>>`.

### Security

Unified identity and authorization primitives for all Dracolich services. Supports authenticated users (JWT) and anonymous users (signed cookie), behind a single `Principal` abstraction so services don't need to care which mechanism produced the identity.

#### Identity types

| Class | Role |
|---|---|
| `Principal` | Immutable record `(type, id)` representing the current caller. Factories: `Principal.user(userId)`, `Principal.anon(anonId)`. Convenience: `isUser()`, `isAnon()`. |
| `PrincipalType` | Enum: `USER` \| `ANON`. |

Services never construct a `Principal` — filters do. Services only read the current one.

#### Reading the current identity

```java
import dm.dracolich.forge.security.ReactiveSecurityContextUtil;

ReactiveSecurityContextUtil.getPrincipal()
    .switchIfEmpty(Mono.error(unauthorized()))
    .flatMap(principal -> { /* ... */ });
```

Returns `Mono<Principal>`. Empty means no identity present in the security context.

#### Ownership checks

```java
import dm.dracolich.forge.security.OwnershipResolver;

boolean owns = OwnershipResolver.ownedBy(principal, resource.getUserId(), resource.getAnonId());
```

Matches strictly by `PrincipalType`: a `USER` principal is only checked against `ownerUserId`, an `ANON` principal only against `ownerAnonId`. Cross-type ID collisions cannot grant access.

#### JWT authentication

`JwtTokenValidator` — interface each service implements with its own validation:

```java
public interface JwtTokenValidator {
    Mono<Claims> validate(String token);
}
```

`JwtAuthenticationWebFilter` — reactive `WebFilter` that reads `Authorization: Bearer <token>`, validates via `JwtTokenValidator`, and populates `Principal.user(subject)` with `ROLE_<accessLevel>` authority.

```java
@Bean
public JwtAuthenticationWebFilter jwtFilter(JwtTokenValidator validator) {
    return new JwtAuthenticationWebFilter(validator);
}
```

- On failure (missing/invalid/expired token): passes through silently, logs WARN. Downstream filters or `authorizeExchange(...authenticated())` enforce the 401.
- Filter order: `10`.

Example `JwtTokenValidator` with EC public key:

```java
@Service
public class JwtValidatorImpl implements JwtTokenValidator {
    private final ECPublicKey publicKey;  // loaded from PEM

    @Override
    public Mono<Claims> validate(String token) {
        return Mono.fromCallable(() ->
            Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
        );
    }
}
```

#### Anonymous identity (signed cookies)

For flows where anonymous users need persistent identity across requests — e.g. editing a draft deck before signing up. Two components:

**`AnonCookieSigner`** — HMAC-SHA256 signer/verifier for `anon_id.expiresAtMillis.hmac` cookies. Constant-time comparison on verify.

```java
@Bean
public AnonCookieSigner anonCookieSigner(@Value("${dracolich.cookie.secret}") String secret) {
    return new AnonCookieSigner(secret);  // secret must be >= 32 chars
}
```

**`AnonCookieFilter`** — web filter that reads the cookie, sets `Principal.anon(id)` when valid, or mints a new id when absent/invalid.

```java
@Bean
public AnonCookieFilter anonCookieFilter(AnonCookieSigner signer,
                                          @Value("${dracolich.cookie.lifetime:PT24H}") Duration lifetime,
                                          @Value("${dracolich.cookie.secure:false}") boolean secure) {
    return new AnonCookieFilter(signer, lifetime, secure);
}
```

Behavior:
- If a `Principal` is already in the context (e.g. JWT filter ran first): **skips**. JWT wins.
- Valid signed cookie present: reads `anon_id`, sets `Principal.anon(anonId)`.
- No cookie or tampered/expired: mints a new `anon_id`, signs + adds to response, sets `Principal.anon(newId)`.

Response cookie attributes: `HttpOnly`, `SameSite=Lax`, `Secure` configurable, `Path=/`. Filter order: `20` (after JWT).

**Reading the anon cookie directly** (for flows like `/claim` that need to verify the cookie matches a resource — not just that the caller is authed):

```java
Optional<String> anonId = AnonCookieFilter.readAnonIdCookie(exchange, signer);
```

#### Typical wiring (services using both)

```java
@Bean
public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http,
                                                   JwtAuthenticationWebFilter jwtFilter,
                                                   AnonCookieFilter anonFilter) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .addFilterAfter(anonFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .authorizeExchange(ex -> ex.anyExchange().permitAll())  // filters don't reject; services enforce via Principal
        .build();
}
```

Services then call `getPrincipal()` + `OwnershipResolver` per endpoint to decide 401/403/200.

#### Required properties

```yaml
dracolich:
  cookie:
    secret: ${ANON_COOKIE_SECRET}   # >= 32 chars, HMAC signing key
    lifetime: PT24H                 # ISO-8601 Duration, default PT24H
    secure: false                   # true in prod (HTTPS only)
```

### Installation

```xml
<!-- Response handling, error framework -->
<dependency>
    <groupId>dm.dracolich.forge</groupId>
    <artifactId>common</artifactId>
    <version>5.0.0</version>
</dependency>

<!-- Provably-fair RNG (optional) -->
<dependency>
    <groupId>dm.dracolich.forge</groupId>
    <artifactId>roller</artifactId>
    <version>5.0.0</version>
</dependency>
```

## Project Structure

```
forge/
├── pom.xml                 # Parent POM
├── common/                 # Shared infrastructure
│   └── src/main/java/dm/dracolich/forge/
│       ├── controller/
│       │   └── ControllerAdvice.java
│       ├── error/
│       │   ├── ApiError.java
│       │   ├── ErrorCode.java
│       │   ├── ErrorCodes.java
│       │   ├── ErrorSeverity.java
│       │   └── DmdError.java
│       ├── exception/
│       │   ├── ResponseException.java
│       │   └── ValidationException.java
│       ├── response/
│       │   ├── DmdResponse.java
│       │   └── DmdResponseWrapper.java
│       └── security/
│           ├── AnonCookieFilter.java
│           ├── AnonCookieSigner.java
│           ├── JwtAuthenticationWebFilter.java
│           ├── JwtTokenValidator.java
│           ├── OwnershipResolver.java
│           ├── Principal.java
│           ├── PrincipalType.java
│           └── ReactiveSecurityContextUtil.java
└── roller/                 # Provably-fair RNG
    └── src/main/java/dm/dracolich/forge/
        ├── Roll.java
        └── to/
            └── Value.java
```

