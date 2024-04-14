package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;

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
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Source ast) {
        // Write the class header and the opening brace
        print("public class Main {");
        newline(0);

        // Visit and generate global variables (properties in Java)
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
            newline(1);
        }

        // Generate the Java main method
        newline(1); // for separation
        print("public static void main(String[] args) {");
        newline(2);
        print("System.exit(new Main().main());");
        newline(1);
        print("}");
        newline(0);

        // Visit and generate functions (methods in Java)
        for (Ast.Function function : ast.getFunctions()) {
            newline(1); // for separation
            visit(function);
            newline(0);
        }

        // Finally, write the closing brace for the class
        print("}");
        //newline(0);

        return null;
    }


//    @Override
//    public Void visit(Ast.Global ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Global ast) {
        String type = convertType(ast.getTypeName());

        // If the variable is a list, adjust the type for array declaration in Java
        if (ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList) {
            type = "double[]"; // Java type for array of decimals
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

    private String convertType(String typeName) {
        if ("Decimal".equals(typeName)) {
            return "double";
        } else if ("Integer".equals(typeName)) {
            return "int";
        }
        // Add other type mappings here if necessary
        return typeName;
    }


//    @Override
//    public Void visit(Ast.Function ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Function ast) {
        // Start with the return type and function name
        String returnType = ast.getReturnTypeName().map(this::convertType).orElse("void");
        print(returnType + " " + ast.getName() + "(");

        // Iterate over parameters to create the comma-separated list
        for (int i = 0; i < ast.getParameters().size(); i++) {
            print(ast.getParameterTypeNames().get(i) + " " + ast.getParameters().get(i));
            if (i < ast.getParameters().size() - 1) {
                print(", ");
            }
        }

        // Close the parameters list and open the function body
        print(") {");
        newline(2); // Assuming function body starts with an indentation level of 1

        for (int i = 0; i < ast.getStatements().size(); i++) {
            visit(ast.getStatements().get(i)); // Visit and print the statement

            // If this is the last statement, adjust the newline call
            if (i == ast.getStatements().size() - 1) {
                newline(1); // Dedent for the closing brace
            } else {
                newline(2); // Same indentation for other statements
            }
        }

        // Close the function body
        print("}");
        newline(indent); // Move to the next line with the current indentation level

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

//    @Override
//    public Void visit(Ast.Statement.Declaration ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
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
                }
                // Add more type inferences here if necessary
            }
            return "Object"; // Default or unknown type
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
    @Override
    public Void visit(Ast.Statement.If ast) {
        // Start the if statement with the condition
        print("if (");
        visit(ast.getCondition()); // Assuming visit method for expressions
        print(") {");
        newline(1);

        // Visit and generate each statement in the 'then' block
        for (Ast.Statement statement : ast.getThenStatements()) {
            visit(statement);
            //newline(1); // Newline after each statement with indentation
        }

        // Decrease indentation and close the 'then' block
        newline(-1);
        print("}");

        // Check for an else block
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            newline(1);

            // Visit and generate each statement in the 'else' block
            for (Ast.Statement statement : ast.getElseStatements()) {
                visit(statement);
                //newline(1); // Newline after each statement with indentation
            }

            // Decrease indentation and close the 'else' block
            newline(-1);
            print("}");
        }

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
        newline(1); // Increase the indentation level for the cases

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
        //newline(-1); // Decrease the indentation level back
        print("}");
        //newline(0); // Move to the next line after the switch statement

        return null;
    }
//    @Override
//    public Void visit(Ast.Statement.Case ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (ast.getValue().isPresent()) {
            // This is a case statement.
            print("case ");
            visit(ast.getValue().get()); // Assuming a visit method is defined for expression types
            print(":");
            newline(2); // Increase indentation for the case statements
        } else {
            // This is the default case.
            print("default:");
            newline(2); // Increase indentation for the default statements
        }

        // Visit and generate each statement in the case or default case
        for (int i = 0; i < ast.getStatements().size(); i++) {
            visit(ast.getStatements().get(i)); // Visit and print the statement

            // If this is the last statement, adjust the newline call
            if (i < ast.getStatements().size() - 1 || (ast.getValue().isPresent() && i == ast.getStatements().size() - 1)) {
                newline(2); // No additional indentation for the last statement
            } else {
                newline(0); // Maintain the specified indentation for other statements
            }
        }

        if (ast.getValue().isPresent()) {
            // Add a break statement for cases, but not for the default case.
            print("break;");
            newline(1);
        }

        // Dedent the indentation level back after case or default case statements
        //newline(-1);

        return null;
    }

//    @Override
//    public Void visit(Ast.Statement.While ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Void visit(Ast.Statement.While ast) {
        // Start the while statement with the condition
        print("while (");
        visit(ast.getCondition()); // Visit and generate the condition expression
        print(") {");
        newline(1); // Increase indentation for the loop body

        // Generate each statement within the while loop body
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement); // Visit and generate each statement
            newline(1); // Newline after each statement with correct indentation
        }

        // Close the while loop
        newline(-1); // Decrease indentation before closing brace
        print("}");
        newline(0); // Move to the next line after the while loop block

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
        if (value instanceof Boolean) {
            print(value.toString());
        } else if (value instanceof Character) {
            print("'" + value + "'");
        } else if (value instanceof String) {
            print("\"" + value + "\"");
        } else if (value instanceof BigDecimal) {
            print(value);
        } else {
            print(value.toString());
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
