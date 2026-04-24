package dm.dracolich.forge.security;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OwnershipResolverTest {

    @Nested
    class UserPrincipal {

        private final Principal principal = Principal.user("user-1");

        @Test
        void matchesWhenUserIdEqual() {
            assertTrue(OwnershipResolver.ownedBy(principal, "user-1", null));
        }

        @Test
        void ignoresAnonIdWhenUserPrincipal() {
            assertTrue(OwnershipResolver.ownedBy(principal, "user-1", "anon-1"));
            assertFalse(OwnershipResolver.ownedBy(principal, "user-2", "user-1"));
        }

        @Test
        void rejectsWhenUserIdDiffers() {
            assertFalse(OwnershipResolver.ownedBy(principal, "user-2", null));
        }

        @Test
        void rejectsWhenOwnerUserIdNull() {
            assertFalse(OwnershipResolver.ownedBy(principal, null, "anon-1"));
        }
    }

    @Nested
    class AnonPrincipal {

        private final Principal principal = Principal.anon("anon-1");

        @Test
        void matchesWhenAnonIdEqual() {
            assertTrue(OwnershipResolver.ownedBy(principal, null, "anon-1"));
        }

        @Test
        void ignoresUserIdWhenAnonPrincipal() {
            assertTrue(OwnershipResolver.ownedBy(principal, "user-1", "anon-1"));
            assertFalse(OwnershipResolver.ownedBy(principal, "anon-1", "anon-2"));
        }

        @Test
        void rejectsWhenAnonIdDiffers() {
            assertFalse(OwnershipResolver.ownedBy(principal, null, "anon-2"));
        }

        @Test
        void rejectsWhenOwnerAnonIdNull() {
            assertFalse(OwnershipResolver.ownedBy(principal, "user-1", null));
        }
    }

    @Test
    void nullPrincipalIsNeverOwner() {
        assertFalse(OwnershipResolver.ownedBy(null, "user-1", "anon-1"));
    }
}
