package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

//    @Override
//    public Environment.PlcObject visit(Ast.Source ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        // Evaluate all globals
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        // Evaluate all functions and save them in the environment
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }
        // Find and call the main function if it exists
        Environment.PlcObject mainResult = Environment.NIL;
        if (scope.lookupFunction("main", 0) != null) {
            mainResult = scope.lookupFunction("main", 0).invoke(List.of());
        } else {
            throw new RuntimeException("No 'main' function defined in the source.");
        }
        return mainResult;
    }


//    @Override
//    public Environment.PlcObject visit(Ast.Global ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        // Check if the global has an initial value.
        Environment.PlcObject value = ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL;

        // Define the variable in the current scope.
        // According to the language specification, globals are always mutable.
        scope.defineVariable(ast.getName(), true, value);

        // According to the specification, return NIL after defining a global.
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        // Define the function in the current scope.
        // Capture the current scope where the function is defined to be used when the function is called.
        Scope definingScope = this.scope;

        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            // Create a new scope for the function call that has the defining scope as its parent.
            Scope functionScope = new Scope(definingScope);
            // Define parameters in the new scope.
            for (int i = 0; i < ast.getParameters().size(); i++) {
                functionScope.defineVariable(ast.getParameters().get(i), true, args.get(i));
            }
            try {
                // Set the current scope to the function's scope.
                this.scope = functionScope;
                // Evaluate the function's body.
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
                // If the function completes without a return, return NIL.
                return Environment.NIL;
            } catch (Return returnValue) {
                // Catch the Return exception and return its value.
                return returnValue.value;
            } finally {
                // Restore the original scope.
                this.scope = definingScope;
            }
        });

        // According to the specification, visit(Ast.Function) should itself return NIL.
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        // Evaluate the expression
        visit(ast.getExpression());

        // According to the assignment, always return NIL for an expression statement
        return Environment.NIL;
    }


//    @Override
//    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
//        throw new UnsupportedOperationException(); //TODO (in lecture)
//    }
    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) { // LOOK AT THIS AGAIN BC THIS IS NOT FROM LECTURE SO MIGHT HAVE TO REDO - AKSHAT!!!
        // Check if the declaration has an initial value.
        Environment.PlcObject value = ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL;

        // Define the variable in the current scope, local variables are always mutable.
        scope.defineVariable(ast.getName(), true, value);

        // According to the specification, return NIL after a declaration.
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("The receiver must be an Access expression.");
        }

        Ast.Expression.Access access = (Ast.Expression.Access) ast.getReceiver();
        Environment.PlcObject value = visit(ast.getValue());

        // Check if the receiver is a list with an index to be accessed
        if (access.getOffset().isPresent()) {
            // This is an assignment to a list element.
            Environment.Variable listVariable = scope.lookupVariable(access.getName());
            // We expect a List here, so we need to cast the raw value to a List of Objects, not PlcObjects.
            List<Object> list = requireType(List.class, listVariable.getValue().getValue());
            // Get the index as BigInteger after evaluating the offset expression and extracting its raw value.
            BigInteger index = requireType(BigInteger.class, visit(access.getOffset().get()).getValue());
            // Extract the raw value from the Environment.PlcObject to perform the assignment in the list at the specified index.
            Object rawValue = value.getValue();
            // Ensure the list is capable of handling the modification by converting it to an ArrayList if necessary.
            if (!(list instanceof ArrayList)) {
                list = new ArrayList<>(list);
                // Update the variable with the new list capable of modification.
                listVariable.setValue(Environment.create(list));
            }
            list.set(index.intValueExact(), rawValue);
        } else {
            // This is a normal variable assignment.
            Environment.Variable variable = scope.lookupVariable(access.getName());
            if (!variable.getMutable()) {
                throw new RuntimeException("Assignment to an immutable variable.");
            }
            variable.setValue(value);
        }

        return Environment.NIL;
    }


    /**
     * Helper method to cast an object to the required type.
     */
    @SuppressWarnings("unchecked")
    private static <T> T requireType(Class<T> type, Object object) {
        if (type.isInstance(object)) {
            return (T) object;
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getClass().getName() + ".");
        }
    }


//    @Override
//    public Environment.PlcObject visit(Ast.Statement.If ast) { // LOOK AT THIS AGAIN BC THIS IS NOT FROM LECTURE SO MIGHT HAVE TO REDO - AKSHAT!!!
//        throw new UnsupportedOperationException(); //TODO
//    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        // Evaluate the condition of the if statement and ensure it is a Boolean
        Boolean condition = requireType(Boolean.class, visit(ast.getCondition()).getValue());

        // Create a new scope for the execution of the statement block
        Scope originalScope = this.scope;
        this.scope = new Scope(originalScope);

        try {
            // Evaluate the appropriate block of statements based on the condition's value
            if (condition) {
                for (Ast.Statement statement : ast.getThenStatements()) {
                    visit(statement);
                }
            } else {
                for (Ast.Statement statement : ast.getElseStatements()) {
                    visit(statement);
                }
            }
        } finally {
            // Restore the original scope after executing the block
            this.scope = originalScope;
        }

        // If statements do not produce a value, so return NIL
        return Environment.NIL;
    }



    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        // Evaluate the condition of the switch statement
        Environment.PlcObject condition = visit(ast.getCondition());

        // Create a new scope for the switch statement block
        Scope originalScope = this.scope;
        this.scope = new Scope(originalScope);

        boolean executedCase = false;

        try {
            // Iterate through cases to find a match
            for (Ast.Statement.Case caseStmt : ast.getCases()) {
                if (!caseStmt.getValue().isPresent()) {
                    // If no value is present, then it's the default case
                    if (!executedCase) {
                        // If no case has been executed, visit the default case
                        visit(caseStmt);
                    }
                    // Break after visiting the default case since it should be the last case
                    break;
                } else {
                    // Evaluate the case's value
                    Environment.PlcObject caseValue = visit(caseStmt.getValue().get());
                    // Check if the case value matches the switch condition
                    if (condition.getValue().equals(caseValue.getValue())) {
                        visit(caseStmt);
                        executedCase = true;
                        // Exit the loop after executing the matched case
                        break;
                    }
                }
            }
        } finally {
            // Restore the original scope
            this.scope = originalScope;
        }

        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        // Simply evaluate each statement in the case.
        for (Ast.Statement stmt : ast.getStatements()) {
            visit(stmt);
        }
        return Environment.NIL;
    }


//    @Override
//    public Environment.PlcObject visit(Ast.Statement.While ast) {
//        throw new UnsupportedOperationException(); //TODO (in lecture)
//    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        // Continuously evaluate the condition and execute the block while the condition is true
        while (requireType(Boolean.class, visit(ast.getCondition()).getValue())) {
            // Execute each statement in the block
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        }
        // The while statement does not produce a value, so return NIL
        return Environment.NIL;
    }




//    @Override
//    public Environment.PlcObject visit(Ast.Statement.Return ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        // Evaluate the return value expression
        Environment.PlcObject value = visit(ast.getValue());

        // Throw the value inside a new Return exception
        throw new Return(value);
    }


//    @Override
//    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL; // Explicitly return Environment.NIL for null literals
        }
        return Environment.create(ast.getLiteral());
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        // Simply evaluate the contained expression and return its value
        return visit(ast.getExpression());
    }


//    @Override
//    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
//        throw new UnsupportedOperationException(); //TODO

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        switch (ast.getOperator()) {
            case "&&":
                boolean leftValueAnd = requireType(Boolean.class, visit(ast.getLeft()).getValue());
                // Short-circuit evaluation for AND.
                if (!leftValueAnd) {
                    return Environment.create(false);
                }
                boolean rightValueAnd = requireType(Boolean.class, visit(ast.getRight()).getValue());
                return Environment.create(leftValueAnd && rightValueAnd);

            case "||":
                boolean leftValueOr = requireType(Boolean.class, visit(ast.getLeft()).getValue());
                // Short-circuit evaluation for OR.
                if (leftValueOr) {
                    return Environment.create(true);
                }
                boolean rightValueOr = requireType(Boolean.class, visit(ast.getRight()).getValue());
                return Environment.create(leftValueOr || rightValueOr);

            case "<":  // The same logic applies to other comparison operators like ">", "<=", ">=".
                Comparable leftValueComp = requireType(Comparable.class, visit(ast.getLeft()).getValue());
                Comparable rightValueComp = requireType(Comparable.class, visit(ast.getRight()).getValue());
                return Environment.create(leftValueComp.compareTo(rightValueComp) < 0);
            case ">":
                Comparable<?> leftComparable = requireType(Comparable.class, visit(ast.getLeft()).getValue());
                Comparable<?> rightComparable = requireType(Comparable.class, visit(ast.getRight()).getValue());
                if (rightComparable.getClass().isAssignableFrom(leftComparable.getClass())) {
                    @SuppressWarnings("unchecked")
                    int comparisonResult = ((Comparable) leftComparable).compareTo(rightComparable);
                    return Environment.create(comparisonResult > 0);
                } else {
                    throw new RuntimeException("Incomparable types for '>' operator.");
                }
            case "==":
                Object leftValueEq = visit(ast.getLeft()).getValue();
                Object rightValueEq = visit(ast.getRight()).getValue();
                return Environment.create(Objects.equals(leftValueEq, rightValueEq));

            case "!=":
                Object leftValueNeq = visit(ast.getLeft()).getValue();
                Object rightValueNeq = visit(ast.getRight()).getValue();
                return Environment.create(!Objects.equals(leftValueNeq, rightValueNeq));

            case "+":
                Object leftValueAdd = visit(ast.getLeft()).getValue();
                Object rightValueAdd = visit(ast.getRight()).getValue();
                if (leftValueAdd instanceof String || rightValueAdd instanceof String) {
                    return Environment.create(leftValueAdd.toString() + rightValueAdd.toString());
                } else if (leftValueAdd instanceof BigInteger && rightValueAdd instanceof BigInteger) {
                    return Environment.create(((BigInteger) leftValueAdd).add((BigInteger) rightValueAdd));
                } else {
                    throw new RuntimeException("Incompatible types for addition.");
                }

            case "-":
                BigInteger leftValueSub = requireType(BigInteger.class, visit(ast.getLeft()).getValue());
                BigInteger rightValueSub = requireType(BigInteger.class, visit(ast.getRight()).getValue());
                return Environment.create(leftValueSub.subtract(rightValueSub));

            case "*":
                BigInteger leftValueMul = requireType(BigInteger.class, visit(ast.getLeft()).getValue());
                BigInteger rightValueMul = requireType(BigInteger.class, visit(ast.getRight()).getValue());
                return Environment.create(leftValueMul.multiply(rightValueMul));

            case "/":
                Object leftValueDiv = visit(ast.getLeft()).getValue();
                Object rightValueDiv = visit(ast.getRight()).getValue();
                if (rightValueDiv.equals(BigInteger.ZERO) || (rightValueDiv instanceof BigDecimal && BigDecimal.ZERO.equals(rightValueDiv))) {
                    throw new RuntimeException("Division by zero.");
                }
                if (leftValueDiv instanceof BigInteger && rightValueDiv instanceof BigInteger) {
                    // Convert BigInteger to BigDecimal for division
                    BigDecimal leftDecimal = new BigDecimal((BigInteger) leftValueDiv);
                    BigDecimal rightDecimal = new BigDecimal((BigInteger) rightValueDiv);
                    return Environment.create(leftDecimal.divide(rightDecimal, RoundingMode.HALF_EVEN));
                } else if (leftValueDiv instanceof BigDecimal && rightValueDiv instanceof BigDecimal) {
                    // Directly divide BigDecimal values
                    return Environment.create(((BigDecimal) leftValueDiv).divide((BigDecimal) rightValueDiv, RoundingMode.HALF_EVEN));
                } else {
                    throw new RuntimeException("Incompatible types for division.");
                }

            case "^":
                BigInteger leftValueExp = requireType(BigInteger.class, visit(ast.getLeft()).getValue());
                BigInteger rightValueExp = requireType(BigInteger.class, visit(ast.getRight()).getValue());
                return Environment.create(leftValueExp.pow(rightValueExp.intValueExact()));

            default:
                throw new RuntimeException("Unsupported binary operator: " + ast.getOperator());
        }
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent()) {
            // Assuming you've already got the listVariable similar to before
            Environment.Variable listVariable = scope.lookupVariable(ast.getName());
            List<Object> list = requireType(List.class, listVariable.getValue().getValue());
            // Evaluate the offset to get the index
            BigInteger index = requireType(BigInteger.class, visit(ast.getOffset().get()).getValue());
            // Access the element in the list
            Object element = list.get(index.intValueExact());
            // Wrap the element in a PlcObject before returning
            return Environment.create(element);
        } else {
            // Access a non-list variable, presumably already handled correctly
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }



    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        List<Environment.PlcObject> evaluatedArgs = new ArrayList<>();

        for (Ast.Expression argument : ast.getArguments()) {
            evaluatedArgs.add(visit(argument));
        }

        return function.invoke(evaluatedArgs);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Object> list = new ArrayList<>();
        for (Ast.Expression value : ast.getValues()) {
            Environment.PlcObject plcValue = visit(value);
            // Extract the raw value from the PlcObject and add it to the list
            list.add(plcValue.getValue());
        }
        // Return a new PlcObject containing the list of raw values
        return Environment.create(list);
    }



    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
