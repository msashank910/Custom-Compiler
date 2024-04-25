package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

//    @Override
//    public Void visit(Ast.Source ast) {
//        // Write the class header and the opening brace
//        print("public class Main {");
//        newline(0);
//        indent++; // increase the indent as we are now inside the class
//
//
//        // Visit and generate global variables (properties in Java)
//        for (Ast.Global global : ast.getGlobals()) {
//            visit(global);
//            newline(indent);
//        }
//
//        newline(indent);
//        // Generate the Java main method
//        print("public static void main(String[] args) {");
//        indent++; // increase the indent as we are now inside a method
//        newline(indent);
//        print("System.exit(new Main().main());");
//        indent--; // decrease the indent as we are about to close the method
//        newline(indent);
//        print("}");
//        newline(indent - 1); // decrease the indent as we are about to close the class
//
//        // Visit and generate functions (methods in Java)
//        for (Ast.Function function : ast.getFunctions()) {
//            newline(indent);
//            visit(function);
//            newline(0);
//        }
//        //newline(0);
//
//        // Finally, write the closing brace for the class
//        print("}");
//        indent--; // reset the indent as we have now closed the class
//
//        return null;
//    }

    @Override
    public Void visit(Ast.Source ast) {
        // Write the class header and the opening brace
        print("public class Main {");
        newline(0);
        newline(++indent); // Increase the indent and then print the newline

        // Visit and generate global variables (properties in Java)
        List<Ast.Global> globals = ast.getGlobals();
        for (int i = 0; i < globals.size(); i++) {
            visit(globals.get(i));
            // Check if this is the last iteration
            if (i < globals.size() - 1) {
                newline(indent); // Normal indentation for all but the last element
            } else {
                newline(0); // No indentation for the last element
                newline(indent);
            }
        }


        // Generate the Java main method
        print("public static void main(String[] args) {");
        newline(++indent); // Increase the indent and then print the newline
        print("System.exit(new Main().main());");
        newline(--indent); // Decrease the indent and then print the newline
        print("}");
        newline(0); // Ensure a newline after the main method

        // Visit and generate functions (methods in Java)
        int temp = indent;
        for (Ast.Function function : ast.getFunctions()) {
            newline(temp);
            visit(function);
            newline(0);
        }

        //newline(0);
        newline(0);
        // Finally, write the closing brace for the class
        //newline(indent); // Decrease the indent before closing the class
        print("}");
        return null;
    }



    @Override
    public Void visit(Ast.Global ast) {
        String type = convertType(ast.getTypeName());

        // Adjust the type if the variable is a list
        if (ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList) {
            type = inferArrayType((Ast.Expression.PlcList) ast.getValue().get(), type); // Infers the correct array type
        }

        // For immutable variables, prepend 'final' keyword
        if (!ast.getMutable()) {
            type = "final " + type;
        }

        // Print the type and variable name
        print(type + " " + ast.getName());

        // If a value is present, print the assignment
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        // End the declaration with a semicolon
        print(";");
        //newline(1); // Move to the next line after the variable declaration

        return null;
    }

    private String inferArrayType(Ast.Expression.PlcList list, String baseType) {
        // Determine if all elements are integers
        boolean allIntegers = list.getValues().stream()
                .allMatch(v -> v instanceof Ast.Expression.Literal && ((Ast.Expression.Literal)v).getLiteral() instanceof Integer);

        // Determine if all elements are strings
        boolean allStrings = list.getValues().stream()
                .allMatch(v -> v instanceof Ast.Expression.Literal && ((Ast.Expression.Literal)v).getLiteral() instanceof String);

        // Return the correct array type based on the contents of the list
        if ("int".equals(baseType) && allIntegers) {
            return "int[]"; // Use int array if all elements are integers
        } else if ("String".equals(baseType) && allStrings) {
            return "String[]"; // Use String array if all elements are strings
        } else if ("double".equals(baseType)) {
            return "double[]"; // Continue using double array as default for this base type
        }

        // Fallback to a more general array type if needed
        return "Object[]"; // General array type if none of the specific conditions are met
    }


    private String convertType(String typeName) {
        if ("Decimal".equals(typeName)) {
            return "double";
        } else if ("Integer".equals(typeName)) {
            return "int";
        } else if ("String".equals(typeName)) {
            return "String"; // Maps "String" to "String", can be adjusted if different mapping is required
        }
        // Add other type mappings here if necessary
        return typeName;
    }



//    @Override
//    public Void visit(Ast.Function ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

//    @Override
//    public Void visit(Ast.Function ast) {
//        // Start with the return type and function name
//        String returnType = ast.getReturnTypeName().map(this::convertType).orElse("void");
//        print(returnType + " " + ast.getName() + "(");
//
//        // Iterate over parameters to create the comma-separated list
//        for (int i = 0; i < ast.getParameters().size(); i++) {
//            print(ast.getParameterTypeNames().get(i) + " " + ast.getParameters().get(i));
//            if (i < ast.getParameters().size() - 1) {
//                print(", ");
//            }
//        }
//
//        // Close the parameters list and open the function body
//        print(") {");
//        newline(2); // Assuming function body starts with an indentation level of 1
//
//        for (int i = 0; i < ast.getStatements().size(); i++) {
//            visit(ast.getStatements().get(i)); // Visit and print the statement
//
//            // If this is the last statement, adjust the newline call
//            if (i == ast.getStatements().size() - 1) {
//                newline(1); // Dedent for the closing brace
//            } else {
//                newline(2); // Same indentation for other statements
//            }
//        }
//
//        // Close the function body
//        print("}");
//        newline(0);
//
//        return null;
//    }

    @Override
    public Void visit(Ast.Function ast) {
        // Start with the return type and function name
        String returnType = ast.getReturnTypeName().map(this::convertType).orElse("void");
        print(returnType + " " + ast.getName() + "(");

        // Handle the parameters with the specified spacing requirements
        for (int i = 0; i < ast.getParameters().size(); i++) {
            String paramType = convertType(ast.getParameterTypeNames().get(i));
            String paramName = ast.getParameters().get(i);
            print(paramType + " " + paramName);
            if (i < ast.getParameters().size() - 1) {
                print(", ");
            }
        }

        print(") {");  // Opening brace on the same line with a space before
        if (!ast.getStatements().isEmpty()) {
            int tempIndent = indent;
            newline(++indent);
            //newline(indent+1);
            // If there are statements, each on a new line with indentation
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
                if (i < ast.getStatements().size() - 1) {
                    //newline(indent+1);  // Only add new line if more statements follow
                    newline(indent);
                }
            }
            newline(tempIndent);  // Closing brace on a new line with original indentation
            //newline(indent);
            print("}");
            //newline(0);
            indent = tempIndent;
            //indent--;
        }else{
            print("}");
            //newline(0);
        }


        return null;
    }






    //    @Override
//    public Void visit(Ast.Statement.Expression ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Void visit(Ast.Statement.Expression ast) {
        // Assuming there is a method to visit expressions and generate Java code for them
        visit(ast.getExpression()); // This will generate the Java code for the expression
        print(";"); // Add a semicolon after the expression
        //newline(1); // Move to the next line with correct indentation
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        // Check for the provided type or infer the type based on the value if not provided.
        String type = ast.getTypeName().map(this::convertType)
                .orElseGet(() -> inferType(ast.getValue().orElse(null)));

        // Print the type and variable name
        print(type + " " + ast.getName());

        // If a value is present, print the assignment
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        // End the declaration with a semicolon
        print(";");
        return null;
    }

    private String inferType(Ast.Expression value) {
        if (value instanceof Ast.Expression.Literal) {
            Object literal = ((Ast.Expression.Literal)value).getLiteral();
            if (literal instanceof Integer) {
                return "int";
            } else if (literal instanceof BigDecimal) {
                return "double";
            } else if (literal instanceof String) {
                return "String";
            }
            // Add more type inferences here if necessary
        }
        return ""; // Default or unknown type
    }





    //    @Override
//    public Void visit(Ast.Statement.Assignment ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // The receiver might be an instance of Ast.Expression.Access, which would contain the name.
        if (ast.getReceiver() instanceof Ast.Expression.Access) {
            Ast.Expression.Access access = (Ast.Expression.Access) ast.getReceiver();
            // If there's no offset, it's a direct variable access
            if (!access.getOffset().isPresent()) {
                print(access.getName());
            } else {
                // For list/array access, there will be an offset present
                print(access.getName() + "[");
                visit(access.getOffset().get()); // Visit the offset expression
                print("]");
            }
        } else {
            // Handle other possible expression types for receiver if necessary
            throw new UnsupportedOperationException("Unsupported receiver expression: " + ast.getReceiver().getClass().getName());
        }

        // Assign the value expression to the receiver
        print(" = ");
        visit(ast.getValue()); // This will generate the Java code for the value expression
        print(";"); // End with a semicolon
        //newline(1); // Assuming this is the correct indentation level

        return null;
    }

//    @Override
//    public Void visit(Ast.Statement.If ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
//    @Override
//    public Void visit(Ast.Statement.If ast) {
//        // Start the if statement with the condition
//        print("if (");
//        visit(ast.getCondition()); // Assuming visit method for expressions
//        print(") {");
//        newline(1);
//
//        // Visit and generate each statement in the 'then' block
//        for (Ast.Statement statement : ast.getThenStatements()) {
//            visit(statement);
//            //newline(1); // Newline after each statement with indentation
//        }
//
//        // Decrease indentation and close the 'then' block
//        newline(-1);
//        print("}");
//
//        // Check for an else block
//        if (!ast.getElseStatements().isEmpty()) {
//            print(" else {");
//            newline(1);
//
//            // Visit and generate each statement in the 'else' block
//            for (Ast.Statement statement : ast.getElseStatements()) {
//                visit(statement);
//                //newline(1); // Newline after each statement with indentation
//            }
//
//            // Decrease indentation and close the 'else' block
//            newline(-1);
//            print("}");
//        }
//
//        return null;
//    }
@Override
public Void visit(Ast.Statement.If ast) {
    // Start the if statement with the condition
    print("if (");
    visit(ast.getCondition()); // Visit the condition expression
    print(") {");
    indent++; // Increase indentation for the 'then' block
    newline(indent);

    // Visit and generate each statement in the 'then' block
    for (int i = 0; i < ast.getThenStatements().size(); i++) {
        visit(ast.getThenStatements().get(i));
        if (i < ast.getThenStatements().size() - 1) {
            newline(indent); // Continue with the same indentation for the next statement
        }
    }

    // Close the 'then' block
    indent--; // Decrease indentation before closing the block
    newline(indent);
    print("}");

    // Check for an else block
    if (!ast.getElseStatements().isEmpty()) {
        print(" else {");
        indent++; // Increase indentation for the 'else' block
        newline(indent);

        // Visit and generate each statement in the 'else' block
        for (int i = 0; i < ast.getElseStatements().size(); i++) {
            visit(ast.getElseStatements().get(i));
            if (i < ast.getElseStatements().size() - 1) {
                newline(indent); // Continue with the same indentation for the next statement
            }
        }

        // Close the 'else' block
        indent--; // Decrease indentation before closing the block
        newline(indent);
        print("}");
    }

    //newline(indent); // Ensure a newline after closing the if-else block, maintaining the current indent
    return null;
}




    //    @Override
//    public Void visit(Ast.Statement.Switch ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Void visit(Ast.Statement.Switch ast) {
        // Start the switch statement with the condition
        print("switch (");
        visit(ast.getCondition()); // Visit the condition of the switch
        print(") {");
        int savedIndentation = indent;
        indent++;
        newline(indent); // Increase the indentation level for the cases

        // Generate each case in the switch
        for (Ast.Statement.Case caseStmt : ast.getCases()) {
            visit(caseStmt); // Visit each case which should handle the case statement generation
            // No need to increase indentation here because visit(caseStmt) should handle it
        }

//        // Generate the default case, if present
//        if (!ast.getCases().isEmpty() && ast.getCases().get(ast.getCases().size() - 1).getValue().isEmpty()) {
//            visit(ast.getCases().get(ast.getCases().size() - 1));
//        }
        // Close the switch statement
        newline(savedIndentation); // Decrease the indentation level back to what it was
        print("}");
        //newline(0); // Move to the next line after the switch statement

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (ast.getValue().isPresent()) {
            // This is a case statement.
            print("case ");
            visit(ast.getValue().get()); // Assuming a visit method is defined for expression types
            print(":");
        } else {
            // This is the default case.
            print("default:");
        }

        indent++; // Increase indentation for the statements within the case or default block
        newline(indent);

        // Visit and generate each statement in the case or default case
        for (int i = 0; i < ast.getStatements().size(); i++) {
            visit(ast.getStatements().get(i)); // Visit and print the statement

            // If this is the last statement, adjust the newline call
            if (i < ast.getStatements().size() - 1 || (ast.getValue().isPresent() && i == ast.getStatements().size() - 1)) {
                newline(indent);
            }
        }

        if (ast.getValue().isPresent()) {
            // Add a break statement for cases, but not for the default case
            print("break;");
            indent--;
            newline(indent); // Ensure proper indentation before break
        }


        return null;
    }


    //    @Override
//    public Void visit(Ast.Statement.While ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Void visit(Ast.Statement.While ast) {
        // Begin the while loop with the condition

        print("while (");
        visit(ast.getCondition()); // Generate the condition expression
        print(") {");
        if (ast.getStatements().isEmpty()) {
            print("}"); // Closing brace on the same line for an empty loop body
        } else {
            // If there are statements, handle each on a new line
            int tempIndent = indent;
            indent++;
            newline(indent); // Increase indentation for the loop body

            List<Ast.Statement> statements = ast.getStatements();
            for (int i = 0; i < statements.size(); i++) {

                visit(statements.get(i)); // Visit the current statement
                // Add a newline only if this is not the last statement
                if (i < statements.size() - 1) {
                    newline(indent);
                }
            }

            indent = tempIndent;
            newline(indent); // Adjust the indentation back for the closing brace
            print("}");
        }
        //newline(indent); // Ensure a new line after the loop in any case
        return null;
    }





    //    @Override
//    public Void visit(Ast.Statement.Return ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Void visit(Ast.Statement.Return ast) {
        // Start with the return keyword
        print("return ");
        // Visit the expression to be returned
        if (ast.getValue() != null) {
            visit(ast.getValue());
        } else {
            // If there's no expression, return null (for methods with no return value)
            print("null");
        }
        // Finish with a semicolon to complete the return statement
        print(";");
        //newline(1); // Proceed to the next line with correct indentation

        return null;
    }


//    @Override
//    public Void visit(Ast.Expression.Literal ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
@Override
public Void visit(Ast.Expression.Literal ast) {
    Object value = ast.getLiteral();
    if (value == null) {
        print("null");
    } else if (value instanceof Boolean) {
        print(value.toString());
    } else if (value instanceof Character) {
        print("'" + value + "'");
    } else if (value instanceof String) {
        print("\"" + value + "\"");
    } else if (value instanceof BigDecimal) {
        print(value.toString()); // Ensure BigDecimal is converted to String safely
    } else {
        print(value.toString()); // Safe for all other non-null objects
    }
    return null;
}


//    @Override
//    public Void visit(Ast.Expression.Group ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Void visit(Ast.Expression.Group ast) {
        // Print the opening parenthesis
        print("(");
        // Visit the nested expression
        visit(ast.getExpression());
        // Print the closing parenthesis
        print(")");
        return null;
    }


//    @Override
//    public Void visit(Ast.Expression.Binary ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Void visit(Ast.Expression.Binary ast) {
        // Handle the power operator as a special case
        if ("^".equals(ast.getOperator())) {
            print("Math.pow(");
            visit(ast.getLeft());
            print(", ");
            visit(ast.getRight());
            print(")");
        } else {
            // Handle all other binary operators
            String operator = translateOperator(ast.getOperator());
            visit(ast.getLeft());
            print(" " + operator + " ");
            visit(ast.getRight());
        }
        return null;
    }

        private String translateOperator(String operator) {
            switch (operator) {
                case "AND": return "&&";
                case "OR": return "||";
                // Add other operator translations as necessary
                default: return operator;
            }
        }


    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent()) {
            // This is an array access
            print(ast.getName() + "[");
            visit(ast.getOffset().get()); // Visit the offset expression for array access
            print("]");
        } else {
            // This is a simple variable access
            print(ast.getName());
        }
        return null;
    }
//    @Override
//    public Void visit(Ast.Expression.Function ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        // Translate the function name to the Java equivalent
        String functionName = translateFunctionName(ast.getName());

        // Start the function call
        print(functionName + "(");

        // Generate the comma-separated argument list
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i)); // Visit and generate the argument expression
            if (i < ast.getArguments().size() - 1) {
                print(", ");
            }
        }

        // Close the function call
        print(")");
        return null;
    }

    private String translateFunctionName(String name) {
        // Placeholder for translation logic
        switch (name) {
            case "print":
                return "System.out.println"; // Translate 'print' to 'System.out.println'
            // Add more translations as necessary
            default:
                return name; // Default to using the name as is
        }
    }




//    @Override
//    public Void visit(Ast.Expression.PlcList ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        // Start the list with an opening brace
        print("{");

        // Iterate over the expressions in the list to generate a comma-separated sequence
        for (int i = 0; i < ast.getValues().size(); i++) {
            visit(ast.getValues().get(i)); // Visit and generate the expression
            if (i < ast.getValues().size() - 1) {
                print(", "); // Add a comma and space after each element except the last
            }
        }

        // End the list with a closing brace
        print("}");
        return null;
    }


}
