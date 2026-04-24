package dm.dracolich.forge.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

public final class ReactiveSecurityContextUtil {

    private ReactiveSecurityContextUtil() {
    }

    public static Mono<Principal> getPrincipal() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(ReactiveSecurityContextUtil::isPrincipal)
                .map(auth -> (Principal) auth.getPrincipal());
    }

    private static boolean isPrincipal(Authentication auth) {
        return auth != null && auth.getPrincipal() instanceof Principal;
    }
}
