import React from 'react';
import * as XLSX from 'xlsx';

const DownloadTemplateButton = () => {
    const handleDownload = () => {
        // Create MCQ Template Sheet
        const mcqData = [
            {
                'Question Text': 'What is 2 + 2?',
                'Option A': '3',
                'Option B': '4',
                'Option C': '5',
                'Option D': '6',
                'Correct Answer': 'B',
                'Marks': '1'
            }
        ];

        // Create Coding Template Sheet
        const codingData = [
            {
                'Title': 'Sum of Numbers',
                'Description': 'Add two integers.',
                'Allowed Languages': 'Python, Java',
                'Marks': '10',
                'Input 1': '5 10',
                'Output 1': '15',
                'Input 2': '3 7',
                'Output 2': '10'
            }
        ];

        // Create workbook
        const workbook = XLSX.utils.book_new();

        // Create MCQ worksheet
        const mcqWorksheet = XLSX.utils.json_to_sheet(mcqData);
        XLSX.utils.book_append_sheet(workbook, mcqWorksheet, 'MCQ_Template');

        // Create Coding worksheet
        const codingWorksheet = XLSX.utils.json_to_sheet(codingData);
        XLSX.utils.book_append_sheet(workbook, codingWorksheet, 'Coding_Template');

        // Generate and download file
        XLSX.writeFile(workbook, 'Question_Upload_Template.xlsx');
    };

    return (
        <button
            onClick={handleDownload}
            className="inline-flex items-center px-4 py-2 bg-green-600 hover:bg-green-700 text-white font-medium rounded-lg shadow-md transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-offset-2"
        >
            <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5 mr-2"
                viewBox="0 0 20 20"
                fill="currentColor"
            >
                <path
                    fillRule="evenodd"
                    d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z"
                    clipRule="evenodd"
                />
            </svg>
            Download Template
        </button>
    );
};

export default DownloadTemplateButton;
