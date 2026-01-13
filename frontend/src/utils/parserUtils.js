import * as XLSX from 'xlsx';

/**
 * Parses an Excel file supporting multiple sheets ("MCQ" and "Coding").
 * Falls back to auto-detection if specific sheets are missing.
 */
export const parseExcel = (file) => {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const data = new Uint8Array(e.target.result);
                const workbook = XLSX.read(data, { type: 'array' });

                let allQuestions = [];

                // 1. Check for Specific Sheets
                const mcqSheet = workbook.Sheets['MCQ'];
                const codingSheet = workbook.Sheets['Coding'];

                if (mcqSheet || codingSheet) {
                    if (mcqSheet) {
                        const mcqData = XLSX.utils.sheet_to_json(mcqSheet, { defval: "" });
                        const parsedMCQs = mcqData.map(row => parseMCQRow(normalizeKeys(row))).filter(q => q);
                        allQuestions = [...allQuestions, ...parsedMCQs];
                    }
                    if (codingSheet) {
                        const codingData = XLSX.utils.sheet_to_json(codingSheet, { defval: "" });
                        const parsedCoding = codingData.map(row => parseCodingRow(normalizeKeys(row))).filter(q => q);
                        allQuestions = [...allQuestions, ...parsedCoding];
                    }
                } else {
                    // 2. Fallback: Parse First Sheet with Auto-Detection
                    const firstSheetName = workbook.SheetNames[0];
                    const jsonData = XLSX.utils.sheet_to_json(workbook.Sheets[firstSheetName], { defval: "" });

                    const detected = jsonData.map((row, index) => {
                        const normalized = normalizeKeys(row);
                        if (normalized['optiona']) return parseMCQRow(normalized);
                        if (normalized['input1']) return parseCodingRow(normalized);

                        // Explicit type check
                        if (normalized['type']?.toUpperCase() === 'MCQ') return parseMCQRow(normalized);
                        if (normalized['type']?.toUpperCase() === 'CODING') return parseCodingRow(normalized);

                        return null;
                    }).filter(q => q);

                    allQuestions = detected;
                }

                resolve(allQuestions);
            } catch (error) {
                reject(error);
            }
        };
        reader.onerror = (error) => reject(error);
        reader.readAsArrayBuffer(file);
    });
};

/**
 * Parses raw text using a State Machine approach.
 * Supports mixed MCQ and Coding questions.
 */
export const parseSmartPaste = (text) => {
    const lines = text.split('\n').map(l => l.trim()).filter(l => l);
    const questions = [];

    let currentQ = null;

    lines.forEach(line => {
        // --- 1. DETECT NEW QUESTION START ---

        // Coding Pattern: "Title: Two Sum"
        const codingStart = line.match(/^Title:\s*(.+)/i);
        if (codingStart) {
            if (currentQ) questions.push(finalizeQuestion(currentQ));
            currentQ = {
                type: 'CODING',
                title: codingStart[1].trim(),
                description: '',
                allowedLanguages: [62, 71, 63, 54], // Default to Java, Py, JS, C++
                marks: 10,
                testCases: []
            };
            return;
        }

        // MCQ Pattern: "1. What is..." or "Q1. ..."
        // We use a lookahead or strict start to avoid matching "1. item" inside a description
        const mcqStart = line.match(/^(?:Q\d+|Q\.|[0-9]+)[\.\)]\s*(.+)/i);
        if (mcqStart) {
            // Safety: If we are in the middle of a coding description, "1. Step one" might trigger this.
            // Heuristic: If key Coding fields (Input/Output) haven't been seen yet, treat as description? 
            // Better: Strict keywords "Ans:" or "A)" usually needed to confirm MCQ, but we have to start capturing somewhere.
            // Let's assume user formats well.
            if (currentQ) questions.push(finalizeQuestion(currentQ));

            // Extract Marks: "Question? (5m)"
            let qText = mcqStart[1];
            let marks = 1;
            const marksMatch = qText.match(/[\(\[](\d+)\s*(?:m|marks?)[\)\]]$/i);
            if (marksMatch) {
                marks = parseInt(marksMatch[1], 10);
                qText = qText.replace(marksMatch[0], '').trim();
            }

            currentQ = {
                type: 'MCQ',
                text: qText,
                options: [],
                correctIndex: null,
                marks: marks
            };
            return;
        }

        if (!currentQ) return;

        // --- 2. PARSE FIELDS BASED ON TYPE ---

        if (currentQ.type === 'CODING') {
            if (line.match(/^Description:/i)) {
                currentQ.description += line.replace(/^Description:/i, '').trim();
                currentQ._mode = 'DESC';
            } else if (line.match(/^(?:Allowed )?Languages:/i)) {
                currentQ.allowedLanguages = parseLanguageString(line.split(':')[1]);
                currentQ._mode = null;
            } else if (line.match(/^Marks:/i)) {
                currentQ.marks = parseInt(line.split(':')[1], 10) || 10;
                currentQ._mode = null;
            } else if (line.match(/^Input:/i)) {
                const val = line.replace(/^Input:/i, '').trim();
                currentQ.testCases.push({ input: val, output: '' });
                currentQ._mode = null;
            } else if (line.match(/^Output:/i)) {
                const val = line.replace(/^Output:/i, '').trim();
                if (currentQ.testCases.length > 0) {
                    currentQ.testCases[currentQ.testCases.length - 1].output = val;
                }
                currentQ._mode = null;
            } else if (line.match(/^(?:Logic|Constraints):/i)) {
                const val = line.split(':')[1];
                currentQ.constraints = parseConstraints(val);
                currentQ._mode = null;
            } else {
                // Continuation
                if (currentQ._mode === 'DESC' || (!currentQ.testCases.length && !currentQ.allowedLanguages.length)) {
                    currentQ.description += (currentQ.description ? '\n' : '') + line;
                }
            }
        }
        else if (currentQ.type === 'MCQ') {
            // Options: "A) Text" or "(A) Text" or "a. Text"
            // Note: Some users might use "a)" so we handle case-insensitive for detection, but standardizing to A-D is good.
            const optionMatch = line.match(/^([A-D])[\.\)\-\]]\s*(.+)/i) || line.match(/^\(([A-D])\)\s*(.+)/i);

            if (optionMatch) {
                const text = optionMatch[2].trim();
                currentQ.options.push(text);
            } else {
                // Answer Key: "Ans: A" or "Correct: B"
                const ansMatch = line.match(/^(?:Ans|Answer|Correct)(?::|\s)\s*([A-D])/i);
                if (ansMatch) {
                    const letter = ansMatch[1].toUpperCase();
                    const map = { 'A': 0, 'B': 1, 'C': 2, 'D': 3 };
                    currentQ.correctIndex = map[letter];
                } else {
                    // Multiline question text if options haven't started
                    if (currentQ.options.length === 0) {
                        currentQ.text += " " + line;
                    } else {
                        // Multiline option? Attach to last option
                        // currentQ.options[currentQ.options.length - 1] += " " + line;
                    }
                }
            }
        }
    });

    if (currentQ) questions.push(finalizeQuestion(currentQ));
    return questions;
};

// --- HELPER FUNCTIONS ---

const normalizeKeys = (row) => {
    const data = {};
    Object.keys(row).forEach(key => {
        // Remove spaces, lowercase
        const cleanKey = key.trim().toLowerCase().replace(/[^a-z0-9]/g, '');
        data[cleanKey] = row[key];
    });
    return data;
};

const parseMCQRow = (row) => {
    if (!row['questiontext'] && !row['question']) return null;

    const correctVal = String(row['correctanswer'] || row['correctoption'] || row['answer'] || '').toUpperCase().trim();
    const map = { 'A': 0, 'B': 1, 'C': 2, 'D': 3 };

    // Handle "Option A" vs "OptionA" keys
    // Since we strip spaces in normalizeKeys, "Option A" -> "optiona"

    return {
        type: "MCQ",
        text: row['questiontext'] || row['question'],
        options: [
            row['optiona'] || '',
            row['optionb'] || '',
            row['optionc'] || '',
            row['optiond'] || ''
        ].filter(o => o), // Remove empty cells
        correctIndex: map[correctVal] !== undefined ? map[correctVal] : -1,
        marks: parseInt(row['marks']) || 1
    };
};

const parseCodingRow = (row) => {
    if (!row['title']) return null;

    const testCases = [];
    // Support up to 5 test cases or dynamic if needed
    for (let i = 1; i <= 5; i++) {
        const inp = row[`input${i}`];
        const out = row[`output${i}`];
        if (inp && out !== undefined) {
            testCases.push({ input: String(inp), output: String(out) });
        }
    }

    return {
        type: "CODING",
        title: row['title'],
        description: row['description'] || '',
        allowedLanguages: parseLanguageString(row['allowedlanguages']),
        marks: parseInt(row['marks']) || 10,
        testCases,
        constraints: parseConstraints(row['constraints'] || row['logic'])
    };
};

const parseLanguageString = (str) => {
    if (!str) return [62, 71, 63, 54]; // Default All
    const ids = [];
    const s = String(str).toLowerCase();

    if (s.includes('java')) ids.push(62); // Java
    if (s.includes('python')) ids.push(71); // Python
    if (s.includes('cpp') || s.includes('c++')) ids.push(54); // C++
    if (s.includes('js') || s.includes('javascript')) ids.push(63); // Node

    return ids.length > 0 ? ids : [62, 71, 63, 54];
};

const parseConstraints = (str) => {
    const constraints = { banLoops: false, requireRecursion: false };
    if (!str) return constraints;

    const s = String(str).toLowerCase();
    if (s.includes('ban loop') || s.includes('no loop')) constraints.banLoops = true;
    if (s.includes('recursion') || s.includes('recursive')) constraints.requireRecursion = true;

    return constraints;
};

const finalizeQuestion = (q) => {
    delete q._mode;
    // Ensure constraints exist if not parsed
    if (!q.constraints) q.constraints = { banLoops: false, requireRecursion: false };
    return q;
};
