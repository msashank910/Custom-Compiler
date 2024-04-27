package plc.project;

import java.util.List;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        String processedInput = applyBackspaces(input);
        this.chars = new CharStream(processedInput);
    }


    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */

    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) {
            if (isWhitespace(chars.get(0))) {
                chars.advance(); // Always skip whitespace
            } else {
                tokens.add(lexToken());
            }
        }
        return tokens;
    }

    private boolean isWhitespace(char c) {
        return Character.isWhitespace(c);
    }



    public String applyBackspaces(String input) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current == '\u0008') {  // Backspace character
                if (result.length() > 0) {
                    result.deleteCharAt(result.length() - 1);  // Remove the last character in result
                }
                // If there's no character to remove, simply ignore the backspace
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }




    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */

    public Token lexToken() {
        if (peek("[A-Za-z]") || peek("@")) {
            return lexIdentifier();
        } else if (peek("\"")) {
            return lexString();
        } else if (peek("\'")) {
            return lexCharacter();
        } else if (peek("[0-9]")) {
            return lexNumber();
        } else if (peek("-") && (chars.has(1) && Character.isDigit(chars.get(1)))) {
            return lexNumber();
        } else {
            return lexOperator();
        }
    }

    public Token lexIdentifier() {
        int startIndex = chars.index;
        while (peek("[A-Za-z0-9_-]") || peek("@")) {
            chars.advance();
        }
        String value = chars.input.substring(startIndex, chars.index);
        return new Token(Token.Type.IDENTIFIER, value, startIndex);
    }

    public Token lexNumber() {
       // System.out.println("Lexing Number at index: " + chars.index);
        boolean isNegative = false;
        int startIndex = chars.index;

        if (match("-")) {
            isNegative = true;
        }
        if (!peek("[0-9]")) {
            throw new ParseException("Expected a digit.", chars.index);
        }

        // Handle potential leading zero that should not be followed by other digits.
        if (match("0")) {
            if (peek("[0-9]")) {
                // If another digit follows, end the current token and do not advance.
                return new Token(Token.Type.INTEGER, "0", startIndex);
            }
        } else {
            while (peek("[0-9]")) {
                chars.advance();
            }
        }

        if (match(".")) {
            // Check if there's a digit after the decimal to confirm it's a decimal number.
            if (!peek("[0-9]")) {
                // If no digit follows, treat the '.' as a pending operator.
                chars.retreat();  // Go back to before the '.'
                return new Token(Token.Type.INTEGER, chars.input.substring(startIndex, chars.index), startIndex);
            }
            // Continue reading the decimal part.
            while (peek("[0-9]")) {
                chars.advance();
            }
            return new Token(Token.Type.DECIMAL, chars.input.substring(startIndex, chars.index), startIndex);
        }

        // If there was no '.', return the integer token.
        return new Token(Token.Type.INTEGER, chars.input.substring(startIndex, chars.index), startIndex);
    }



    public Token lexCharacter() {
        if (!match("'")) {
            throw new ParseException("Expected a single quote to start a character literal.", chars.index);
        }
        int startIndex = chars.index - 1;

        // Handle escape sequences
        if (peek("\\")) {
            // Move past the backslash
            chars.advance();
            // Check for valid escape characters
            if (!peek("[bnrt'\"\\\\]")) {  // Make sure to check after advancing past the backslash
                throw new ParseException("Invalid escape sequence in character literal.", chars.index);
            }
            chars.advance(); // Advance past the escape character
        } else {
            // Check for invalid characters (non-escaped single quote, newline, or carriage return)
            if (peek("['\n\r\\\\]")) {  // Note: Add backslash to the invalid set if not escaped
                throw new ParseException("Invalid character in character literal.", chars.index);
            }
            chars.advance(); // Advance past the character if it's valid
        }

        // Ensure the character literal is closed properly
        if (!match("'")) {
            throw new ParseException("Expected a single quote to end a character literal.", chars.index);
        }
        // Get the entire character literal including the surrounding quotes
        String characterLiteral = chars.input.substring(startIndex, chars.index);
        return new Token(Token.Type.CHARACTER, characterLiteral, startIndex);
    }


    public Token lexString() {
       // System.out.println("Starting string literal lexing at index: " + chars.index);

        if (!match("\"")) {
           // System.out.println("Error 1: " + chars.index);
            throw new ParseException("Expected a double quote to start a string literal.", chars.index);
        }
        int startIndex = chars.index - 1;

        StringBuilder literal = new StringBuilder();

        while (true) {
            if (!chars.has(0)) {
                throw new ParseException("Unterminated string literal.", chars.index);
            } else if (peek("\"")) {
                chars.advance();
                break;
            } else if (peek("\n") || peek("\r")) {
               // System.out.println("Newline in unescaped string literal at index: " + chars.index);
                throw new ParseException("Newline in unescaped string literal.", chars.index);
            } else if (match("\\")) {
                //System.out.println("Escape sequence detected at index: " + chars.index);
                if (!chars.has(0)) {
                   // System.out.println("Unterminated string literal at index: " + startIndex);
                    throw new ParseException("Unterminated escape sequence.", chars.index);
                }

                char nextChar = chars.get(0);
                if ("bnrt'\"\\".indexOf(nextChar) != -1) {
                    literal.append("\\").append(nextChar);
                    chars.advance();
                } else {
                   // System.out.println("Error 3: " + chars.index);
                    throw new ParseException("Invalid escape sequence in string literal.", chars.index);
                }
            } else {
                literal.append(chars.get(0));
                chars.advance();
            }
        }
        return new Token(Token.Type.STRING, "\"" + literal.toString() + "\"", startIndex);
    }





    public void lexEscape() {
        if (!match("\\")) {
            throw new ParseException("Expected a backslash for escape sequence.", chars.index);
        }
        if (!peek("[bnrt'\"\\\\]")) {
            throw new ParseException("Invalid escape sequence.", chars.index);
        }
        chars.advance();
    }

    public Token lexOperator() {
        //System.out.println("Lexing Operator at index: " + chars.index);
        int startIndex = chars.index;

        // Handle compound operators.
        String[] compoundOperators = {"==", "!=", "&&", "||"};
        for (String op : compoundOperators) {
            if (match(op)) {
               // System.out.println("Matched compound operator: " + op + " at index: " + startIndex);
                return new Token(Token.Type.OPERATOR, op, startIndex);
            }
        }

        // Single character operator.
        if (chars.has(0)) {
            char opChar = chars.get(0);
            chars.advance();
           // System.out.println("Matched single-character operator: " + opChar + " at index: " + startIndex);
            return new Token(Token.Type.OPERATOR, String.valueOf(opChar), startIndex);
        } else {
            throw new ParseException("Expected an operator.", chars.index);
        }
    }






    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i)) {
                return false;
            }
            String regex = patterns[i].replace("\\", "\\\\");
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(String.valueOf(chars.get(i)));
            if (!matcher.find()) {
                return false;
            }
        }
        return true;
    }


    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */

    public boolean match(String... patterns) {
        for (String pattern : patterns) {
            if (chars.has(pattern.length() - 1)) {
                boolean matches = true;
                for (int i = 0; i < pattern.length(); i++) {
                    if (chars.get(i) != pattern.charAt(i)) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    for (int i = 0; i < pattern.length(); i++) {
                        chars.advance();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void retreat() {
            if (index > 0) {
                index--;
            }
        }


        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
