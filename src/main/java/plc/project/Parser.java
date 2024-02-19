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

    public Ast.Source parseSource() throws ParseException {
        //this may be written wrong, the first while loop might only need to include list
        //come back to this later - Sashank

        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();

        while (peek("LIST") || peek("VAL") || peek("VAR")) {
            globals.add(parseGlobal());
        }

        while (peek("FUN")) {
            functions.add(parseFunction());
        }

        return new Ast.Source(globals, functions);
    }


    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */

    public Ast.Global parseGlobal() throws ParseException {
        Ast.Global result;
        if (match("LIST")) {
            result = parseList();
        } else if (match("VAR")) {
            result = parseMutable();
        } else if (match("VAL")) {
            result = parseImmutable();
        } else {
            throw new ParseException("Expected global declaration", tokens.get(0).getIndex());
        }

        // After parsing the global, check for a semicolon - Sashank
        if (!match(";")) {
            throw new ParseException("Expected ';' after global declaration", tokens.get(0).getIndex());
        }

        return result;
    }



    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */

    public Ast.Global parseList() throws ParseException {
        if (!match("LIST")) {
            throw new ParseException("Expected 'LIST'", getNextTokenExpectedIndex());
        }
        Token nameToken = tokens.get(0); // Get the current token which should be the identifier
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier", nameToken.getIndex());
        }
        //Edit made to check if token is identifier

        String name = nameToken.getLiteral(); // Get the literal value of the token
        if (!match("=")) {
            throw new ParseException("Expected '='", getNextTokenExpectedIndex());
        }
        if (!match("[")) {
            throw new ParseException("Expected '['", getNextTokenExpectedIndex());
        }
        List<Ast.Expression> values = new ArrayList<>();
        if (!peek("]")) { // Check if the list is not empty
            do {
                values.add(parseExpression()); // Parse the first expression
            } while (match(",")); // Continue parsing expressions if there's a comma
        }
        if (!match("]")) {
            throw new ParseException("Expected ']'",getNextTokenExpectedIndex());
        }

//        if (!match(";")) {
//            throw new ParseException("Expected ';'", tokens.get(0).getIndex());
//        }
        //Dont think this is needed

        Ast.Expression listExpression = new Ast.Expression.PlcList(values);
        return new Ast.Global(name, true, Optional.of(listExpression)); // Lists are always mutable
    }


    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */

    public Ast.Global parseMutable() throws ParseException {
        if (!match("VAR")) {
            throw new ParseException("Expected 'VAR'", getNextTokenExpectedIndex());
        }

        Token nameToken = tokens.get(0);
        if (!match(Token.Type.IDENTIFIER)) {

            throw new ParseException("Expected identifier", getNextTokenExpectedIndex());
        }
        //Edit made to check if token is identifier
        String name = nameToken.getLiteral();

        Optional<Ast.Expression> value = Optional.empty(); // Default to no initializer
        if (match("=")) {
            value = Optional.of(parseExpression()); // Parse the initializer expression
        }


//        if (!match(";")) {
//            throw new ParseException("Expected ';'", getNextTokenExpectedIndex());
//        }
        // semicolon handled in global
        return new Ast.Global(name, true, value); // 'true' for mutable
    }


    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        if (!match("VAL")) {
            throw new ParseException("Expected 'VAL'", getNextTokenExpectedIndex());
        }
        Token nameToken = tokens.get(0);
        if (!match(Token.Type.IDENTIFIER)) {

            throw new ParseException("Expected identifier", getNextTokenExpectedIndex());
        }
        String name = nameToken.getLiteral();

        if (!match("=")) {
            throw new ParseException("Expected '='", getNextTokenExpectedIndex());
        }
        Ast.Expression value = parseExpression(); // Parse the initializer expression

//        if (!match(";")) {
//            throw new ParseException("Expected ';'", getNextTokenExpectedIndex());
//        }
        //handled in global

        return new Ast.Global(name, false, Optional.of(value)); // 'false' for immutable
    }


    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        if (!match("FUN")) {
            throw new ParseException("Expected 'FUN'", getNextTokenExpectedIndex());
        }
        Token nameToken = tokens.get(0);
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected function name (identifier)", getNextTokenExpectedIndex());
        }
        String name = nameToken.getLiteral();       //changed this to conform with prior functions

        if (!match("(")) {
            throw new ParseException("Expected '(' after function name", getNextTokenExpectedIndex());
        }

        List<String> parameters = new ArrayList<>();
        if (!peek(")")) { // If there are parameters
            do {
                if (!match(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected parameter (identifier)", getNextTokenExpectedIndex());
                }
                parameters.add(tokens.get(-1).getLiteral());
            } while (match(","));               //85% sure this works, come back later
        }

        if (!match(")")) {
            throw new ParseException("Expected ')' after parameters", getNextTokenExpectedIndex());
        }

        if (!match("DO")) {
            throw new ParseException("Expected 'DO' after function declaration", getNextTokenExpectedIndex());
        }

        List<Ast.Statement> statements = parseBlock();

        if (!match("END")) {
            throw new ParseException("Expected 'END' to close function definition", getNextTokenExpectedIndex());
        }

        return new Ast.Function(name, parameters, statements);
    }


    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */

    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {          //Don't advance "END", done in parseFunction
            statements.add(parseStatement());
          
            // Optionally, ensure each statement is followed by a semicolon if your grammar requires it
            // if (!match(";")) {
            //     throw new ParseException("Expected ';'", getNextTokenExpectedIndex());
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
//        if (peek("LET")) {
//            return parseDeclarationStatement();
//        } else if (peek("IF")) {
//            return parseIfStatement();
//        } else if (peek("WHILE")) {
//            return parseWhileStatement();
//        } else if (peek("RETURN")) {
//            return parseReturnStatement();
//        } else if (peek("SWITCH")) { // Add this condition to handle switch statements
//            return parseSwitchStatement();
//        } else {
//            // Handle assignments or expression statements.
//            if (peek(Token.Type.IDENTIFIER) && tokens.has(1) && "=".equals(tokens.get(1).getLiteral())) {
//                return parseAssignmentStatement();
//            } else {
//                // It's considered an expression statement.
//                return parseExpressionStatement();
//            }
//        }
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
        } else if (peek("SWITCH")) {
            return parseSwitchStatement();
        } else if (peek(Token.Type.IDENTIFIER)) {
            // Use a lookahead approach to distinguish between assignment and expression statement without needing AccessOptional.
            return parseIdentifierInitiatedStatement();
        } else {
////OLD CODE QUESTIONABLE FUNCTIONALITY
//             // Handle assignments or expression statements.
//             if (peek(Token.Type.IDENTIFIER) && tokens.has(1) && "=".equals(tokens.get(1).getLiteral())) {
//                 return parseAssignmentStatement();   //check if there are more tokens after identifier and if equal to "="
//             } else {
//                 // It's considered an expression statement.
//                 return parseExpressionStatement();
            throw new ParseException("Unrecognized statement.", getNextTokenExpectedIndex());
        }
    }

    private Ast.Statement parseIdentifierInitiatedStatement() throws ParseException {
        // Look ahead to check if this is an assignment statement.
        boolean isAssignment = false;
        for (int i = 1; tokens.has(i); i++) {
            if (tokens.get(i).getLiteral().equals("=")) {
                isAssignment = true;
                break;
            }
        }

        if (isAssignment) {
            Ast.Expression target = parseExpression(); // Parses the LHS including potential complex expressions like list[index].
            if (!match("=")) {
                throw new ParseException("Expected '=' in assignment statement.", getNextTokenExpectedIndex());
            }
            Ast.Expression value = parseExpression(); // Parse the RHS of the assignment.
            if (!match(";")) {
                throw new ParseException("Expected ';' at the end of the assignment statement.", getNextTokenExpectedIndex());
            }
            return new Ast.Statement.Assignment(target, value);
        } else {
            Ast.Expression expr = parseExpression(); // Parse as a simple expression statement.
            if (!match(";")) {
                // This is the problematic area based on your description.
                throw new ParseException("Expected ';' at the end of the expression statement.", getNextTokenExpectedIndex());
            }
            return new Ast.Statement.Expression(expr);
        }
    }




    private Ast.Statement parseAssignmentStatement() throws ParseException {
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier in assignment statement.", getNextTokenExpectedIndex());
        }
        String name = tokens.get(-1).getLiteral();
        if (!match("=")) {
            throw new ParseException("Expected '=' in assignment statement.", getNextTokenExpectedIndex());
        }

        Ast.Expression value = parseExpression(); // Parse the right-hand side expression

        if (!match(";")) {
            System.out.println("IN assignement" + getNextTokenExpectedIndex());

            throw new ParseException("Expected ';' at the end of the assignment statement.",getNextTokenExpectedIndex());
        }

        return new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), name), value);
    }


    private Ast.Statement parseExpressionStatement() throws ParseException {
        Ast.Expression expression = parseExpression(); // Parse the expression
        if (!match(";")) {
            System.out.println("IN Exp: " + getNextTokenExpectedIndex());

            throw new ParseException("Expected ';' at the end of the expression statement.", getNextTokenExpectedIndex());
        }
        return new Ast.Statement.Expression(expression);
    }



    private int getNextTokenExpectedIndex() {
        if (tokens.has(0)) {
            // If there are tokens left, calculate the next expected index based on the current token's position
            Token currentToken = tokens.get(0);
            return currentToken.getIndex() + currentToken.getLiteral().length();
        } else if (tokens.get(-1) != null) {
            // If there are no tokens left, calculate based on the last token in the stream
            Token lastToken = tokens.get(-1);
            return lastToken.getIndex() + lastToken.getLiteral().length();
        } else {
            // Default to 0 if there are no tokens at all, though this should be rare given the parsing context
            return 0;
        }
    }




    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if (!match("LET")) {
            throw new ParseException("Expected 'LET'", getNextTokenExpectedIndex());
        }
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier after 'LET'", getNextTokenExpectedIndex());
        }
        String name = tokens.get(-1).getLiteral(); // Retrieve the identifier's name

        Optional<Ast.Expression> value = Optional.empty(); // Default to no initializer
        if (match("=")) {
            value = Optional.of(parseExpression()); // Parse the initializer expression if exists
        }

        if (!match(";")) {
            throw new ParseException("Expected ';' at the end of the declaration statement.", getNextTokenExpectedIndex());
        }

        return new Ast.Statement.Declaration(name, value);
    }


    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */

    public Ast.Statement.If parseIfStatement() throws ParseException {
        if (!match("IF")) {
            throw new ParseException("Expected 'IF'", getNextTokenExpectedIndex());
        }

        Ast.Expression condition = parseExpression(); // Parse the condition expression

        if (!match("DO")) {
            throw new ParseException("Expected 'DO' after 'IF' condition", getNextTokenExpectedIndex());
        }

        List<Ast.Statement> thenStatements = parseBlock(); // Parse the block of statements to execute if the condition is true

        List<Ast.Statement> elseStatements = new ArrayList<>(); // Prepare to hold any else statements
        if (match("ELSE")) {
          
//            if (!match("DO")) {
//                throw new ParseException("Expected 'DO' after 'ELSE'", tokens.get(0).getIndex());
//            }
//getNextTokenExpectedIndex() <-- use this as replacement for tokens.get(0).getIndex()

            elseStatements = parseBlock(); // Parse the block of statements to execute if the condition is false
        }

        if (!match("END")) {
            throw new ParseException("Expected 'END' to close the 'IF' statement", getNextTokenExpectedIndex());
        }

        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }


    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */

    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        if (!match("SWITCH")) {
            throw new ParseException("Expected 'SWITCH'", getNextTokenExpectedIndex());
        }

        Ast.Expression condition = parseExpression(); // Parse the switch expression

        List<Ast.Statement.Case> cases = new ArrayList<>();
        boolean foundDefault = false;

        // Loop to process 'CASE' and 'DEFAULT' statements
        while (peek("CASE") || (peek("DEFAULT") && !foundDefault)) {
            Ast.Statement.Case caseStatement = parseCaseStatement();
            if (caseStatement.getValue().isEmpty()) { // Check if it's a DEFAULT case
                if (foundDefault) {
                    throw new ParseException("Multiple 'DEFAULT' cases found", getNextTokenExpectedIndex());
                }
                foundDefault = true;
            }
            cases.add(caseStatement);
        }

        if (!foundDefault) {
            throw new ParseException("Missing 'DEFAULT' case in switch statement", tokens.get(-1).getIndex());
        }

        if (!match("END")) {
            throw new ParseException("Expected 'END' to close the 'SWITCH' statement", getNextTokenExpectedIndex());
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
                throw new ParseException("Expected ':' after 'CASE' expression", getNextTokenExpectedIndex());
            }
        }
        // Or check if it's a DEFAULT statement
        else if (match("DEFAULT")) {
            if (!match(":")) {
                throw new ParseException("Expected ':' after 'DEFAULT'", getNextTokenExpectedIndex());
            }
        } else {
            // If neither CASE nor DEFAULT, throw an exception
            throw new ParseException("Expected 'CASE' or 'DEFAULT' in switch statement", getNextTokenExpectedIndex());
        }

        List<Ast.Statement> statements = parseBlock(); // Parse the block of statements for this case/default
        return new Ast.Statement.Case(caseExpression, statements);
    }


    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */

    public Ast.Statement.While parseWhileStatement() throws ParseException {
        if (!match("WHILE")) {
            throw new ParseException("Expected 'WHILE'", getNextTokenExpectedIndex());
        }
        Ast.Expression condition = parseExpression(); // Parse the condition expression
        if (!match("DO")) {
            throw new ParseException("Expected 'DO' after 'WHILE' condition", getNextTokenExpectedIndex());
        }
        List<Ast.Statement> statements = parseBlock(); // Parse the block of statements to execute in the loop
        if (!match("END")) {
            throw new ParseException("Expected 'END' to close the 'WHILE' statement", getNextTokenExpectedIndex());
        }
        return new Ast.Statement.While(condition, statements);
    }


    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */

    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        if (!match("RETURN")) {
            throw new ParseException("Expected 'RETURN'", getNextTokenExpectedIndex());
        }
        Ast.Expression value = parseExpression(); // Parse the expression to be returned
        if (!match(";")) {
            throw new ParseException("Expected ';' at the end of the return statement.", getNextTokenExpectedIndex());
        }
        return new Ast.Statement.Return(value);
    }




    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
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
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.STRING)) {
            String stringLiteral = tokens.get(-1).getLiteral();
            String processedStringLiteral = processEscapeCharacters(stringLiteral.substring(1, stringLiteral.length() - 1));
            return new Ast.Expression.Literal(processedStringLiteral);
        } else if (match(Token.Type.CHARACTER)) {
            String characterLiteral = tokens.get(-1).getLiteral();
            String processedCharacterLiteral = processEscapeCharacters(characterLiteral.substring(1, characterLiteral.length() - 1));
            if (processedCharacterLiteral.length() == 1) {
                return new Ast.Expression.Literal(processedCharacterLiteral.charAt(0));
            } else {
                throw new ParseException("Malformed character literal", tokens.get(-1).getIndex());
            }
        } else if (match(Token.Type.IDENTIFIER)) {
            String identifier = tokens.get(-1).getLiteral();

            // Handle NIL literal
            if ("NIL".equals(identifier)) {
                return new Ast.Expression.Literal(null);
            }

            // Handle boolean literals
            if ("TRUE".equals(identifier.toUpperCase())) {
                return new Ast.Expression.Literal(true);
            } else if ("FALSE".equals(identifier.toUpperCase())) {
                return new Ast.Expression.Literal(false);
            }

            // Handle function calls or variable access
            if (match("(")) {
                List<Ast.Expression> arguments = new ArrayList<>();
                if (!peek(")")) {
                    do {
                        arguments.add(parseExpression());
                    } while (match(","));
                }
                if (!match(")")) {
                    throw new ParseException("Expected ')'", getNextTokenExpectedIndex());
                }
                return new Ast.Expression.Function(identifier, arguments);
            } else if (match("[")) {
                Ast.Expression index = parseExpression();
                if (!match("]")) {
                    throw new ParseException("Expected ']'", getNextTokenExpectedIndex());
                }
                return new Ast.Expression.Access(Optional.of(index), identifier);
            } else {
                return new Ast.Expression.Access(Optional.empty(), identifier);
            }
        } else if (match("(")) {
            Ast.Expression expression = parseExpression();
            if (!match(")")) {
                throw new ParseException("Expected ')'", getNextTokenExpectedIndex());
            }
            return new Ast.Expression.Group(expression);
        } else {
            throw new ParseException("Expected a primary expression", getNextTokenExpectedIndex());
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
