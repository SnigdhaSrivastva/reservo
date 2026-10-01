package reservo.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void verifiesCorrectPasswordAndRejectsWrongOne() {
        String stored = PasswordUtil.hashPassword("correct horse");
        assertTrue(PasswordUtil.verifyPassword("correct horse", stored));
        assertFalse(PasswordUtil.verifyPassword("wrong horse", stored));
    }

    @Test
    void saltsEachHash() {
        assertNotEquals(PasswordUtil.hashPassword("same"), PasswordUtil.hashPassword("same"));
    }

    @Test
    void usesVersionedFormat() {
        assertTrue(PasswordUtil.hashPassword("pw").matches("pbkdf2\\$\\d+\\$[A-Za-z0-9+/=]+\\$[A-Za-z0-9+/=]+"));
    }

    @Test
    void rejectsMalformedOrLegacyHashes() {
        assertFalse(PasswordUtil.verifyPassword("test123", null));
        assertFalse(PasswordUtil.verifyPassword("test123", "not-a-hash"));
        assertFalse(PasswordUtil.verifyPassword("test123",
                "ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae"));
        assertFalse(PasswordUtil.verifyPassword("test123", "pbkdf2$abc$###$###"));
    }
}
