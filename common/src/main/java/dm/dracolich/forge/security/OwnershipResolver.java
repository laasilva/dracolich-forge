package dm.dracolich.forge.security;

public final class OwnershipResolver {

    private OwnershipResolver() {
    }

    public static boolean ownedBy(Principal principal, String ownerUserId, String ownerAnonId) {
        if (principal == null) {
            return false;
        }
        return switch (principal.type()) {
            case USER -> ownerUserId != null && ownerUserId.equals(principal.id());
            case ANON -> ownerAnonId != null && ownerAnonId.equals(principal.id());
        };
    }
}
