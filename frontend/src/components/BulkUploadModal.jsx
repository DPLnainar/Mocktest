import React, { useState } from 'react';
import { Upload, Download, CheckCircle, XCircle, AlertTriangle, FileText, X } from 'lucide-react';
import { questionAPI } from '../services/testAPI';
import toast from 'react-hot-toast';

/**
 * Enhanced Bulk Upload Component with Summary Modal and Error Reporting
 */
const BulkUploadModal = ({ isOpen, onClose, onSuccess, testType = 'HYBRID' }) => {
    const [uploadFile, setUploadFile] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [uploadResult, setUploadResult] = useState(null);
    const [showResultModal, setShowResultModal] = useState(false);

    if (!isOpen) return null;

    const handleFileSelect = (e) => {
        const file = e.target.files[0];
        if (file) {
            setUploadFile(file);
            setUploadResult(null);
        }
    };

    const handleUpload = async () => {
        if (!uploadFile) {
            toast.error('Please select a file first');
            return;
        }

        setUploading(true);
        try {
            const response = await questionAPI.bulkUpload(uploadFile);
            const result = response.data;

            // Expected backend response format:
            // { total: 100, saved: 95, failed: 5, errors: [{row: 12, reason: "Missing Input"}], questionIds: [...] }

            setUploadResult({
                total: result.total || result.successCount + result.errorCount || 0,
                saved: result.saved || result.successCount || 0,
                failed: result.failed || result.errorCount || 0,
                errors: result.errors || [],
                questionIds: result.questionIds || []
            });

            setShowResultModal(true);

            if (result.saved > 0 || result.successCount > 0) {
                toast.success(`Successfully uploaded ${result.saved || result.successCount} questions!`);
                if (onSuccess) {
                    onSuccess(result);
                }
            }

        } catch (error) {
            console.error('Upload error:', error);
            toast.error(error.response?.data?.message || 'Failed to upload questions');

            // Show error modal even on complete failure
            setUploadResult({
                total: 0,
                saved: 0,
                failed: 0,
                errors: [{ row: 0, reason: error.response?.data?.message || 'Upload failed' }]
            });
            setShowResultModal(true);
        } finally {
            setUploading(false);
        }
    };

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
        setUploadResult(null);
    };

    const closeMainModal = () => {
        setUploadFile(null);
        setUploadResult(null);
        setShowResultModal(false);
        onClose();
    };

    return (
        <>
            {/* Main Upload Modal */}
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                <div className="bg-gray-800 rounded-lg p-6 max-w-md w-full mx-4 shadow-2xl">
                    <div className="flex justify-between items-center mb-4">
                        <h2 className="text-xl font-bold text-white flex items-center gap-2">
                            <Upload size={24} className="text-blue-400" />
                            Bulk Upload Questions
                        </h2>
                        <button
                            onClick={closeMainModal}
                            className="text-gray-400 hover:text-white transition"
                        >
                            <X size={24} />
                        </button>
                    </div>

                    <div className="space-y-4">
                        {/* File Input */}
                        <div className="border-2 border-dashed border-gray-600 rounded-lg p-6 text-center hover:border-gray-500 transition">
                            <input
                                type="file"
                                accept=".csv, .xlsx, .xls"
                                onChange={handleFileSelect}
                                className="hidden"
                                id="bulk-upload-input"
                            />
                            <label htmlFor="bulk-upload-input" className="cursor-pointer">
                                <FileText size={48} className="mx-auto text-gray-500 mb-2" />
                                <p className="text-white font-semibold">
                                    {uploadFile ? uploadFile.name : 'Click to select file'}
                                </p>
                                <p className="text-sm text-gray-400 mt-1">
                                    Supports .xlsx, .xls, .csv
                                </p>
                            </label>
                        </div>

                        {/* Upload Button */}
                        <button
                            onClick={handleUpload}
                            disabled={!uploadFile || uploading}
                            className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded-lg font-semibold transition flex items-center justify-center gap-2"
                        >
                            {uploading ? (
                                <>
                                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                                    Uploading...
                                </>
                            ) : (
                                <>
                                    <Upload size={20} />
                                    Upload Questions
                                </>
                            )}
                        </button>

                        {/* Info */}
                        <div className="bg-gray-700 p-3 rounded text-sm text-gray-300">
                            <p className="font-semibold mb-1">📋 Upload Format:</p>
                            <ul className="list-disc list-inside space-y-1 text-xs">
                                <li>MCQ: Type, Question, Marks, Options, Answer</li>
                                <li>Coding: Type, Title, Description, Test Cases</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>

            {/* Result Summary Modal */}
            {showResultModal && uploadResult && (
                <div className="fixed inset-0 bg-black bg-opacity-70 flex items-center justify-center z-[60]">
                    <div className="bg-gray-800 rounded-lg p-6 max-w-2xl w-full mx-4 shadow-2xl max-h-[90vh] overflow-y-auto">
                        {/* Header */}
                        <div className="flex justify-between items-center mb-6">
                            <h2 className="text-2xl font-bold text-white">Upload Complete</h2>
                            <button
                                onClick={closeResultModal}
                                className="text-gray-400 hover:text-white transition"
                            >
                                <X size={24} />
                            </button>
                        </div>

                        {/* Summary Stats */}
                        <div className="grid grid-cols-3 gap-4 mb-6">
                            <div className="bg-gray-700 p-4 rounded-lg text-center">
                                <div className="text-3xl font-bold text-blue-400">{uploadResult.total}</div>
                                <div className="text-sm text-gray-300 mt-1">Total Rows</div>
                            </div>
                            <div className="bg-green-900/30 p-4 rounded-lg text-center border border-green-700">
                                <div className="text-3xl font-bold text-green-400 flex items-center justify-center gap-2">
                                    <CheckCircle size={28} />
                                    {uploadResult.saved}
                                </div>
                                <div className="text-sm text-green-300 mt-1">Saved Successfully</div>
                            </div>
                            <div className="bg-red-900/30 p-4 rounded-lg text-center border border-red-700">
                                <div className="text-3xl font-bold text-red-400 flex items-center justify-center gap-2">
                                    <XCircle size={28} />
                                    {uploadResult.failed}
                                </div>
                                <div className="text-sm text-red-300 mt-1">Failed</div>
                            </div>
                        </div>

                        {/* Success Message */}
                        {uploadResult.saved > 0 && (
                            <div className="bg-green-900/20 border border-green-700 rounded-lg p-4 mb-4 flex items-start gap-3">
                                <CheckCircle size={24} className="text-green-400 flex-shrink-0 mt-0.5" />
                                <div>
                                    <p className="text-green-300 font-semibold">
                                        ✅ {uploadResult.saved} Question{uploadResult.saved !== 1 ? 's' : ''} Saved Successfully
                                    </p>
                                    <p className="text-green-400 text-sm mt-1">
                                        Questions have been added to the question bank.
                                    </p>
                                </div>
                            </div>
                        )}

                        {/* Error Section */}
                        {uploadResult.failed > 0 && uploadResult.errors && uploadResult.errors.length > 0 && (
                            <div className="bg-red-900/20 border border-red-700 rounded-lg p-4 mb-4">
                                <div className="flex items-start gap-3 mb-3">
                                    <AlertTriangle size={24} className="text-red-400 flex-shrink-0 mt-0.5" />
                                    <div className="flex-1">
                                        <p className="text-red-300 font-semibold">
                                            ❌ {uploadResult.failed} Row{uploadResult.failed !== 1 ? 's' : ''} Failed
                                        </p>
                                        <p className="text-red-400 text-sm mt-1">
                                            Review the errors below and fix the issues in your file.
                                        </p>
                                    </div>
                                </div>

                                {/* Error List */}
                                <div className="bg-gray-900 rounded p-3 max-h-60 overflow-y-auto">
                                    <table className="w-full text-sm">
                                        <thead className="text-gray-400 border-b border-gray-700">
                                            <tr>
                                                <th className="text-left py-2 px-2">Row</th>
                                                <th className="text-left py-2 px-2">Error</th>
                                            </tr>
                                        </thead>
                                        <tbody className="text-gray-300">
                                            {uploadResult.errors.map((error, idx) => (
                                                <tr key={idx} className="border-b border-gray-800">
                                                    <td className="py-2 px-2 font-mono text-red-400">
                                                        #{error.row || 'N/A'}
                                                    </td>
                                                    <td className="py-2 px-2">
                                                        {error.reason || error.message || 'Unknown error'}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>

                                {/* Download Error Report Button */}
                                <button
                                    onClick={downloadErrorReport}
                                    className="mt-4 w-full py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg font-semibold transition flex items-center justify-center gap-2"
                                >
                                    <Download size={18} />
                                    Download Error Report (CSV)
                                </button>
                            </div>
                        )}

                        {/* Close Button */}
                        <button
                            onClick={closeResultModal}
                            className="w-full py-3 bg-gray-700 hover:bg-gray-600 text-white rounded-lg font-semibold transition"
                        >
                            Close
                        </button>
                    </div>
                </div>
            )}
        </>
    );
};

export default BulkUploadModal;
