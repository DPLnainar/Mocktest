package com.examportal.parser.service.impl;

import com.examportal.parser.model.VerificationResult;
import com.examportal.parser.model.VerificationRule;
import com.examportal.parser.service.ParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Python Parser Service
 * 
 * Uses pattern matching for Python code verification
 * 
 * Detects:
 * - Built-in sort methods (sorted(), list.sort())
 * - List comprehensions
 * - For loops vs recursion
 * - Specific function calls
 */
@Service
public class PythonParserService implements ParserService {

    private static final Logger log = LoggerFactory.getLogger(PythonParserService.class);

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
        log.debug("Python code verification completed in {}ms. Passed: {}", 
                result.getParsingTimeMs(), result.isPassed());

        return result;
    }

    private void checkRule(String code, String[] lines, VerificationRule rule, VerificationResult result) {
        String construct = rule.getConstruct().toLowerCase();

        switch (construct) {
            case "sorted":
            case "sort":
                checkForbiddenSort(code, lines, rule, result);
                break;

            case "list_comprehension":
                checkForbiddenConstruct(code, lines, rule, 
                    Pattern.compile("\\[[^\\]]*for\\s+\\w+\\s+in[^\\]]*\\]"), 
                    "list comprehension", result);
                break;

            case "for_loop":
            case "for loop":
                checkForbiddenConstruct(code, lines, rule, 
                    Pattern.compile("\\bfor\\s+\\w+\\s+in\\b"), 
                    "for loop", result);
                break;

            case "while_loop":
            case "while loop":
                checkForbiddenConstruct(code, lines, rule, 
                    Pattern.compile("\\bwhile\\s+"), 
                    "while loop", result);
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
                    checkForbiddenPattern(code, lines, rule, pattern, result);
                } else {
                    checkRequiredPattern(code, lines, rule, pattern, result);
                }
        }
    }

    private void checkForbiddenSort(String code, String[] lines, VerificationRule rule, VerificationResult result) {
        Pattern sortedPattern = Pattern.compile("\\bsorted\\s*\\(");
        Pattern sortMethodPattern = Pattern.compile("\\.sort\\s*\\(");

        java.util.regex.Matcher matcher = sortedPattern.matcher(code);
        if (matcher.find()) {
            addViolation(code, lines, rule, matcher.start(), 
                "Forbidden: sorted() function detected", result);
            return;
        }

        matcher = sortMethodPattern.matcher(code);
        if (matcher.find()) {
            addViolation(code, lines, rule, matcher.start(), 
                "Forbidden: .sort() method detected", result);
        }
    }

    private void checkForbiddenConstruct(String code, String[] lines, VerificationRule rule, 
                                        Pattern pattern, String constructName, VerificationResult result) {
        java.util.regex.Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            addViolation(code, lines, rule, matcher.start(), 
                String.format("Forbidden: %s detected. %s", constructName, rule.getErrorMessage()), 
                result);
        }
    }

    private void checkRequiredRecursion(String code, String[] lines, VerificationRule rule, VerificationResult result) {
        // Check if any function calls itself
        Pattern funcPattern = Pattern.compile("def\\s+(\\w+)\\s*\\([^)]*\\):");
        java.util.regex.Matcher funcMatcher = funcPattern.matcher(code);

        boolean hasRecursion = false;
        while (funcMatcher.find()) {
            String funcName = funcMatcher.group(1);
            int funcStart = funcMatcher.end();
            
            // Find function end (simplified - looks for next def or end of file)
            int funcEnd = code.indexOf("\ndef ", funcStart);
            if (funcEnd == -1) funcEnd = code.length();

            String funcBody = code.substring(funcStart, funcEnd);
            Pattern callPattern = Pattern.compile("\\b" + funcName + "\\s*\\(");
            if (callPattern.matcher(funcBody).find()) {
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
        // Check for nested loops
        Pattern nestedLoopPattern = Pattern.compile("for\\s+\\w+\\s+in[^:]+:[^f]*for\\s+\\w+\\s+in", Pattern.DOTALL);
        
        boolean hasNestedLoops = nestedLoopPattern.matcher(code).find();
        boolean hasSwapping = Pattern.compile("\\w+\\s*,\\s*\\w+\\s*=\\s*\\w+\\s*,\\s*\\w+").matcher(code).find() ||
                            code.contains("temp");

        if (!hasNestedLoops || !hasSwapping) {
            result.getViolations().add(VerificationResult.Violation.builder()
                    .rule(rule)
                    .lineNumber(1)
                    .codeSnippet("(entire code)")
                    .message("Required: Solution must implement Bubble Sort logic (nested loops with swapping)")
                    .build());
        }
    }

    private void checkForbiddenPattern(String code, String[] lines, VerificationRule rule, 
                                      Pattern pattern, VerificationResult result) {
        java.util.regex.Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            addViolation(code, lines, rule, matcher.start(), 
                rule.getErrorMessage() != null ? rule.getErrorMessage() : 
                "Forbidden construct found: " + rule.getConstruct(), 
                result);
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

    private void addViolation(String code, String[] lines, VerificationRule rule, 
                             int position, String message, VerificationResult result) {
        int lineNumber = getLineNumber(code, position);
        String snippet = lines[lineNumber - 1].trim();

        result.getViolations().add(VerificationResult.Violation.builder()
                .rule(rule)
                .lineNumber(lineNumber)
                .codeSnippet(snippet)
                .message(message)
                .build());
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
            // Basic Python syntax checks
            int indentLevel = 0;
            String[] lines = code.split("\n");

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                // Check for basic indentation consistency
                int leadingSpaces = line.length() - line.stripLeading().length();
                
                // Check for mismatched parentheses, brackets, braces
                int parenCount = 0, bracketCount = 0, braceCount = 0;
                for (char c : line.toCharArray()) {
                    switch (c) {
                        case '(': parenCount++; break;
                        case ')': parenCount--; break;
                        case '[': bracketCount++; break;
                        case ']': bracketCount--; break;
                        case '{': braceCount++; break;
                        case '}': braceCount--; break;
                    }
                }
                
                if (parenCount < 0 || bracketCount < 0 || braceCount < 0) {
                    return true; // Syntax error
                }
            }

            return false;
        } catch (Exception e) {
            log.error("Error checking Python syntax", e);
            return true;
        }
    }

    @Override
    public String getSupportedLanguage() {
        return "PYTHON";
    }
}
