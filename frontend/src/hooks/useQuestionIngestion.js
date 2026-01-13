import { useState, useCallback } from 'react';
import { toast } from 'react-hot-toast';
import { parseExcel, parseTSV, parseCodingExcel, parseCodingString, convertSimpleTextToJSON } from '../utils/questionParser';

/**
 * Hook to handle file ingestion for questions.
 * @param {Object} allowedTypes - { mcq: boolean, coding: boolean }
 */
export const useQuestionIngestion = (allowedTypes) => {
    const [parsedQuestions, setParsedQuestions] = useState([]);
    const [isParsing, setIsParsing] = useState(false);

    /**
     * Validates a single row against allowed types.
     */
    const validateRow = useCallback((row) => {
        const type = row.type ? row.type.toUpperCase() : 'MCQ'; // Default to MCQ if missing

        if (type === 'MCQ' && !allowedTypes.mcq) {
            return { valid: false, error: 'MCQ questions are not allowed in this test type.' };
        }
        if (type === 'CODING' && !allowedTypes.coding) {
            return { valid: false, error: 'Coding questions are not allowed in this test type.' };
        }
        if (!row.questionText || !row.questionText.trim()) {
            return { valid: false, error: 'Question text is missing.' };
        }
        return { valid: true };
    }, [allowedTypes]);

    const processData = useCallback(async (data) => {
        const validQuestions = [];
        let errors = 0;

        data.forEach((row, index) => {
            const validation = validateRow(row);
            if (validation.valid) {
                validQuestions.push({
                    ...row,
                    originalIndex: index
                });
            } else {
                errors++;
                console.warn(`Row ${index + 1} rejected: ${validation.error}`, row);
            }
        });

        if (errors > 0) {
            toast.error(`${errors} questions rejected due to validation errors. See console.`);
        }

        if (validQuestions.length > 0) {
            setParsedQuestions(prev => [...prev, ...validQuestions]);
            toast.success(`Successfully parsed ${validQuestions.length} questions.`);
        }

        return validQuestions;
    }, [validateRow]);

    const handleFileUpload = async (file) => {
        setIsParsing(true);
        try {
            let data = [];
            let parsed = false;

            if (allowedTypes.coding) {
                try {
                    data = await parseCodingExcel(file);
                    if (data && data.length > 0) parsed = true;
                } catch (e) {
                    console.warn("Coding excel parse failed, trying standard:", e);
                }
            }

            if (!parsed && allowedTypes.mcq) {
                data = await parseExcel(file);
            }

            await processData(data);
        } catch (error) {
            console.error(error);
            toast.error('Failed to parse file: ' + error.message);
        } finally {
            setIsParsing(false);
        }
    };

    const handlePaste = async (text) => {
        setIsParsing(true);
        try {
            let data = [];
            let parsed = false;

            // 1. Try Coding Parse if allowed
            if (allowedTypes.coding) {
                const codingQs = parseCodingString(text);
                if (codingQs && codingQs.length > 0) {
                    data = codingQs;
                    parsed = true;
                }
            }

            // 2. Fallback to MCQ if not parsed as Coding
            if (!parsed && allowedTypes.mcq) {
                // Try robust block parsing first
                let parsedBlocks = convertSimpleTextToJSON(text);
                if (parsedBlocks && parsedBlocks.length > 0) {
                    data = parsedBlocks;
                } else {
                    // Fallback to TSV
                    data = parseTSV(text);
                }
            }

            await processData(data);
        } catch (error) {
            console.error(error);
            toast.error('Failed to parse text: ' + error.message);
        } finally {
            setIsParsing(false);
        }
    };

    const clearQuestions = () => setParsedQuestions([]);

    return {
        parsedQuestions,
        isParsing,
        handleFileUpload,
        handlePaste,
        validateRow,
        clearQuestions
    };
};
