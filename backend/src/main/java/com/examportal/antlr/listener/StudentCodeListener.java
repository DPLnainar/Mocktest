package com.examportal.antlr.listener;

import com.examportal.antlr.SimpleJavaBaseListener;
import com.examportal.antlr.SimpleJavaParser;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class StudentCodeListener extends SimpleJavaBaseListener {

    private final List<String> violations = new ArrayList<>();
    private final Map<String, Boolean> constraints;
    private String currentMethodName = null;
    private boolean recursionDetected = false;

    public StudentCodeListener() {
        this(new HashMap<>());
    }

    public StudentCodeListener(Map<String, Boolean> constraints) {
        this.constraints = constraints != null ? constraints : new HashMap<>();
    }

    public List<String> getViolations() {
        if (Boolean.TRUE.equals(constraints.get("requireRecursion")) && !recursionDetected) {
            List<String> finalViolations = new ArrayList<>(violations);
            finalViolations.add("Logic Constraint: Recursion is required but not detected.");
            return finalViolations;
        }
        return violations;
    }

    // 1. Enter Method Declaration: Track current method name for recursion check
    @Override
    public void enterMethodDeclaration(SimpleJavaParser.MethodDeclarationContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            currentMethodName = ctx.IDENTIFIER().getText();
        }
    }

    @Override
    public void exitMethodDeclaration(SimpleJavaParser.MethodDeclarationContext ctx) {
        currentMethodName = null;
    }

    // 2. Check for banned method calls (Security) & Recursion
    @Override
    public void enterMethodCall(SimpleJavaParser.MethodCallContext ctx) {
        String callText = ctx.getText(); // e.g., "System.exit(0)"

        // Security Check
        if (callText.contains("System.exit")) {
            violations.add("Security Violation: 'System.exit()' is forbidden.");
        }
        if (callText.contains("Runtime.getRuntime")) {
            violations.add("Security Violation: 'Runtime.getRuntime()' is forbidden.");
        }
        if (callText.contains("java.io.File") || callText.contains("File(") || callText.contains("FileReader")
                || callText.contains("FileWriter")) {
            violations.add("Security Violation: File I/O operations are forbidden.");
        }
        if (callText.contains("ProcessBuilder") || callText.contains("java.lang.ProcessBuilder")) {
            violations.add("Security Violation: 'ProcessBuilder' is forbidden.");
        }

        // Recursion Check
        // MethodCall context usually looks like: expression '.' IDENTIFIER '(' ...
        // We need to extract the method name being called.
        // In my grammar: expression '.' IDENTIFIER '(' expressionList? ')' # methodCall

        TerminalNode identifier = ctx.IDENTIFIER();
        if (identifier != null && currentMethodName != null) {
            String calledMethod = identifier.getText();
            if (calledMethod.equals(currentMethodName)) {
                recursionDetected = true;
                // It's a simplistic recursion check (doesn't account for overloading logic, but
                // good enough for simple constraints)
                // violations.add("Logic Check: Recursion detected in method '" +
                // currentMethodName + "'");
                // Uncomment above if you want to BAN recursion.
                // Or we can just log it if we want to 'Verify' it used recursion.
            }
        }
    }

    // 3. Loop Constraints
    @Override
    public void enterForStatement(SimpleJavaParser.ForStatementContext ctx) {
        if (Boolean.TRUE.equals(constraints.get("banLoops"))) {
            violations.add("Logic Constraint: 'for' loops are not allowed. Use recursion.");
        }
    }

    @Override
    public void enterWhileStatement(SimpleJavaParser.WhileStatementContext ctx) {
        if (Boolean.TRUE.equals(constraints.get("banLoops"))) {
            violations.add("Logic Constraint: 'while' loops are not allowed. Use recursion.");
        }
    }

    @Override
    public void enterDoWhileStatement(SimpleJavaParser.DoWhileStatementContext ctx) {
        if (Boolean.TRUE.equals(constraints.get("banLoops"))) {
            violations.add("Logic Constraint: 'do-while' loops are not allowed. Use recursion.");
        }
    }
}
