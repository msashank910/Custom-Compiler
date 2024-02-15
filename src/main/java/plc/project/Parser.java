package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
//    public Ast.Source parseSource() throws ParseException {
//        throw new UnsupportedOperationException(); //TODO
//    }

    //comment
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();

        // Parse global declarations as long as we have LIST, VAL, or VAR tokens
        while (peek("LIST") || peek("VAL") || peek("VAR")) {
            globals.add(parseGlobal());
        }

        // Parse function declarations as long as we have FUN tokens
        while (peek("FUN")) {
            functions.add(parseFunction());
        }

        return new Ast.Source(globals, functions);
    }


    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
//    public Ast.Global parseGlobal() throws ParseException {
//        throw new UnsupportedOperationException(); //TODO
//    }
    public Ast.Global parseGlobal() throws ParseException {
        if (match("LIST")) {
            return parseList();
        } else if (match("VAR")) {
            return parseMutable();
        } else if (match("VAL")) {
            return parseImmutable();
        } else {
            throw new ParseException("Expected global declaration", tokens.get(0).getIndex());
        }
    }


    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
//    public Ast.Global parseList() throws ParseException {
//        throw new UnsupportedOperationException(); //TODO
//    }

    public Ast.Global parseList() throws ParseException {
        if (!match("LIST")) {
            throw new ParseException("Expected 'LIST'", tokens.get(0).getIndex());
        }
        Token nameToken = tokens.get(0); // Get the current token which should be the identifier
        tokens.advance(); // Now advance the token stream
        String name = nameToken.getLiteral(); // Get the literal value of the token
        if (!match("=")) {
            throw new ParseException("Expected '='", tokens.get(0).getIndex());
        }
        if (!match("[")) {
            throw new ParseException("Expected '['", tokens.get(0).getIndex());
        }
        List<Ast.Expression> values = new ArrayList<>();
        if (!peek("]")) { // Check if the list is not empty
            do {
                values.add(parseExpression()); // Parse the first expression
            } while (match(",")); // Continue parsing expressions if there's a comma
        }
        if (!match("]")) {
            throw new ParseException("Expected ']'", tokens.get(0).getIndex());
        }
        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.get(0).getIndex());
        }

        Ast.Expression listExpression = new Ast.Expression.PlcList(values);
        return new Ast.Global(name, true, Optional.of(listExpression)); // Assuming list is mutable for this examp
    }


    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
//    public Ast.Global parseMutable() throws ParseException {
//        throw new UnsupportedOperationException(); //TODO
//    }

    public Ast.Global parseMutable() throws ParseException {
        if (!match("VAR")) {
            throw new ParseException("Expected 'VAR'", tokens.get(0).getIndex());
        }
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier", tokens.get(0).getIndex());
        }
        String name = tokens.get(-1).getLiteral(); // Assuming you have a way to get the last matched token

        Optional<Ast.Expression> value = Optional.empty(); // Default to no initializer
        if (match("=")) {
            value = Optional.of(parseExpression()); // Parse the initializer expression
        }

        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.get(0).getIndex());
        }

        return new Ast.Global(name, true, value); // 'true' for mutable
    }


    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        if (!match("VAL")) {
            throw new ParseException("Expected 'VAL'", tokens.get(0).getIndex());
        }
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier", tokens.get(0).getIndex());
        }
        String name = tokens.get(-1).getLiteral(); // Assuming this retrieves the literal of the previously matched IDENTIFIER token

        if (!match("=")) {
            throw new ParseException("Expected '='", tokens.get(0).getIndex());
        }
        Ast.Expression value = parseExpression(); // Parse the initializer expression

        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.get(0).getIndex());
        }

        return new Ast.Global(name, false, Optional.of(value)); // 'false' for immutable
    }


    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        if (!match("FUN")) {
            throw new ParseException("Expected 'FUN'", tokens.get(0).getIndex());
        }
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected function name (identifier)", tokens.get(0).getIndex());
        }
        String name = tokens.get(-1).getLiteral(); // Assuming this gets the literal of the function name

        if (!match("(")) {
            throw new ParseException("Expected '(' after function name", tokens.get(0).getIndex());
        }

        List<String> parameters = new ArrayList<>();
        if (!peek(")")) { // If there are parameters
            do {
                if (!match(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected parameter (identifier)", tokens.get(0).getIndex());
                }
                parameters.add(tokens.get(-1).getLiteral());
            } while (match(","));
        }

        if (!match(")")) {
            throw new ParseException("Expected ')' after parameters", tokens.get(0).getIndex());
        }

        if (!match("DO")) {
            throw new ParseException("Expected 'DO' after function declaration", tokens.get(0).getIndex());
        }

        List<Ast.Statement> statements = parseBlock(); // You will need to implement this method

        if (!match("END")) {
            throw new ParseException("Expected 'END' to close function definition", tokens.get(0).getIndex());
        }

        return new Ast.Function(name, parameters, statements);
    }


    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
//    public List<Ast.Statement> parseBlock() throws ParseException {
//        throw new UnsupportedOperationException(); //TODO
//    }

    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();

        // Assuming the block ends with "END" and each statement ends with ";"
        // This loop will keep parsing statements until it hits the "END" keyword
        // This is simplistic and might need adjustment based on your grammar's specifics
        while (!peek("END")) {
            statements.add(parseStatement());
            // Optionally, ensure each statement is followed by a semicolon if your grammar requires it
            // if (!match(";")) {
            //     throw new ParseException("Expected ';'", tokens.get(0).getIndex());
            // }
        }

        return statements;
    }


    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
//    public Ast.Statement parseStatement() throws ParseException {
//        throw new UnsupportedOperationException(); //TODO
//    }

    public Ast.Statement parseStatement() throws ParseException {
        if (peek("LET")) {
            return parseDeclarationStatement();
        } else if (peek("IF")) {
            return parseIfStatement();
        } else if (peek("WHILE")) {
            return parseWhileStatement();
        } else if (peek("RETURN")) {
            return parseReturnStatement();
        } else if (peek("SWITCH")) { // Add this condition to handle switch statements
            return parseSwitchStatement();
        } else {
            // Handle assignments or expression statements.
            if (peek(Token.Type.IDENTIFIER) && tokens.has(1) && "=".equals(tokens.get(1).getLiteral())) {
                return parseAssignmentStatement();
            } else {
                // It's considered an expression statement.
                return parseExpressionStatement();
            }
        }
    }


    private Ast.Statement parseAssignmentStatement() throws ParseException {
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier in assignment statement.", tokens.get(0).getIndex());
        }
        // After matching, tokens.get(-1) should give you the last matched token, which is the identifier.
        String name = tokens.get(-1).getLiteral(); // Assuming this gets the literal of the previously matched IDENTIFIER token

        if (!match("=")) {
            throw new ParseException("Expected '=' in assignment statement.", tokens.get(0).getIndex());
        }

        Ast.Expression value = parseExpression(); // Parse the right-hand side expression

        if (!match(";")) {
            throw new ParseException("Expected ';' at the end of the assignment statement.", tokens.get(0).getIndex());
        }

        return new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), name), value);
    }
    private Ast.Statement parseExpressionStatement() throws ParseException {
        Ast.Expression expression = parseExpression(); // Parse the expression

        if (!match(";")) {
            throw new ParseException("Expected ';' at the end of the expression statement.", tokens.get(0).getIndex());
        }

        return new Ast.Statement.Expression(expression);
    }



    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if (!match("LET")) {
            throw new ParseException("Expected 'LET'", tokens.get(0).getIndex());
        }
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier after 'LET'", tokens.get(0).getIndex());
        }
        String name = tokens.get(-1).getLiteral(); // Retrieve the identifier's name

        Optional<Ast.Expression> value = Optional.empty(); // Default to no initializer
        if (match("=")) {
            value = Optional.of(parseExpression()); // Parse the initializer expression if exists
        }

        if (!match(";")) {
            throw new ParseException("Expected ';' at the end of the declaration statement.", tokens.get(0).getIndex());
        }

        return new Ast.Statement.Declaration(name, value);
    }


    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
//    public Ast.Statement.If parseIfStatement() throws ParseException {
//        throw new UnsupportedOperationException(); //TODO
//    }

    public Ast.Statement.If parseIfStatement() throws ParseException {
        if (!match("IF")) {
            throw new ParseException("Expected 'IF'", tokens.get(0).getIndex());
        }

        Ast.Expression condition = parseExpression(); // Parse the condition expression

        if (!match("DO")) {
            throw new ParseException("Expected 'DO' after 'IF' condition", tokens.get(0).getIndex());
        }

        List<Ast.Statement> thenStatements = parseBlock(); // Parse the block of statements to execute if the condition is true

        List<Ast.Statement> elseStatements = new ArrayList<>(); // Prepare to hold any else statements
        if (match("ELSE")) {
            if (!match("DO")) {
                throw new ParseException("Expected 'DO' after 'ELSE'", tokens.get(0).getIndex());
            }
            elseStatements = parseBlock(); // Parse the block of statements to execute if the condition is false
        }

        if (!match("END")) {
            throw new ParseException("Expected 'END' to close the 'IF' statement", tokens.get(0).getIndex());
        }

        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }


    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
//    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
//        throw new UnsupportedOperationException(); //TODO
//    }
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        if (!match("SWITCH")) {
            throw new ParseException("Expected 'SWITCH'", tokens.get(0).getIndex());
        }

        Ast.Expression condition = parseExpression(); // Parse the switch expression

        List<Ast.Statement.Case> cases = new ArrayList<>();
        boolean foundDefault = false;

        // Loop to process 'CASE' and 'DEFAULT' statements
        while (peek("CASE") || (peek("DEFAULT") && !foundDefault)) {
            Ast.Statement.Case caseStatement = parseCaseStatement();
            if (caseStatement.getValue().isEmpty()) { // Check if it's a DEFAULT case
                if (foundDefault) {
                    throw new ParseException("Multiple 'DEFAULT' cases found", tokens.get(0).getIndex());
                }
                foundDefault = true;
            }
            cases.add(caseStatement);
        }

        if (!foundDefault) {
            throw new ParseException("Missing 'DEFAULT' case in switch statement", tokens.get(-1).getIndex());
        }

        if (!match("END")) {
            throw new ParseException("Expected 'END' to close the 'SWITCH' statement", tokens.get(0).getIndex());
        }

        return new Ast.Statement.Switch(condition, cases);
    }



    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        Optional<Ast.Expression> caseExpression = Optional.empty(); // Will remain empty for DEFAULT case

        // Check if it's a CASE statement
        if (match("CASE")) {
            caseExpression = Optional.of(parseExpression()); // Parse the CASE expression
            if (!match(":")) {
                throw new ParseException("Expected ':' after 'CASE' expression", tokens.get(0).getIndex());
            }
        }
        // Or check if it's a DEFAULT statement
        else if (match("DEFAULT")) {
            if (!match(":")) {
                throw new ParseException("Expected ':' after 'DEFAULT'", tokens.get(0).getIndex());
            }
        } else {
            // If neither CASE nor DEFAULT, throw an exception
            throw new ParseException("Expected 'CASE' or 'DEFAULT' in switch statement", tokens.get(0).getIndex());
        }

        List<Ast.Statement> statements = parseBlock(); // Parse the block of statements for this case/default

        return new Ast.Statement.Case(caseExpression, statements);
    }


    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
//    public Ast.Statement.While parseWhileStatement() throws ParseException {
//        throw new UnsupportedOperationException(); //TODO
//    }

    public Ast.Statement.While parseWhileStatement() throws ParseException {
        if (!match("WHILE")) {
            throw new ParseException("Expected 'WHILE'", tokens.get(0).getIndex());
        }

        Ast.Expression condition = parseExpression(); // Parse the condition expression

        if (!match("DO")) {
            throw new ParseException("Expected 'DO' after 'WHILE' condition", tokens.get(0).getIndex());
        }

        List<Ast.Statement> statements = parseBlock(); // Parse the block of statements to execute in the loop

        if (!match("END")) {
            throw new ParseException("Expected 'END' to close the 'WHILE' statement", tokens.get(0).getIndex());
        }

        return new Ast.Statement.While(condition, statements);
    }


    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
//    public Ast.Statement.Return parseReturnStatement() throws ParseException {
//        throw new UnsupportedOperationException(); //TODO
//    }

    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        if (!match("RETURN")) {
            throw new ParseException("Expected 'RETURN'", tokens.get(0).getIndex());
        }

        Ast.Expression value = parseExpression(); // Parse the expression to be returned

        if (!match(";")) {
            throw new ParseException("Expected ';' at the end of the return statement.", tokens.get(0).getIndex());
        }

        return new Ast.Statement.Return(value);
    }




    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        // Delegate to the next precedence level in your language's expression hierarchy.
        // Assuming logical expressions are at the top, but you might change this
        // to start with the actual top-level precedence in your grammar.
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */


    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression result = parseEqualityExpression();
        while (match("&&")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseEqualityExpression();
            result = new Ast.Expression.Binary(operator, result, right);
        }
        return result;
    }

    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression result = parseAdditiveExpression();
        while (match("==")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseAdditiveExpression();
            result = new Ast.Expression.Binary(operator, result, right);
        }
        return result;
    }





    /**
     * Parses the {@code comparison-expression} rule.
     */

    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression expression = parseAdditiveExpression(); // Start with an additive expression

        // Loop as long as there are comparison operators, indicating continuation of the comparison expression
        while (match("<", ">", "==", "!=")) {
            String operator = tokens.get(-1).getLiteral(); // Get the operator
            Ast.Expression right = parseAdditiveExpression(); // Parse the right-hand side additive expression
            expression = new Ast.Expression.Binary(operator, expression, right); // Combine into a binary expression
        }

        return expression; // Return the built expression
    }


    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression result = parseMultiplicativeExpression();
        while (match("+")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseMultiplicativeExpression();
            result = new Ast.Expression.Binary(operator, result, right);
        }
        return result;
    }



    /**
     * Parses the {@code multiplicative-expression} rule.
     */

    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression result = parsePrimaryExpression();
        while (match("*")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parsePrimaryExpression();
            result = new Ast.Expression.Binary(operator, result, right);
        }
        return result;
    }


    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match(Token.Type.INTEGER)) {
            // Parsing an integer literal
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            // Parsing a decimal literal
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.STRING)) {
            // Parsing a string literal with escape characters
            String stringLiteral = tokens.get(-1).getLiteral();
            String processedStringLiteral = processEscapeCharacters(stringLiteral.substring(1, stringLiteral.length() - 1)); // Removing quotes and processing escape characters
            return new Ast.Expression.Literal(processedStringLiteral);
        } else if (match(Token.Type.CHARACTER)) {
            // Parsing a character literal with escape characters
            String characterLiteral = tokens.get(-1).getLiteral();
            if (characterLiteral.length() >= 3) { // Expecting format like 'c' or '\n'
                String processedCharacterLiteral = processEscapeCharacters(characterLiteral.substring(1, characterLiteral.length() - 1)); // Removing quotes and processing escape characters
                if (processedCharacterLiteral.length() == 1) { // Ensuring single character after processing
                    char character = processedCharacterLiteral.charAt(0);
                    return new Ast.Expression.Literal(character);
                } else {
                    throw new ParseException("Malformed character literal", tokens.get(-1).getIndex());
                }
            } else {
                throw new ParseException("Malformed character literal", tokens.get(-1).getIndex());
            }
        } else if (match(Token.Type.IDENTIFIER)) {
            String identifier = tokens.get(-1).getLiteral();
            // Check for boolean literals
            if ("TRUE".equals(identifier.toUpperCase())) {
                return new Ast.Expression.Literal(true);
            } else if ("FALSE".equals(identifier.toUpperCase())) {
                return new Ast.Expression.Literal(false);
            }
            if (match("(")) {
                // Function call
                List<Ast.Expression> arguments = new ArrayList<>();
                if (!peek(")")) {
                    do {
                        arguments.add(parseExpression());
                    } while (match(","));
                }
                if (!match(")")) {
                    throw new ParseException("Expected ')'", tokens.get(0).getIndex());
                }
                return new Ast.Expression.Function(identifier, arguments);
            } else if (match("[")) {
                // List index access
                Ast.Expression index = parseExpression();
                if (!match("]")) {
                    throw new ParseException("Expected ']", tokens.get(0).getIndex());
                }
                return new Ast.Expression.Access(Optional.of(index), identifier);
            } else {
                // Variable access
                return new Ast.Expression.Access(Optional.empty(), identifier);
            }
        } else if (match("(")) {
            Ast.Expression expression = parseExpression(); // Parse the inner expression
            if (!match(")")) {
                throw new ParseException("Expected ')'", tokens.get(0).getIndex());
            }
            // Now, the expression could be part of a larger binary operation. Delegate to handle binary ops.
            // Return the grouped expression directly if no binary operation follows.
            // Note: The actual implementation may vary based on how you've structured binary operation parsing.
            return new Ast.Expression.Group(expression);
        } else {
            throw new ParseException("Expected a primary expression", tokens.get(0).getIndex());
        }
    }

    private String processEscapeCharacters(String literal) {
        return literal
                .replace("\\b", "\b")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\'", "'")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }



    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    public boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    public boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }

        return peek;
    }


    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
