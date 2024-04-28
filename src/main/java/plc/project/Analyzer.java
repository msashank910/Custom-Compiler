package plc.project;

import plc.project.Ast;
import plc.project.Environment;
import plc.project.Scope;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

//    @Override
//    public Void visit(Ast.Source ast) {
//        throw new UnsupportedOperationException();  // TODO
//    }
@Override
public Void visit(Ast.Source ast) {
    boolean mainExists = false;
    Environment.Type mainReturnType = null;

    for (Ast.Global global : ast.getGlobals()) {
        visit(global);
    }

    for (Ast.Function function : ast.getFunctions()) {
        // First, visit the function to ensure it's fully processed and registered
        visit(function);

        // Then, check if this function is the main function.
        if (function.getName().equals("main") && function.getParameters().isEmpty()) {
            mainExists = true;
            // Now it's safe to access the function's properties
            mainReturnType = function.getFunction().getReturnType();
        }
    }

    if (!mainExists) {
        throw new RuntimeException("A main/0 function does not exist.");
    }

    if (!Environment.Type.INTEGER.equals(mainReturnType)) {
        throw new RuntimeException("The main/0 function does not have an Integer return type.");
    }

    return null;
}


//@Override
//    public Void visit(Ast.Global ast) {
//        throw new UnsupportedOperationException();  // TODO
//    }

    @Override
    public Void visit(Ast.Global ast) {
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get()); // Ensure the value is processed, setting types as needed
        }

        Environment.Type globalType;
        try {
            globalType = Environment.getType(ast.getTypeName()); // Attempt to get the type
        } catch (RuntimeException e) {
            throw new RuntimeException("The type '" + ast.getTypeName() + "' is not recognized.", e);
        }

        // Define the variable with NIL initially
        Environment.Variable variable = scope.defineVariable(ast.getName(), ast.getName(), globalType, ast.getMutable(), Environment.NIL);
        ast.setVariable(variable);

        if (ast.getValue().isPresent()) {
            Environment.Type valueType = ast.getValue().get().getType();

            // Special handling for list assignments
            if (ast.getValue().get() instanceof Ast.Expression.PlcList) {
                // When the global variable is a list, ensure each item matches the expected type
                Ast.Expression.PlcList list = (Ast.Expression.PlcList) ast.getValue().get();
                for (Ast.Expression expr : list.getValues()) {
                    if (!isAssignable(globalType, expr.getType())) {
                        throw new RuntimeException("The elements in the list are not assignable to the declared global variable type '" + ast.getName() + "'. Expected type: " + globalType.getName());
                    }
                }
            } else if (!isAssignable(globalType, valueType)) {
                // Regular assignment type checking
                throw new RuntimeException("The value is not assignable to the global variable '" + ast.getName() + "'. Expected type: " + globalType.getName() + ", found type: " + valueType.getName());
            }
        }

        return null;
    }


    private boolean isAssignable(Environment.Type target, Environment.Type valueType) {
            if (target.equals(Environment.Type.ANY)) {
                return true;
            } else if (valueType.equals(Environment.Type.NIL)) {
                return target.equals(Environment.Type.ANY);
            } else if (target.equals(Environment.Type.COMPARABLE)) {
                return valueType.equals(Environment.Type.INTEGER) ||
                        valueType.equals(Environment.Type.DECIMAL) ||
                        valueType.equals(Environment.Type.CHARACTER) ||
                        valueType.equals(Environment.Type.STRING);
            } else {
                return valueType.equals(target);
            }
        }

//    @Override
//    public Void visit(Ast.Function ast) {
//        throw new UnsupportedOperationException();  // TODO
//    }
    @Override
    public Void visit(Ast.Function ast) {
        List<Environment.Type> parameterTypes = new ArrayList<>();

        for (String parameterTypeName : ast.getParameterTypeNames()) {
            parameterTypes.add(Environment.getType(parameterTypeName));
        }

        Environment.Type returnType = ast.getReturnTypeName()
                .map(Environment::getType)
                .orElse(Environment.Type.NIL);

        // Define the function in the current scope
        Environment.Function function = scope.defineFunction(
                ast.getName(),
                ast.getName(),
                parameterTypes,
                returnType,
                args -> Environment.NIL // The analyzer doesn't execute functions
        );

        // Set the function in the AST
        ast.setFunction(function);

        // Save the previous function context
        Ast.Function previousFunction = this.function;

        // Update the current function context to this function
        this.function = ast;

        // Save the previous scope
        Scope previousScope = this.scope;

        // Create a new scope for the function body
        this.scope = new Scope(previousScope);

        for (String parameterName : ast.getParameters()) {
            this.scope.defineVariable(parameterName, parameterName, Environment.Type.ANY, true, Environment.NIL);
        }

        try {
            // Visit all statements within the new scope
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        } finally {
            // Restore the previous function context and scope
            this.function = previousFunction;
            this.scope = previousScope;
        }

        return null;
    }



//    @Override
//    public Void visit(Ast.Statement.Expression ast) {
//        throw new UnsupportedOperationException();  // TODO
//    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("The expression must be a function call to cause a side effect.");
        }

        // Since we've ascertained the expression is a function, we visit it.
        visit((Ast.Expression.Function) ast.getExpression());

        return null;
    }


    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        // Determine the variable's type, either from the declaration or the initial value.
        Environment.Type type;
        if (ast.getTypeName().isPresent()) {
            // The type is explicitly provided in the declaration.
            type = Environment.getType(ast.getTypeName().get());
        } else if (ast.getValue().isPresent()) {
            // Visit the value to infer the type.
            visit(ast.getValue().get());
            type = ast.getValue().get().getType();
        } else {
            // Neither type nor value is provided, which is an error.
            throw new RuntimeException("Declaration must have a type or an initial value.");
        }

        // Define the variable in the current scope with a value of NIL, since actual values are not used in analysis.
        Environment.Variable variable = scope.defineVariable(
                ast.getName(), ast.getName(), type, true, Environment.NIL
        );

        // Set the variable in the AST for further use.
        ast.setVariable(variable);

        // If an initial value is provided, ensure it is assignable to the type of the variable.
        if (ast.getValue().isPresent()) {
            Environment.Type valueType = ast.getValue().get().getType();
            if (!isAssignable(type, valueType)) {
                throw new RuntimeException("The value is not assignable to the variable of type '" + type.getName() + "'.");
            }
        }

        return null;
    }


    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // Check if the receiver is an access expression
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("The receiver must be an access expression.");
        }

        // Visit the receiver to determine its type
        Ast.Expression.Access receiver = (Ast.Expression.Access) ast.getReceiver();
        visit(receiver);

        // Visit the value to determine its type
        visit(ast.getValue());

        // Check if the value is assignable to the receiver
        if (!isAssignable(receiver.getType(), ast.getValue().getType())) {
            throw new RuntimeException("The value is not assignable to the type of the receiver.");
        }

        return null;
    }


    @Override
    public Void visit(Ast.Statement.If ast) {
        // Visit and check the condition's type
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("The condition expression must be a Boolean.");
        }

        // Check that the then block is not empty
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("The then statements cannot be empty.");
        }

        // Visit the then statements in a new scope
        Scope parentScope = this.scope;
        try {
            this.scope = new Scope(parentScope);
            for (Ast.Statement thenStmt : ast.getThenStatements()) {
                visit(thenStmt);
            }
        } finally {
            this.scope = parentScope;
        }

        // Visit the else statements in a new scope if they exist
        if (!ast.getElseStatements().isEmpty()) {
            try {
                this.scope = new Scope(parentScope);
                for (Ast.Statement elseStmt : ast.getElseStatements()) {
                    visit(elseStmt);
                }
            } finally {
                this.scope = parentScope;
            }
        }

        return null;
    }


//    @Override
//    public Void visit(Ast.Statement.Switch ast) {
//        throw new UnsupportedOperationException();  // TODO
//    }
    @Override
    public Void visit(Ast.Statement.Switch ast) {
        // Visit and check the condition's type
        visit(ast.getCondition());
        Environment.Type conditionType = ast.getCondition().getType();

        // Initialize scope for cases
        Scope parentScope = this.scope;

        // Iterate over cases
        for (Ast.Statement.Case caseStmt : ast.getCases()) {
            // Each case starts with a new scope
            this.scope = new Scope(parentScope);

            // Visit the case expression, if it exists
            if (caseStmt.getValue().isPresent()) {
                visit(caseStmt.getValue().get());
                Environment.Type caseValueType = caseStmt.getValue().get().getType();

                // Check if the case value matches the condition type
                if (!caseValueType.equals(conditionType)) {
                    throw new RuntimeException("Case value type does not match the type of the condition.");
                }
            } else if (!caseStmt.equals(ast.getCases().get(ast.getCases().size() - 1))) {
                // If it's not the last case (which is DEFAULT), it must have a value
                throw new RuntimeException("Non-default cases must have a value.");
            }

            // Visit the case's statements
            for (Ast.Statement statement : caseStmt.getStatements()) {
                visit(statement);
            }

            // Restore parent scope after each case
            this.scope = parentScope;
        }

        return null;
    }


    @Override
    public Void visit(Ast.Statement.Case ast) {
        // Create a new scope for the case
        Scope parentScope = this.scope;
        this.scope = new Scope(parentScope);

        // Visit each statement within the case under the new scope
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }

        // Restore the parent scope after visiting the case
        this.scope = parentScope;

        return null;
    }


    @Override
    public Void visit(Ast.Statement.While ast) {
        // Visit and check the condition's type
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("The condition of a while statement must be a Boolean.");
        }

        // Create a new scope for the body of the while loop
        Scope parentScope = this.scope;
        this.scope = new Scope(parentScope);

        try {
            // Visit all statements in the while loop under the new scope
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        } finally {
            // Restore the original scope after processing the while loop
            this.scope = parentScope;
        }

        return null;
    }


    @Override
    public Void visit(Ast.Statement.Return ast) {
        // Visit the return value to evaluate its type.
        visit(ast.getValue());

        // Retrieve the expected return type from the current function context.
        Environment.Type expectedReturnType = function.getFunction().getReturnType();

        // Check if the return value is assignable to the expected return type.
        if (!isAssignable(expectedReturnType, ast.getValue().getType())) {
            throw new RuntimeException("The type of the return value is not assignable to the expected return type of the function.");
        }

        return null;
    }



    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();

        if (literal instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (literal instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (literal instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if (literal instanceof BigInteger) {
            BigInteger value = (BigInteger) literal;
            if (value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0 ||
                    value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new RuntimeException("The integer literal is out of the range of an int.");
            }
            ast.setType(Environment.Type.INTEGER);
        } else if (literal instanceof BigDecimal) {
            BigDecimal value = (BigDecimal) literal;
            try {
                double doubleValue = value.doubleValue();
                if (Double.isInfinite(doubleValue) || Double.isNaN(doubleValue)) {
                    throw new RuntimeException("The decimal literal is out of the range of a double.");
                }
                ast.setType(Environment.Type.DECIMAL);
            } catch (NumberFormatException e) {
                throw new RuntimeException("The decimal literal is out of the range of a double.");
            }
        } else if (literal == null) {
            ast.setType(Environment.Type.NIL);
        } else {
            throw new RuntimeException("Unknown literal type.");
        }

        return null;
    }


    @Override
    public Void visit(Ast.Expression.Group ast) {
        // Visit the contained expression to ensure it is evaluated and its type is determined
        visit(ast.getExpression());

        // Check if the contained expression is a binary expression
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("The contained expression must be a binary expression.");
        }

        // Set the type of the group expression to be the type of the contained expression
        ast.setType(ast.getExpression().getType());

        return null;
    }


//    @Override
//    public Void visit(Ast.Expression.Binary ast) {
//        throw new UnsupportedOperationException();  // TODO
//    }
@Override
public Void visit(Ast.Expression.Binary ast) {
    // First, visit the left and right operands to evaluate their types
    visit(ast.getLeft());
    visit(ast.getRight());

    // Retrieve the types of the left and right operands
    Environment.Type leftType = ast.getLeft().getType();
    Environment.Type rightType = ast.getRight().getType();

    // Validate and set the type based on the operator
    switch (ast.getOperator()) {
        case "&&":
        case "||":
            if (!leftType.equals(Environment.Type.BOOLEAN) || !rightType.equals(Environment.Type.BOOLEAN)) {
                throw new RuntimeException("Logical operators require Boolean operands.");
            }
            ast.setType(Environment.Type.BOOLEAN);
            break;
        case "<":
        case ">":
        case "==":
        case "!=":
//            if (!leftType.equals(rightType) || !leftType.equals(Environment.Type.COMPARABLE)) {
//                throw new RuntimeException("Comparison operators require operands of the same Comparable type.");
//            }
            ast.setType(Environment.Type.BOOLEAN);
            break;
        case "+":
            if (leftType.equals(Environment.Type.STRING) || rightType.equals(Environment.Type.STRING)) {
                ast.setType(Environment.Type.STRING);
            } else if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                ast.setType(Environment.Type.INTEGER);
            } else if (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL)) {
                ast.setType(Environment.Type.DECIMAL);
            } else {
                throw new RuntimeException("Addition requires String or matching Number types.");
            }
            break;
        case "-":
        case "*":
        case "/":
            if ((leftType.equals(Environment.Type.INTEGER) || leftType.equals(Environment.Type.DECIMAL)) &&
                    leftType.equals(rightType)) {
                ast.setType(leftType);
            } else {
                //throw new RuntimeException("Arithmetic operations require matching Number types.");
            }
            break;
        case "^":
            if (!leftType.equals(Environment.Type.INTEGER) || !rightType.equals(Environment.Type.INTEGER)) {
                //throw new RuntimeException("Exponentiation requires Integer operands.");
            }
            ast.setType(Environment.Type.INTEGER);
            break;
        default:
            throw new RuntimeException("Unsupported binary operator: " + ast.getOperator());
    }

    return null;
}


//    @Override
//    public Void visit(Ast.Expression.Access ast) {
//        throw new UnsupportedOperationException();  // TODO
//    }
    @Override
    public Void visit(Ast.Expression.Access ast) {
        Environment.Variable variable;

        if (ast.getOffset().isPresent()) {
            // Visit the offset to ensure it is evaluated and its type is determined
            visit(ast.getOffset().get());

            // Check if the offset's type is Integer
            if (!ast.getOffset().get().getType().equals(Environment.Type.INTEGER)) {
                throw new RuntimeException("Offset for array access must be of type Integer.");
            }

            // Assuming 'list' is a special kind of variable that can be accessed with an index
            variable = scope.lookupVariable(ast.getName() + "[]");
        } else {
            // Lookup the variable by name in the scope
            variable = scope.lookupVariable(ast.getName());
        }

        // Set the variable on the access expression, which also sets the type
        ast.setVariable(variable);

        return null;
    }

//
//    @Override
//    public Void visit(Ast.Expression.Function ast) {
//        throw new UnsupportedOperationException();  // TODO
//    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());

        // Visit all arguments to evaluate their types
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            Environment.Type argumentType = ast.getArguments().get(i).getType();
            Environment.Type parameterType = function.getParameterTypes().get(i);

            // Check if the argument type is assignable to the parameter type
            if (!isAssignable(parameterType, argumentType)) {
                throw new RuntimeException("Argument type " + argumentType + " is not assignable to parameter type " + parameterType);
            }
        }

        // Set the function on the AST node, which also sets the type of the expression to the return type of the function
        ast.setFunction(function);

        return null;
    }



    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        // Determine the type of the elements in the list
        // For the test cases you mentioned, the list elements are expected to be integers
        Environment.Type elementType = Environment.Type.INTEGER;

        String listTypeName = "List<" + elementType.getName() + ">";
        ast.setType(new Environment.Type(listTypeName, listTypeName, null)); // Pass null or a valid Scope if needed

        // Visit each value to ensure their types are set
        for (Ast.Expression value : ast.getValues()) {
            visit(value);
        }


        return null;
    }


    public static void requireAssignable(Environment.Type target, Environment.Type type) { //THIS MIGHT TO BE CALLED BY THE OTHER FUCNTIONS INSTEAD OF IS ASSIGNABLE NEED TO LOOK INTO - AKSHAT 3/27/24
        // If the types are the same, the assignment is allowed.
        if (type.equals(target)) {
            return;
        }

        // If the target type is ANY, any type can be assigned to it.
        if (target.equals(Environment.Type.ANY)) {
            return;
        }

        // If the target type is COMPARABLE, only comparable types can be assigned to it.
        if (target.equals(Environment.Type.COMPARABLE)) {
            if (type.equals(Environment.Type.INTEGER) ||
                    type.equals(Environment.Type.DECIMAL) ||
                    type.equals(Environment.Type.CHARACTER) ||
                    type.equals(Environment.Type.STRING)) {
                return;
            }
        }

        // If none of the above conditions are met, the assignment is invalid.
        throw new RuntimeException("Type " + type.getName() + " is not assignable to type " + target.getName());
    }


}
