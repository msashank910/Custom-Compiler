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

import static org.junit.jupiter.api.Assertions.*;

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

        assertDoesNotThrow(() -> {
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

    @Test
    public void testChainedLessThanComparison() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "<", 5),
                new Token(Token.Type.IDENTIFIER, "expr2", 7),
                new Token(Token.Type.OPERATOR, "<", 12),
                new Token(Token.Type.IDENTIFIER, "expr3", 14)
        );
        Parser parser = new Parser(tokens);

        Ast.Expression expected = new Ast.Expression.Binary("<",
                new Ast.Expression.Binary("<",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                ),
                new Ast.Expression.Access(Optional.empty(), "expr3")
        );

        Ast.Expression result = assertDoesNotThrow(() -> parser.parseExpression(),
                "Expected successful parsing of chained less than comparison.");
        assertEquals(expected, result, "The parsed AST does not match the expected structure for 'expr1 < expr2 < expr3'.");
    }

    @Test
    public void testChainedGreaterThanComparison() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, ">", 5),
                new Token(Token.Type.IDENTIFIER, "expr2", 7),
                new Token(Token.Type.OPERATOR, ">", 12),
                new Token(Token.Type.IDENTIFIER, "expr3", 14)
        );
        Parser parser = new Parser(tokens);

        Ast.Expression expected = new Ast.Expression.Binary(">",
                new Ast.Expression.Binary(">",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                ),
                new Ast.Expression.Access(Optional.empty(), "expr3")
        );

        Ast.Expression result = assertDoesNotThrow(() -> parser.parseExpression(),
                "Expected successful parsing of chained greater than comparison.");
        assertEquals(expected, result, "The parsed AST does not match the expected structure for 'expr1 > expr2 > expr3'.");
    }

    @Test
    public void testChainedNotEqualsComparison() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "!=", 5),
                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                new Token(Token.Type.OPERATOR, "!=", 13),
                new Token(Token.Type.IDENTIFIER, "expr3", 16)
        );
        Parser parser = new Parser(tokens);

        Ast.Expression expected = new Ast.Expression.Binary("!=",
                new Ast.Expression.Binary("!=",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                ),
                new Ast.Expression.Access(Optional.empty(), "expr3")
        );

        Ast.Expression result = assertDoesNotThrow(() -> parser.parseExpression(),
                "Expected successful parsing of chained not equals comparison.");
        assertEquals(expected, result, "The parsed AST does not match the expected structure for 'expr1 != expr2 != expr3'.");
    }

    @Test
    public void testChainedAdditionExpression() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "+", 5),
                new Token(Token.Type.IDENTIFIER, "expr2", 7),
                new Token(Token.Type.OPERATOR, "+", 12),
                new Token(Token.Type.IDENTIFIER, "expr3", 14)
        );
        Parser parser = new Parser(tokens);

        // Construct the expected AST structure for "expr1 + expr2 + expr3"
        Ast.Expression expected = new Ast.Expression.Binary("+",
                new Ast.Expression.Binary("+",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                ),
                new Ast.Expression.Access(Optional.empty(), "expr3")
        );

        // Attempt to parse the expression and compare the result to the expected AST
        Ast.Expression result = assertDoesNotThrow(() -> parser.parseExpression(),
                "Expected successful parsing of chained addition expression.");
        assertEquals(expected, result, "The parsed AST does not match the expected structure for 'expr1 + expr2 + expr3'.");
    }

    @Test
    public void testChainedDivisionExpression() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "/", 5),
                new Token(Token.Type.IDENTIFIER, "expr2", 7),
                new Token(Token.Type.OPERATOR, "/", 12),
                new Token(Token.Type.IDENTIFIER, "expr3", 14)
        );
        Parser parser = new Parser(tokens);

        // Construct the expected AST structure for "expr1 / expr2 / expr3"
        Ast.Expression expected = new Ast.Expression.Binary("/",
                new Ast.Expression.Binary("/",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                ),
                new Ast.Expression.Access(Optional.empty(), "expr3")
        );

        // Attempt to parse the expression and compare the result to the expected AST
        Ast.Expression result = assertDoesNotThrow(() -> parser.parseExpression(),
                "Expected successful parsing of chained division expression.");
        assertEquals(expected, result, "The parsed AST does not match the expected structure for 'expr1 / expr2 / expr3'.");
    }


    @Test
    public void testChainedExponentiationExpression() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "^", 5),
                new Token(Token.Type.IDENTIFIER, "expr2", 7),
                new Token(Token.Type.OPERATOR, "^", 12),
                new Token(Token.Type.IDENTIFIER, "expr3", 14)
        );
        Parser parser = new Parser(tokens);

        // Construct the expected AST structure for "expr1 ^ expr2 ^ expr3"
        Ast.Expression expected = new Ast.Expression.Binary("^",
                new Ast.Expression.Binary("^",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                ),
                new Ast.Expression.Access(Optional.empty(), "expr3")
        );

        // Attempt to parse the expression and compare the result to the expected AST
        Ast.Expression result = assertDoesNotThrow(() -> parser.parseExpression(),
                "Expected successful parsing of chained exponentiation expression.");
        assertEquals(expected, result, "The parsed AST does not match the expected structure for 'expr1 ^ expr2 ^ expr3'.");
    }

    @Test
    public void testAndWithMissingOperand() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "&&", 5)
                // Missing operand after &&
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing operand after &&
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing operand after '&&'.");
    }

    @Test
    public void testOrWithMissingOperand() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "||", 5)
                // Missing operand after ||
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing operand after ||
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing operand after '||'.");
    }

    @Test
    public void testLessThanWithMissingOperand() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "<", 5)
                // Missing operand after <
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing operand after '<'
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing operand after '<'.");
    }

    @Test
    public void testGreaterThanWithMissingOperand() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, ">", 5)
                // Missing operand after >
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing operand after '>'
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing operand after '>'.");
    }

    @Test
    public void testEqualsWithMissingOperand() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "==", 5)
                // Missing operand after ==
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing operand after '=='
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing operand after '=='.");
    }

    @Test
    public void testNotEqualsWithMissingOperand() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "!=", 5)
                // Missing operand after !=
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing operand after '!='
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing operand after '!='.");
    }

    @Test
    public void testAdditionWithMissingOperand() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "+", 5)
                // Missing operand after +
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing operand after '+'
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing operand after '+'.");
    }

    @Test
    public void testSubtractionWithMissingOperand() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "-", 5)
                // Missing operand after -
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing operand after '-'
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing operand after '-'.");
    }

    @Test
    public void testMultiplicationWithMissingOperand() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "*", 5)
                // Missing operand after *
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing operand after '*'
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing operand after '*'.");
    }

    @Test
    public void testDivisionWithMissingOperand() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "/", 5)
                // Missing operand after /
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing operand after '/'
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing operand after '/'.");
    }

    @Test
    public void testExponentiationWithMissingOperand() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "^", 5)
                // Missing operand after ^
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing operand after '^'
        assertThrows(ParseException.class, () -> parser.parseExpression(),
                "Expected ParseException for missing operand after '^'.");
    }

    @Test
    public void testFunctionCallWithComplexArgument() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "name", 0),
                new Token(Token.Type.OPERATOR, "(", 4),
                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                new Token(Token.Type.OPERATOR, "/", 10),
                new Token(Token.Type.IDENTIFIER, "expr2", 11),
                new Token(Token.Type.OPERATOR, ")", 16)
        );
        Parser parser = new Parser(tokens);

        Assertions.assertDoesNotThrow(() -> {
            Ast.Expression result = parser.parseExpression();
            Assertions.assertTrue(result instanceof Ast.Expression.Function);

            Ast.Expression.Function functionCall = (Ast.Expression.Function) result;
            Assertions.assertEquals("name", functionCall.getName());
            Assertions.assertEquals(1, functionCall.getArguments().size());

            Ast.Expression argument = functionCall.getArguments().get(0);
            Assertions.assertTrue(argument instanceof Ast.Expression.Binary);

            Ast.Expression.Binary binaryExpression = (Ast.Expression.Binary) argument;
            Assertions.assertEquals("/", binaryExpression.getOperator());
            Assertions.assertTrue(binaryExpression.getLeft() instanceof Ast.Expression.Access);
            Assertions.assertTrue(binaryExpression.getRight() instanceof Ast.Expression.Access);

            Ast.Expression.Access leftAccess = (Ast.Expression.Access) binaryExpression.getLeft();
            Ast.Expression.Access rightAccess = (Ast.Expression.Access) binaryExpression.getRight();
            Assertions.assertEquals("expr1", leftAccess.getName());
            Assertions.assertEquals("expr2", rightAccess.getName());
        }, "Expected to parse function call with complex argument without throwing ParseException.");
    }

    @Test
    public void testMissingClosingParenthesis() {
        // Prepare a list of tokens that represent an expression with a missing closing parenthesis
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.OPERATOR, "(", 0),
                new Token(Token.Type.IDENTIFIER, "expr", 1)
                // Notice the missing closing parenthesis token here
        );
        Parser parser = new Parser(tokens);

        // Expecting a ParseException to be thrown due to the missing closing parenthesis
        ParseException thrown = assertThrows(ParseException.class, () -> {
            parser.parseExpression(); // Attempt to parse the expression
        }, "A ParseException was expected due to the missing closing parenthesis.");

        // Assert that the ParseException contains the correct index where the closing parenthesis was expected
        // Assuming that the index in the ParseException message refers to the position in the token stream
        // Adjust this according to how your ParseException and parser report error indices
        assertTrue(thrown.getMessage().contains("Expected ')'"), "The error message should indicate the missing closing parenthesis.");
        assertEquals(5, thrown.getIndex(), "The error index should point to where the closing parenthesis was expected.");
    }


    @Test
    public void testMissingDoAfterIfCondition() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "IF", 0),
                new Token(Token.Type.IDENTIFIER, "expr", 3),
                new Token(Token.Type.IDENTIFIER, "THEN", 8)
        );
        Parser parser = new Parser(tokens);

        try {
            parser.parseStatement();
            fail("Expected a ParseException to be thrown due to missing 'DO'");
        } catch (ParseException e) {
            int expectedIndex = 8;
            assertEquals(expectedIndex, e.getIndex(), "The ParseException did not contain the expected index.");
        }
    }


    @Test
    public void testInvalidClosingParenthesis() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.OPERATOR, "(", 0),
                // Assuming 'parseExpression' can correctly handle 'expr'
                new Token(Token.Type.IDENTIFIER, "expr", 1),
                // Incorrectly using a closing square bracket instead of a parenthesis
                new Token(Token.Type.OPERATOR, "]", 5) // Assuming ']' is at index 5
        );
        Parser parser = new Parser(tokens); // Wrapping tokens in a TokenStream instance for the Parser

        try {
            parser.parseExpression(); // Attempt to parse, which should throw the ParseException
            fail("Expected a ParseException to be thrown due to invalid closing parenthesis");
        } catch (ParseException e) {
            // Check that the ParseException accurately reports the index of the error
            int expectedIndex = 5; // Expecting the error at the position of the incorrect ']' token
            assertEquals(expectedIndex, e.getIndex(), "The ParseException did not contain the expected index for the invalid closing parenthesis.");
        }
    }


    @Test
    public void testInvalidExpressionWithQuestionMark() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.OPERATOR, "?", 0)
        );
        Parser parser = new Parser(tokens);

        try {
            parser.parseExpression();
            fail("Expected a ParseException to be thrown due to an invalid expression containing '?'");
        } catch (ParseException e) {
            int expectedIndex = 0;
            assertEquals(expectedIndex, e.getIndex(), "The ParseException did not contain the expected index.");
        }
    }



    @Test
    public void testBasicSwitch() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                // Token for the switch expression
                new Token(Token.Type.IDENTIFIER, "expr", 7),
                new Token(Token.Type.IDENTIFIER, "DEFAULT", 12),
                // Token for the default statement
                new Token(Token.Type.IDENTIFIER, "stmt", 20),
                new Token(Token.Type.IDENTIFIER, "END", 25)
        );
        Parser parser = new Parser(tokens);

        try {
            Ast.Statement.Switch result = parser.parseSwitchStatement();
            assertNotNull(result, "The parser did not correctly parse the basic switch statement.");
            // Further assertions can be made here regarding the structure of the parsed switch statement
        } catch (ParseException e) {
            fail("Expected the basic switch statement to be parsed without errors.");
        }
    }


    @Test
    public void testCaseSwitch() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                // Token for the first switch expression
                new Token(Token.Type.IDENTIFIER, "expr1", 7),
                new Token(Token.Type.IDENTIFIER, "CASE", 13),
                // Token for the case expression
                new Token(Token.Type.IDENTIFIER, "expr2", 18),
                new Token(Token.Type.OPERATOR, ":", 24),
                // Token for the first case statement
                new Token(Token.Type.IDENTIFIER, "stmt1", 26),
                new Token(Token.Type.IDENTIFIER, "DEFAULT", 32),
                // Token for the default statement
                new Token(Token.Type.IDENTIFIER, "stmt2", 40),
                new Token(Token.Type.IDENTIFIER, "END", 46)
        );
        Parser parser = new Parser(tokens);

        try {
            Ast.Statement result = parser.parseStatement();
            assertNotNull(result, "The parser did not correctly parse the case switch statement.");
            // Further assertions can be made here regarding the structure of the parsed switch statement
        } catch (ParseException e) {
            fail(e.getMessage());
        }
    }


    @Test
    public void testEmptySwitch() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                // Token for the switch expression
                new Token(Token.Type.IDENTIFIER, "expr", 7),
                new Token(Token.Type.IDENTIFIER, "DEFAULT", 12),
                new Token(Token.Type.IDENTIFIER, "END", 20)
        );
        Parser parser = new Parser(tokens);

        try {
            Ast.Statement.Switch result = parser.parseSwitchStatement();
            assertNotNull(result, "The parser did not correctly parse the empty switch statement.");
            // Further assertions can be made here regarding the structure of the parsed switch statement
        } catch (ParseException e) {
            fail("Expected the empty switch statement to be parsed without errors.");
        }
    }

    @Test
    public void testIfStatement() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "IF", 0),
                new Token(Token.Type.IDENTIFIER, "expr", 3),
                new Token(Token.Type.IDENTIFIER, "DO", 8),
                new Token(Token.Type.IDENTIFIER, "stmt", 11),
                new Token(Token.Type.IDENTIFIER, "END", 16)
        );
        Parser parser = new Parser(tokens);

        try {
            Ast.Statement result = parser.parseStatement();
            assertNotNull(result, "The parser did not correctly parse the IF statement.");
            assertTrue(result instanceof Ast.Statement.If, "Parsed statement is not an instance of Ast.Statement.If");
        } catch (ParseException e) {
            fail("Expected the IF statement to be parsed without errors.");
        }
    }


    @Test
    public void testIfElseStatement() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "IF", 0),
                new Token(Token.Type.IDENTIFIER, "expr", 3),
                new Token(Token.Type.IDENTIFIER, "DO", 8),
                new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                new Token(Token.Type.IDENTIFIER, "ELSE", 17),
                new Token(Token.Type.IDENTIFIER, "stmt2", 22),
                new Token(Token.Type.IDENTIFIER, "END", 28)
        );
        Parser parser = new Parser(tokens);

        try {
            Ast.Statement result = parser.parseStatement();
            assertNotNull(result, "The parser did not correctly parse the IF-ELSE statement.");
            // Further checks can be added here to verify the structure of the parsed statement
        } catch (ParseException e) {
            fail("Expected the IF-ELSE statement to be parsed without errors.");
        }
    }


    @Test
    public void testMissingDoDetected() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "IF", 0),
                new Token(Token.Type.IDENTIFIER, "expr", 3),
                // Missing DO
                new Token(Token.Type.IDENTIFIER, "stmt", 8),
                new Token(Token.Type.IDENTIFIER, "END", 13)
        );
        Parser parser = new Parser(tokens);

        try {
            Ast.Statement result = parser.parseStatement();
            fail("Expected a ParseException due to missing 'DO'.");
        } catch (ParseException e) {
            assertEquals("Expected 'DO' after 'IF' condition", e.getMessage(), "The ParseException did not contain the expected message.");
        }
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
