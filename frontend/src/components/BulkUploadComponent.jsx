import React, { useState } from 'react';
import * as XLSX from 'xlsx';
import { Upload, Clipboard, Download, CheckCircle, XCircle, AlertTriangle, FileText, X } from 'lucide-react';
import { questionAPI } from '../services/testAPI';
import { QuestionParser } from '../utils/QuestionParser';
import toast from 'react-hot-toast';

/**
 * Universal Bulk Upload Component
 * Supports: File Upload (CSV/Excel) + Smart Paste + Unified Feedback Modal
 */
const BulkUploadComponent = ({ onSuccess, testType = 'HYBRID', initialTab = 'file', hideTabs = false }) => {
    const [activeTab, setActiveTab] = useState(initialTab); // 'file' or 'paste'
    // pasteMode state removed - auto-detection used
    const [uploadFile, setUploadFile] = useState(null);
    const [pasteText, setPasteText] = useState('');
    const [uploading, setUploading] = useState(false);
    const [uploadResult, setUploadResult] = useState(null);
    const [showResultModal, setShowResultModal] = useState(false);

    // ============ FILE UPLOAD HANDLER ============
    const handleFileSelect = (e) => {
        const file = e.target.files[0];
        if (file) {
            setUploadFile(file);
            setUploadResult(null);
        }
    };

    const handleFileUpload = async () => {
        if (!uploadFile) {
            toast.error('Please select a file first');
            return;
        }

        setUploading(true);
        try {
            // Use QuestionParser for Excel/CSV
            const questions = await QuestionParser.parseExcel(uploadFile);

            // Bulk Create via Backend
            const response = await questionAPI.bulkCreate(questions);
            const result = response.data;

            // Attach original parsed questions to result for frontend usage
            result.parsedQuestions = questions;

            processUploadResult(result);
        } catch (error) {
            console.error('Upload error:', error);
            handleUploadError(error);
        } finally {
            setUploading(false);
        }
    };

    // ============ SMART PASTE HANDLER ============
    const handlePasteUpload = async () => {
        if (!pasteText.trim()) {
            toast.error('Please paste some content first');
            return;
        }

        setUploading(true);
        try {
            // Use QuestionParser with specific mode
            const questions = QuestionParser.parseSmartPaste(pasteText);

            if (questions.length === 0) {
                toast.error("Could not parse any questions. Check format.");
                setUploading(false);
                return;
            }

            // Bulk Create via Backend
            const response = await questionAPI.bulkCreate(questions);
            const result = response.data;

            // Attach original parsed questions to result for frontend usage
            result.parsedQuestions = questions;

            processUploadResult(result);
        } catch (error) {
            console.error('Paste upload error:', error);
            handleUploadError(error);
        } finally {
            setUploading(false);
        }
    };

    // ============ UNIFIED RESULT PROCESSOR ============
    const processUploadResult = (result) => {
        // Normalize response format (handle both formats)
        const normalizedResult = {
            total: result.total || (result.successCount + result.errorCount) || 0,
            saved: result.saved || result.successCount || 0,
            failed: result.failed || result.errorCount || 0,
            errors: result.errors || [],
            questionIds: result.questionIds || []
        };

        setUploadResult(normalizedResult);
        setShowResultModal(true);

        if (normalizedResult.saved > 0) {
            toast.success(`Successfully uploaded ${normalizedResult.saved} questions!`);
            if (onSuccess) {
                // Pass parsed questions along with the result for proper UI rendering
                onSuccess({ ...normalizedResult, parsedQuestions: result.parsedQuestions || [] });
            }
        }

        if (normalizedResult.failed > 0) {
            toast.error(`${normalizedResult.failed} questions failed to upload`);
        }
    };

    const handleUploadError = (error) => {
        const errorMessage = error.response?.data?.message || 'Upload failed';
        toast.error(errorMessage);

        setUploadResult({
            total: 0,
            saved: 0,
            failed: 1,
            errors: [{ row: 0, reason: errorMessage }]
        });
        setShowResultModal(true);
    };

    // ============ CSV ERROR REPORT GENERATOR ============
    const generateErrorReport = (errors) => {
        if (!errors || errors.length === 0) return '';

        // CSV Header
        let csv = 'Row Number,Error Reason,Details\n';

        // CSV Rows
        errors.forEach(error => {
            const row = error.row || 'N/A';
            const reason = (error.reason || error.message || 'Unknown error').replace(/"/g, '""');
            const details = (error.details || '').replace(/"/g, '""');
            csv += `${row},"${reason}","${details}"\n`;
        });

        return csv;
    };

    const downloadErrorReport = () => {
        if (!uploadResult || !uploadResult.errors || uploadResult.errors.length === 0) {
            toast.error('No errors to download');
            return;
        }

        const csv = generateErrorReport(uploadResult.errors);
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);

        const link = document.createElement('a');
        link.href = url;
        link.download = `upload_errors_${new Date().toISOString().split('T')[0]}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);

        toast.success('Error report downloaded!');
    };

    const closeResultModal = () => {
        setShowResultModal(false);
        setUploadFile(null);
        setPasteText('');
        setUploadResult(null);
    };

    // ============ TEMPLATE GENERATORS ============
    // ============ TEMPLATE GENERATORS ============
    const handleDownloadTemplate = () => {
        // Create a new workbook
        const workbook = XLSX.utils.book_new();

        // --- Sheet 1: MCQ_Questions (If MCQ or HYBRID) ---
        if (testType === 'MCQ' || testType === 'MCQ_ONLY' || testType === 'HYBRID') {
            const mcqHeaders = ["Question Text", "Option A", "Option B", "Option C", "Option D", "Correct Answer", "Marks"];
            const mcqSample = [
                {
                    "Question Text": "What is the time complexity of Binary Search?",
                    "Option A": "O(n)",
                    "Option B": "O(log n)",
                    "Option C": "O(1)",
                    "Option D": "O(n^2)",
                    "Correct Answer": "B",
                    "Marks": 5
                }
            ];
            const mcqSheet = XLSX.utils.json_to_sheet(mcqSample, { header: mcqHeaders });
            XLSX.utils.book_append_sheet(workbook, mcqSheet, "MCQ_Questions");
        }

        // --- Sheet 2: Coding_Questions (If CODING or HYBRID) ---
        if (testType === 'CODING' || testType === 'CODING_ONLY' || testType === 'HYBRID') {
            const codingHeaders = ["Title", "Description", "Constraints", "Allowed Languages", "Marks", "Input 1", "Output 1", "Input 2", "Output 2"];
            const codingSample = [
                {
                    "Title": "Fibonacci Recursive",
                    "Description": "Calculate Nth number using recursion.",
                    "Constraints": "Ban Loops, Require Recursion",
                    "Allowed Languages": "Java, Python",
                    "Marks": 10,
                    "Input 1": "5",
                    "Output 1": "5",
                    "Input 2": "", // Optional
                    "Output 2": "" // Optional
                }
            ];
            const codingSheet = XLSX.utils.json_to_sheet(codingSample, { header: codingHeaders });
            XLSX.utils.book_append_sheet(workbook, codingSheet, "Coding_Questions");
        }

        // Write and download
        let fileName = "Hybrid_Test_Template.xlsx";
        if (testType === 'MCQ' || testType === 'MCQ_ONLY') {
            fileName = "MCQ_Template.xlsx";
        } else if (testType === 'CODING' || testType === 'CODING_ONLY') {
            fileName = "Coding_Template.xlsx";
        }

        XLSX.writeFile(workbook, fileName);
        toast.success(`Downloaded ${fileName}`);
    };

    const copyPasteFormat = () => {
        let text = "";
        const mcqFormat = `1. Question Text?\nA) Option A\nB) Option B\nC) Option C\nD) Option D\nAnswer: B`;
        const codingFormat = `Title: Question Title\nMarks: 10\nDescription: Problem details...\nConstraints: Ban Loops\nInput: 5 5\nOutput: 10`;

        if (testType === 'MCQ' || testType === 'MCQ_ONLY') {
            text = mcqFormat;
        } else if (testType === 'CODING' || testType === 'CODING_ONLY') {
            text = codingFormat;
        } else {
            text = `${mcqFormat}\n\n-- OR --\n\n${codingFormat}`;
        }

        navigator.clipboard.writeText(text);
        toast.success("Format copied to clipboard!");
    };

    // ============ RENDER ============
    return (
        <div className="bg-gray-800 rounded-lg p-6 text-white">
            {/* Header */}
            <h2 className="text-2xl font-bold mb-6 flex items-center gap-2">
                <Upload size={28} className="text-blue-400" />
                Bulk Question Uploader
            </h2>

            {/* Tabs - Only show if hideTabs is false */}
            {!hideTabs && (
                <div className="flex gap-4 mb-6 border-b border-gray-700">
                    <button
                        onClick={() => setActiveTab('file')}
                        className={`px-6 py-3 font-semibold transition-all ${activeTab === 'file'
                            ? 'text-blue-400 border-b-2 border-blue-400'
                            : 'text-gray-400 hover:text-white'
                            }`}
                    >
                        <FileText size={20} className="inline mr-2" />
                        File Upload (CSV/Excel)
                    </button>
                    <button
                        onClick={() => setActiveTab('paste')}
                        className={`px-6 py-3 font-semibold transition-all ${activeTab === 'paste'
                            ? 'text-blue-400 border-b-2 border-blue-400'
                            : 'text-gray-400 hover:text-white'
                            }`}
                    >
                        <Clipboard size={20} className="inline mr-2" />
                        Smart Paste
                    </button>
                </div>
            )}

            {/* Content Area */}
            <div className="min-h-[300px]">
                {/* FILE UPLOAD TAB */}
                {activeTab === 'file' && (
                    <div className="space-y-4">
                        {/* File Input */}
                        <div className="border-2 border-dashed border-gray-600 rounded-lg p-8 text-center hover:border-blue-500 transition-all cursor-pointer bg-gray-900/50">
                            <input
                                type="file"
                                accept=".csv, .xlsx, .xls"
                                onChange={handleFileSelect}
                                className="hidden"
                                id="file-upload-input"
                            />
                            <label htmlFor="file-upload-input" className="cursor-pointer">
                                <FileText size={64} className="mx-auto text-gray-500 mb-4" />
                                <p className="text-xl font-semibold text-white mb-2">
                                    {uploadFile ? uploadFile.name : 'Click to Upload File'}
                                </p>
                                <p className="text-sm text-gray-400">
                                    Supports .xlsx, .xls, .csv formats
                                </p>
                            </label>
                        </div>

                        {/* Actions: Upload & Download Template */}
                        <div className="flex gap-3">
                            <button
                                onClick={handleFileUpload}
                                disabled={!uploadFile || uploading}
                                className="flex-1 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded-lg font-bold text-lg transition flex items-center justify-center gap-2"
                            >
                                {uploading ? (
                                    <>
                                        <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                                        Uploading...
                                    </>
                                ) : (
                                    <>
                                        <Upload size={20} />
                                        Upload File
                                    </>
                                )}
                            </button>

                            {/* Template Buttons */}
                            <div className="flex gap-2">
                                <button
                                    onClick={handleDownloadTemplate}
                                    className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded inline-flex items-center gap-2 transition"
                                    title="Download Excel Template"
                                >
                                    <Download size={20} />
                                    Download Template
                                </button>
                            </div>
                        </div>

                        {/* Info */}
                        <div className="bg-gray-700 p-4 rounded-lg text-sm text-gray-300">
                            <p className="font-semibold mb-2">📋 Expected Format:</p>
                            <ul className="list-disc list-inside space-y-1">
                                {(testType === 'MCQ' || testType === 'HYBRID') && (
                                    <li><strong>MCQ:</strong> Type, Question Text, Marks, Options (A-D), Correct Answer</li>
                                )}
                                {(testType === 'CODING' || testType === 'HYBRID') && (
                                    <li><strong>Coding:</strong> Type, Title, Description, Marks, Test Cases (Input/Output)</li>
                                )}
                            </ul>
                        </div>
                    </div>
                )}

                {/* SMART PASTE TAB */}
                {activeTab === 'paste' && (
                    <div className="space-y-4">
                        {/* UNIFIED HEADER */}
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="font-semibold text-lg text-white">Paste Questions</h3>

                            {/* Copy Format Button */}
                            <button
                                onClick={() => copyPasteFormat()}
                                className="text-sm bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition shadow-sm font-medium"
                            >
                                <Clipboard size={16} /> Copy Format
                            </button>
                        </div>

                        {/* Textarea */}
                        <textarea
                            value={pasteText}
                            onChange={(e) => setPasteText(e.target.value)}
                            placeholder={
                                (testType === 'MCQ' || testType === 'MCQ_ONLY')
                                    ? `Paste MCQ questions here...\n\nExample:\n1. What is Java?\nA) Language\nB) Coffee\nAnswer: A`
                                    : (testType === 'CODING' || testType === 'CODING_ONLY')
                                        ? `Paste Coding questions here...\n\nExample:\nTitle: Sum of Array Elements\nDescription: Write a function that returns the sum of all numbers in an array.\nConstraints: Ban Loops, Require Recursion\nAllowed Languages: Java, Python\nMarks: 10\nInput: 1 2 3 4 5\nOutput: 15\nInput: 10 -10\nOutput: 0`
                                        : `Paste mixed MCQs and Coding questions here...\n\n-- Example 1 (MCQ) --\n1. What is the capital of France?\nA) London\nB) Paris\nC) Berlin\nD) Madrid\nAnswer: B\n\n-- Example 2 (Coding) --\nTitle: Sum of Array Elements\nDescription: Write a function that returns the sum of all numbers in an array.\nConstraints: Ban Loops, Require Recursion\nAllowed Languages: Java, Python\nMarks: 10\nInput: 1 2 3 4 5\nOutput: 15`
                            }
                            className="w-full h-80 bg-gray-900 border border-gray-700 rounded-lg p-5 font-mono text-sm text-gray-300 focus:outline-none focus:border-blue-500 resize-none leading-relaxed"
                        />

                        {/* Parse & Upload Button */}
                        <button
                            onClick={handlePasteUpload}
                            disabled={!pasteText.trim() || uploading}
                            className="w-full py-4 bg-green-600 hover:bg-green-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded-lg font-bold text-lg transition flex items-center justify-center gap-3 shadow-lg hover:shadow-green-900/50"
                        >
                            {uploading ? (
                                <>
                                    <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-white"></div>
                                    Processing...
                                </>
                            ) : (
                                <>
                                    <Clipboard size={24} />
                                    Parse & Upload Questions
                                </>
                            )}
                        </button>
                    </div>
                )}
            </div>

            {/* UNIFIED RESULT MODAL */}
            {showResultModal && uploadResult && (
                <div className="fixed inset-0 bg-black bg-opacity-70 flex items-center justify-center z-50">
                    <div className="bg-gray-800 rounded-lg p-8 max-w-3xl w-full mx-4 shadow-2xl max-h-[90vh] overflow-y-auto">
                        {/* Header */}
                        <div className="flex justify-between items-center mb-6">
                            <h2 className="text-3xl font-bold text-white">Upload Complete</h2>
                            <button
                                onClick={closeResultModal}
                                className="text-gray-400 hover:text-white transition"
                            >
                                <X size={28} />
                            </button>
                        </div>

                        {/* Summary Stats */}
                        <div className="grid grid-cols-3 gap-6 mb-8">
                            <div className="bg-gray-700 p-6 rounded-lg text-center">
                                <div className="text-4xl font-bold text-blue-400 mb-2">
                                    {uploadResult.total}
                                </div>
                                <div className="text-sm text-gray-300 font-semibold">Total Rows</div>
                            </div>
                            <div className="bg-green-900/30 p-6 rounded-lg text-center border-2 border-green-700">
                                <div className="text-4xl font-bold text-green-400 flex items-center justify-center gap-2 mb-2">
                                    <CheckCircle size={32} />
                                    {uploadResult.saved}
                                </div>
                                <div className="text-sm text-green-300 font-semibold">✅ Saved</div>
                            </div>
                            <div className="bg-red-900/30 p-6 rounded-lg text-center border-2 border-red-700">
                                <div className="text-4xl font-bold text-red-400 flex items-center justify-center gap-2 mb-2">
                                    <XCircle size={32} />
                                    {uploadResult.failed}
                                </div>
                                <div className="text-sm text-red-300 font-semibold">❌ Failed</div>
                            </div>
                        </div>

                        {/* Success Message */}
                        {uploadResult.saved > 0 && (
                            <div className="bg-green-900/20 border-2 border-green-700 rounded-lg p-5 mb-6 flex items-start gap-4">
                                <CheckCircle size={28} className="text-green-400 flex-shrink-0 mt-1" />
                                <div>
                                    <p className="text-green-300 font-bold text-lg mb-1">
                                        ✅ {uploadResult.saved} Question{uploadResult.saved !== 1 ? 's' : ''} Saved Successfully
                                    </p>
                                    <p className="text-green-400 text-sm">
                                        Questions have been added to the question bank and are ready to use.
                                    </p>
                                </div>
                            </div>
                        )}

                        {/* Error Section */}
                        {uploadResult.failed > 0 && uploadResult.errors && uploadResult.errors.length > 0 && (
                            <div className="bg-red-900/20 border-2 border-red-700 rounded-lg p-5 mb-6">
                                <div className="flex items-start gap-4 mb-4">
                                    <AlertTriangle size={28} className="text-red-400 flex-shrink-0 mt-1" />
                                    <div className="flex-1">
                                        <p className="text-red-300 font-bold text-lg mb-1">
                                            ❌ {uploadResult.failed} Row{uploadResult.failed !== 1 ? 's' : ''} Failed
                                        </p>
                                        <p className="text-red-400 text-sm">
                                            Review the errors below and fix the issues in your file.
                                        </p>
                                    </div>
                                </div>

                                {/* Error Table */}
                                <div className="bg-gray-900 rounded-lg p-4 max-h-72 overflow-y-auto">
                                    <table className="w-full text-sm">
                                        <thead className="text-gray-400 border-b border-gray-700 sticky top-0 bg-gray-900">
                                            <tr>
                                                <th className="text-left py-3 px-3 font-semibold">Row #</th>
                                                <th className="text-left py-3 px-3 font-semibold">Error Reason</th>
                                            </tr>
                                        </thead>
                                        <tbody className="text-gray-300">
                                            {uploadResult.errors.map((error, idx) => (
                                                <tr key={idx} className="border-b border-gray-800 hover:bg-gray-800/50">
                                                    <td className="py-3 px-3 font-mono text-red-400 font-bold">
                                                        #{error.row || 'N/A'}
                                                    </td>
                                                    <td className="py-3 px-3">
                                                        {typeof error === 'string' ? error : (error.reason || error.message || 'Unknown error')}
                                                        {error.details && (
                                                            <span className="block text-xs text-gray-500 mt-1">
                                                                {error.details}
                                                            </span>
                                                        )}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>

                                {/* Download Error Report Button */}
                                <button
                                    onClick={downloadErrorReport}
                                    className="mt-5 w-full py-3 bg-red-600 hover:bg-red-700 text-white rounded-lg font-bold transition flex items-center justify-center gap-3 text-lg"
                                >
                                    <Download size={22} />
                                    Download Error Report (CSV)
                                </button>
                            </div>
                        )}

                        {/* Close Button */}
                        <button
                            onClick={closeResultModal}
                            className="w-full py-4 bg-gray-700 hover:bg-gray-600 text-white rounded-lg font-bold text-lg transition"
                        >
                            Close
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default BulkUploadComponent;
