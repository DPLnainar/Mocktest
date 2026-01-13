import { useEffect, useState } from 'react';
import api from '../services/api';
import { useTestStore } from '../store/testStore';
import { useAuthStore } from '../store/authStore';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

export default function StudentTestListPage() {
    const { tests, loading, fetchAvailableTests } = useTestStore();
    const { logout } = useAuthStore();
    const navigate = useNavigate();
    const [completedTestIds, setCompletedTestIds] = useState(new Set());

    useEffect(() => {
        fetchAvailableTests();
        fetchHistory();
    }, []);

    const fetchHistory = async () => {
        try {
            const { data } = await api.get('/student/history');
            const completed = new Set(data.map(attempt => attempt.testId));
            setCompletedTestIds(completed);
        } catch (error) {
            console.error('Failed to fetch history:', error);
        }
    };

    const handleLogout = () => {
        if (window.confirm('Are you sure you want to logout?')) {
            logout();
            navigate('/login');
            toast.success('Logged out successfully');
        }
    }

    const categorizeTests = () => {
        // ... existing code ...
        const now = new Date();
        const upcoming = [];
        const active = [];
        const expired = [];

        tests.forEach((test) => {
            const start = new Date(test.startDateTime);
            const end = new Date(test.endDateTime);

            if (now < start) {
                upcoming.push(test);
            } else if (now > end) {
                expired.push(test);
            } else {
                active.push(test);
            }
        });

        return { upcoming, active, expired };
    };

    const { upcoming, active, expired } = categorizeTests();

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-900 flex items-center justify-center">
                <div className="text-white text-xl">Loading tests...</div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-900 p-8">
            <div className="max-w-6xl mx-auto">
                <div className="flex justify-between items-center mb-8">
                    <h1 className="text-4xl font-bold text-white">My Tests</h1>
                    <button
                        onClick={handleLogout}
                        className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition border border-red-500"
                    >
                        Logout
                    </button>
                </div>

                {/* Active Tests */}
                {active.length > 0 && (
                    <section className="mb-8">
                        <h2 className="text-2xl font-semibold text-green-400 mb-4 flex items-center gap-2">
                            <span className="inline-block w-3 h-3 bg-green-500 rounded-full animate-pulse"></span>
                            Active Now
                        </h2>
                        <div className="grid gap-4">
                            {active.map((test) => (
                                <TestCard
                                    key={test.id}
                                    test={test}
                                    status="active"
                                    navigate={navigate}
                                    isCompleted={completedTestIds.has(test.id)}
                                />
                            ))}
                        </div>
                    </section>
                )}

                {/* Upcoming Tests */}
                {upcoming.length > 0 && (
                    <section className="mb-8">
                        <h2 className="text-2xl font-semibold text-yellow-400 mb-4">Upcoming</h2>
                        <div className="grid gap-4">
                            {upcoming.map((test) => (
                                <TestCard key={test.id} test={test} status="upcoming" navigate={navigate} />
                            ))}
                        </div>
                    </section>
                )}

                {/* Expired Tests */}
                {expired.length > 0 && (
                    <section>
                        <h2 className="text-2xl font-semibold text-gray-400 mb-4">Past Tests</h2>
                        <div className="grid gap-4">
                            {expired.map((test) => (
                                <TestCard key={test.id} test={test} status="expired" navigate={navigate} />
                            ))}
                        </div>
                    </section>
                )}

                {tests.length === 0 && (
                    <div className="text-center text-gray-400 py-20">
                        <p className="text-xl">No tests available at the moment</p>
                        <p className="mt-2">Check back later for upcoming tests</p>
                    </div>
                )}
            </div>
        </div>
    );
}

function TestCard({ test, status, navigate, isCompleted }) {
    const formatDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    const getTimeDifference = () => {
        if (status !== 'active') return null;

        const now = new Date();
        const end = new Date(test.endDateTime);
        const diff = end - now;
        const hours = Math.floor(diff / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

        return `${hours}h ${minutes}m remaining`;
    };

    const handleStartTest = () => {
        navigate(`/student/test/${test.id}`);
    };

    return (
        <div className={`bg-gray-800 rounded-lg p-6 border ${status === 'active' ? 'border-green-500' :
            status === 'upcoming' ? 'border-yellow-500' :
                'border-gray-700'
            }`}>
            <div className="flex justify-between items-start mb-4">
                <div>
                    <h3 className="text-2xl font-semibold text-white mb-2">{test.title}</h3>
                    {test.description && (
                        <p className="text-gray-400 mb-2">{test.description}</p>
                    )}
                    <div className="flex gap-2">
                        <span className={`px-3 py-1 text-xs rounded-full border ${test.type === 'MCQ_ONLY' ? 'border-purple-500 text-purple-400' :
                            test.type === 'CODING_ONLY' ? 'border-blue-500 text-blue-400' :
                                'border-green-500 text-green-400'
                            }`}>
                            {test.type?.replace('_ONLY', '')}
                        </span>
                    </div>
                </div>
                <span className={`px-4 py-2 rounded-full font-semibold ${status === 'active' ? 'bg-green-600 text-white' :
                    status === 'upcoming' ? 'bg-yellow-600 text-white' :
                        'bg-gray-600 text-gray-300'
                    }`}>
                    {status === 'active' ? 'LIVE' :
                        status === 'upcoming' ? 'UPCOMING' : 'ENDED'}
                </span>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6 text-sm">
                <div>
                    <p className="text-gray-500">Start Time</p>
                    <p className="text-white font-medium">{formatDate(test.startDateTime)}</p>
                </div>
                <div>
                    <p className="text-gray-500">End Time</p>
                    <p className="text-white font-medium">{formatDate(test.endDateTime)}</p>
                </div>
                <div>
                    <p className="text-gray-500">Duration</p>
                    <p className="text-white font-medium">{test.durationMinutes} minutes</p>
                </div>
            </div>

            {status === 'active' && (
                <>
                    <p className="text-yellow-400 mb-4 font-medium">{getTimeDifference()}</p>
                    {isCompleted ? (
                        <button
                            onClick={() => navigate('/student/history')}
                            className="w-full px-6 py-4 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition font-bold text-lg border border-gray-600"
                        >
                            View Results (Completed)
                        </button>
                    ) : (
                        <button
                            onClick={handleStartTest}
                            className="w-full px-6 py-4 bg-green-600 hover:bg-green-700 text-white rounded-lg transition font-bold text-lg"
                        >
                            Start Test →
                        </button>
                    )}
                </>
            )}

            {status === 'upcoming' && (
                <div className="text-gray-400 text-center py-2">
                    Test starts {formatDate(test.startDateTime)}
                </div>
            )}

            {status === 'expired' && (
                <button
                    className="w-full px-6 py-3 bg-gray-700 text-gray-400 rounded-lg cursor-not-allowed"
                    disabled
                >
                    Test Ended
                </button>
            )}
        </div>
    );
}
