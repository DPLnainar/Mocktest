import React, { useState } from 'react';
import { Upload, Clipboard, FileText, CheckCircle, AlertCircle, Download, Check } from 'lucide-react';
import { parseExcel, parseSmartPaste } from '../utils/parserUtils';
import * as XLSX from 'xlsx';

const QuestionUploader = ({ onDataParsed, testType = 'MCQ_ONLY', initialTab = 'file', hideTabs = false }) => {
    const [activeTab, setActiveTab] = useState(initialTab); // 'file' or 'paste'
    const [parsing, setParsing] = useState(false);
    const [parsedData, setParsedData] = useState([]);
    const [pasteText, setPasteText] = useState('');
    const [error, setError] = useState('');

    // --- TEMPLATE DOWNLOAD ---
    const handleDownloadTemplate = () => {
        const wb = XLSX.utils.book_new();

        if (testType === 'MCQ_ONLY' || testType === 'HYBRID') {
            const mcqHeaders = ["Type", "Question Text", "Marks", "Option A", "Option B", "Option C", "Option D", "Correct Answer"];
            const mcqRow = ["MCQ", "What is 2+2?", "1", "3", "4", "5", "6", "B"];
            const wsData = [mcqHeaders, mcqRow];
            const ws = XLSX.utils.aoa_to_sheet(wsData);
            XLSX.utils.book_append_sheet(wb, ws, "MCQ");
        }

        if (testType === 'CODING_ONLY' || testType === 'HYBRID') {
            const codingHeaders = ["Type", "Title", "Description", "Marks", "Allowed Languages", "Input 1", "Output 1", "Input 2", "Output 2", "Input 3", "Output 3"];
            const codingRow = ["CODING", "Sum Two", "Return sum of a and b", "10", "Java, Python", "10 20", "30", "5 5", "10", "", ""];
            const wsData = [codingHeaders, codingRow];
            const ws = XLSX.utils.aoa_to_sheet(wsData);
            XLSX.utils.book_append_sheet(wb, ws, "Coding");
        }

        XLSX.writeFile(wb, `exam_template_${testType.toLowerCase()}.xlsx`);
    };

    // --- HANDLERS ---

    const handleFileUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setParsing(true);
        setError('');
        setParsedData([]);

        try {
            const data = await parseExcel(file);
            if (data.length === 0) {
                setError("No valid questions found. Please check column headers.");
            } else {
                setParsedData(data);
                // Wait for user confirmation before sending
            }
        } catch (err) {
            console.error(err);
            setError("Failed to parse file. Ensure it is a valid Excel/CSV.");
        } finally {
            setParsing(false);
        }
    };

    const handlePasteParse = () => {
        if (!pasteText.trim()) return;

        setParsing(true);
        setError('');
        setParsedData([]);

        try {
            const data = parseSmartPaste(pasteText);
            if (data.length === 0) {
                setError("Could not detect any questions. Please check the format.");
            } else {
                setParsedData(data);
                // Wait for user confirmation
            }
        } catch (err) {
            console.error(err);
            setError("Failed to parse text.");
        } finally {
            setParsing(false);
        }
    };

    const handleConfirmImport = () => {
        if (parsedData.length > 0 && onDataParsed) {
            onDataParsed(parsedData);
            setParsedData([]); // Clear after import
            setPasteText('');
        }
    };

    // --- RENDER ---

    return (
        <div className="text-white">
            {/* Header & Tabs - Hidden if hideTabs is true */}
            {!hideTabs && (
                <>
                    <h2 className="text-xl font-bold mb-4 flex items-center gap-2">
                        <Upload size={24} className="text-blue-400" />
                        Bulk Question Uploader
                    </h2>

                    {/* Tabs */}
                    <div className="flex gap-4 mb-6 border-b border-gray-700 pb-2">
                        <button
                            onClick={() => setActiveTab('file')}
                            className={`px-4 py-2 rounded-t-lg transition-colors ${activeTab === 'file' ? 'bg-gray-700 text-white border-b-2 border-blue-500' : 'text-gray-400 hover:text-white'}`}
                        >
                            File Upload
                        </button>
                        <button
                            onClick={() => setActiveTab('paste')}
                            className={`px-4 py-2 rounded-t-lg transition-colors ${activeTab === 'paste' ? 'bg-gray-700 text-white border-b-2 border-blue-500' : 'text-gray-400 hover:text-white'}`}
                        >
                            Smart Paste
                        </button>
                    </div>
                </>
            )}

            {/* Content Area */}
            <div className="min-h-[200px]">
                {activeTab === 'file' && (
                    <div className="space-y-4">
                        <div className="flex justify-between items-center bg-gray-700 p-3 rounded border border-gray-600">
                            <span className="text-gray-300 text-sm">Need the correct format?</span>
                            <button
                                onClick={handleDownloadTemplate}
                                className="flex items-center gap-2 text-sm text-blue-400 hover:text-blue-300 transition"
                            >
                                <Download size={16} /> Download {testType.replace('_', ' ')} Template
                            </button>
                        </div>

                        <div className="text-center p-8 border-2 border-dashed border-gray-600 rounded-lg hover:border-gray-500 transition-colors bg-gray-900/50">
                            <input
                                type="file"
                                accept=".csv, .xlsx, .xls"
                                className="hidden"
                                id="file-upload"
                                onChange={handleFileUpload}
                            />
                            <label htmlFor="file-upload" className="cursor-pointer flex flex-col items-center">
                                <FileText size={48} className="text-gray-500 mb-2" />
                                <span className="text-lg font-semibold text-gray-300">Click to Upload Excel / CSV</span>
                                <span className="text-sm text-gray-500 mt-1">Supports MCQ and Coding formats</span>
                            </label>
                        </div>
                    </div>
                )}

                {activeTab === 'paste' && (
                    <div className="space-y-3">
                        <div className="flex justify-between items-center bg-gray-700 p-2 rounded border border-gray-600 mb-2">
                            <span className="text-xs text-gray-400">Supported: "1. Question..." or "Title: ..."</span>
                            <button
                                className="text-xs text-blue-400"
                                onClick={() => {
                                    let ex = "1. Java is?\nA) Lang\nB) Coffee\nAnswer: A";
                                    if (testType === 'HYBRID') {
                                        ex = "1. What is Java?\nA) Language\nB) Coffee\nAnswer: A\n\nTitle: Sum Two Numbers\nMarks: 10\nDescription: Calculate sum.\nInput: 5 5\nOutput: 10";
                                    } else if (testType.includes('CODING')) {
                                        ex = "Title: Sum\nMarks: 10\nInput: 5 5\nOutput: 10";
                                    }
                                    navigator.clipboard.writeText(ex);
                                }}
                            >
                                Copy Example
                            </button>
                        </div>
                        <textarea
                            className="w-full h-64 bg-gray-900 border border-gray-700 rounded p-4 font-mono text-sm text-gray-300 focus:outline-none focus:border-blue-500"
                            placeholder={
                                testType === 'HYBRID'
                                    ? "1. What is Java?\nA) Language\nB) Coffee\nAnswer: A\n\nTitle: Sum Two Numbers\nMarks: 10\nInput: 5 5\nOutput: 10"
                                    : testType.includes('CODING')
                                        ? "Title: ...\nInput: ...\nOutput: ..."
                                        : "1. Question...\nA) Opt1\nB) Opt2\nAnswer: A"
                            }
                            value={pasteText}
                            onChange={(e) => setPasteText(e.target.value)}
                        />
                        <button
                            onClick={handlePasteParse}
                            disabled={!pasteText.trim() || parsing}
                            className="w-full py-2 bg-blue-600 hover:bg-blue-700 text-white rounded font-semibold transition disabled:opacity-50"
                        >
                            {parsing ? "Parsing..." : "Parse Questions"}
                        </button>
                    </div>
                )}
            </div>

            {/* Status & Error */}
            {error && (
                <div className="mt-4 p-3 bg-red-900/30 border border-red-800 rounded flex items-center gap-2 text-red-200">
                    <AlertCircle size={18} /> {error}
                </div>
            )}

            {/* Preview Section */}
            {parsedData.length > 0 && (
                <div className="mt-8 border-t border-gray-700 pt-6">
                    <div className="flex justify-between items-center mb-4">
                        <h3 className="text-lg font-semibold text-green-400 flex items-center gap-2">
                            <CheckCircle size={20} /> found {parsedData.length} questions
                        </h3>
                        <button
                            onClick={handleConfirmImport}
                            className="flex items-center gap-2 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded font-bold shadow-lg transition transform hover:scale-105"
                        >
                            <Check size={18} /> Confirm & Import
                        </button>
                    </div>

                    <div className="space-y-2 max-h-60 overflow-y-auto pr-2 bg-gray-900 p-2 rounded">
                        {parsedData.map((q, i) => (
                            <div key={i} className="bg-gray-800 p-3 rounded border border-gray-700">
                                <div className="flex justify-between items-start mb-1">
                                    <span className={`text-xs px-2 py-0.5 rounded font-bold ${q.type === 'MCQ' ? 'bg-blue-900 text-blue-200' : 'bg-purple-900 text-purple-200'}`}>
                                        {q.type}
                                    </span>
                                    <span className="text-xs text-gray-400">Marks: {q.marks}</span>
                                </div>
                                <h4 className="font-semibold text-white truncate text-sm">{q.type === 'MCQ' ? q.text : q.title}</h4>

                                {q.type === 'MCQ' && (
                                    <div className="text-sm text-gray-400 mt-1 pl-2 border-l-2 border-gray-600">
                                        {q.options.length} Options • Correct: {['A', 'B', 'C', 'D'][q.correctIndex] || '?'}
                                    </div>
                                )}
                                {q.type === 'CODING' && (
                                    <div className="text-sm text-gray-400 mt-1 pl-2 border-l-2 border-gray-600">
                                        {q.testCases.length} Test Cases • Languages: {q.allowedLanguages?.join(', ') || 'All'}
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};

export default QuestionUploader;
