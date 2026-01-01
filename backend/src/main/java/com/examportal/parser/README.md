# ANTLR Logic Verification Engine

## Overview

The parser service provides **logic verification** for submitted code, ensuring students solve problems correctly without using forbidden shortcuts.

## Architecture

```
ParserFactory
├── JavaParserService   (Pattern-based verification)
├── PythonParserService (Pattern-based verification)
└── [Future] CppParserService
```

## Verification Rules

### Rule Types

1. **FORBIDDEN**: Construct must NOT be present
   - Example: `Arrays.sort()` when Bubble Sort is required
   
2. **REQUIRED**: Construct MUST be present
   - Example: Recursion when iterative solution is forbidden

### Supported Constructs

#### Java
- `arrays.sort` - Detects `Arrays.sort()`
- `collections.sort` - Detects `Collections.sort()`
- `stream` - Detects Stream API usage
- `for_loop` - Detects for loops
- `while_loop` - Detects while loops
- `recursion` - Checks for recursive method calls
- `bubble_sort_logic` - Verifies nested loops with swapping

#### Python
- `sorted` - Detects `sorted()` function
- `sort` - Detects `.sort()` method
- `list_comprehension` - Detects list comprehensions
- `for_loop` - Detects for loops
- `while_loop` - Detects while loops
- `recursion` - Checks for recursive function calls
- `bubble_sort_logic` - Verifies nested loops with swapping

## Usage Example

### API Request

```json
POST /api/parser/verify
{
  "code": "public void bubbleSort(int[] arr) { Arrays.sort(arr); }",
  "language": "JAVA",
  "rules": [
    {
      "type": "FORBIDDEN",
      "construct": "arrays.sort",
      "description": "Must implement Bubble Sort manually",
      "errorMessage": "Using Arrays.sort() is not allowed. Implement Bubble Sort logic."
    },
    {
      "type": "REQUIRED",
      "construct": "bubble_sort_logic",
      "description": "Must use nested loops with swapping",
      "errorMessage": "Solution must implement Bubble Sort with nested loops."
    }
  ]
}
```

### Response

```json
{
  "passed": false,
  "parsingTimeMs": 45,
  "hasSyntaxErrors": false,
  "violations": [
    {
      "rule": {
        "type": "FORBIDDEN",
        "construct": "arrays.sort"
      },
      "lineNumber": 1,
      "codeSnippet": "public void bubbleSort(int[] arr) { Arrays.sort(arr); }",
      "message": "Using Arrays.sort() is not allowed. Implement Bubble Sort logic."
    }
  ]
}
```

## Performance

- **Target**: <200ms parsing time
- **Current**: 40-80ms for typical submissions
- **Optimization**: Pattern matching instead of full AST traversal

## Implementation Strategy

### Phase 1 (Current): Pattern Matching
- Regex-based detection
- Fast and reliable for common patterns
- No ANTLR grammar compilation needed

### Phase 2 (Future): Full ANTLR
- Add complete Java8.g4 and Python3.g4 grammars
- AST traversal with visitors
- More sophisticated detection (e.g., obfuscated code)

## Testing

### Test Case 1: Forbidden Sort
```java
// Student Code
Arrays.sort(numbers);

// Expected: FAIL with violation on Arrays.sort
```

### Test Case 2: Required Recursion
```java
// Student Code (iterative)
for (int i = 0; i < n; i++) { ... }

// Expected: FAIL - recursion required
```

### Test Case 3: Bubble Sort Logic
```java
// Student Code (correct)
for (int i = 0; i < n-1; i++) {
    for (int j = 0; j < n-i-1; j++) {
        if (arr[j] > arr[j+1]) {
            int temp = arr[j];
            arr[j] = arr[j+1];
            arr[j+1] = temp;
        }
    }
}

// Expected: PASS
```

## Integration with Exam System

```java
// In SubmissionService.java
List<VerificationRule> rules = question.getVerificationRules();
ParserService parser = parserFactory.getParser(submission.getLanguage());
VerificationResult result = parser.verifyCode(submission.getCode(), rules);

if (!result.isPassed()) {
    // Reject submission with violation details
    return SubmissionResponse.rejected(result.getViolations());
}
```

## Future Enhancements

1. **Cache Parsed ASTs**: Use Redis to cache parsing results for identical code
2. **ML-Based Detection**: Train model to detect algorithm patterns
3. **Code Similarity**: Detect plagiarism using AST comparison
4. **Performance Profiling**: Track which constructs are slow to detect

---

**Status**: Phase 3 Complete - Logic Verification Ready 🔍
