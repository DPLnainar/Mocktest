import { useEffect, useState } from 'react';
import { useTestStore } from '../store/testStore';
import toast from 'react-hot-toast';

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
    const [formData, setFormData] = useState({
        type: 'MCQ',
        questionText: '',
        marks: 1,
        optionA: '',
        optionB: '',
        optionC: '',
        optionD: '',
        correctOption: 'A',
        languageId: 62, // Java
        starterCode: '',
    });

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await createQuestion(formData);
            onClose();
        } catch (error) {
            // Error handled in store
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-gray-800 rounded-lg p-6 max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                <h2 className="text-2xl font-bold text-white mb-6">Create Question</h2>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-gray-300 mb-2">Question Type</label>
                        <select
                            value={formData.type}
                            onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                            className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                        >
                            <option value="MCQ">Multiple Choice (MCQ)</option>
                            <option value="CODING">Coding Question</option>
                        </select>
                    </div>

                    <div>
                        <label className="block text-gray-300 mb-2">Question Text *</label>
                        <textarea
                            required
                            value={formData.questionText}
                            onChange={(e) => setFormData({ ...formData, questionText: e.target.value })}
                            className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                            rows="3"
                        />
                    </div>

                    <div>
                        <label className="block text-gray-300 mb-2">Marks *</label>
                        <input
                            type="number"
                            required
                            min="1"
                            value={formData.marks}
                            onChange={(e) => setFormData({ ...formData, marks: parseInt(e.target.value) })}
                            className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                        />
                    </div>

                    {formData.type === 'MCQ' && (
                        <>
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-gray-300 mb-2">Option A *</label>
                                    <input
                                        type="text"
                                        required
                                        value={formData.optionA}
                                        onChange={(e) => setFormData({ ...formData, optionA: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                                    />
                                </div>
                                <div>
                                    <label className="block text-gray-300 mb-2">Option B *</label>
                                    <input
                                        type="text"
                                        required
                                        value={formData.optionB}
                                        onChange={(e) => setFormData({ ...formData, optionB: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                                    />
                                </div>
                                <div>
                                    <label className="block text-gray-300 mb-2">Option C *</label>
                                    <input
                                        type="text"
                                        required
                                        value={formData.optionC}
                                        onChange={(e) => setFormData({ ...formData, optionC: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                                    />
                                </div>
                                <div>
                                    <label className="block text-gray-300 mb-2">Option D *</label>
                                    <input
                                        type="text"
                                        required
                                        value={formData.optionD}
                                        onChange={(e) => setFormData({ ...formData, optionD: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                                    />
                                </div>
                            </div>
                            <div>
                                <label className="block text-gray-300 mb-2">Correct Answer *</label>
                                <select
                                    value={formData.correctOption}
                                    onChange={(e) => setFormData({ ...formData, correctOption: e.target.value })}
                                    className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                                >
                                    <option value="A">A</option>
                                    <option value="B">B</option>
                                    <option value="C">C</option>
                                    <option value="D">D</option>
                                </select>
                            </div>
                        </>
                    )}

                    {formData.type === 'CODING' && (
                        <>
                            <div>
                                <label className="block text-gray-300 mb-2">Language ID (Judge0) *</label>
                                <input
                                    type="number"
                                    required
                                    value={formData.languageId}
                                    onChange={(e) => setFormData({ ...formData, languageId: parseInt(e.target.value) })}
                                    className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                                    placeholder="e.g., 62 for Java, 71 for Python"
                                />
                            </div>
                            <div>
                                <label className="block text-gray-300 mb-2">Starter Code (optional)</label>
                                <textarea
                                    value={formData.starterCode}
                                    onChange={(e) => setFormData({ ...formData, starterCode: e.target.value })}
                                    className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none font-mono text-sm"
                                    rows="5"
                                />
                            </div>
                        </>
                    )}

                    <div className="flex gap-3 pt-4">
                        <button
                            type="submit"
                            className="flex-1 px-6 py-3 bg-green-600 hover:bg-green-700 text-white rounded-lg transition font-semibold"
                        >
                            Create Question
                        </button>
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-6 py-3 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
