-- Add sample questions to Test ID 14 (Simplified version)
-- This script adds 5 MCQ questions and 2 coding questions

-- First, add MCQ questions
INSERT INTO questions (question_text, type, marks, department, option_a, option_b, option_c, option_d, correct_option, explanation, created_at, updated_at)
VALUES 
('What is the default value of a boolean variable in Java?', 'MCQ', 2, 'CSE', 'true', 'false', 'null', '0', 'B', 'In Java, the default value of a boolean variable is false.', NOW(), NOW()),
('Which data structure uses LIFO (Last In First Out) principle?', 'MCQ', 2, 'CSE', 'Queue', 'Stack', 'Array', 'Linked List', 'B', 'Stack follows LIFO principle where the last element inserted is the first one to be removed.', NOW(), NOW()),
('What is the time complexity of binary search?', 'MCQ', 2, 'CSE', 'O(n)', 'O(log n)', 'O(n^2)', 'O(1)', 'B', 'Binary search has O(log n) time complexity as it divides the search space in half each iteration.', NOW(), NOW()),
('Which of the following is NOT a pillar of Object-Oriented Programming?', 'MCQ', 2, 'CSE', 'Encapsulation', 'Inheritance', 'Compilation', 'Polymorphism', 'C', 'The four pillars of OOP are Encapsulation, Inheritance, Polymorphism, and Abstraction. Compilation is not a pillar.', NOW(), NOW()),
('Which SQL command is used to retrieve data from a database?', 'MCQ', 2, 'CSE', 'INSERT', 'UPDATE', 'SELECT', 'DELETE', 'C', 'SELECT is used to retrieve/query data from a database.', NOW(), NOW());

-- Add Coding Questions
INSERT INTO questions (question_text, type, marks, department, starter_code, test_cases, allowed_language_ids, explanation, created_at, updated_at)
VALUES 
('Write a function that takes two integers as input and returns their sum.

Input Format: Two integers a and b on a single line separated by space.
Output Format: A single integer representing the sum of a and b.
Example: Input: 5 3, Output: 8', 
'CODING', 5, 'CSE', 
'public class Solution {
    public static int sum(int a, int b) {
        // Write your code here
        return 0;
    }
    
    public static void main(String[] args) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        int a = sc.nextInt();
        int b = sc.nextInt();
        System.out.println(sum(a, b));
    }
}',
'[{"input":"5 3","expectedOutput":"8","isHidden":false},{"input":"10 20","expectedOutput":"30","isHidden":false},{"input":"-5 5","expectedOutput":"0","isHidden":true}]',
ARRAY[62,71,54],
'Simply add the two numbers and return the result.',
NOW(), NOW()),

('Write a function that reverses a given string.

Input Format: A single line containing a string.
Output Format: The reversed string.
Example: Input: hello, Output: olleh',
'CODING', 5, 'CSE',
'public class Solution {
    public static String reverse(String str) {
        // Write your code here
        return "";
    }
    
    public static void main(String[] args) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        String input = sc.nextLine();
        System.out.println(reverse(input));
    }
}',
'[{"input":"hello","expectedOutput":"olleh","isHidden":false},{"input":"world","expectedOutput":"dlrow","isHidden":false},{"input":"Java","expectedOutput":"avaJ","isHidden":true}]',
ARRAY[62,71,54],
'Use a loop or built-in methods to reverse the string.',
NOW(), NOW());

-- Link questions to Test ID 14
-- Get the last 7 question IDs and link them
DO $$
DECLARE
    q_id BIGINT;
    q_type VARCHAR(20);
    idx INT := 1;
BEGIN
    FOR q_id, q_type IN 
        SELECT id, type::VARCHAR 
        FROM questions 
        WHERE created_at >= NOW() - INTERVAL '1 minute'
        ORDER BY id DESC
        LIMIT 7
    LOOP
        INSERT INTO test_questions (test_id, question_id, marks, section_name, order_index)
        VALUES (
            14,
            q_id,
            CASE WHEN q_type = 'MCQ' THEN 2 ELSE 5 END,
            CASE WHEN q_type = 'MCQ' THEN 'Part-I: Multiple Choice' ELSE 'Part-II: Coding' END,
            8 - idx
        );
        idx := idx + 1;
    END LOOP;
END $$;

-- Show results
SELECT 
    tq.test_id,
    tq.question_id,
    LEFT(q.question_text, 50) as question_preview,
    q.type,
    tq.marks,
    tq.section_name,
    tq.order_index
FROM test_questions tq
JOIN questions q ON tq.question_id = q.id
WHERE tq.test_id = 14
ORDER BY tq.order_index;
