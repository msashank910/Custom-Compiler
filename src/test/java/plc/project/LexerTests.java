package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Leading Underscore", "_abc", false)   //still broken
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Leading Zero", "01", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);

    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Unicode", "\"ρ★⚡\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Newline Unterminated", "\"unterminated\n\"", false) // This test should fail due to the newline
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Tab", "\f", false),
                Arguments.of("Rho", "\u03C1", true)
                //Arguments.of("Rho", "ρ", true)

        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    @Test
    void testBackspaceInIdentifier() {
        String input = "one" + ((char) 0x0008) + "two";
        Lexer lexer = new Lexer(input);
        List<Token> tokens = lexer.lex();
        Assertions.assertEquals(1, tokens.size(), "Expected one token.");
        Assertions.assertEquals("ontwo", tokens.get(0).getLiteral(), "Expected literal to concatenate without whitespace.");
        Assertions.assertEquals(Token.Type.IDENTIFIER, tokens.get(0).getType(), "Expected token type IDENTIFIER.");
    }


    @Test
    void testMixedWhitespaceBetweenIntegers() {
        String input = "123 " + ((char) 0x0008) + ((char) 0x000A) + ((char) 0x000D) + "\t123";
        Lexer lexer = new Lexer(input);
        List<Token> tokens = lexer.lex();
        Assertions.assertEquals(2, tokens.size(), "Expected two tokens.");
        Assertions.assertEquals("123", tokens.get(0).getLiteral(), "Expected first integer to be recognized.");
        Assertions.assertEquals("123", tokens.get(1).getLiteral(), "Expected second integer to be recognized.");
        Assertions.assertEquals(Token.Type.INTEGER, tokens.get(0).getType(), "Expected first token type INTEGER.");
        Assertions.assertEquals(Token.Type.INTEGER, tokens.get(1).getType(), "Expected second token type INTEGER.");
    }


    @Test
    void testVerticalTabAndFormFeedInIdentifier() {
        String input = "abc" + ((char) 0x000B) + ((char) 0x000C) + "def";
        Lexer lexer = new Lexer(input);
        List<Token> tokens = lexer.lex();

        // Expecting two tokens: "abc" and "def", because vertical tab and form feed are treated as whitespace and skipped
        Assertions.assertEquals(2, tokens.size(), "Expected two tokens.");

        // First token is "abc"
        Assertions.assertEquals("abc", tokens.get(0).getLiteral(), "Expected first token to be 'abc'.");
        Assertions.assertEquals(Token.Type.IDENTIFIER, tokens.get(0).getType(), "Expected first token type IDENTIFIER.");

        // Second token is "def"
        Assertions.assertEquals("def", tokens.get(1).getLiteral(), "Expected second token to be 'def'.");
        Assertions.assertEquals(Token.Type.IDENTIFIER, tokens.get(1).getType(), "Expected second token type IDENTIFIER.");
    }



    @Test
    void testLeadingUnderscore() {
        String input = "_abc";
        Lexer lexer = new Lexer(input);
        List<Token> tokens = lexer.lex();

        // Expecting two tokens: an operator for the underscore and an identifier for "abc"
        Assertions.assertEquals(2, tokens.size(), "Expected two tokens.");

        // First token is the underscore, recognized as an operator
        Assertions.assertEquals("_", tokens.get(0).getLiteral(), "Expected first token to be an underscore.");
        Assertions.assertEquals(Token.Type.OPERATOR, tokens.get(0).getType(), "Expected first token type OPERATOR.");

        // Second token is "abc"
        Assertions.assertEquals("abc", tokens.get(1).getLiteral(), "Expected second token to be 'abc'.");
        Assertions.assertEquals(Token.Type.IDENTIFIER, tokens.get(1).getType(), "Expected second token type IDENTIFIER.");
    }

    @Test
    void testSpecialEscapesString() {
        String input = "\"sq\\\'dq\\\"bs\\\\\"";
        Lexer lexer = new Lexer(input);
        List<Token> tokens = lexer.lex();

        // Expecting a single STRING token
        Assertions.assertEquals(1, tokens.size(), "Expected one token.");

        // The token should be the string with the special escape characters preserved
        Assertions.assertEquals("\"sq\\'dq\\\"bs\\\\\"", tokens.get(0).getLiteral(), "Expected string with special escapes.");
        Assertions.assertEquals(Token.Type.STRING, tokens.get(0).getType(), "Expected token type STRING.");
    }



    @Test
    public void testComplexInput() {
        String input = "abc 123 456.789 'c' \"string\" %";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "abc", 0),
                new Token(Token.Type.INTEGER, "123", 4),
                new Token(Token.Type.DECIMAL, "456.789", 8),
                new Token(Token.Type.CHARACTER, "'c'", 16),
                new Token(Token.Type.STRING, "\"string\"", 20),
                new Token(Token.Type.OPERATOR, "%", 29)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }


    @Test
    public void testLeadingDecimal() {
        String input = ".5";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.OPERATOR, ".", 0),
                new Token(Token.Type.INTEGER, "5", 1)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }


    @Test
    public void testDoubleDotsAfterNumber() {
        String input = "1..0";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.INTEGER, "1", 0),
                new Token(Token.Type.OPERATOR, ".", 1),
                new Token(Token.Type.OPERATOR, ".", 2),
                new Token(Token.Type.INTEGER, "0", 3)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }


    @Test
    public void onePointTwoPointThree() {
        String input = "1.2.3";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.DECIMAL, "1.2", 0),
                new Token(Token.Type.OPERATOR, ".", 3),
                new Token(Token.Type.INTEGER, "3", 4)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }


    public void zeroOne() {
        String input = "01";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.INTEGER, "0", 0),
                new Token(Token.Type.INTEGER, "1", 1)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }


    @Test
    public void oneDotToString() {
        String input = "1.toString()";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.INTEGER, "1", 0),
                new Token(Token.Type.OPERATOR, ".", 1),
                new Token(Token.Type.IDENTIFIER, "toString", 2),
                new Token(Token.Type.OPERATOR, "(", 10),
                new Token(Token.Type.OPERATOR, ")", 11)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }

    @Test
    public void spaceshipOperator() {
        String input = "<=>";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.OPERATOR, "<", 0),
                new Token(Token.Type.OPERATOR, "=", 1),
                new Token(Token.Type.OPERATOR, ">", 2)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }


    @Test
    public void nullCharacter() {
        String input = "\u2400";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.OPERATOR, "\u2400", 0)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }

    @Test
    public void testPrintFunctionCall() {
        String input = "print(\"Hello\", separator, \"World!\")";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "print", 0),
                new Token(Token.Type.OPERATOR, "(", 5),
                new Token(Token.Type.STRING, "\"Hello\"", 6),
                new Token(Token.Type.OPERATOR, ",", 13),
                new Token(Token.Type.IDENTIFIER, "separator", 15),
                new Token(Token.Type.OPERATOR, ",", 24),
                new Token(Token.Type.STRING, "\"World!\"", 26),
                new Token(Token.Type.OPERATOR, ")", 34)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }


    @Test
    public void testSwitchCaseStatement() {
        String input = "SWITCH expr CASE expr1: stmt1; CASE expr2: stmt2; DEFAULT stmt3; END";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                new Token(Token.Type.IDENTIFIER, "expr", 7),
                new Token(Token.Type.IDENTIFIER, "CASE", 12),
                new Token(Token.Type.IDENTIFIER, "expr1", 17),
                new Token(Token.Type.OPERATOR, ":", 22),
                new Token(Token.Type.IDENTIFIER, "stmt1", 24),
                new Token(Token.Type.OPERATOR, ";", 29),
                new Token(Token.Type.IDENTIFIER, "CASE", 31),
                new Token(Token.Type.IDENTIFIER, "expr2", 36),
                new Token(Token.Type.OPERATOR, ":", 41),
                new Token(Token.Type.IDENTIFIER, "stmt2", 43),
                new Token(Token.Type.OPERATOR, ";", 48),
                new Token(Token.Type.IDENTIFIER, "DEFAULT", 50),
                new Token(Token.Type.IDENTIFIER, "stmt3", 58),
                new Token(Token.Type.OPERATOR, ";", 63),
                new Token(Token.Type.IDENTIFIER, "END", 65)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }

    @Test
    public void testExpressionLexing() {
        String input = "x + 1 == y / 2.0 - 3";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "x", 0),
                new Token(Token.Type.OPERATOR, "+", 2),
                new Token(Token.Type.INTEGER, "1", 4),
                new Token(Token.Type.OPERATOR, "==", 6),
                new Token(Token.Type.IDENTIFIER, "y", 9),
                new Token(Token.Type.OPERATOR, "/", 11),
                new Token(Token.Type.DECIMAL, "2.0", 13),
                new Token(Token.Type.OPERATOR, "-", 17),
                new Token(Token.Type.INTEGER, "3", 19)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }

    @Test
    public void testObjectFieldMethodAccess() {
        String input = "obj.field.method()";
        Lexer lexer = new Lexer(input);
        List<Token> actualTokens = lexer.lex();
        List<Token> expectedTokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "obj", 0),
                new Token(Token.Type.OPERATOR, ".", 3),
                new Token(Token.Type.IDENTIFIER, "field", 4),
                new Token(Token.Type.OPERATOR, ".", 9),
                new Token(Token.Type.IDENTIFIER, "method", 10),
                new Token(Token.Type.OPERATOR, "(", 16),
                new Token(Token.Type.OPERATOR, ")", 17)
        );

        Assertions.assertEquals(expectedTokens.size(), actualTokens.size(), "Number of tokens does not match.");
        for (int i = 0; i < expectedTokens.size(); i++) {
            Assertions.assertEquals(expectedTokens.get(i), actualTokens.get(i), "Token at index " + i + " does not match.");
        }
    }



}


