package com.examportal.service;

import com.examportal.dto.CodeVerificationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CodeVerificationService {

    public CodeVerificationResult verifyCode(String code, String language) {
        if (code == null || code.trim().isEmpty()) {
            return CodeVerificationResult.failure(List.of("Code cannot be empty"));
        }

        return switch (language.toLowerCase()) {
            case "java" -> verifyJavaCode(code);
            case "python", "python3" -> verifyPythonCode(code);
            case "c", "cpp", "c++" -> verifyCCode(code);
            default -> CodeVerificationResult.success(); // Allow other languages
        };
    }

    private CodeVerificationResult verifyJavaCode(String code) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check for class declaration
        if (!Pattern.compile("class\\s+\\w+").matcher(code).find()) {
            errors.add("Missing class declaration");
        }

        // Check for balanced braces
        if (!hasBalancedBraces(code)) {
            errors.add("Unbalanced braces { }");
        }

        // Check for balanced parentheses
        if (!hasBalancedParentheses(code)) {
            errors.add("Unbalanced parentheses ( )");
        }

        // Check for unclosed strings
        if (hasUnclosedStrings(code)) {
            errors.add("Unclosed string literal");
        }

        // Warning: Missing main method (not always required)
        if (!code.contains("main")) {
            warnings.add("No main method found - code may not execute");
        }

        // Warning: Missing semicolons (basic check)
        if (countOccurrences(code, ';') < 1) {
            warnings.add("Very few semicolons - check statement endings");
        }

        if (errors.isEmpty()) {
            return CodeVerificationResult.builder()
                    .valid(true)
                    .warnings(warnings)
                    .message("Java code validation passed")
                    .build();
        }

        return CodeVerificationResult.builder()
                .valid(false)
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    private CodeVerificationResult verifyPythonCode(String code) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check for balanced parentheses
        if (!hasBalancedParentheses(code)) {
            errors.add("Unbalanced parentheses ( )");
        }

        // Check for balanced brackets
        if (!hasBalancedBrackets(code)) {
            errors.add("Unbalanced brackets [ ]");
        }

        // Check for unclosed strings
        if (hasUnclosedStrings(code)) {
            errors.add("Unclosed string literal");
        }

        // Basic indentation check (Python is indent-sensitive)
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().endsWith(":") && i + 1 < lines.length) {
                String nextLine = lines[i + 1];
                if (!nextLine.startsWith(" ") && !nextLine.startsWith("\t") && !nextLine.trim().isEmpty()) {
                    warnings.add("Line " + (i + 2) + " may need indentation after ':'");
                    break;
                }
            }
        }

        if (errors.isEmpty()) {
            return CodeVerificationResult.builder()
                    .valid(true)
                    .warnings(warnings)
                    .message("Python code validation passed")
                    .build();
        }

        return CodeVerificationResult.failure(errors);
    }

    private CodeVerificationResult verifyCCode(String code) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check for balanced braces
        if (!hasBalancedBraces(code)) {
            errors.add("Unbalanced braces { }");
        }

        // Check for balanced parentheses
        if (!hasBalancedParentheses(code)) {
            errors.add("Unbalanced parentheses ( )");
        }

        // Check for main function
        if (!Pattern.compile("int\\s+main\\s*\\(").matcher(code).find() &&
            !Pattern.compile("void\\s+main\\s*\\(").matcher(code).find()) {
            warnings.add("No main() function found");
        }

        // Check for include statements
        if (!code.contains("#include")) {
            warnings.add("No #include statements found");
        }

        // Check for semicolons
        if (countOccurrences(code, ';') < 1) {
            warnings.add("Very few semicolons - check statement endings");
        }

        if (errors.isEmpty()) {
            return CodeVerificationResult.builder()
                    .valid(true)
                    .warnings(warnings)
                    .message("C/C++ code validation passed")
                    .build();
        }

        return CodeVerificationResult.failure(errors);
    }

    // Helper methods
    private boolean hasBalancedBraces(String code) {
        return isBalanced(code, '{', '}');
    }

    private boolean hasBalancedParentheses(String code) {
        return isBalanced(code, '(', ')');
    }

    private boolean hasBalancedBrackets(String code) {
        return isBalanced(code, '[', ']');
    }

    private boolean isBalanced(String code, char open, char close) {
        int count = 0;
        boolean inString = false;
        boolean inComment = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            // Skip strings
            if (c == '"' && (i == 0 || code.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }

            if (inString) continue;

            // Skip comments (basic)
            if (i < code.length() - 1 && code.charAt(i) == '/' && code.charAt(i + 1) == '/') {
                inComment = true;
            }
            if (c == '\n') {
                inComment = false;
            }
            if (inComment) continue;

            if (c == open) count++;
            if (c == close) count--;

            if (count < 0) return false; // More closing than opening
        }

        return count == 0;
    }

    private boolean hasUnclosedStrings(String code) {
        int doubleQuotes = 0;
        int singleQuotes = 0;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            // Skip escaped quotes
            if (i > 0 && code.charAt(i - 1) == '\\') {
                continue;
            }

            if (c == '"') doubleQuotes++;
            if (c == '\'') singleQuotes++;
        }

        return (doubleQuotes % 2 != 0) || (singleQuotes % 2 != 0);
    }

    private int countOccurrences(String str, char c) {
        int count = 0;
        for (char ch : str.toCharArray()) {
            if (ch == c) count++;
        }
        return count;
    }
}
