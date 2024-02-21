package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserExpressionTests {

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Statement.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function("name", Arrays.asList()))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Statement.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "name"),
                                new Ast.Expression.Access(Optional.empty(), "value")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expression.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expression.Literal(Boolean.TRUE)
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expression.Literal(new BigInteger("1"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expression.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expression.Literal('c')
                ),
                Arguments.of("String Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expression.Literal("string")
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expression.Literal("Hello,\nWorld!")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expression.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                //(expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        ))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expression.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                //expr1 && expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expression.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expression.Access(Optional.empty(), "name")
                ),
                Arguments.of("List Index Access",
                        Arrays.asList(
                                //list[expr]
                                new Token(Token.Type.IDENTIFIER, "list", 0),
                                new Token(Token.Type.OPERATOR, "[", 4),
                                new Token(Token.Type.IDENTIFIER, "expr", 5),
                                new Token(Token.Type.OPERATOR, "]", 9)
                        ),
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")), "list")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expression.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                //name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        ))
                )
        );
    }



    //TEST CASES FROM THE FINAL ONE
    @Test
    public void testMissingSemicolonAfterIdentifierExpression() {
        // Constructing the token list for an expression statement "x" without the semicolon
        List<Token> tokens = Arrays.asList(
                //new Token(Token.Type.IDENTIFIER, "x", 0)
                new Token(Token.Type.IDENTIFIER, "x", 0),
                new Token(Token.Type.OPERATOR, "=", 1),
                new Token(Token.Type.INTEGER, "10", 2)
                // Semicolon is intentionally missing here to simulate the error condition
        );

        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing semicolon
        assertThrows(ParseException.class, () -> parser.parseStatement(),
                "Expected ParseException for missing semicolon after identifier expression statement.");
    }

    @Test
    public void testListAssignmentWithSimpleExpression() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "list", 0),
                new Token(Token.Type.OPERATOR, "[", 4),
                new Token(Token.Type.IDENTIFIER, "offset", 5),
                new Token(Token.Type.OPERATOR, "]", 11),
                new Token(Token.Type.OPERATOR, "=", 12),
                new Token(Token.Type.IDENTIFIER, "expr", 13),
                new Token(Token.Type.OPERATOR, ";", 17)
        );
        Parser parser = new Parser(tokens);
        Ast.Statement expected = new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "offset")), "list"),
                new Ast.Expression.Access(Optional.empty(), "expr")
        );

        Assertions.assertDoesNotThrow(() -> {
            Ast.Statement result = parser.parseStatement();
            assertEquals(expected, result);
        });
    }

    @Test
    public void testListAssignmentWithComplexExpression() {
        // Prepare the tokens for "list[offset] = expr1 + expr2;"
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "list", 0),
                new Token(Token.Type.OPERATOR, "[", 4),
                new Token(Token.Type.IDENTIFIER, "offset", 5),
                new Token(Token.Type.OPERATOR, "]", 11),
                new Token(Token.Type.OPERATOR, "=", 12),
                new Token(Token.Type.IDENTIFIER, "expr1", 13),
                new Token(Token.Type.OPERATOR, "+", 18),
                new Token(Token.Type.IDENTIFIER, "expr2", 19),
                new Token(Token.Type.OPERATOR, ";", 24)
        );
        Parser parser = new Parser(tokens);

        // Expected AST structure
        Ast.Statement expected = new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "offset")), "list"),
                new Ast.Expression.Binary("+",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                )
        );
        // Parse the tokens and assert the resulting AST matches the expected structure
        Ast.Statement result = parser.parseStatement();
        assertEquals(expected, result, "The parsed AST does not match the expected structure.");
    }

    //Missing SemiColon
    @Test
    public void testAssignmentMissingSemicolon() {
        // Prepare the tokens for "name = expr" without the terminating semicolon
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "name", 0),
                new Token(Token.Type.OPERATOR, "=", 4),
                new Token(Token.Type.IDENTIFIER, "expr", 5)
                // Notice the missing semicolon token here
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing semicolon
        assertThrows(ParseException.class, () -> parser.parseStatement(),
                "Expected ParseException for missing semicolon after identifier expression statement.");
    }


    @Test
    public void testGroupExpressionMissingClosingParenthesis() {
        // Prepare the tokens for "(expr" without the closing parenthesis
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.OPERATOR, "(", 0),
                new Token(Token.Type.IDENTIFIER, "expr", 1)
                // Notice the missing closing parenthesis token here
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing closing parenthesis
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing closing parenthesis in group expression.");
    }


    @Test
    public void testNilLiteralExpression() {
        // Assuming your Token class and Parser are structured to handle NIL literals
        List<Token> tokens = Collections.singletonList(
                new Token(Token.Type.IDENTIFIER, "NIL", 0)
                // Use the appropriate Token.Type for NIL if different
        );
        Parser parser = new Parser(tokens);

        // Assuming your AST has a specific representation for NIL literals, like Ast.Expression.Literal with null value
        Ast.Expression expected = new Ast.Expression.Literal(null);

        // Parse the token into an AST
        Ast.Expression result = parser.parseExpression();

        // Assert that the parsed AST matches the expected NIL literal representation
        assertEquals(expected, result, "The parsed expression should correctly represent a NIL literal.");
    }

    @Test
    public void testLogicalOrExpression() {
        // Prepare the tokens for "expr1 || expr2"
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "||", 5),
                new Token(Token.Type.IDENTIFIER, "expr2", 8)
        );
        Parser parser = new Parser(tokens);

        // The expected AST for the "expr1 || expr2" expression
        Ast.Expression expected = new Ast.Expression.Binary(
                "||",
                new Ast.Expression.Access(Optional.empty(), "expr1"),
                new Ast.Expression.Access(Optional.empty(), "expr2")
        );

        // Attempt to parse the "expr1 || expr2" expression
        Ast.Expression result = parser.parseExpression();

        // Verify that the parsed AST matches the expected AST
        Assertions.assertEquals(expected, result, "The parser did not correctly parse the logical 'or' expression.");
    }

    @Test
    public void testLessThanComparison() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "<", 6),
                new Token(Token.Type.IDENTIFIER, "expr2", 8)
        );
        Parser parser = new Parser(tokens);

        Ast.Expression expected = new Ast.Expression.Binary(
                "<",
                new Ast.Expression.Access(Optional.empty(), "expr1"),
                new Ast.Expression.Access(Optional.empty(), "expr2")
        );

        Ast.Expression result = parser.parseExpression();
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testGreaterThanComparison() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, ">", 6),
                new Token(Token.Type.IDENTIFIER, "expr2", 8)
        );
        Parser parser = new Parser(tokens);

        Ast.Expression expected = new Ast.Expression.Binary(
                ">",
                new Ast.Expression.Access(Optional.empty(), "expr1"),
                new Ast.Expression.Access(Optional.empty(), "expr2")
        );

        Ast.Expression result = parser.parseExpression();
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testNotEqualsComparison() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "!=", 6),
                new Token(Token.Type.IDENTIFIER, "expr2", 9)
        );
        Parser parser = new Parser(tokens);

        Ast.Expression expected = new Ast.Expression.Binary(
                "!=",
                new Ast.Expression.Access(Optional.empty(), "expr1"),
                new Ast.Expression.Access(Optional.empty(), "expr2")
        );

        Ast.Expression result = parser.parseExpression();
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testSubtractionExpression() {
        // Prepare the tokens for "expr1 - expr2"
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "-", 6),
                new Token(Token.Type.IDENTIFIER, "expr2", 8)
        );
        Parser parser = new Parser(tokens);

        // Construct the expected AST node for the expression
        Ast.Expression expected = new Ast.Expression.Binary(
                "-",
                new Ast.Expression.Access(Optional.empty(), "expr1"),
                new Ast.Expression.Access(Optional.empty(), "expr2")
        );

        // Parse the expression and assert equality
        Ast.Expression result = parser.parseExpression();
        Assertions.assertEquals(expected, result, "The parsed subtraction expression does not match the expected AST structure.");
    }

    @Test
    public void testDivisionExpression() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "/", 6),
                new Token(Token.Type.IDENTIFIER, "expr2", 8)
        );
        Parser parser = new Parser(tokens);
        Ast.Expression expected = new Ast.Expression.Binary(
                "/",
                new Ast.Expression.Access(Optional.empty(), "expr1"),
                new Ast.Expression.Access(Optional.empty(), "expr2")
        );

        Ast.Expression result = parser.parseExpression();
        Assertions.assertEquals(expected, result);

    }


    @Test
    public void testExponationExpression() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "^", 6),
                new Token(Token.Type.IDENTIFIER, "expr2", 8)
        );
        Parser parser = new Parser(tokens);
        Ast.Expression expected = new Ast.Expression.Binary(
                "^",
                new Ast.Expression.Access(Optional.empty(), "expr1"),
                new Ast.Expression.Access(Optional.empty(), "expr2")
        );

        Ast.Expression result = parser.parseExpression();
        Assertions.assertEquals(expected, result);

    }

    @Test
    public void testMultipleLogicalOrOperators() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "||", 5),
                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                new Token(Token.Type.OPERATOR, "||", 13),
                new Token(Token.Type.IDENTIFIER, "expr3", 16)
        );
        Parser parser = new Parser(tokens);

        // The expected AST structure for "expr1 || expr2 || expr3"
        Ast.Expression expected = new Ast.Expression.Binary("||",
                new Ast.Expression.Binary("||",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                ),
                new Ast.Expression.Access(Optional.empty(), "expr3")
        );

        // Attempt to parse the expression and assert equality
        Ast.Expression result = parser.parseExpression();
        assertEquals(expected, result, "The parsed AST does not match the expected structure for 'expr1 || expr2 || expr3'.");
    }











    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            assertEquals(expected, function.apply(parser));
        } else {
            assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}
