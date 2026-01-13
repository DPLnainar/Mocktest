import { questionAPI } from '../services/testAPI';

// Strategy Interface (Implicit)
// parse(file): Promise<{ questionIds: number[] }>

const MCQParserStrategy = {
    parse: async (file) => {
        // In a real implementation, we might parse client-side to validate headers first.
        // For now, we delegate to the backend's bulk upload which handles CSV/XLSX.
        // We could verify extension here.
        if (!file.name.match(/\.(csv|xlsx|xls)$/)) {
            throw new Error("Invalid file format. Please upload CSV or Excel.");
        }
        return await questionAPI.bulkUpload(file);
    }
};

const CodingParserStrategy = {
    parse: async (file) => {
        // Placeholder for future Coding-specific parser
        // e.g., validating ZIP files or specific CSV structure for test cases
        if (!file.name.match(/\.(csv|xlsx|zip)$/)) {
            throw new Error("Invalid file format for Coding questions.");
        }
        // For now, reuse bulkUpload but ideally this hits a different endpoint 
        // or the backend handles it based on content.
        return await questionAPI.bulkUpload(file);
    }
};

export const getFileParserStrategy = (type) => {
    switch (type) {
        case 'CODING_ONLY':
            return CodingParserStrategy;
        case 'MCQ_ONLY':
            return MCQParserStrategy;
        case 'HYBRID':
        default:
            // Hybrid might need a smarter strategy or user selection.
            // Defaulting to MCQ strategy which currently handles generic bulk upload.
            return MCQParserStrategy;
    }
};
