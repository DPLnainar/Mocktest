import * as XLSX from 'xlsx';

/**
 * Utility for parsing questions from various formats (Excel, Text Paste)
 */
export const QuestionParser = {

    /**
     * 1. Helper: Parse Constraints String
     * Input: "Ban Loops, Require Recursion"
     * Output: { forbidLoops: boolean, requireRecursion: boolean }
     */
    parseConstraints: (constraintString) => {
        if (!constraintString || typeof constraintString !== 'string') {
            return { forbidLoops: false, requireRecursion: false };
        }

        const lower = constraintString.toLowerCase();
        return {
            forbidLoops: lower.includes('loop'), // "Ban Loops" or "No Loops"
            requireRecursion: lower.includes('recursion') // "Require Recursion"
        };
    },

    /**
     * 2. Parse Excel/CSV File
     * Uses xlsx (SheetJS)
     */
    parseExcel: async (file) => {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();

            reader.onload = (e) => {
                try {
                    const data = new Uint8Array(e.target.result);
                    const workbook = XLSX.read(data, { type: 'array' });
                    // Iterate through all sheets or just the first? Usually first.
                    // But if template has multiple sheets, we might want to check them.
                    // For now, let's stick to parsing the first sheet as per standard behavior,
                    // or concat all sheets if multiple exist.

                    let allQuestions = [];

                    workbook.SheetNames.forEach(sheetName => {
                        const sheet = workbook.Sheets[sheetName];
                        const jsonData = XLSX.utils.sheet_to_json(sheet);

                        const sheetQuestions = jsonData.map((row, index) => {
                            // Auto-detect type
                            // MCQ if 'Correct Answer' exists. Coding if 'Title' exists (and maybe not 'Correct Answer')
                            const isMCQ = row['Correct Answer'] !== undefined;

                            if (isMCQ) {
                                return {
                                    tempId: Date.now() + Math.random() + index,
                                    type: 'MCQ',
                                    questionText: row['Question Text'] || `Question ${index + 1}`,
                                    marks: parseInt(row['Marks']) || 1,
                                    optionA: row['Option A'],
                                    optionB: row['Option B'],
                                    optionC: row['Option C'],
                                    optionD: row['Option D'],
                                    correctOption: row['Correct Answer']
                                };
                            } else {
                                // Coding Question Parsing Logic
                                const title = row['Title'] || `Coding Question ${index + 1}`;
                                const description = row['Description'] || '';
                                const constraintsConfig = QuestionParser.parseConstraints(row['Constraints']);

                                // Allowed Languages
                                let allowedLanguages = [];
                                if (row['Allowed Languages']) {
                                    allowedLanguages = row['Allowed Languages'].toString().split(',').map(s => s.trim());
                                }

                                // Test Case Extraction
                                const testCases = [];
                                // Loop through keys "Input 1", "Input 2" ...
                                let i = 1;
                                while (row[`Input ${i}`] !== undefined) {
                                    testCases.push({
                                        input: String(row[`Input ${i}`]),
                                        output: String(row[`Output ${i}`] || '')
                                    });
                                    i++;
                                }
                                // Fallback for single "Input"/"Output" columns
                                if (testCases.length === 0 && row['Input']) {
                                    testCases.push({
                                        input: String(row['Input']),
                                        output: String(row['Output'] || '')
                                    });
                                }

                                return {
                                    type: "CODE", // Standardized type
                                    title: title,
                                    description: description,
                                    constraints_config: constraintsConfig,
                                    testCases: testCases,
                                    allowedLanguages: allowedLanguages.length > 0 ? allowedLanguages : undefined, // Optional in schema
                                    memoryLimit: 1024,
                                    timeLimit: 2,
                                    marks: parseInt(row['Marks']) || 10,
                                    tempId: Date.now() + Math.random() + index
                                };
                            }
                        });
                        allQuestions = [...allQuestions, ...sheetQuestions];
                    });

                    resolve(allQuestions);
                } catch (error) {
                    reject(error);
                }
            };

            reader.onerror = (error) => reject(error);
            reader.readAsArrayBuffer(file);
        });
    },

    /**
     * 3. Parse Smart Paste Text
     * Logic: Regex parsing for blocks
     */
    parseSmartPaste: (text) => {
        if (!text) return [];

        // Split by double newline or "---" separator to separate questions
        const rawBlocks = text.split(/\n\s*\n|---/g).filter(b => b.trim().length > 0);

        return rawBlocks.map((block, index) => {
            // Attempt to detect MCQ vs Coding
            const hasOptions = block.match(/[A-D]\)/i);
            const hasAnswer = block.match(/Answer:/i);

            if (hasOptions || hasAnswer) {
                // MCQ Logic
                const lines = block.split('\n');
                const question = {
                    tempId: Date.now() + Math.random() + index,
                    type: 'MCQ',
                    marks: 1,
                    questionText: lines[0].replace(/^\d+[\.\)]\s*/, '').trim()
                };

                const optionAMatch = block.match(/A\)(.*?)(?=(B\)|C\)|D\)|Answer:|$))/s);
                const optionBMatch = block.match(/B\)(.*?)(?=(C\)|D\)|Answer:|$))/s);
                const optionCMatch = block.match(/C\)(.*?)(?=(D\)|Answer:|$))/s);
                const optionDMatch = block.match(/D\)(.*?)(?=(Answer:|$))/s);
                const answerMatch = block.match(/Answer:\s*([A-D])/i);

                if (optionAMatch) question.optionA = optionAMatch[1].trim();
                if (optionBMatch) question.optionB = optionBMatch[1].trim();
                if (optionCMatch) question.optionC = optionCMatch[1].trim();
                if (optionDMatch) question.optionD = optionDMatch[1].trim();
                if (answerMatch) question.correctOption = answerMatch[1].toUpperCase();

                return question;
            } else {
                // Coding Logic (Key: Value parsing)
                const titleMatch = block.match(/Title:\s*(.+)/i);
                const descMatch = block.match(/Description:\s*(.+)/i);
                const constraintsMatch = block.match(/Constraints:\s*(.+)/i);
                const marksMatch = block.match(/Marks:\s*(\d+)/i);
                const languagesMatch = block.match(/Allowed Languages:\s*(.+)/i);

                const inputRegex = /Input:\s*(.+?)(?=\nOutput:|$)/gi;

                // Parse Test Cases (Input: ... Output: ...)
                const testCases = [];

                // We use a loop to match pairs. 
                // Alternative safer approach: Split by "Input:" and parse chunks
                const inputParts = block.split(/Input:\s*/i).slice(1); // skip first empty

                inputParts.forEach(part => {
                    const outputSplit = part.split(/Output:\s*/i);
                    if (outputSplit.length >= 2) {
                        const inp = outputSplit[0].trim();
                        const out = outputSplit[1].split('\n')[0].trim(); // Take first line of output or until next field
                        testCases.push({ input: inp, output: out });
                    }
                });

                let allowedLanguages = [62, 71, 54, 63]; // Default ALL
                if (languagesMatch) {
                    const langsStr = languagesMatch[1].toLowerCase();
                    allowedLanguages = [];
                    if (langsStr.includes('java')) allowedLanguages.push(62);
                    if (langsStr.includes('python')) allowedLanguages.push(71);
                    if (langsStr.includes('c++') || langsStr.includes('cpp')) allowedLanguages.push(54);
                    if (langsStr.includes('javascript') || langsStr.includes('js')) allowedLanguages.push(63);
                }

                if (titleMatch) {
                    return {
                        tempId: Date.now() + Math.random() + index,
                        type: 'CODING',
                        title: titleMatch[1].trim(),
                        description: descMatch ? descMatch[1].trim() : '',
                        constraints_config: QuestionParser.parseConstraints(constraintsMatch ? constraintsMatch[1] : ''),
                        marks: marksMatch ? parseInt(marksMatch[1]) : 10,
                        allowedLanguages: allowedLanguages,
                        testCases: testCases,
                        memoryLimit: 1024,
                        timeLimit: 2
                    };
                }
            }
        });
    }
};
