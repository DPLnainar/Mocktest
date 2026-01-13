import { useEffect, useState } from 'react';
import { useTestStore } from '../store/testStore';
import toast from 'react-hot-toast';
import QuestionForm from '../components/QuestionForm';

export default function QuestionBankPage() {
    const { questions, loading, fetchQuestions, createQuestion, bulkUploadQuestions } = useTestStore();
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [uploadFile, setUploadFile] = useState(null);

    useEffect(() => {
        fetchQuestions();
    }, []);

    const handleBulkUpload = async () => {
        if (!uploadFile) {
            toast.error('Please select a file');
            return;
        }

        try {
            await bulkUploadQuestions(uploadFile);
            setUploadFile(null);
        } catch (error) {
            // Error handled in store
        }
    };

    return (
        <div className="min-h-screen bg-gray-900 p-8">
            <div className="max-w-7xl mx-auto">
                <div className="flex justify-between items-center mb-8">
                    <h1 className="text-3xl font-bold text-white">Question Bank</h1>
                    <div className="flex gap-3">
                        <button
                            onClick={() => setShowCreateModal(true)}
                            className="px-6 py-3 bg-green-600 hover:bg-green-700 text-white rounded-lg transition font-semibold"
                        >
                            + Add Question
                        </button>
                    </div>
                </div>

                {/* Bulk Upload Section */}
                <div className="bg-gray-800 rounded-lg p-6 border border-gray-700 mb-6">
                    <h2 className="text-xl font-semibold text-white mb-4">Bulk Upload Questions</h2>
                    <p className="text-gray-400 mb-4 text-sm">
                        Upload multiple questions at once using an Excel file (.xlsx)
                    </p>
                    <div className="flex gap-3 items-center">
                        <input
                            type="file"
                            accept=".xlsx"
                            onChange={(e) => setUploadFile(e.target.files[0])}
                            className="text-gray-300"
                        />
                        <button
                            onClick={handleBulkUpload}
                            disabled={!uploadFile}
                            className="px-6 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 text-white rounded-lg transition"
                        >
                            Upload
                        </button>
                        <a
                            href="/question-template.xlsx"
                            download
                            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition text-sm"
                        >
                            Download Template
                        </a>
                    </div>
                </div>

                {/* Questions List */}
                {loading ? (
                    <div className="text-center text-white py-12">Loading questions...</div>
                ) : questions.length === 0 ? (
                    <div className="text-center text-gray-400 py-12">
                        No questions found. Add your first question!
                    </div>
                ) : (
                    <div className="grid gap-4">
                        {questions.map((question) => (
                            <QuestionCard key={question.id} question={question} />
                        ))}
                    </div>
                )}

                {showCreateModal && (
                    <CreateQuestionModal onClose={() => setShowCreateModal(false)} />
                )}
            </div>
        </div>
    );
}

function QuestionCard({ question }) {
    return (
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
            <div className="flex justify-between items-start mb-3">
                <div className="flex items-center gap-3">
                    <span className={`px-3 py-1 rounded-full text-sm ${question.type === 'MCQ' ? 'bg-purple-600' : 'bg-blue-600'
                        } text-white`}>
                        {question.type}
                    </span>
                    <span className="text-gray-400 text-sm">{question.marks} marks</span>
                </div>
            </div>
            <p className="text-white text-lg mb-3">{question.questionText}</p>

            {question.type === 'MCQ' && (
                <div className="grid grid-cols-2 gap-2 text-sm text-gray-300">
                    <div>A. {question.optionA}</div>
                    <div>B. {question.optionB}</div>
                    <div>C. {question.optionC}</div>
                    <div>D. {question.optionD}</div>
                </div>
            )}

            {question.type === 'CODING' && (
                <p className="text-gray-400 text-sm">Language ID: {question.languageId}</p>
            )}
        </div>
    );
}

function CreateQuestionModal({ onClose }) {
    const { createQuestion } = useTestStore();
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (formData) => {
        setLoading(true);
        try {
            await createQuestion(formData);
            onClose();
        } catch (error) {
            // Error handled in store
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-gray-800 rounded-lg p-6 max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                <div className="flex justify-between items-center mb-6">
                    <h2 className="text-2xl font-bold text-white">Create Question</h2>
                    <button onClick={onClose} className="text-gray-400 hover:text-white text-2xl">&times;</button>
                </div>

                <QuestionForm onSubmit={handleSubmit} loading={loading} />

                <div className="mt-4">
                    <button
                        onClick={onClose}
                        className="w-full py-3 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition"
                    >
                        Cancel
                    </button>
                </div>
            </div>
        </div>
    );
}
