import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import toast from 'react-hot-toast';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export default function StudentHistoryPage() {
    const navigate = useNavigate();
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchHistory();
    }, []);

    const fetchHistory = async () => {
        try {
            const { data } = await api.get('/student/history');
            setHistory(data);
        } catch (error) {
            toast.error('Failed to load history');
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-900 flex items-center justify-center">
                <div className="text-white text-xl">Loading history...</div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-900 p-8">
            <div className="max-w-6xl mx-auto">
                <button
                    onClick={() => navigate('/student/tests')}
                    className="mb-6 flex items-center gap-2 text-gray-400 hover:text-white transition group"
                >
                    <span className="transform group-hover:-translate-x-1 transition">←</span> Back to Dashboard
                </button>
                <div className="flex justify-between items-center mb-8">
                    <h1 className="text-4xl font-bold text-white">My Test History</h1>
                </div>

                {history.length === 0 ? (
                    <div className="text-center text-gray-400 py-20">
                        <p className="text-xl">No test history yet</p>
                        <p className="mt-2">Complete some tests to see your history here</p>
                    </div>
                ) : (
                    <div className="grid gap-6">
                        {history.map((attempt) => (
                            <TestHistoryCard key={attempt.id} attempt={attempt} />
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}

function TestHistoryCard({ attempt }) {
    const navigate = useNavigate();
    const formatDate = (dateStr) => {
        if (!dateStr) return 'N/A';
        // Ensure date is treated as UTC if it comes without timezone info
        const safeDateStr = dateStr.endsWith('Z') ? dateStr : `${dateStr}Z`;
        const date = new Date(safeDateStr);
        return date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    const getScoreBadge = () => {
        if (!attempt.score || !attempt.totalMarks) return null;
        const percentage = (attempt.score / attempt.totalMarks) * 100;

        return (
            <div
                className={`px-6 py-3 rounded-lg font-bold text-2xl ${percentage >= 80
                    ? 'bg-green-600 text-white'
                    : percentage >= 60
                        ? 'bg-yellow-600 text-white'
                        : 'bg-red-600 text-white'
                    }`}
            >
                {attempt.score.toFixed(1)} / {attempt.totalMarks}
                <div className="text-sm font-normal mt-1">{percentage.toFixed(1)}%</div>
            </div>
        );
    };

    return (
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
            <div className="flex justify-between items-start mb-4">
                <div className="flex-1">
                    <h3 className="text-2xl font-semibold text-white mb-2">{attempt.testTitle || `Test #${attempt.testId}`}</h3>
                    <div className="text-gray-400 text-sm space-y-1">
                        <p>Started: {formatDate(attempt.startedAt)}</p>
                        <p>Submitted: {formatDate(attempt.submittedAt)}</p>
                        <p>Status: <span className="text-blue-400">{attempt.status}</span></p>
                    </div>
                </div>
                {getScoreBadge()}
            </div>

            {attempt.violationCount > 0 && (
                <div className="mb-4 p-3 bg-red-900 bg-opacity-30 border border-red-600 rounded">
                    <p className="text-red-400 text-sm">
                        ⚠️ {attempt.violationCount} violation(s) detected during this test
                    </p>
                    {attempt.autoSubmitted && (
                        <p className="text-red-400 text-sm mt-1">Test was auto-submitted due to violations</p>
                    )}
                </div>
            )}

            <div className="flex gap-4">
                {(attempt.status === 'SUBMITTED' || attempt.status === 'FROZEN' || new Date(attempt.testEndDate) < new Date()) ? (
                    <button
                        onClick={() => navigate(`/student/history/tests/${attempt.testId}/review`)}
                        className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition"
                    >
                        View Detailed Review
                    </button>
                ) : (
                    <button
                        onClick={() => navigate(`/student/test/${attempt.testId}`)}
                        className="px-6 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg transition"
                    >
                        Resume Test
                    </button>
                )}

                {(attempt.status === 'IN_PROGRESS' || attempt.status === 'STARTED') && new Date(attempt.testEndDate) > new Date() && (
                    <div className="flex items-center text-yellow-500 text-sm">
                        <span className="mr-2">●</span> Active Test
                    </div>
                )}
            </div>
        </div>
    );
}
