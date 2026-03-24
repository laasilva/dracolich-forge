package dm.dracolich.forge.security;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

public interface JwtTokenValidator {
    Mono<Claims> validate(String token);
}
