import { useEffect, useState } from 'react';
import { useTestStore } from '../store/testStore';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

import { useRef } from 'react';
import { useAuthStore } from '../store/authStore';

export default function ModeratorTestsPage() {
    const { tests, loading, fetchTests, deleteTest } = useTestStore();
    const { logout } = useAuthStore();
    const [showCreateModal, setShowCreateModal] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
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

    return (
        <div className="min-h-screen bg-gray-900 p-8">
            <div className="max-w-7xl mx-auto">
                <div className="flex justify-between items-center mb-8">
                    <h1 className="text-3xl font-bold text-white">Test Management</h1>

                    <div className="flex gap-4">
                        <button
                            onClick={() => navigate('/moderator/questions')}
                            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition"
                        >
                            Manage Questions
                        </button>
                        <button
                            onClick={() => setShowCreateModal(true)}
                            className="px-6 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg transition font-semibold"
                        >
                            + Create Test
                        </button>
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
                ) : tests.length === 0 ? (
                    <div className="text-center text-gray-400 py-12">
                        No tests found. Create your first test!
                    </div>
                ) : (
                    <div className="grid gap-6">
                        {tests.map((test) => (
                            <TestCard key={test.id} test={test} onDelete={handleDelete} />
                        ))}
                    </div>
                )}

                {showCreateModal && (
                    <CreateTestModal onClose={() => setShowCreateModal(false)} />
                )}
            </div>
        </div>
    );
}

function TestCard({ test, onDelete }) {
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

    return (
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
            <div className="flex justify-between items-start mb-4">
                <div>
                    <h3 className="text-xl font-semibold text-white mb-2">{test.title}</h3>
                    {test.description && (
                        <p className="text-gray-400 text-sm">{test.description}</p>
                    )}
                </div>
                {getStatusBadge()}
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
                    <p className="text-white">{test.questionIds?.length || 0}</p>
                </div>
            </div>

            <div className="flex gap-3">
                <button
                    onClick={() => window.location.href = `/moderator/tests/${test.id}/analytics`}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded text-sm transition"
                >
                    View Results
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
    const { createTest, fetchQuestions, questions } = useTestStore();
    const [formData, setFormData] = useState({
        title: '',
        description: '',
        startDateTime: '',
        endDateTime: '',
        durationMinutes: 60,
        questionIds: [],
        testType: 'HYBRID', // MCQ, CODING, HYBRID
    });

    useEffect(() => {
        fetchQuestions();
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            // Append type to description or title if backend doesn't support it
            // For now, we'll prefix the description
            const finalData = {
                ...formData,
                description: `[${formData.testType}] ${formData.description}`
            };
            await createTest(finalData);
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
                            value={formData.testType}
                            onChange={(e) => setFormData({ ...formData, testType: e.target.value })}
                            className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                        >
                            <option value="HYBRID">Hybrid (MCQ & Coding)</option>
                            <option value="MCQ_ONLY">MCQ Only</option>
                            <option value="CODING_ONLY">Coding Only</option>
                        </select>
                        <p className="text-gray-500 text-xs mt-1">
                            This labels the test pattern for students.
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

                    <div>
                        <label className="block text-gray-300 mb-2">
                            Questions (Manage in Question Bank)
                        </label>
                        <p className="text-gray-500 text-sm">
                            You can add {formData.testType === 'MCQ_ONLY' ? 'MCQ' : formData.testType === 'CODING_ONLY' ? 'Coding' : 'Mixed'} questions after creating the test.
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
