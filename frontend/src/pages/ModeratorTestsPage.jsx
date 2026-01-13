import { useEffect, useState } from 'react';
import { useTestStore } from '../store/testStore';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuthStore } from '../store/authStore';


import QuestionForm from '../components/QuestionForm';
import { Menu } from 'lucide-react';

export default function ModeratorTestsPage() {
    const { tests, loading, fetchTests, deleteTest } = useTestStore();
    const { logout } = useAuthStore();
    const [managingTest, setManagingTest] = useState(null);
    const [showMenu, setShowMenu] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        console.log("ModeratorTestsPage Mounted. Fetching tests...");
        fetchTests();
    }, []);

    const handleLogout = () => {
        if (window.confirm('Are you sure you want to logout?')) {
            logout();
            navigate('/login');
            toast.success('Logged out successfully');
        }
    }

    const handleDelete = async (id) => {
        if (window.confirm('Are you sure you want to delete this test?')) {
            try {
                await deleteTest(id);
            } catch (error) {
                // Error already shown in store
            }
        }
    };

    const handlePublish = async (test) => {
        const newStatus = test.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED';
        const action = newStatus === 'PUBLISHED' ? 'publish' : 'unpublish';

        if (window.confirm(`Are you sure you want to ${action} this test?`)) {
            try {
                const { testAPI } = await import('../services/testAPI');
                await testAPI.publishTest(test.id, newStatus);
                toast.success(`Test ${action}ed successfully!`);
                fetchTests(); // Refresh the list
            } catch (error) {
                toast.error(`Failed to ${action} test`);
            }
        }
    };

    return (
        <div className="min-h-screen bg-gray-900 p-8">
            <div className="max-w-7xl mx-auto">
                <div className="flex justify-between items-center mb-8">
                    <h1 className="text-3xl font-bold text-white">Test Management</h1>

                    <div className="flex gap-4 items-center">
                        {/* Hamburger Menu */}
                        <div className="relative">
                            <button
                                onClick={() => setShowMenu(!showMenu)}
                                className="p-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                            >
                                <Menu size={24} />
                            </button>

                            {showMenu && (
                                <div className="absolute right-0 mt-2 w-48 bg-gray-800 rounded-lg shadow-xl border border-gray-700 z-50">
                                    <button
                                        onClick={() => {
                                            navigate('/moderator/test');
                                            setShowMenu(false);
                                        }}
                                        className="w-full text-left px-4 py-3 text-white hover:bg-gray-700 first:rounded-t-lg transition"
                                    >
                                        + Create Test
                                    </button>
                                    <button
                                        onClick={() => {
                                            navigate('/moderator/tests');
                                            setShowMenu(false);
                                        }}
                                        className="w-full text-left px-4 py-3 text-white hover:bg-gray-700 border-t border-gray-700 transition"
                                    >
                                        Test Management
                                    </button>
                                </div>
                            )}
                        </div>

                        <button
                            onClick={handleLogout}
                            className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition border border-red-500"
                        >
                            Logout
                        </button>
                    </div>
                </div>

                {loading ? (
                    <div className="text-center text-white py-12">Loading tests...</div>
                ) : (!tests || tests.length === 0) ? (
                    <div className="text-center text-gray-400 py-12">
                        No tests found. Create your first test!
                    </div>
                ) : (
                    <div className="grid gap-6">
                        {tests.map((test) => (
                            <TestCard
                                key={test.id}
                                test={test}
                                onDelete={handleDelete}
                                onPublish={handlePublish}
                            />
                        ))}
                    </div>
                )}



                {managingTest && (
                    <ManageTestQuestionsModal
                        test={managingTest}
                        onClose={() => setManagingTest(null)}
                    />
                )}
            </div>
        </div>
    );
}

function TestCard({ test, onDelete, onPublish }) {
    const formatDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    const getStatusBadge = () => {
        // Use backend status if available, otherwise fall back to time-based
        if (test.status) {
            const statusConfig = {
                'DRAFT': { bg: 'bg-yellow-600', text: 'Draft' },
                'PUBLISHED': { bg: 'bg-green-600', text: 'Published' },
                'ARCHIVED': { bg: 'bg-gray-600', text: 'Archived' }
            };
            const config = statusConfig[test.status] || statusConfig['DRAFT'];
            return <span className={`px-3 py-1 ${config.bg} text-white text-sm rounded-full`}>{config.text}</span>;
        }

        // Fallback to time-based status
        const now = new Date();
        const start = new Date(test.startDateTime);
        const end = new Date(test.endDateTime);

        if (now < start) {
            return <span className="px-3 py-1 bg-yellow-600 text-white text-sm rounded-full">Upcoming</span>;
        } else if (now > end) {
            return <span className="px-3 py-1 bg-gray-600 text-white text-sm rounded-full">Expired</span>;
        } else {
            return <span className="px-3 py-1 bg-green-600 text-white text-sm rounded-full">Active</span>;
        }
    };

    const isPublished = test.status === 'PUBLISHED';

    return (
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
            <div className="flex justify-between items-start mb-4">
                <div>
                    <h3 className="text-xl font-semibold text-white mb-2">{test.title}</h3>
                    {test.description && (
                        <p className="text-gray-400 text-sm">{test.description}</p>
                    )}
                </div>
                <div className="flex flex-col items-end gap-2">
                    {getStatusBadge()}
                    <span className={`px-3 py-1 text-xs rounded-full border ${test.type === 'MCQ_ONLY' ? 'border-purple-500 text-purple-400' :
                        test.type === 'CODING_ONLY' ? 'border-blue-500 text-blue-400' :
                            'border-green-500 text-green-400'
                        }`}>
                        {test.type?.replace('_ONLY', '')}
                    </span>
                </div>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4 text-sm">
                <div>
                    <p className="text-gray-500">Start</p>
                    <p className="text-white">{formatDate(test.startDateTime)}</p>
                </div>
                <div>
                    <p className="text-gray-500">End</p>
                    <p className="text-white">{formatDate(test.endDateTime)}</p>
                </div>
                <div>
                    <p className="text-gray-500">Duration</p>
                    <p className="text-white">{test.durationMinutes} min</p>
                </div>
                <div>
                    <p className="text-gray-500">Questions</p>
                    <p className="text-white">{test.testQuestions?.length || 0}</p>
                </div>
            </div>

            <div className="flex gap-3 flex-wrap">
                <button
                    onClick={() => window.location.href = `/moderator/tests/${test.id}/analytics`}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded text-sm transition"
                >
                    View Results
                </button>
                {/* Publish/Unpublish button */}
                <button
                    onClick={() => onPublish(test)}
                    className={`px-4 py-2 ${isPublished ? 'bg-orange-600 hover:bg-orange-700' : 'bg-purple-600 hover:bg-purple-700'} text-white rounded text-sm transition`}
                >
                    {isPublished ? '📤 Unpublish' : '🚀 Publish'}
                </button>
                <button
                    onClick={() => onDelete(test.id)}
                    className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded text-sm transition"
                >
                    Delete
                </button>
            </div>
        </div>
    );
}

function CreateTestModal({ onClose }) {
    const { createTest, bulkUploadQuestions, pasteQuestions } = useTestStore();
    const [uploadFile, setUploadFile] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const [activeTab, setActiveTab] = useState('upload'); // 'upload' or 'paste'
    const [pastedText, setPastedText] = useState('');
    const [formData, setFormData] = useState({
        title: '',
        description: '',
        startDateTime: '',
        endDateTime: '',
        durationMinutes: 60,
        testQuestions: [],
        type: 'HYBRID', // MCQ_ONLY, CODING_ONLY, HYBRID
        testType: '', // "Placement Drive", "Practice", "Contest"
        instructions: '', // Rich text instructions
    });
    const [targetSection, setTargetSection] = useState('Part-I');

    const handleFileUpload = async () => {
        if (!uploadFile) {
            toast.error('Please select a file to upload');
            return;
        }
        setIsUploading(true);
        try {
            const { questionAPI } = await import('../services/testAPI');
            const result = await questionAPI.bulkUpload(uploadFile);
            // Assuming result contains an array of uploaded question IDs
            if (result && result.questionIds) {
                const newTQs = result.questionIds.map(id => ({
                    questionId: id,
                    marks: 1,
                    sectionName: targetSection,
                    orderIndex: formData.testQuestions.length + result.questionIds.indexOf(id),
                }));
                setFormData(prev => ({
                    ...prev,
                    testQuestions: [...prev.testQuestions, ...newTQs],
                }));
                toast.success(`Successfully uploaded ${result.questionIds.length} questions to ${targetSection}!`);
            } else {
                toast.error('Upload succeeded but no question IDs returned');
            }
        } catch (error) {
            toast.error('Failed to upload questions');
        } finally {
            setIsUploading(false);
            setUploadFile(null);
        }
    };

    const handlePasteUpload = async () => {
        if (!pastedText.trim()) {
            toast.error('Please paste some text first');
            return;
        }

        setIsUploading(true);
        try {
            const result = await pasteQuestions(pastedText);
            if (result && result.questionIds) {
                const newTQs = result.questionIds.map(id => ({
                    questionId: id,
                    marks: 1,
                    sectionName: targetSection,
                    orderIndex: formData.testQuestions.length
                }));

                setFormData(prev => ({
                    ...prev,
                    testQuestions: [...prev.testQuestions, ...newTQs]
                }));
                toast.success(`Successfully processed ${result.successCount} questions to ${targetSection}!`);
            }
            setPastedText('');
        } catch (error) {
            // Error handled in store
        } finally {
            setIsUploading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await createTest(formData);
            onClose();
        } catch (error) {
            // Error handled in store
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-gray-800 rounded-lg p-6 max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                <h2 className="text-2xl font-bold text-white mb-6">Create New Test</h2>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-gray-300 mb-2">Title *</label>
                        <input
                            type="text"
                            required
                            value={formData.title}
                            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                            className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                        />
                    </div>

                    <div>
                        <label className="block text-gray-300 mb-2">Test Format</label>
                        <select
                            value={formData.type}
                            onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                            className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                        >
                            <option value="HYBRID">Hybrid (MCQ & Coding)</option>
                            <option value="MCQ_ONLY">MCQ Only</option>
                            <option value="CODING_ONLY">Coding Only</option>
                        </select>
                        <p className="text-gray-500 text-xs mt-1">
                            This determines which questions can be added to the test.
                        </p>
                    </div>

                    <div>
                        <label className="block text-gray-300 mb-2">Description</label>
                        <textarea
                            value={formData.description}
                            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                            className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                            rows="3"
                        />
                    </div>

                    <div>
                        <label className="block text-gray-300 mb-2">Test Type (Optional)</label>
                        <select
                            value={formData.testType}
                            onChange={(e) => setFormData({ ...formData, testType: e.target.value })}
                            className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                        >
                            <option value="">Select Type</option>
                            <option value="Placement Drive">Placement Drive</option>
                            <option value="Practice">Practice</option>
                            <option value="Contest">Contest</option>
                            <option value="Mock Test">Mock Test</option>
                        </select>
                        <p className="text-gray-500 text-xs mt-1">
                            Categorize your test for better organization.
                        </p>
                    </div>

                    <div>
                        <label className="block text-gray-300 mb-2">Instructions for Students (Optional)</label>
                        <textarea
                            value={formData.instructions}
                            onChange={(e) => setFormData({ ...formData, instructions: e.target.value })}
                            className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                            rows="4"
                            placeholder="Enter any special instructions, rules, or guidelines for students taking this test..."
                        />
                        <p className="text-gray-500 text-xs mt-1">
                            These instructions will be shown to students before they start the test.
                        </p>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-gray-300 mb-2">Start Date & Time *</label>
                            <input
                                type="datetime-local"
                                required
                                value={formData.startDateTime}
                                onChange={(e) => setFormData({ ...formData, startDateTime: e.target.value })}
                                className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                            />
                        </div>

                        <div>
                            <label className="block text-gray-300 mb-2">End Date & Time *</label>
                            <input
                                type="datetime-local"
                                required
                                value={formData.endDateTime}
                                onChange={(e) => setFormData({ ...formData, endDateTime: e.target.value })}
                                className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-gray-300 mb-2">Duration (minutes) *</label>
                        <input
                            type="number"
                            required
                            min="1"
                            value={formData.durationMinutes}
                            onChange={(e) => setFormData({
                                ...formData,
                                durationMinutes: e.target.value === '' ? '' : parseInt(e.target.value)
                            })}
                            className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                        />
                    </div>

                    <div className="bg-gray-700 rounded-lg border border-gray-600 overflow-hidden">
                        <div className="flex border-b border-gray-600">
                            <button
                                type="button"
                                onClick={() => setActiveTab('upload')}
                                className={`flex-1 py-2 text-sm font-medium transition ${activeTab === 'upload' ? 'bg-blue-600 text-white' : 'text-gray-400 hover:text-white'}`}
                            >
                                📁 File Upload
                            </button>
                            <button
                                type="button"
                                onClick={() => setActiveTab('paste')}
                                className={`flex-1 py-2 text-sm font-medium transition ${activeTab === 'paste' ? 'bg-blue-600 text-white' : 'text-gray-400 hover:text-white'}`}
                            >
                                📋 Paste Text
                            </button>
                        </div>

                        <div className="p-4">
                            <div className="mt-4 px-3 py-2 bg-gray-800 rounded border border-gray-600">
                                <label className="text-gray-500 text-[10px] uppercase font-bold block mb-1">Target Section for Upload</label>
                                <div className="flex gap-2">
                                    <input
                                        type="text"
                                        value={targetSection}
                                        onChange={(e) => setTargetSection(e.target.value)}
                                        className="flex-1 bg-gray-700 text-white text-xs px-2 py-1 rounded border border-gray-500"
                                        placeholder="e.g. Part-I"
                                    />
                                    <div className="flex gap-1">
                                        {['Part-I', 'Part-II', 'Section-A'].map(s => (
                                            <button
                                                key={s}
                                                type="button"
                                                onClick={() => setTargetSection(s)}
                                                className={`px-2 py-1 rounded text-[10px] ${targetSection === s ? 'bg-blue-600 text-white' : 'bg-gray-600 text-gray-300'}`}
                                            >
                                                {s}
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            </div>

                            {formData.testQuestions.length > 0 && (
                                <p className="text-green-400 text-xs mt-3 flex items-center gap-1 font-bold">
                                    <span className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></span>
                                    {formData.testQuestions.length} questions added across {new Set(formData.testQuestions.map(q => q.sectionName)).size} sections
                                </p>
                            )}
                        </div>
                    </div>

                    <div>
                        <label className="block text-gray-300 mb-2">
                            Additional Options
                        </label>
                        <p className="text-gray-500 text-sm">
                            You can also add specific {formData.type === 'MCQ_ONLY' ? 'MCQ' : formData.type === 'CODING_ONLY' ? 'Coding' : 'existing'} questions from your bank after creating the test.
                        </p>
                    </div>

                    <div className="flex gap-3 pt-4">
                        <button
                            type="submit"
                            className="flex-1 px-6 py-3 bg-green-600 hover:bg-green-700 text-white rounded-lg transition font-semibold"
                        >
                            Create Test & Add Questions
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

function ManageTestQuestionsModal({ test, onClose }) {
    const { questions: allQuestions, fetchQuestions, updateTestQuestions, createQuestion } = useTestStore();
    const [loading, setLoading] = useState(false);
    const [showAddManual, setShowAddManual] = useState(false);

    // Convert incoming testQuestions to local state
    const [selectedQuestions, setSelectedQuestions] = useState(
        test.testQuestions ? test.testQuestions.map(tq => ({
            questionId: tq.questionId,
            marks: tq.marks,
            sectionName: tq.sectionName || 'Part-I',
            orderIndex: tq.orderIndex
        })) : []
    );

    useEffect(() => {
        fetchQuestions();
    }, []);

    const handleToggleQuestion = (question) => {
        setSelectedQuestions(prev => {
            const exists = prev.find(q => q.questionId === question.id);
            if (exists) {
                return prev.filter(q => q.questionId !== question.id);
            } else {
                return [...prev, {
                    questionId: question.id,
                    marks: question.marks,
                    sectionName: 'Part-I',
                    orderIndex: prev.length
                }];
            }
        });
    };

    const handleUpdateTQ = (qId, field, value) => {
        setSelectedQuestions(prev => prev.map(tq =>
            tq.questionId === qId ? { ...tq, [field]: value } : tq
        ));
    };

    const handleSave = async () => {
        setLoading(true);
        try {
            await updateTestQuestions(test.id, selectedQuestions);
            onClose();
        } catch (error) {
            // Handled in store
        } finally {
            setLoading(false);
        }
    };

    // Group selected questions by section
    const groupedSelected = selectedQuestions.reduce((acc, tq) => {
        const section = tq.sectionName || 'Part-I';
        if (!acc[section]) acc[section] = [];
        acc[section].push(tq);
        return acc;
    }, {});

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-gray-800 rounded-lg p-6 max-w-6xl w-full max-h-[95vh] flex flex-col">
                <div className="flex justify-between items-center mb-6">
                    <div>
                        <h2 className="text-2xl font-bold text-white">Manage Questions</h2>
                        <p className="text-gray-400 text-sm">Test: {test.title}</p>
                    </div>
                    <button onClick={onClose} className="text-gray-400 hover:text-white text-2xl">&times;</button>
                </div>

                <div className="flex-1 overflow-hidden flex gap-6">
                    {/* Left Side: Question Bank Selection */}
                    <div className="w-1/2 flex flex-col gap-4">
                        <div className="flex justify-between items-center">
                            <h3 className="text-lg font-semibold text-white">Question Bank</h3>
                            <button
                                onClick={() => setShowAddManual(true)}
                                className="px-3 py-1 bg-green-600 hover:bg-green-700 text-white rounded text-xs font-bold transition"
                            >
                                + New Question
                            </button>
                        </div>

                        {showAddManual ? (
                            <div className="p-4 bg-gray-750 rounded border border-gray-700 overflow-y-auto">
                                <QuestionForm
                                    onSubmit={async (data) => {
                                        const q = await createQuestion(data);
                                        handleToggleQuestion(q);
                                        setShowAddManual(false);
                                    }}
                                    loading={loading}
                                />
                            </div>
                        ) : (
                            <div className="overflow-y-auto pr-2 custom-scrollbar space-y-3">
                                {allQuestions
                                    .filter(q => {
                                        if (test.type === 'MCQ_ONLY') return q.type === 'MCQ';
                                        if (test.type === 'CODING_ONLY') return q.type === 'CODING';
                                        return true;
                                    })
                                    .map((q) => {
                                        const isSelected = selectedQuestions.some(sq => sq.questionId === q.id);
                                        return (
                                            <div
                                                key={q.id}
                                                onClick={() => handleToggleQuestion(q)}
                                                className={`p-4 rounded-lg border cursor-pointer transition ${isSelected
                                                    ? 'bg-blue-900 bg-opacity-30 border-blue-500'
                                                    : 'bg-gray-700 bg-opacity-50 border-gray-600 hover:border-gray-400'
                                                    }`}
                                            >
                                                <div className="flex justify-between items-center">
                                                    <div>
                                                        <div className="flex items-center gap-2 mb-1">
                                                            <span className={`text-[10px] px-2 py-0.5 rounded-full ${q.type === 'MCQ' ? 'bg-purple-600' : 'bg-blue-600'} text-white`}>
                                                                {q.type}
                                                            </span>
                                                            <span className="text-gray-400 text-xs">{q.marks} default marks</span>
                                                        </div>
                                                        <p className="text-white text-sm line-clamp-2">{q.questionText}</p>
                                                    </div>
                                                    {isSelected && <span className="text-blue-400 text-xl font-bold">✓</span>}
                                                </div>
                                            </div>
                                        );
                                    })}
                            </div>
                        )}
                    </div>

                    {/* Right Side: Selected Questions with Sections and Marks */}
                    <div className="w-1/2 flex flex-col gap-4 border-l border-gray-700 pl-6">
                        <h3 className="text-lg font-semibold text-white">
                            Selected Questions ({selectedQuestions.length})
                        </h3>

                        <div className="overflow-y-auto pr-2 custom-scrollbar space-y-6">
                            {Object.entries(groupedSelected).map(([section, items]) => (
                                <div key={section} className="space-y-3">
                                    <div className="flex items-center gap-2 border-b border-gray-700 pb-1">
                                        <input
                                            type="text"
                                            value={section}
                                            onChange={(e) => {
                                                const newVal = e.target.value || 'General';
                                                setSelectedQuestions(prev => prev.map(tq =>
                                                    tq.sectionName === section ? { ...tq, sectionName: newVal } : tq
                                                ));
                                            }}
                                            className="bg-transparent text-blue-400 font-bold focus:outline-none focus:border-blue-500 border-b border-transparent text-sm"
                                        />
                                        <span className="text-gray-500 text-[10px] uppercase">Section</span>
                                    </div>

                                    {items.map((tq) => {
                                        const fullQ = allQuestions.find(q => q.id === tq.questionId);
                                        return (
                                            <div key={tq.questionId} className="bg-gray-750 p-4 rounded-xl border border-gray-700 shadow-sm">
                                                <div className="flex justify-between items-start mb-4">
                                                    <p className="text-white text-sm line-clamp-2 pr-4">{fullQ?.questionText}</p>
                                                    <button
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            handleToggleQuestion({ id: tq.questionId });
                                                        }}
                                                        className="text-gray-500 hover:text-red-400 transition"
                                                    >
                                                        <span className="text-xl">&times;</span>
                                                    </button>
                                                </div>
                                                <div className="grid grid-cols-2 gap-4">
                                                    <div>
                                                        <label className="text-gray-500 text-[10px] uppercase block mb-1 font-semibold">Marks</label>
                                                        <input
                                                            type="number"
                                                            value={tq.marks}
                                                            onChange={(e) => handleUpdateTQ(tq.questionId, 'marks', parseInt(e.target.value) || 0)}
                                                            className="w-full bg-gray-700 text-white text-sm px-3 py-1.5 rounded-lg border border-gray-600 focus:border-blue-500 outline-none"
                                                        />
                                                    </div>
                                                    <div>
                                                        <label className="text-gray-500 text-[10px] uppercase block mb-1 font-semibold">Assign Section</label>
                                                        <select
                                                            value={tq.sectionName}
                                                            onChange={(e) => {
                                                                if (e.target.value === '+ New Section') {
                                                                    const sn = prompt("Enter new section name:", "Part-II");
                                                                    if (sn) handleUpdateTQ(tq.questionId, 'sectionName', sn);
                                                                } else {
                                                                    handleUpdateTQ(tq.questionId, 'sectionName', e.target.value);
                                                                }
                                                            }}
                                                            className="w-full bg-gray-700 text-white text-sm px-3 py-1.5 rounded-lg border border-gray-600 focus:border-blue-500 outline-none"
                                                        >
                                                            {Object.keys(groupedSelected).map(s => <option key={s} value={s}>{s}</option>)}
                                                            <option value="+ New Section">+ New Section</option>
                                                        </select>
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            ))}

                            {selectedQuestions.length === 0 && (
                                <div className="text-center py-20">
                                    <div className="text-gray-600 text-4xl mb-4">Empty</div>
                                    <p className="text-gray-500 italic">Select questions from the left to configure sections and marks.</p>
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                <div className="flex gap-4 pt-6 mt-6 border-t border-gray-700">
                    <button
                        onClick={handleSave}
                        disabled={loading || selectedQuestions.length === 0}
                        className="flex-1 px-6 py-4 bg-blue-600 hover:bg-blue-700 text-white rounded-xl transition font-bold disabled:opacity-50 shadow-lg shadow-blue-900/20"
                    >
                        {loading ? 'Saving Configuration...' : 'Confirm & Save Changes'}
                    </button>
                    <button
                        onClick={onClose}
                        className="px-8 py-4 bg-gray-700 hover:bg-gray-600 text-white rounded-xl transition font-semibold"
                    >
                        Cancel
                    </button>
                </div>
            </div>
        </div>
    );
}
