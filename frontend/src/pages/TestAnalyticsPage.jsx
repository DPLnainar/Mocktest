import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import api from '../services/api';
import toast from 'react-hot-toast';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

import { Home } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

// Duplicate imports removed

export default function TestAnalyticsPage() {
    const { testId } = useParams();
    const navigate = useNavigate();
    const [results, setResults] = useState([]);
    const [testDetails, setTestDetails] = useState(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('results'); // results, attendance, violations

    useEffect(() => {
        fetchData();
    }, [testId]);

    const fetchData = async () => {
        try {
            const [resultsRes, testRes] = await Promise.all([
                api.get(`/analytics/tests/${testId}/results`),
                api.get(`/tests/${testId}`)
            ]);
            setResults(resultsRes.data);
            setTestDetails(testRes.data);
        } catch (error) {
            toast.error('Failed to load analytics data');
        } finally {
            setLoading(false);
        }
    };

    const handleExportExcel = async () => {
        try {
            const response = await api.get(
                `/analytics/tests/${testId}/export/excel`,
                {
                    responseType: 'blob',
                }
            );

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `${testDetails?.title || 'test'}-results.xlsx`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            toast.success('Excel exported successfully');
        } catch (error) {
            toast.error('Failed to export Excel');
        }
    };

    const getAttendedStudents = () => results.filter((r) => r.attendanceStatus === 'ATTENDED');
    const getAbsentStudents = () => results.filter((r) => r.attendanceStatus === 'NOT_ATTENDED');
    const getStudentsWithViolations = () => results.filter((r) => r.violationCount > 0);

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-900 flex items-center justify-center">
                <div className="text-white text-xl">Loading analytics...</div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-900 p-8">
            <div className="max-w-7xl mx-auto">
                <div className="flex justify-between items-center mb-8">
                    <div className="flex items-center gap-4">
                        <button
                            onClick={() => navigate('/moderator/tests')}
                            className="p-2 bg-gray-800 hover:bg-gray-700 text-gray-300 hover:text-white rounded-lg transition"
                            title="Back to Dashboard"
                        >
                            <Home size={24} />
                        </button>
                        <div>
                            <h1 className="text-3xl font-bold text-white">
                                {testDetails?.title || 'Test Analytics'}
                            </h1>
                            <p className="text-gray-400 text-sm">
                                {testDetails?.type?.replace('_', ' ')}
                            </p>
                        </div>
                    </div>
                    <button
                        onClick={handleExportExcel}
                        className="px-6 py-3 bg-green-600 hover:bg-green-700 text-white rounded-lg transition font-semibold"
                    >
                        📊 Export to Excel
                    </button>
                </div>

                {/* Tabs */}
                <div className="flex gap-4 mb-6 border-b border-gray-700">
                    <button
                        onClick={() => setActiveTab('results')}
                        className={`px-6 py-3 font-semibold ${activeTab === 'results'
                            ? 'text-blue-400 border-b-2 border-blue-400'
                            : 'text-gray-400 hover:text-white'
                            }`}
                    >
                        Results ({getAttendedStudents().length})
                    </button>
                    <button
                        onClick={() => setActiveTab('attendance')}
                        className={`px-6 py-3 font-semibold ${activeTab === 'attendance'
                            ? 'text-blue-400 border-b-2 border-blue-400'
                            : 'text-gray-400 hover:text-white'
                            }`}
                    >
                        Attendance ({results.length})
                    </button>
                    <button
                        onClick={() => setActiveTab('violations')}
                        className={`px-6 py-3 font-semibold ${activeTab === 'violations'
                            ? 'text-blue-400 border-b-2 border-blue-400'
                            : 'text-gray-400 hover:text-white'
                            }`}
                    >
                        Violations ({getStudentsWithViolations().length})
                    </button>
                </div>

                {/* Results Tab */}
                {
                    activeTab === 'results' && (
                        <div className="bg-gray-800 rounded-lg overflow-hidden">
                            <table className="w-full">
                                <thead className="bg-gray-700">
                                    <tr>
                                        <th className="px-6 py-3 text-left text-white">Reg No</th>
                                        <th className="px-6 py-3 text-left text-white">Name</th>
                                        <th className="px-6 py-3 text-left text-white">Score</th>
                                        <th className="px-6 py-3 text-left text-white">%</th>
                                        <th className="px-6 py-3 text-left text-white">Violations</th>
                                        <th className="px-6 py-3 text-left text-white">Status</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-700">
                                    {getAttendedStudents().map((result) => (
                                        <tr key={result.studentId} className="hover:bg-gray-700">
                                            <td className="px-6 py-4 text-gray-300">{result.registrationNumber}</td>
                                            <td className="px-6 py-4 text-white">{result.studentName}</td>
                                            <td className="px-6 py-4 text-white">
                                                {result.score?.toFixed(1)} / {result.totalMarks}
                                            </td>
                                            <td className="px-6 py-4">
                                                <span
                                                    className={`px-3 py-1 rounded-full text-sm ${result.percentage >= 80
                                                        ? 'bg-green-600 text-white'
                                                        : result.percentage >= 60
                                                            ? 'bg-yellow-600 text-white'
                                                            : 'bg-red-600 text-white'
                                                        }`}
                                                >
                                                    {result.percentage?.toFixed(1)}%
                                                </span>
                                            </td>
                                            <td className="px-6 py-4">
                                                {result.violationCount > 0 ? (
                                                    <span className="text-red-400">{result.violationCount} ⚠️</span>
                                                ) : (
                                                    <span className="text-green-400">None</span>
                                                )}
                                            </td>
                                            <td className="px-6 py-4">
                                                {result.autoSubmitted && (
                                                    <span className="px-2 py-1 bg-red-600 text-white text-xs rounded">
                                                        Auto-Submitted
                                                    </span>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )
                }

                {/* Attendance Tab */}
                {
                    activeTab === 'attendance' && (
                        <div className="grid grid-cols-2 gap-6">
                            <div>
                                <h3 className="text-xl font-semibold text-green-400 mb-4">
                                    Attended ({getAttendedStudents().length})
                                </h3>
                                <div className="bg-gray-800 rounded-lg p-4 space-y-2">
                                    {getAttendedStudents().map((result) => (
                                        <div key={result.studentId} className="text-white py-2 border-b border-gray-700">
                                            {result.registrationNumber} - {result.studentName}
                                        </div>
                                    ))}
                                </div>
                            </div>
                            <div>
                                <h3 className="text-xl font-semibold text-red-400 mb-4">
                                    Absent ({getAbsentStudents().length})
                                </h3>
                                <div className="bg-gray-800 rounded-lg p-4 space-y-2">
                                    {getAbsentStudents().map((result) => (
                                        <div key={result.studentId} className="text-gray-400 py-2 border-b border-gray-700">
                                            {result.registrationNumber} - {result.studentName}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    )
                }

                {/* Violations Tab */}
                {
                    activeTab === 'violations' && (
                        <div className="bg-gray-800 rounded-lg overflow-hidden">
                            <table className="w-full">
                                <thead className="bg-gray-700">
                                    <tr>
                                        <th className="px-6 py-3 text-left text-white">Student</th>
                                        <th className="px-6 py-3 text-left text-white">Total Violations</th>
                                        <th className="px-6 py-3 text-left text-white">Details</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-700">
                                    {getStudentsWithViolations().map((result) => (
                                        <tr key={result.studentId} className="hover:bg-gray-700">
                                            <td className="px-6 py-4 text-white">
                                                {result.registrationNumber} - {result.studentName}
                                            </td>
                                            <td className="px-6 py-4">
                                                <span className="text-red-400 font-semibold">{result.violationCount}</span>
                                            </td>
                                            <td className="px-6 py-4 text-gray-300 text-sm">
                                                {result.violationSummary &&
                                                    Object.entries(result.violationSummary)
                                                        .map(([type, count]) => `${type}: ${count}`)
                                                        .join(', ')}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )
                }
            </div >
        </div >
    );
}
