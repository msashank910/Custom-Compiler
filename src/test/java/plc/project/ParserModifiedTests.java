package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import plc.project.Ast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 *
 * Tests the TYPED Parser grammar.
 */
final class ParserModifiedTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, List<Token> tokens, Ast.Source expected) {
        test(tokens, expected, Parser::parseSource);
    }


    @Test
    void testFunctionAndGlobalWithSameName() {
        // Input tokens for the test
        List<Token> input = Arrays.asList(
                // FUN name() DO stmt; END
                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                new Token(Token.Type.IDENTIFIER, "name", 4),
                new Token(Token.Type.OPERATOR, "(", 8),
                new Token(Token.Type.OPERATOR, ")", 9),
                new Token(Token.Type.IDENTIFIER, "DO", 11),
                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                new Token(Token.Type.OPERATOR, ";", 18),
                new Token(Token.Type.IDENTIFIER, "END", 20),
                // VAR name: Type = expr;
                new Token(Token.Type.IDENTIFIER, "VAR", 24),
                new Token(Token.Type.IDENTIFIER, "name", 28),
                new Token(Token.Type.OPERATOR, ":", 33),
                new Token(Token.Type.IDENTIFIER, "Type", 35),
                new Token(Token.Type.OPERATOR, "=", 40),
                new Token(Token.Type.IDENTIFIER, "expr", 42),
                new Token(Token.Type.OPERATOR, ";", 46)
        );

        Parser parser = new Parser(input);
        try {
            Ast result = parser.parseSource();
            System.out.println("Parsing completed successfully: " + result);
        } catch (ParseException e) {
            System.out.println("ParseException caught: " + e.getMessage());
            throw e;  // rethrow if you still want the test to fail due to the exception
        }
    }



    @Test
    void testInvalidClosingParenthesis() {
        // Define tokens for the input "(expr]"
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.OPERATOR, "(", 0),
                new Token(Token.Type.IDENTIFIER, "expr", 1),
                new Token(Token.Type.OPERATOR, "]", 5)  // Incorrect closing bracket at index 5
        );

        // Expect the parser to throw a ParseException pointing to the index of the incorrect token ']'
        Exception exception = Assertions.assertThrows(ParseException.class, () -> {
            Parser parser = new Parser(tokens);
            parser.parseExpression();  // Assuming 'parseExpression' is the method for handling expressions
        });

        // Check if the exception message and index are as expected
        Assertions.assertTrue(exception.getMessage().contains("Expected ')'"));
        Assertions.assertEquals(5, ((ParseException) exception).getIndex());
    }

    @Test
    void testMissingClosingParenthesis() {
        // Define tokens that simulate a missing closing parenthesis
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.OPERATOR, "(", 0),
                new Token(Token.Type.IDENTIFIER, "expr", 1)
                // Notice the absence of a closing parenthesis token
        );

        // Expect the parser to throw a ParseException because of the missing ')'
        Exception exception = Assertions.assertThrows(ParseException.class, () -> {
            Parser parser = new Parser(tokens);
            parser.parseExpression();  // Assuming 'parseExpression' method is used for parsing expressions including groupings
        });

        // Check if the exception message and index are as expected
        ParseException parseException = (ParseException) exception;
        Assertions.assertTrue(parseException.getMessage().contains("Expected ')'"));
        // Compute the index based on the last token's index and its length
        int expectedIndex = tokens.get(tokens.size() - 1).getIndex() + tokens.get(tokens.size() - 1).getLiteral().length();
        System.out.println("expected Index: " + expectedIndex);
        Assertions.assertEquals(expectedIndex, parseException.getIndex());
    }
    @Test
    void testInvalidExpressionWithQuestionMark() {
        // Create a list of tokens with a single invalid token '?'
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.OPERATOR, "?", 0)
        );

        // Expected to throw ParseException because '?' is not recognized as a valid expression
        Assertions.assertThrows(ParseException.class, () -> {
            Parser parser = new Parser(tokens);
            parser.parseExpression();  // Assuming 'parseExpression' handles parsing of individual expressions
        });
    }

    @Test
    void testMissingOperandInBinaryExpression() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                new Token(Token.Type.OPERATOR, "+", 5)  // Note: Missing right-hand operand
        );

        // Expected to throw ParseException because of the missing operand
        Assertions.assertThrows(ParseException.class, () -> {
            Parser parser = new Parser(tokens);
            parser.parseExpression();  // Adjust if the parser method name for parsing expressions is different
        });
    }


    @Test
    void testTrailingCommaInFunctionCall() {
        List<Token> tokens = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "name", 0),
                new Token(Token.Type.OPERATOR, "(", 4),
                new Token(Token.Type.IDENTIFIER, "expr", 5),
                new Token(Token.Type.OPERATOR, ",", 9),
                new Token(Token.Type.OPERATOR, ")", 10)
        );

        // Expected to throw ParseException because of the trailing comma
        Assertions.assertThrows(ParseException.class, () -> {
            Parser parser = new Parser(tokens);
            parser.parseExpression();  // Assuming parseExpression is the method that handles expressions including function calls
        });
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Zero Statements",
                        Arrays.asList(),
                        new Ast.Source(Arrays.asList(), Arrays.asList())
                ),
                Arguments.of("Global - Immutable",
                        Arrays.asList(
                                //LET name: Type = expr;
                                new Token(Token.Type.IDENTIFIER, "VAL", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ":", 9),
                                new Token(Token.Type.IDENTIFIER, "Type", 11),
                                new Token(Token.Type.OPERATOR, "=", 15),
                                new Token(Token.Type.IDENTIFIER, "expr", 17),
                                new Token(Token.Type.OPERATOR, ";", 21)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Global("name", "Type", false, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Function",
                        Arrays.asList(
                                //FUN name(): Type DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 8),
                                new Token(Token.Type.OPERATOR, ")", 9),
                                new Token(Token.Type.OPERATOR, ":", 10),
                                new Token(Token.Type.IDENTIFIER, "Type", 12),
                                new Token(Token.Type.IDENTIFIER, "DO", 17),
                                new Token(Token.Type.IDENTIFIER, "stmt", 20),
                                new Token(Token.Type.OPERATOR, ";", 24),
                                new Token(Token.Type.IDENTIFIER, "END", 26)
                        ),
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(new Ast.Function("name", Arrays.asList(), Arrays.asList(), Optional.of("Type"), Arrays.asList(
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))
                                )))
                        )
                )
        );
    }

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
    void testDeclarationStatement(String test, List<Token> tokens, Ast.Statement.Declaration expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Definition",
                        Arrays.asList(
                                //LET name: Type;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ":", 8),
                                new Token(Token.Type.IDENTIFIER, "Type", 10),
                                new Token(Token.Type.OPERATOR, ";", 14)
                        ),
                        new Ast.Statement.Declaration("name", Optional.of("Type"), Optional.empty())
                ),
                Arguments.of("Initialization",
                        Arrays.asList(
                                //LET name = expr;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.OPERATOR, ";", 15)
                        ),
                        new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
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
    void testIfStatement(String test, List<Token> tokens, Ast.Statement.If expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        Arrays.asList(
                                //IF expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt", 11),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "END", 17)
                        ),
                        new Ast.Statement.If(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Else",
                        Arrays.asList(
                                //IF expr DO stmt1; ELSE stmt2; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                                new Token(Token.Type.OPERATOR, ";", 16),
                                new Token(Token.Type.IDENTIFIER, "ELSE", 18),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 23),
                                new Token(Token.Type.OPERATOR, ";", 28),
                                new Token(Token.Type.IDENTIFIER, "END", 30)
                        ),
                        new Ast.Statement.If(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1"))),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")))
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWhileStatement(String test, List<Token> tokens, Ast.Statement.While expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of("While",
                        Arrays.asList(
                                //WHILE expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 20)
                        ),
                        new Ast.Statement.While(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testReturnStatement(String test, List<Token> tokens, Ast.Statement.Return expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testReturnStatement() {
        return Stream.of(
                Arguments.of("Return Statement",
                        Arrays.asList(
                                //RETURN expr;
                                new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.OPERATOR, ";", 11)
                        ),
                        new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "expr"))
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
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
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

    @Test
    void testExample1() {
        List<Token> input = Arrays.asList(
                /* VAR first: Integer = 1;
                 * FUN main(): Integer DO
                 *     WHILE first != 10 DO
                 *         print(first);
                 *         first = first + 1;
                 *     END
                 * END
                 */
                //VAR first: Integer = 1;
                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                new Token(Token.Type.IDENTIFIER, "first", 4),
                new Token(Token.Type.OPERATOR, ":", 10),
                new Token(Token.Type.IDENTIFIER, "Integer", 11),
                new Token(Token.Type.OPERATOR, "=", 19),
                new Token(Token.Type.INTEGER, "1", 21),
                new Token(Token.Type.OPERATOR, ";", 22),
                //FUN main(): Integer DO
                new Token(Token.Type.IDENTIFIER, "FUN", 24),
                new Token(Token.Type.IDENTIFIER, "main", 28),
                new Token(Token.Type.OPERATOR, "(", 32),
                new Token(Token.Type.OPERATOR, ")", 33),
                new Token(Token.Type.OPERATOR, ":", 34),
                new Token(Token.Type.IDENTIFIER, "Integer", 36),
                new Token(Token.Type.IDENTIFIER, "DO", 44),
                //    WHILE first != 10 DO
                new Token(Token.Type.IDENTIFIER, "WHILE", 51),
                new Token(Token.Type.IDENTIFIER, "first", 57),
                new Token(Token.Type.OPERATOR, "!=", 63),
                new Token(Token.Type.INTEGER, "10", 66),
                new Token(Token.Type.IDENTIFIER, "DO", 69),
                //        print(first);
                new Token(Token.Type.IDENTIFIER, "print", 80),
                new Token(Token.Type.OPERATOR, "(", 85),
                new Token(Token.Type.IDENTIFIER, "first", 86),
                new Token(Token.Type.OPERATOR, ")", 91),
                new Token(Token.Type.OPERATOR, ";", 92),
                //        first = first + 1;
                new Token(Token.Type.IDENTIFIER, "first", 102),
                new Token(Token.Type.OPERATOR, "=", 108),
                new Token(Token.Type.IDENTIFIER, "first", 110),
                new Token(Token.Type.OPERATOR, "+", 116),
                new Token(Token.Type.INTEGER, "1", 118),
                new Token(Token.Type.OPERATOR, ";", 119),
                //    END
                new Token(Token.Type.IDENTIFIER, "END", 125),
                //END
                new Token(Token.Type.IDENTIFIER, "END", 129)
        );
        Ast.Source expected = new Ast.Source(
                Arrays.asList(new Ast.Global("first", "Integer", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))),
                Arrays.asList(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                        new Ast.Statement.While(
                                new Ast.Expression.Binary("!=",
                                        new Ast.Expression.Access(Optional.empty(), "first"),
                                        new Ast.Expression.Literal(BigInteger.TEN)
                                ),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function("print", Arrays.asList(
                                                        new Ast.Expression.Access(Optional.empty(), "first"))
                                                )
                                        ),
                                        new Ast.Statement.Assignment(
                                                new Ast.Expression.Access(Optional.empty(), "first"),
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Access(Optional.empty(), "first"),
                                                        new Ast.Expression.Literal(BigInteger.ONE)
                                                )
                                        )
                                )
                        )
                ))
        ));
        test(input, expected, Parser::parseSource);
    }


    @Test
    void testFunctionStmt() {
        List<Token> input = Arrays.asList(
                // FUN name() DO
                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                new Token(Token.Type.IDENTIFIER, "name", 4),
                new Token(Token.Type.OPERATOR, "(", 8),
                new Token(Token.Type.OPERATOR, ")", 9),
                new Token(Token.Type.IDENTIFIER, "DO", 11),
                // stmt;
                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                new Token(Token.Type.OPERATOR, ";", 18),
                // END
                new Token(Token.Type.IDENTIFIER, "END", 20)
        );

        Ast.Source expected = new Ast.Source(
                Arrays.asList(), // Empty globals list
                Arrays.asList(
                        new Ast.Function(
                                "name", // Function name
                                Arrays.asList(), // Empty parameters list
                                Arrays.asList(), // Empty parameter types list
                                Optional.empty(), // Assuming no explicit return type specified
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(
                                                        "stmt", // Function call name
                                                        Arrays.asList() // No arguments
                                                )
                                        )
                                )
                        )
                )
        );


        // Assuming there's a test utility method similar to 'test(input, expected, Parser::parseSource)'
        test(input, expected, Parser::parseSource);
    }

    @Test
    void testGlobalFunction() {
        List<Token> input = Arrays.asList(
                // VAL name: Type = expr;
                new Token(Token.Type.IDENTIFIER, "VAL", 0),
                new Token(Token.Type.IDENTIFIER, "name", 4),
                new Token(Token.Type.OPERATOR, ":", 9),
                new Token(Token.Type.IDENTIFIER, "Type", 11),
                new Token(Token.Type.OPERATOR, "=", 16),
                new Token(Token.Type.IDENTIFIER, "expr", 18),
                new Token(Token.Type.OPERATOR, ";", 22),
                // FUN name() DO stmt; END
                new Token(Token.Type.IDENTIFIER, "FUN", 24),
                new Token(Token.Type.IDENTIFIER, "name", 28),
                new Token(Token.Type.OPERATOR, "(", 32),
                new Token(Token.Type.OPERATOR, ")", 33),
                new Token(Token.Type.IDENTIFIER, "DO", 35),
                new Token(Token.Type.IDENTIFIER, "stmt", 38),
                new Token(Token.Type.OPERATOR, ";", 42),
                new Token(Token.Type.IDENTIFIER, "END", 44)
        );

        Ast.Source expected = new Ast.Source(
                Arrays.asList(
                        new Ast.Global(
                                "name",
                                "Type",
                                false, // false because it's declared with VAL, indicating immutability
                                Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")) // this is correct
                        )
                ),
                Arrays.asList(
                        new Ast.Function(
                                "name",
                                Arrays.asList(), // No parameters
                                Arrays.asList(), // Assuming no types for parameters for simplicity
                                Optional.empty(), // Assuming no return type specified
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Access(Optional.empty(), "stmt")
                                                // Assuming "stmt" is an identifier access or an expression used as a statement
                                        )
                                )
                        )
                )
        );

        // Assuming there's a method to test like test(input, expected, Parser::parseSource)
        test(input, expected, Parser::parseSource);
    }


    @Test
    void testFunctionGlobal() {
        List<Token> input = Arrays.asList(
                // FUN name() DO stmt; END
                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                new Token(Token.Type.IDENTIFIER, "name", 4),
                new Token(Token.Type.OPERATOR, "(", 8),
                new Token(Token.Type.OPERATOR, ")", 9),
                new Token(Token.Type.IDENTIFIER, "DO", 11),
                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                new Token(Token.Type.OPERATOR, ";", 18),
                new Token(Token.Type.IDENTIFIER, "END", 20),
                // VAR name: Type = expr;
                new Token(Token.Type.IDENTIFIER, "VAR", 24),
                new Token(Token.Type.IDENTIFIER, "name", 28),
                new Token(Token.Type.OPERATOR, ":", 33),
                new Token(Token.Type.IDENTIFIER, "Type", 35),
                new Token(Token.Type.OPERATOR, "=", 40),
                new Token(Token.Type.IDENTIFIER, "expr", 42),
                new Token(Token.Type.OPERATOR, ";", 46)
        );

        Ast.Source expected = new Ast.Source(
                Arrays.asList(
                        new Ast.Global(
                                "name",
                                "Type",
                                true, // true because it's declared with VAR, indicating mutability

                                Optional.of(new Ast.Expression.Literal("expr")) // Assuming expr is a simple literal for demonstration
                        )
                ),
                Arrays.asList(
                        new Ast.Function(
                                "name",
                                Arrays.asList(), // No parameters
                                Arrays.asList(), // Assuming no types for parameters for simplicity
                                Optional.empty(), // Assuming no return type specified
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(
                                                        "stmt", // Assuming 'stmt' is a function call without arguments
                                                        Arrays.asList() // No arguments
                                                )
                                        )
                                )
                        )
                )
        );

        // Assuming there's a method to test like test(input, expected, Parser::parseSource)
        test(input, expected, Parser::parseSource);
    }


    @Test
    void testBaselineFunction() {
        List<Token> input = Arrays.asList(
                // FUN name() DO stmt; END
                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                new Token(Token.Type.IDENTIFIER, "name", 4),
                new Token(Token.Type.OPERATOR, "(", 8),
                new Token(Token.Type.OPERATOR, ")", 9),
                new Token(Token.Type.IDENTIFIER, "DO", 11),
                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                new Token(Token.Type.OPERATOR, ";", 18),
                new Token(Token.Type.IDENTIFIER, "END", 20)
        );

        Ast.Source expected = new Ast.Source(
                Arrays.asList(), // No global variables
                Arrays.asList(
                        new Ast.Function(
                                "name", // Function name
                                Arrays.asList(), // No parameters
                                Arrays.asList(), // No parameter types, assuming parameters list above is correct
                                Optional.empty(), // No specified return type
//                                new Ast.Statement.While(
//                                        new Ast.Expression.Access(Optional.empty(), "expr"),
//                                        Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))
//                                )
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))
//                                Arrays.asList(
//                                        new Ast.Statement.Expression(
//                                                new Ast.Expression.Access(
//                                                        "stmt", // Assuming 'stmt' is a function call without arguments
//                                                        new Ast.Expression.Access(Optional.empty(), "stmt")
//                                                )
//                                        )
//                                )
                        )
                )
        );

        // Assuming there's a method similar to test(input, expected, Parser::parseSource) for conducting the test
        test(input, expected, Parser::parseSource);
    }


    @Test
    void testFunctionWithArgumentAndType() {
        List<Token> input = Arrays.asList(
                // FUN name(arg: Type) DO stmt; END
                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                new Token(Token.Type.IDENTIFIER, "name", 4),
                new Token(Token.Type.OPERATOR, "(", 8),
                new Token(Token.Type.IDENTIFIER, "arg", 9),
                new Token(Token.Type.OPERATOR, ":", 13),
                new Token(Token.Type.IDENTIFIER, "Type", 15),
                new Token(Token.Type.OPERATOR, ")", 19),
                new Token(Token.Type.IDENTIFIER, "DO", 21),
                new Token(Token.Type.IDENTIFIER, "stmt", 24),
                new Token(Token.Type.OPERATOR, ";", 28),
                new Token(Token.Type.IDENTIFIER, "END", 30)
        );

        Ast.Source expected = new Ast.Source(
                Arrays.asList(), // No global variables
                Arrays.asList(
                        new Ast.Function(
                                "name", // Function name
                                Arrays.asList("arg"), // Parameters list with one parameter named "arg"
                                Arrays.asList("Type"), // Parameter types list corresponding to "arg"
                                Optional.empty(), // No specified return type
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(
                                                        "stmt", // Assuming 'stmt' is a function call without arguments
                                                        Arrays.asList() // No arguments for the stmt call
                                                )
                                        )
                                )
                        )
                )
        );

        // Assuming there's a method similar to test(input, expected, Parser::parseSource) for conducting the test
        test(input, expected, Parser::parseSource);
    }


    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}
