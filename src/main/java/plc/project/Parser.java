package plc.project;

import plc.project.Ast;

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
        if (peek("LIST")) {
            result = parseList();
        } else if (peek("VAR")) {
            result = parseMutable();
        } else if (peek("VAL")) {
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
        Token nameToken = tokens.get(0); // Get the current token which should be the name identifier
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier", nameToken.getIndex());
        }
        String name = nameToken.getLiteral(); // Save the identifier's literal value as name

        if (!match(":")) {
            throw new ParseException("Expected ':'", getNextTokenExpectedIndex());
        }

        // Process the type identifier, which follows the colon
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected type identifier after ':'", getNextTokenExpectedIndex());
        }
        String typeName = tokens.get(-1).getLiteral(); // Capture the type identifier

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
            throw new ParseException("Expected ']'", getNextTokenExpectedIndex());
        }

        Ast.Expression listExpression = new Ast.Expression.PlcList(values);
        return new Ast.Global(name, typeName, true, Optional.of(listExpression)); // Lists are always mutable, include typeName
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
        String name = nameToken.getLiteral();

        if (!match(":")) {
            throw new ParseException("Expected ':' after identifier", getNextTokenExpectedIndex());
        }

        // Expecting type identifier after ':'
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected type identifier after ':'", getNextTokenExpectedIndex());
        }
        String typeName = tokens.get(-1).getLiteral(); // Capture the type identifier

        Optional<Ast.Expression> value = Optional.empty(); // Default to no initializer
        if (match("=")) {
            value = Optional.of(parseExpression()); // Parse the initializer expression
        }

        // The semicolon is checked in the calling 'parseGlobal' method, as noted.
        return new Ast.Global(name, typeName, true, value); // 'true' for mutable, include typeName
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

        if (!match(":")) {
            throw new ParseException("Expected ':' after identifier", getNextTokenExpectedIndex());
        }

        // Expecting and consuming the type identifier after ':'
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected type identifier after ':'", getNextTokenExpectedIndex());
        }
        String typeName = tokens.get(-1).getLiteral(); // Capture the type identifier

        if (!match("=")) {
            throw new ParseException("Expected '='", getNextTokenExpectedIndex());
        }

        Ast.Expression value = parseExpression(); // Parse the initializer expression

        // Include the captured type identifier in the Ast.Global constructor
        return new Ast.Global(name, typeName, false, Optional.of(value)); // 'false' for immutable
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
        String name = nameToken.getLiteral();
        System.out.println("name token: " + name);

        if (!match("(")) {
            throw new ParseException("Expected '(' after function name", getNextTokenExpectedIndex());
        }

        List<String> parameters = new ArrayList<>();
        List<String> parameterTypeNames = new ArrayList<>();
        while (!peek(")")) {
            System.out.println("nothing will run here");

            if (!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected parameter name (identifier)", getNextTokenExpectedIndex());
            }
            String paramName = tokens.get(-1).getLiteral();

            if (!match(":")) {
                throw new ParseException("Expected ':' after parameter name", getNextTokenExpectedIndex());
            }
            if (!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected parameter type after ':'", getNextTokenExpectedIndex());
            }
            String paramType = tokens.get(-1).getLiteral();

            parameters.add(paramName);
            parameterTypeNames.add(paramType);

            if (!peek(")")) {
                if (!match(",")) {
                    throw new ParseException("Expected ',' between parameters", getNextTokenExpectedIndex());
                }
            }
        }

        if (!match(")")) {
            throw new ParseException("Expected ')' after parameters", getNextTokenExpectedIndex());
        }

        Optional<String> returnType = Optional.empty(); // Default return type
        if (match(":")) {
            if (!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected return type identifier after ':'", getNextTokenExpectedIndex());
            }
            returnType = Optional.of(tokens.get(-1).getLiteral());
        }

        if (!match("DO")) {
            throw new ParseException("Expected 'DO' after function declaration", getNextTokenExpectedIndex());
        }

        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
            // Optional semicolon handling here if your grammar requires it.
        }

        if (!match("END")) {
            throw new ParseException("Expected 'END' to close function definition", getNextTokenExpectedIndex());
        }

        // Use the constructor that accepts parameter types and an optional return type.
        return new Ast.Function(name, parameters, parameterTypeNames, returnType, statements);
    }




    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */

    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {          //Don't advance "END", done in parseFunction
            if(peek("CASE"))
                return statements;
            else if(peek("DEFAULT"))
                return statements;
            else if(peek("ELSE"))
                return statements;
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
    public Ast.Statement parseStatement() throws ParseException {
        System.out.println("Debug here: " + peekTokenLiteral());
        if (peek("LET")) {
            return parseDeclarationStatement();
        } else if (peek("IF")) {
            return parseIfStatement();
        } else if (peek("WHILE")) {
            //System.out.println("in while case for parse statement: "+ peekTokenLiteral());
            return parseWhileStatement();
        } else if (peek("RETURN")) {
            return parseReturnStatement();
        } else if (peek("SWITCH")) {
            return parseSwitchStatement();
        } else if (peek(Token.Type.IDENTIFIER)) {
            System.out.println("stmt is an identifier");

            // Use a lookahead approach to distinguish between assignment and expression statement without needing AccessOptional.
            return parseIdentifierInitiatedStatement();
        } else {
            throw new ParseException("Unrecognized statement.", getNextTokenExpectedIndex());
        }
    }

    private Ast.Statement parseIdentifierInitiatedStatement() throws ParseException {
        Ast.Expression initialExpression = parseExpression(); // Parse the LHS which might be an Access or Function call.

        if (peek("=")) {
            // This confirms it's an assignment statement, so consume '=' and parse the RHS.
            match("="); // Now we're sure it's an assignment, consume '='.
            Ast.Expression value = parseExpression(); // Parse the RHS of the assignment.
            if (!match(";")) {
                throw new ParseException("Expected ';' at the end of the assignment statement.", getNextTokenExpectedIndex());
            }
            // Ensure the initial expression is a valid target for assignment (Access).
            if (!(initialExpression instanceof Ast.Expression.Access)) {
                throw new ParseException("Left-hand side of an assignment must be a variable.", getNextTokenExpectedIndex());
            }
            return new Ast.Statement.Assignment((Ast.Expression.Access) initialExpression, value);
        } else {
            // If not an assignment, it was just a simple expression statement.
            // Confirm that it's properly terminated with a semicolon.
            if (!match(";")) {
                throw new ParseException("Expected ';' at the end of the expression statement.", getNextTokenExpectedIndex());
            }
            return new Ast.Statement.Expression(initialExpression);
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
        // Assuming this method is called right after identifying a missing "DO" token,
        // which should be immediately after the current token (the "IF" condition).

        if (tokens.has(0)) {
            // If there is a next token, we are not at the end, so use the current token's position.
            Token currentToken = tokens.get(0);
            // Adjust the calculation to return the start index of the next token, which is what we actually want.
            return currentToken.getIndex();
        } else if (!tokens.tokens.isEmpty()) {
            // If there are no tokens left to process (no next token), calculate based on the last processed token.
            Token lastToken = tokens.tokens.get(tokens.index - 1);
            // Return the position immediately after the last token, assuming 'getIndex()' returns the starting index.
            return lastToken.getIndex() + lastToken.getLiteral().length();
        } else {
            // Default to 0 if there are no tokens at all, which should be rare.
            return 0;
        }
    }





    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement parseDeclarationStatement() throws ParseException {
        if (!match("LET")) {
            throw new ParseException("Expected 'LET'", getNextTokenExpectedIndex());
        }
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier after 'LET'", getNextTokenExpectedIndex());
        }
        String name = tokens.get(-1).getLiteral();

        Optional<String> type = Optional.empty();
        if (match(":")) {
            if (!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected type identifier after ':'", getNextTokenExpectedIndex());
            }
            type = Optional.of(tokens.get(-1).getLiteral());
        }
        Optional<Ast.Expression> initializer = Optional.empty(); // Default to no initializer
        if (match("=")) {
            initializer = Optional.of(parseExpression()); // Parse the initializer expression if it exists
        }
        if (!match(";")) {
            throw new ParseException("Expected ';' at the end of the declaration statement.", getNextTokenExpectedIndex());
        }
        return new Ast.Statement.Declaration(name, type, initializer);
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

        List<Ast.Statement> thenStatements = new ArrayList<>();
        while (!peek("ELSE") && !peek("END")) {
            thenStatements.add(parseStatement());
            // Consume semicolon after statement, if present, but not before ELSE or END
            if (peek(";") && !(peek("ELSE", 1) || peek("END", 1))) {
                match(";");
            }
        }

        List<Ast.Statement> elseStatements = new ArrayList<>();
        if (match("ELSE")) {
            while (!peek("END")) {
                elseStatements.add(parseStatement());
                // Consume semicolon after statement, if present, but not before END
                if (peek(";") && !peek("END", 1)) {
                    match(";");
                }
            }
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
            //System.out.println("Entering parseCase");
            Ast.Statement.Case caseStatement = parseCaseStatement();
            if (caseStatement.getValue().isEmpty()) { // Check if it's a DEFAULT case
                if (foundDefault) {
                    throw new ParseException("Multiple 'DEFAULT' cases found", getNextTokenExpectedIndex());
                }
                foundDefault = true;
            }
            //System.out.println(caseStatement.getValue());
            //System.out.println(caseStatement.getStatements());
            //System.out.println(peek("DEFAULT"));
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
            //System.out.println("We made it to Default!!");
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

//    public Ast.Statement.While parseWhileStatement() throws ParseException {
//        System.out.println("Entering parseWhileStatement");
//        System.out.println("Current token before matching 'WHILE': " + peekTokenLiteral());
//        if (!match("WHILE")) {
//            throw new ParseException("Expected 'WHILE'", getNextTokenExpectedIndex());
//        }
//        System.out.println("Matched 'WHILE'");
//
//        Ast.Expression condition = parseExpression(); // Parse the condition expression
//        if (!match("DO")) {
//            throw new ParseException("Expected 'DO' after 'WHILE' condition", getNextTokenExpectedIndex());
//        }
//        List<Ast.Statement> statements = parseBlock(); // Parse the block of statements to execute in the loop
//        if (!match("END")) {
//            throw new ParseException("Expected 'END' to close the 'WHILE' statement", getNextTokenExpectedIndex());
//        }
//
//        System.out.println("Exiting parseWhileStatement");
//        return new Ast.Statement.While(condition, statements);
//    }

    public Ast.Statement.While parseWhileStatement() throws ParseException {
        if (!match("WHILE")) {
            throw new ParseException("Expected 'WHILE'", getNextTokenExpectedIndex());
        }

        Ast.Expression condition = parseExpression(); // Parse the condition expression
        if (!match("DO")) {
            throw new ParseException("Expected 'DO' after 'WHILE' condition", getNextTokenExpectedIndex());
        }

        List<Ast.Statement> statements = new ArrayList<>();

        // Keep parsing statements until "END" is encountered.
        while (!peek("END")) {
            statements.add(parseStatement());
            // If a semicolon is peeked (and not at the end), consume it as part of statement termination.
            if (peek(";") && !peek("END", 1)) {
                match(";");
            }
        }

        if (!match("END")) {
            throw new ParseException("Expected 'END' to close the 'WHILE' statement", getNextTokenExpectedIndex());
        }

        return new Ast.Statement.While(condition, statements);
    }


    private String peekTokenLiteral() {
        return tokens.has(0) ? tokens.get(0).getLiteral() : "No token available";
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
        Ast.Expression result = parseEqualityExpression(); // Start with an equality expression.

        // Process logical operators (both '&&' and '||').
        while (true) { // Use a loop to handle continuous logical operations
            if (match("&&")) {
                String operator = tokens.get(-1).getLiteral(); // Get the matched logical operator.
                Ast.Expression right = parseEqualityExpression(); // Parse the right-hand side expression.

                // Construct a new binary expression with the operator and both sides.
                result = new Ast.Expression.Binary(operator, result, right);
            } else if (match("||")) {
                String operator = tokens.get(-1).getLiteral(); // Similarly handle the "||" operator
                Ast.Expression right = parseEqualityExpression(); // Parse the right-hand side expression for "||"

                // Construct a new binary expression for the "||" operator.
                result = new Ast.Expression.Binary(operator, result, right);
            } else {
                break; // Exit the loop if no more logical operations are found
            }
        }

        return result;
    }



    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression result = parseComparisonExpression(); // Start with comparison expressions

        while (true) { // Loop to handle multiple equality operations
            if (match("==")) {
                String operator = "==";
                Ast.Expression right = parseComparisonExpression();
                result = new Ast.Expression.Binary(operator, result, right);
            } else if (match("!=")) {
                String operator = "!=";
                Ast.Expression right = parseComparisonExpression();
                result = new Ast.Expression.Binary(operator, result, right);
            } else {
                break; // No more equality operators, exit the loop
            }
        }

        return result;
    }

//    public Ast.Expression parseEqualityExpression() throws ParseException {
//        Ast.Expression result = parseAdditiveExpression();
//        while (match("==")) {
//            String operator = tokens.get(-1).getLiteral();
//            Ast.Expression right = parseAdditiveExpression();
//            result = new Ast.Expression.Binary(operator, result, right);
//        }
//        return result;
//    }





    /**
     * Parses the {@code comparison-expression} rule.
     */


    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression result = parseAdditiveExpression(); // Start with additive expressions

        while (true) { // Loop to handle multiple comparison operations
            if (match("<")) {
                String operator = "<";
                Ast.Expression right = parseAdditiveExpression();
                result = new Ast.Expression.Binary(operator, result, right);
            } else if (match(">")) {
                String operator = ">";
                Ast.Expression right = parseAdditiveExpression();
                result = new Ast.Expression.Binary(operator, result, right);
            } else if (match("<=")) {
                String operator = "<=";
                Ast.Expression right = parseAdditiveExpression();
                result = new Ast.Expression.Binary(operator, result, right);
            } else if (match(">=")) {
                String operator = ">=";
                Ast.Expression right = parseAdditiveExpression();
                result = new Ast.Expression.Binary(operator, result, right);
            } else {
                break; // No more comparison operators, exit the loop
            }
        }

        return result;
    }

//    public Ast.Expression parseComparisonExpression() throws ParseException {
//        Ast.Expression expression = parseAdditiveExpression(); // Start with an additive expression
//
//        // Loop as long as there are comparison operators, indicating continuation of the comparison expression
//        while (match("<", ">", "==", "!=")) {
//            String operator = tokens.get(-1).getLiteral(); // Get the operator
//            Ast.Expression right = parseAdditiveExpression(); // Parse the right-hand side additive expression
//            expression = new Ast.Expression.Binary(operator, expression, right); // Combine into a binary expression
//        }
//
//        return expression; // Return the built expression
//    }


    /**
     * Parses the {@code additive-expression} rule.
     */
//    public Ast.Expression parseAdditiveExpression() throws ParseException {
//        Ast.Expression result = parseMultiplicativeExpression();
//        while (match("+")) {
//            String operator = tokens.get(-1).getLiteral();
//            Ast.Expression right = parseMultiplicativeExpression();
//            result = new Ast.Expression.Binary(operator, result, right);
//        }
//        return result;
//    }

    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression result = parseMultiplicativeExpression(); // Start with lower precedence
        while (peek("+") || peek("-")) { // Check for both '+' and '-' operators
            boolean matchPlus = match("+");
            boolean matchMinus = match("-");
            String operator = matchPlus ? "+" : "-";
            Ast.Expression right = parseMultiplicativeExpression(); // Parse right-hand side
            result = new Ast.Expression.Binary(operator, result, right); // Construct binary expression
        }
        return result;
    }




    /**
     * Parses the {@code multiplicative-expression} rule.
     */

    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression result = parsePrimaryExpression();
        // Use individual checks for each operator
        while (true) {
            if (match("*")) {
                String operator = "*";
                Ast.Expression right = parsePrimaryExpression();
                result = new Ast.Expression.Binary(operator, result, right);
            } else if (match("/")) {
                String operator = "/";
                Ast.Expression right = parsePrimaryExpression();
                result = new Ast.Expression.Binary(operator, result, right);
            } else if (match("^")) {
                String operator = "^";
                Ast.Expression right = parsePrimaryExpression();
                result = new Ast.Expression.Binary(operator, result, right);
            } else {
                break; // No more operators to process, exit the loop
            }
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
        // Print all tokens left to parse
        System.out.println("Remaining tokens to parse:");
        for (int i = 0; i < tokens.tokens.size(); i++) {
            System.out.println("Token[" + i + "]: " + tokens.tokens.get(i).getType() + " - '" + tokens.tokens.get(i).getLiteral() + "'");
        }

        // Print current and next token for immediate context
        System.out.println("Current token: " + tokens.get(0).getLiteral() + ", Next token: " + (tokens.has(1) ? tokens.get(1).getLiteral() : "None"));


        if (match("NIL")) {
            System.out.println("parsePrimaryExpression: Matched NIL");
            return new Ast.Expression.Literal(null);
        }
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
                System.out.println("Detected '(': Starting to parse function call for identifier: " + identifier);

                List<Ast.Expression> arguments = new ArrayList<>();
                if (!peek(")")) {
                    System.out.println("Arguments detected: Parsing arguments for function call.");
                    do {
                        arguments.add(parseExpression());
                        System.out.println("Parsed argument for function: " + identifier);

                    } while (match(","));
                }else{
                    System.out.println("No arguments detected for function call.");
                }
                if (!match(")")) {
                    throw new ParseException("Expected ')'", getNextTokenExpectedIndex());
                }
                System.out.println("Function call parsed successfully for identifier: " + identifier);
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
