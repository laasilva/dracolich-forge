package dm.dracolich.forge.security;

import java.util.Objects;

public record Principal(PrincipalType type, String id) {

    public Principal {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
    }

    public static Principal user(String userId) {
        return new Principal(PrincipalType.USER, userId);
    }

    public static Principal anon(String anonId) {
        return new Principal(PrincipalType.ANON, anonId);
    }

    public boolean isUser() {
        return type == PrincipalType.USER;
    }

    public boolean isAnon() {
        return type == PrincipalType.ANON;
    }
}
