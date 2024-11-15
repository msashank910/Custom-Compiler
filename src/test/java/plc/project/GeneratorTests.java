package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import plc.project.Ast.Statement.While;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class GeneratorTests {

    @Test
    public void testFunctionWithParametersAndPrintStatements() {
        // Define the function name
        String functionName = "func";

        // Define the parameters as a list of strings
        List<String> parameterNames = Arrays.asList("x", "y", "z");

        // Define the return type as Optional.empty() since the function does not return a value
        Optional<String> returnType = Optional.empty();

        // Define the parameter types, assuming there is a way in your environment to get these types by string name
        List<String> parameterTypes = Arrays.asList("Integer", "Decimal", "String");

        // Define the statements inside the function
        List<Ast.Statement> statements = Arrays.asList(
                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "x")))),
                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "y")))),
                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "z"))))
        );

        // Create the function AST node using the correct constructor signature
        Ast.Function function = init(new Ast.Function(functionName, parameterNames, parameterTypes, returnType, statements), ast -> {});

        // The expected Java code that the AST should generate
        String expected = String.join(System.lineSeparator(),
                "Void func(int x, double y, String z) {",
                "    System.out.println(x);",
                "    System.out.println(y);",
                "    System.out.println(z);",
                "}"
        );

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        test(function, expected);
    }

// Define the 'test' and 'init' helper methods as per the initial testing framework setup


    @Test
    public void testIntegerListInitialization() {
        // Define the elements of the list
        Ast.Expression.Literal expr1 = new Ast.Expression.Literal(BigInteger.valueOf(1));
        Ast.Expression.Literal expr2 = new Ast.Expression.Literal(BigInteger.valueOf(2));
        Ast.Expression.Literal expr3 = new Ast.Expression.Literal(BigInteger.valueOf(3));

        // Set the type for each expression to Integer
        expr1.setType(Environment.Type.INTEGER);
        expr2.setType(Environment.Type.INTEGER);
        expr3.setType(Environment.Type.INTEGER);

        // Define the global variable for the list, initializing it with the list of expressions
        Ast.Global global = new Ast.Global(
                "list",     // Name of the variable
                "Integer",  // Type of the list elements
                true,       // Indicates if the variable is mutable
                Optional.of(new Ast.Expression.PlcList(Arrays.asList(expr1, expr2, expr3))) // Initialization
        );

        // Use the 'init' helper method to set up the variable with an environment setting if needed
        Ast.Global astList = init(global, ast -> ast.setVariable(new Environment.Variable(
                "list", "list", Environment.Type.INTEGER, true, Environment.create(Arrays.asList(1, 2, 3)))
        ));

        // The expected Java code that the AST should generate
        String expected = "int[] list = {1, 2, 3};";

        // Call the test helper method to compile the AST and check if the generated code matches 'expected'
        test(astList, expected);
    }

    @Test
    public void testSumCalculationFunction() {
        // Define the AST for the main method in a Main class that calculates the sum
        Ast.Source source = new Ast.Source(
                Arrays.asList(
                        // Assuming that 'i' and 'sum' are class-level fields and not local variables
                        new Ast.Global("i", true, Optional.empty()),
                        new Ast.Global("sum", true, Optional.empty())
                ),
                Arrays.asList(
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "i"),
                                        new Ast.Expression.Literal(BigInteger.ONE)
                                ),
                                new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "sum"),
                                        new Ast.Expression.Literal(BigInteger.ZERO)
                                ),
                                new While(
                                        new Ast.Expression.Binary("<",
                                                new Ast.Expression.Access(Optional.empty(), "i"),
                                                new Ast.Expression.Literal(BigInteger.valueOf(50))
                                        ),
                                        Arrays.asList(
                                                new Ast.Statement.Assignment(
                                                        new Ast.Expression.Access(Optional.empty(), "sum"),
                                                        new Ast.Expression.Binary("+",
                                                                new Ast.Expression.Access(Optional.empty(), "sum"),
                                                                new Ast.Expression.Access(Optional.empty(), "i")
                                                        )
                                                ),
                                                new Ast.Statement.Assignment(
                                                        new Ast.Expression.Access(Optional.empty(), "i"),
                                                        new Ast.Expression.Binary("+",
                                                                new Ast.Expression.Access(Optional.empty(), "i"),
                                                                new Ast.Expression.Literal(BigInteger.ONE)
                                                        )
                                                )
                                        )
                                ),
                                new Ast.Statement.Expression(
                                        new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "sum")))
                                ),
                                new Ast.Statement.Return(
                                        new Ast.Expression.Literal(BigInteger.ZERO)
                                )
                        ))
                )
        );

        // The expected Java code that the AST should generate
        String expected = String.join(System.lineSeparator(),
                "public class Main {",
                "    ",
                "    int sum;",
                "    int i;",
                "",
                "    public static void main(String[] args) {",
                "        System.exit(new Main().main());",
                "    }",
                "",
                "    int main() {",
                "        i = 1;",
                "        sum = 0;",
                "        while (i < 50) {",
                "            sum = sum + i;",
                "            i = i + 1;",
                "        }",
                "        System.out.println(sum);",
                "        return 0;", // Assuming a return is required from the main method.
                "    }",
                "}"
        );

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(source);
        Assertions.assertEquals(expected, writer.toString().trim());
    }

// ...include all necessary mock classes, helper methods, and enums here...


        @Test
    public void testListWithSpecificSize() {
        // Assuming 'Ast.Expression.Access' directly handles array indices if an offset is provided.
        Ast.Expression arrayName = new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(5)), "lName");

        // The expected Java code that the AST should generate
        String expected = "lName[5]";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(arrayName);
        Assertions.assertEquals(expected, writer.toString().trim());
    }


    @Test
    public void testVariableName() {
        // Define the AST for a variable access
        Ast.Expression variableAccess = new Ast.Expression.Access(Optional.empty(), "name");

        // The expected Java code that the AST should generate
        String expected = "name";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(variableAccess);
        Assertions.assertEquals(expected, writer.toString().trim());
    }
    @Test
    public void testNilLiteral() {
        // Define the AST for a Nil literal
        Ast.Expression.Literal nilLiteral = init(new Ast.Expression.Literal(null), ast -> ast.setType(Environment.Type.NIL));

        // The expected Java code that the AST should generate
        String expected = "null";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(nilLiteral);
        Assertions.assertEquals(expected, writer.toString().trim());
    }


    @Test
    public void testWhileLoopWithComparisonCondition() {
        // Define the AST for a while loop with a comparison condition
        While whileLoop = init(new While(
                init(new Ast.Expression.Binary("<",
                        init(new Ast.Expression.Access(Optional.empty(), "num"), ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, true, Environment.NIL))),
                        init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function("function", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "num")
                        )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL))))
                )
        ), ast -> {});

        // The expected Java code that the AST should generate
        String expected = "while (num < 10) {\n" +
                "    function(num);\n" +
                "}";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(whileLoop);
        Assertions.assertEquals(expected, writer.toString().trim());
    }



     @Test
    public void testEmptyWhileLoop() {
        // Define the AST for an empty while loop
        While whileLoop = init(new While(
                init(new Ast.Expression.Access(Optional.empty(), "cond"), ast -> ast.setVariable(new Environment.Variable("cond", "cond", Environment.Type.BOOLEAN, true, Environment.NIL))),
                Collections.emptyList() // No statements in the loop body
        ), ast -> {});

        // The expected Java code that the AST should generate
        String expected = "while (cond) {}";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(whileLoop);
        Assertions.assertEquals(expected, writer.toString().trim());
    }

    @Test
    public void testNestedWhileLoopsWithFunctionCall() {
        // Define the AST for the nested while loops
        While innerWhileLoop = init(new While(
                init(new Ast.Expression.Access(Optional.empty(), "cond2"), ast -> ast.setVariable(new Environment.Variable("cond2", "cond2", Environment.Type.BOOLEAN, true, Environment.NIL))),
                Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function("function", Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL))))
                )
        ), ast -> {});

        While outerWhileLoop = init(new While(
                init(new Ast.Expression.Access(Optional.empty(), "cond1"), ast -> ast.setVariable(new Environment.Variable("cond1", "cond1", Environment.Type.BOOLEAN, true, Environment.NIL))),
                Arrays.asList(innerWhileLoop)
        ), ast -> {});

        // The expected Java code that the AST should generate
        String expected = "while (cond1) {\n" +
                "    while (cond2) {\n" +
                "        function(1);\n" +
                "    }\n" +
                "}";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(outerWhileLoop);
        Assertions.assertEquals(expected, writer.toString().trim());
    }


    @Test
    public void testWhileLoopWithMultipleFunctionCalls() {
        // Define the AST for a while loop with multiple function calls
        While whileLoop = init(new While(
                init(new Ast.Expression.Access(Optional.empty(), "cond"), ast -> ast.setVariable(new Environment.Variable("cond", "cond", Environment.Type.BOOLEAN, true, Environment.NIL))),
                Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function("function", Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL)))),
                        new Ast.Statement.Expression(init(new Ast.Expression.Function("function", Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.valueOf(2)), ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL)))),
                        new Ast.Statement.Expression(init(new Ast.Expression.Function("function", Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.valueOf(3)), ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL))))
                )
        ), ast -> {});

        // The expected Java code that the AST should generate
        String expected = "while (cond) {\n" +
                "    function(1);\n" +
                "    function(2);\n" +
                "    function(3);\n" +
                "}";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(whileLoop);
        Assertions.assertEquals(expected, writer.toString().trim());
    }


    @Test
    public void testWhileLoop() {
        // Define the AST for a while loop with a condition and a function call within the loop body
        While whileLoop = init(new While(
                init(new Ast.Expression.Access(Optional.empty(), "cond"), ast -> ast.setVariable(new Environment.Variable("cond", "cond", Environment.Type.BOOLEAN, true, Environment.NIL))),
                Arrays.asList(
                        new Ast.Statement.Expression(
                                init(new Ast.Expression.Function("function", Arrays.asList(init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)))),
                                        ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL)))
                        )
                )
        ), ast -> {});

        // The expected Java code that the AST should generate
        String expected = "while (cond) {\n" +
                "    function(1);\n" +
                "}";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(whileLoop);
        Assertions.assertEquals(expected, writer.toString().trim());
    }



    @Test
    public void testSimpleVariableAssignment() {
        // Define the AST for a simple assignment statement where a variable 'name' is assigned the integer value 1
        Ast.Statement.Assignment assignment = init(new Ast.Statement.Assignment(
                init(new Ast.Expression.Access(Optional.empty(), "name"), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL))),
                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
        ), ast -> {});

        // The expected Java code that the AST should generate
        String expected = "name = 1;";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        test(assignment, expected);
    }


    @Test
    public void testStringVariableInitialization() {
        // Define the AST for a declaration statement where a variable 'str' is initialized with the string "string"
        Ast.Statement.Declaration declaration = init(new Ast.Statement.Declaration("str", Optional.empty(), Optional.of(
                init(new Ast.Expression.Literal("string"), ast -> ast.setType(Environment.Type.STRING))
        )), ast -> ast.setVariable(new Environment.Variable("str", "str", Environment.Type.STRING, true, Environment.create("string"))));

        // The expected Java code that the AST should generate
        String expected = "String str = \"string\";";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        test(declaration, expected);
    }



    @Test
    public void testFunctionCall() {
        // Define the AST for a function that calls another function with an integer argument
        Ast.Function function = init(new Ast.Function("func", Collections.emptyList(), Collections.emptyList(), Optional.of("String"), Arrays.asList(
                new Ast.Statement.Expression(init(new Ast.Expression.Function("function", Arrays.asList(
                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL))))
        )), ast -> {});

        // The expected Java code that the AST should generate
        String expected = "String func() {\n    function(1);\n}";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        test(function, expected);
    }

    @Test
    public void testMultipleFunctionCalls() {
        // Define the AST for a function that calls 'function' multiple times with different integer arguments
        Ast.Function function = init(new Ast.Function("func", Collections.emptyList(), Collections.emptyList(), Optional.of("String"), Arrays.asList(
                new Ast.Statement.Expression(init(new Ast.Expression.Function("function", Arrays.asList(
                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL)))),
                new Ast.Statement.Expression(init(new Ast.Expression.Function("function", Arrays.asList(
                        init(new Ast.Expression.Literal(BigInteger.valueOf(2)), ast -> ast.setType(Environment.Type.INTEGER))
                )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL)))),
                new Ast.Statement.Expression(init(new Ast.Expression.Function("function", Arrays.asList(
                        init(new Ast.Expression.Literal(BigInteger.valueOf(3)), ast -> ast.setType(Environment.Type.INTEGER))
                )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL))))
        )), ast -> {});

        // The expected Java code that the AST should generate
        String expected = "String func() {\n" +
                "    function(1);\n" +
                "    function(2);\n" +
                "    function(3);\n" +
                "}";

        // Call the test method to compile the AST and check if the generated code matches 'expected'
        test(function, expected);
    }


// Helper methods `test` and `init` as previously defined


    @Test
    public void testEmptyFunction() {
        // Define the AST for a function that returns an empty string
        Ast.Function function = init(new Ast.Function("empty", Collections.emptyList(), Collections.emptyList(), Optional.of("String"), Collections.emptyList()),
                ast -> {
                    // Your logic for setting up the function, e.g., setting the return type to String
                });

        // The expected Java code that the AST should generate
        String expected = "String empty() {}";

        // Here should be the implementation of the 'test' method that compiles the AST
        // and checks that the generated code matches 'expected'
        test(function, expected);
    }
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, Ast.Source ast, String expected) {
        test(ast, expected);
    }




    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Hello, World!",
                        // FUN main(): Integer DO
                        //     print("Hello, World!");
                        //     RETURN 0;
                        // END
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                        new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(
                                                init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                        new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                                )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))))
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int main() {",
                                "        System.out.println(\"Hello, World!\");",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                        )
                )
        );
    }
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testVariableAndFunction(String test, Ast.Source ast, String expected) { // AKSHAT WROTE THIS
        test(ast, expected);
    }

    private static Stream<Arguments> testVariableAndFunction() {
        return Stream.of(
                Arguments.of("Variable and Function Test",
                        // VAR x: Integer;
                        // FUN main(): Integer DO
                        //     RETURN -1;
                        // END
                        new Ast.Source(
                                Arrays.asList(
                                        // Assuming the variable 'x' is immutable, hence 'false' is passed for the 'mutable' argument.
                                        // Replace 'false' with 'true' if the variable should be mutable.
                                        new Ast.Global("x", false, Optional.empty())
                                ),
                                Arrays.asList(
                                        init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                init(new Ast.Statement.Return(
                                                        init(new Ast.Expression.Literal(BigInteger.valueOf(-1)), ast -> ast.setType(Environment.Type.INTEGER))
                                                ), ast -> {})
                                        )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    final Any x;", // Assuming 'x' is private and not set, since no value is provided.
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int main() {",
                                "        return -1;",
                                "    }",
                                "",
                                "}"
                        )
                )
        );
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMultipleGlobalFunctions(String test, Ast.Source ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testMultipleGlobalFunctions() {
        return Stream.of(
                Arguments.of("Multiple Variables and Functions Test",
                        // VAR x: Integer;
                        // VAR y: Decimal;
                        // VAR z: String;
                        // FUN f(): Integer DO RETURN x; END
                        // FUN g(): Decimal DO RETURN y; END
                        // FUN h(): String DO RETURN z; END


                        // FUN main(): Integer DO RETURN -1; END
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Global("x", false, Optional.empty()),
                                        new Ast.Global("Y", false, Optional.empty()),
                                        new Ast.Global("Z", false, Optional.empty())
                                ),
                                Arrays.asList(
                                        init(new Ast.Function("f", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))

                                        )), ast -> ast.setFunction(new Environment.Function("f", "f", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))),
                                        init(new Ast.Function("g", Arrays.asList(), Arrays.asList(), Optional.of("Decimal"), Arrays.asList(
                                                new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                                        )), ast -> ast.setFunction(new Environment.Function("g", "g", Arrays.asList(), Environment.Type.DECIMAL, args -> Environment.NIL))),
                                        init(new Ast.Function("h", Arrays.asList(), Arrays.asList(), Optional.of("String"), Arrays.asList(
                                                new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                                        )), ast -> ast.setFunction(new Environment.Function("h", "h", Arrays.asList(), Environment.Type.STRING,args -> Environment.NIL))),
                                        init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.valueOf(-1)), ast -> ast.setType(Environment.Type.INTEGER)))
                                        )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    private int x;",
                                "    private BigDecimal y;",
                                "    private String z;",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int f() {",
                                "        return x;",
                                "    }",
                                "",
                                "    BigDecimal g() {",
                                "        return y;",
                                "    }",
                                "",
                                "    String h() {",
                                "        return z;",
                                "    }",
                                "",
                                "    int main() {",
                                "        return -1;",
                                "    }",
                                "",
                                "}"
                        )
                )
        );
    }

    @Test
    public void testStringListOneElement() {
        // LIST strs: String = ["abc"];
        Ast.Expression.Literal expr1 = new Ast.Expression.Literal("abc");
        expr1.setType(Environment.Type.STRING);

        Ast.Global global = new Ast.Global("strs", "String", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(expr1))));
        Ast.Global astList = init(global, ast -> ast.setVariable(new Environment.Variable("strs", "strs", Environment.Type.STRING, true, Environment.create(Arrays.asList("abc")))));

        String expected = new String("String[] strs = {\"abc\"};");
        test(astList, expected);
    }

    @Test
    public void testStringListMultipleElements() {
        // LIST strs: String = ["abc", "efg", "123"];
        Ast.Expression.Literal expr1 = new Ast.Expression.Literal("abc");
        Ast.Expression.Literal expr2 = new Ast.Expression.Literal("efg");
        Ast.Expression.Literal expr3 = new Ast.Expression.Literal("123");
        expr1.setType(Environment.Type.STRING);
        expr2.setType(Environment.Type.STRING);
        expr3.setType(Environment.Type.STRING);

        Ast.Global global = new Ast.Global("strs", "String", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(expr1, expr2, expr3))));
        Ast.Global astList = init(global, ast -> ast.setVariable(new Environment.Variable("strs", "strs", Environment.Type.STRING, true, Environment.create(Arrays.asList("abc", "efg", "123")))));

        String expected = new String("String[] strs = {\"abc\", \"efg\", \"123\"};");
        test(astList, expected);
    }




    @Test
    void testList() {
        // LIST list: Decimal = [1.0, 1.5, 2.0];
        Ast.Expression.Literal expr1 = new Ast.Expression.Literal(new BigDecimal("1.0"));
        Ast.Expression.Literal expr2 = new Ast.Expression.Literal(new BigDecimal("1.5"));
        Ast.Expression.Literal expr3 = new Ast.Expression.Literal(new BigDecimal("2.0"));
        expr1.setType(Environment.Type.DECIMAL);
        expr2.setType(Environment.Type.DECIMAL);
        expr3.setType(Environment.Type.DECIMAL);

        Ast.Global global = new Ast.Global("list", "Decimal", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(expr1, expr2, expr3))));
        Ast.Global astList = init(global, ast -> ast.setVariable(new Environment.Variable("list", "list", Environment.Type.DECIMAL, true, Environment.create(Arrays.asList(new Double(1.0), new Double(1.5), new Double(2.0))))));

        String expected = new String("double[] list = {1.0, 1.5, 2.0};");
        test(astList, expected);
    }

    @Test
    void testIntList() {
        // LIST list: Decimal = [1.0, 1.5, 2.0];
        Ast.Expression.Literal expr1 = new Ast.Expression.Literal(Integer.valueOf("1"));
        Ast.Expression.Literal expr2 = new Ast.Expression.Literal(Integer.valueOf("2"));
        Ast.Expression.Literal expr3 = new Ast.Expression.Literal(Integer.valueOf("5"));
        expr1.setType(Environment.Type.INTEGER);
        expr2.setType(Environment.Type.INTEGER);
        expr3.setType(Environment.Type.INTEGER);

        Ast.Global global = new Ast.Global("list", "Integer", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(expr1, expr2, expr3))));
        Ast.Global astList = init(global, ast -> ast.setVariable(new Environment.Variable("list", "list", Environment.Type.INTEGER, true, Environment.create(Arrays.asList(Integer.valueOf("1"), Integer.valueOf("2"), Integer.valueOf("5"))))));

        String expected = new String("int[] list = {1, 2, 5};");
        test(astList, expected);
    }



    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationJVM(String test, Ast.Statement.Declaration ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testDeclarationJVM() {
        return Stream.of(
                Arguments.of("Simple Type Declaration",
                        // VAR name: Type;
                        init(new Ast.Statement.Declaration("name", Optional.of("Type"), Optional.empty()),
                                ast -> ast.setVariable(new Environment.Variable("name", "name",Environment.Type.DECIMAL, true, Environment.NIL))),
                        "Type name;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL))),
                        "int name;"
                ),
                Arguments.of("Initialization",
                        // LET name = 1.0;
                        init(new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                                init(new Ast.Expression.Literal(new BigDecimal("1.0")),ast -> ast.setType(Environment.Type.DECIMAL))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, true, Environment.NIL))),
                        "double name = 1.0;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        // IF expr DO
                        //     stmt;
                        // END
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt"), ast -> ast.setVariable(new Environment.Variable("stmt", "stmt", Environment.Type.NIL, true, Environment.NIL))))),
                                Arrays.asList()
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt;",
                                "}"
                        )
                ),
                Arguments.of("Else",
                        // IF expr DO
                        //     stmt1;
                        // ELSE
                        //     stmt2;
                        // END
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt1"), ast -> ast.setVariable(new Environment.Variable("stmt1", "stmt1", Environment.Type.NIL, true, Environment.NIL))))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt2"), ast -> ast.setVariable(new Environment.Variable("stmt2", "stmt2", Environment.Type.NIL, true, Environment.NIL)))))
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt1;",
                                "} else {",
                                "    stmt2;",
                                "}"
                        )
                )
        );
    }



    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSwitchStatement(String test, Ast.Statement.Switch ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testSwitchStatement() {
        return Stream.of(
                Arguments.of("Switch",
                        // SWITCH letter
                        //     CASE 'y':
                        //         print("yes");
                        //         letter = 'n';
                        //         break;
                        //     DEFAULT
                        //         print("no");
                        // END
                        new Ast.Statement.Switch(
                                init(new Ast.Expression.Access(Optional.empty(), "letter"), ast -> ast.setVariable(new Environment.Variable("letter", "letter", Environment.Type.CHARACTER, true, Environment.create('y')))),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(init(new Ast.Expression.Literal('y'), ast -> ast.setType(Environment.Type.CHARACTER))),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function("print", Arrays.asList(init(new Ast.Expression.Literal("yes"), ast -> ast.setType(Environment.Type.STRING)))),
                                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))
                                                                )
                                                        ),
                                                        new Ast.Statement.Assignment(
                                                                init(new Ast.Expression.Access(Optional.empty(), "letter"), ast -> ast.setVariable(new Environment.Variable("letter", "letter", Environment.Type.CHARACTER, true, Environment.create('y')))),
                                                                init(new Ast.Expression.Literal('n'), ast -> ast.setType(Environment.Type.CHARACTER))
                                                        )
                                                )
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function("print", Arrays.asList(init(new Ast.Expression.Literal("no"), ast -> ast.setType(Environment.Type.STRING)))),
                                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "switch (letter) {",
                                "    case 'y':",
                                "        System.out.println(\"yes\");",
                                "        letter = 'n';",
                                "        break;",
                                "    default:",
                                "        System.out.println(\"no\");",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpression(String test, Ast.Expression.Binary ast, String expected) {
        test(ast, expected);
    }
    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        // TRUE && FALSE
                        init(new Ast.Expression.Binary("&&",
                                init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                init(new Ast.Expression.Literal(false), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "true && false"
                ),
                Arguments.of("Concatenation",
                        // "Ben" + 10
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal("Ben"), ast -> ast.setType(Environment.Type.STRING)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.STRING)),
                        "\"Ben\" + 10"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpression(String test, Ast.Expression.Function ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Print",
                        // print("Hello, World!")
                        init(new Ast.Expression.Function("print", Arrays.asList(
                                init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))),
                        "System.out.println(\"Hello, World!\")"
                )
        );
    }

    /**
     * Helper function for tests, using a StringWriter as the output stream.
     */
    private static void test(Ast ast, String expected) {
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(ast);
        Assertions.assertEquals(expected, writer.toString());
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}
