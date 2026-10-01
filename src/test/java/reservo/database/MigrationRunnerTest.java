package reservo.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MigrationRunnerTest {

    private static String[] split(String sql) {
        String[] parts = MigrationRunner.splitStatements(sql);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    @Test
    void splitsOnSemicolons() {
        assertArrayEquals(new String[] {"CREATE TABLE a (id INT)", "CREATE TABLE b (id INT)"},
                split("CREATE TABLE a (id INT);\nCREATE TABLE b (id INT);"));
    }

    @Test
    void keepsStatementsThatArePrecededByComments() {
        String[] parts = split("-- users table\nCREATE TABLE users (id INT);");
        assertEquals(1, parts.length);
        assertEquals("CREATE TABLE users (id INT)", parts[0]);
    }

    @Test
    void apostropheInCommentDoesNotSwallowFollowingStatements() {
        String sql = "CREATE TABLE a (id INT -- H2 doesn't support this\n);\nCREATE TABLE b (id INT);";
        String[] parts = split(sql);
        assertEquals(2, parts.length);
        assertTrue(parts[1].startsWith("CREATE TABLE b"));
    }

    @Test
    void ignoresSemicolonsInsideStringLiterals() {
        String[] parts = split("INSERT INTO t VALUES ('a;b');\nINSERT INTO t VALUES ('c');");
        assertEquals(2, parts.length);
        assertEquals("INSERT INTO t VALUES ('a;b')", parts[0]);
    }

    @Test
    void doubleDashInsideStringIsNotAComment() {
        String[] parts = split("INSERT INTO t VALUES ('--not a comment');");
        assertArrayEquals(new String[] {"INSERT INTO t VALUES ('--not a comment')"}, parts);
    }
}
