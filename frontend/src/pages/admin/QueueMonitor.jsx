import { useState, useEffect } from 'react';
import { testAPI } from '../../services/testAPI';
import toast from 'react-hot-toast';

/**
 * Admin Queue Monitor Dashboard
 * Displays real-time queue statistics and metrics
 */
export default function QueueMonitor() {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchStats();
        const interval = setInterval(fetchStats, 5000); // Refresh every 5 seconds
        return () => clearInterval(interval);
    }, []);

    const fetchStats = async () => {
        try {
            const { data } = await testAPI.getQueueStats();
            setStats(data);
            setLoading(false);
            setError(null);
        } catch (err) {
            console.error('Failed to fetch queue stats', err);
            setError('Failed to load queue statistics');
            toast.error('Failed to fetch queue stats');
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-900 flex items-center justify-center">
                <div className="text-white text-xl">Loading queue statistics...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen bg-gray-900 flex items-center justify-center">
                <div className="text-red-400 text-xl">{error}</div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-900 p-8">
            <div className="max-w-7xl mx-auto">
                <div className="flex items-center justify-between mb-8">
                    <h1 className="text-3xl font-bold text-white">Queue Monitor</h1>
                    <div className="text-sm text-gray-400">
                        Auto-refresh: 5s
                    </div>
                </div>

                {/* Stats Cards Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
                    <StatCard
                        title="Current Queue Depth"
                        value={stats.currentDepth}
                        color="blue"
                        icon="📊"
                    />
                    <StatCard
                        title="Failed Submissions (24h)"
                        value={stats.failedCount}
                        color="red"
                        icon="❌"
                    />
                    <StatCard
                        title="Active Consumers"
                        value={stats.activeConsumers}
                        color="green"
                        icon="⚡"
                    />
                    <StatCard
                        title="Avg Processing Time"
                        value={`${stats.avgProcessingTimeMs}ms`}
                        color="purple"
                        icon="⏱️"
                    />
                    <StatCard
                        title="Processed (1h)"
                        value={stats.processedLastHour}
                        color="yellow"
                        icon="📈"
                    />
                    <StatCard
                        title="Processed (24h)"
                        value={stats.processedLastDay}
                        color="indigo"
                        icon="📊"
                    />
                </div>

                {/* Status Indicators */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="bg-gray-800 rounded-lg p-6">
                        <h3 className="text-white text-lg font-semibold mb-4">Queue Health</h3>
                        <div className="space-y-3">
                            <HealthIndicator
                                label="Queue Status"
                                status={stats.currentDepth < 50 ? 'healthy' : stats.currentDepth < 100 ? 'warning' : 'critical'}
                                value={stats.currentDepth < 50 ? 'Normal' : stats.currentDepth < 100 ? 'High Load' : 'Overloaded'}
                            />
                            <HealthIndicator
                                label="Consumer Status"
                                status={stats.activeConsumers > 0 ? 'healthy' : 'critical'}
                                value={stats.activeConsumers > 0 ? 'Active' : 'No Consumers'}
                            />
                            <HealthIndicator
                                label="Error Rate"
                                status={stats.failedCount < 10 ? 'healthy' : stats.failedCount < 50 ? 'warning' : 'critical'}
                                value={stats.failedCount < 10 ? 'Low' : stats.failedCount < 50 ? 'Moderate' : 'High'}
                            />
                        </div>
                    </div>

                    <div className="bg-gray-800 rounded-lg p-6">
                        <h3 className="text-white text-lg font-semibold mb-4">Quick Actions</h3>
                        <div className="space-y-3">
                            <button
                                onClick={fetchStats}
                                className="w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition"
                            >
                                🔄 Refresh Now
                            </button>
                            <button
                                onClick={() => toast.success('Feature coming soon!')}
                                className="w-full px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition"
                            >
                                📥 Export Metrics
                            </button>
                            <button
                                onClick={() => toast.success('Feature coming soon!')}
                                className="w-full px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition"
                            >
                                🔔 Configure Alerts
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

function StatCard({ title, value, color, icon }) {
    const colors = {
        blue: 'bg-blue-600',
        red: 'bg-red-600',
        green: 'bg-green-600',
        purple: 'bg-purple-600',
        yellow: 'bg-yellow-600',
        indigo: 'bg-indigo-600'
    };

    return (
        <div className={`${colors[color]} rounded-lg p-6 shadow-lg transform hover:scale-105 transition`}>
            <div className="flex items-center justify-between mb-2">
                <h3 className="text-white text-sm font-medium">{title}</h3>
                <span className="text-2xl">{icon}</span>
            </div>
            <p className="text-white text-3xl font-bold">{value}</p>
        </div>
    );
}

function HealthIndicator({ label, status, value }) {
    const statusColors = {
        healthy: 'bg-green-500',
        warning: 'bg-yellow-500',
        critical: 'bg-red-500'
    };

    return (
        <div className="flex items-center justify-between">
            <span className="text-gray-300 text-sm">{label}</span>
            <div className="flex items-center gap-2">
                <span className="text-white text-sm font-medium">{value}</span>
                <div className={`w-3 h-3 rounded-full ${statusColors[status]}`}></div>
            </div>
        </div>
    );
}
