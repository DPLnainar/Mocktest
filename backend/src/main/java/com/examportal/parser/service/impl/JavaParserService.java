package com.examportal.parser.service.impl;

import com.examportal.parser.model.VerificationResult;
import com.examportal.parser.model.VerificationRule;
import com.examportal.parser.service.ParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.antlr.v4.runtime.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Java Parser Service
 * 
 * Uses simplified pattern matching for initial implementation
 * Can be enhanced with full ANTLR grammar later
 * 
 * Detects:
 * - Built-in sort methods (Arrays.sort, Collections.sort)
 * - Stream API usage
 * - For loops vs recursion
 * - Specific method calls
 */
@Service
public class JavaParserService implements ParserService {

    private static final Logger log = LoggerFactory.getLogger(JavaParserService.class);

    @Override
    public VerificationResult verifyCode(String code, List<VerificationRule> rules) {
        long startTime = System.currentTimeMillis();

        VerificationResult result = new VerificationResult();
        result.setPassed(true);
        result.setViolations(new ArrayList<>());

        // Check for syntax errors first
        if (hasSyntaxErrors(code)) {
            result.setHasSyntaxErrors(true);
            result.setSyntaxErrorMessage("Code contains syntax errors");
            result.setPassed(false);
            result.setParsingTimeMs(System.currentTimeMillis() - startTime);
            return result;
        }

        // Split code into lines for line number reporting
        String[] lines = code.split("\n");

        // Apply each verification rule
        for (VerificationRule rule : rules) {
            checkRule(code, lines, rule, result);
        }

        result.setParsingTimeMs(System.currentTimeMillis() - startTime);
        log.debug("Code verification completed in {}ms. Passed: {}", 
                result.getParsingTimeMs(), result.isPassed());

        return result;
    }

    private void checkRule(String code, String[] lines, VerificationRule rule, VerificationResult result) {
        String construct = rule.getConstruct().toLowerCase();

        switch (construct) {
            case "arrays.sort":
            case "collections.sort":
                checkForbiddenMethod(code, lines, rule, Pattern.compile("(Arrays|Collections)\\.sort\\s*\\("), result);
                break;

            case "stream":
            case "streams":
                checkForbiddenMethod(code, lines, rule, Pattern.compile("\\.stream\\s*\\("), result);
                break;

            case "for_loop":
            case "for loop":
                checkForbiddenConstruct(code, lines, rule, Pattern.compile("\\bfor\\s*\\("), "for loop", result);
                break;

            case "while_loop":
            case "while loop":
                checkForbiddenConstruct(code, lines, rule, Pattern.compile("\\bwhile\\s*\\("), "while loop", result);
                break;

            case "recursion":
                checkRequiredRecursion(code, lines, rule, result);
                break;

            case "bubble_sort_logic":
                checkBubbleSortLogic(code, lines, rule, result);
                break;

            default:
                // Generic pattern matching
                Pattern pattern = Pattern.compile("\\b" + Pattern.quote(rule.getConstruct()) + "\\b");
                if (rule.getType() == VerificationRule.RuleType.FORBIDDEN) {
                    checkForbiddenMethod(code, lines, rule, pattern, result);
                } else {
                    checkRequiredPattern(code, lines, rule, pattern, result);
                }
        }
    }

    private void checkForbiddenMethod(String code, String[] lines, VerificationRule rule, 
                                     Pattern pattern, VerificationResult result) {
        java.util.regex.Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            int lineNumber = getLineNumber(code, matcher.start());
            String snippet = lines[lineNumber - 1].trim();

            result.getViolations().add(VerificationResult.Violation.builder()
                    .rule(rule)
                    .lineNumber(lineNumber)
                    .codeSnippet(snippet)
                    .message(rule.getErrorMessage() != null ? 
                            rule.getErrorMessage() : 
                            "Forbidden construct found: " + rule.getConstruct())
                    .build());
        }
    }

    private void checkForbiddenConstruct(String code, String[] lines, VerificationRule rule, 
                                        Pattern pattern, String constructName, VerificationResult result) {
        java.util.regex.Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            int lineNumber = getLineNumber(code, matcher.start());
            String snippet = lines[lineNumber - 1].trim();

            result.getViolations().add(VerificationResult.Violation.builder()
                    .rule(rule)
                    .lineNumber(lineNumber)
                    .codeSnippet(snippet)
                    .message(String.format("Forbidden: %s usage detected. %s", 
                            constructName, rule.getErrorMessage()))
                    .build());
        }
    }

    private void checkRequiredRecursion(String code, String[] lines, VerificationRule rule, VerificationResult result) {
        // Simple heuristic: method calls itself
        // Extract method name and check if it's called within the method
        Pattern methodPattern = Pattern.compile("(public|private|protected)?\\s+\\w+\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{");
        java.util.regex.Matcher methodMatcher = methodPattern.matcher(code);

        boolean hasRecursion = false;
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(2);
            int methodStart = methodMatcher.end();
            
            // Find method end (simplified - looks for matching brace)
            int braceCount = 1;
            int methodEnd = methodStart;
            for (int i = methodStart; i < code.length() && braceCount > 0; i++) {
                if (code.charAt(i) == '{') braceCount++;
                if (code.charAt(i) == '}') braceCount--;
                methodEnd = i;
            }

            String methodBody = code.substring(methodStart, methodEnd);
            Pattern callPattern = Pattern.compile("\\b" + methodName + "\\s*\\(");
            if (callPattern.matcher(methodBody).find()) {
                hasRecursion = true;
                break;
            }
        }

        if (!hasRecursion && rule.getType() == VerificationRule.RuleType.REQUIRED) {
            result.getViolations().add(VerificationResult.Violation.builder()
                    .rule(rule)
                    .lineNumber(1)
                    .codeSnippet("(entire code)")
                    .message(rule.getErrorMessage() != null ? 
                            rule.getErrorMessage() : 
                            "Required: Solution must use recursion")
                    .build());
        }
    }

    private void checkBubbleSortLogic(String code, String[] lines, VerificationRule rule, VerificationResult result) {
        // Check for nested loops (characteristic of bubble sort)
        Pattern nestedLoopPattern = Pattern.compile("for\\s*\\([^)]*\\)\\s*\\{[^}]*for\\s*\\([^)]*\\)", Pattern.DOTALL);
        
        boolean hasNestedLoops = nestedLoopPattern.matcher(code).find();
        boolean hasSwapping = code.contains("temp") || Pattern.compile("\\w+\\s*=\\s*\\w+;[^;]*\\w+\\s*=\\s*\\w+;").matcher(code).find();

        if (!hasNestedLoops || !hasSwapping) {
            result.getViolations().add(VerificationResult.Violation.builder()
                    .rule(rule)
                    .lineNumber(1)
                    .codeSnippet("(entire code)")
                    .message("Required: Solution must implement Bubble Sort logic (nested loops with swapping)")
                    .build());
        }
    }

    private void checkRequiredPattern(String code, String[] lines, VerificationRule rule, 
                                     Pattern pattern, VerificationResult result) {
        java.util.regex.Matcher matcher = pattern.matcher(code);
        if (!matcher.find() && rule.getType() == VerificationRule.RuleType.REQUIRED) {
            result.getViolations().add(VerificationResult.Violation.builder()
                    .rule(rule)
                    .lineNumber(1)
                    .codeSnippet("(entire code)")
                    .message(rule.getErrorMessage() != null ? 
                            rule.getErrorMessage() : 
                            "Required construct not found: " + rule.getConstruct())
                    .build());
        }
    }

    private int getLineNumber(String code, int position) {
        int lineNumber = 1;
        for (int i = 0; i < position && i < code.length(); i++) {
            if (code.charAt(i) == '\n') {
                lineNumber++;
            }
        }
        return lineNumber;
    }

    @Override
    public boolean hasSyntaxErrors(String code) {
        try {
            // Basic syntax checks
            int braceCount = 0;
            int parenCount = 0;
            int bracketCount = 0;

            for (char c : code.toCharArray()) {
                switch (c) {
                    case '{': braceCount++; break;
                    case '}': braceCount--; break;
                    case '(': parenCount++; break;
                    case ')': parenCount--; break;
                    case '[': bracketCount++; break;
                    case ']': bracketCount--; break;
                }
            }

            return braceCount != 0 || parenCount != 0 || bracketCount != 0;
        } catch (Exception e) {
            log.error("Error checking syntax", e);
            return true;
        }
    }

    @Override
    public String getSupportedLanguage() {
        return "JAVA";
    }
}
