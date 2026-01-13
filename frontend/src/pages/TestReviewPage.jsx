import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import toast from 'react-hot-toast';

export default function TestReviewPage() {
    const { testId } = useParams();
    const navigate = useNavigate();
    const [review, setReview] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchReview();
    }, [testId]);

    const fetchReview = async () => {
        try {
            const { data } = await api.get(`/student/history/tests/${testId}/review`);
            setReview(data);
        } catch (error) {
            toast.error('Failed to load test review');
            navigate('/student/history');
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-900 flex items-center justify-center">
                <div className="text-white text-xl">Loading review...</div>
            </div>
        );
    }

    if (!review) return null;

    // Group questions by section
    const groupedQuestions = review.questions.reduce((acc, q) => {
        const section = q.sectionName || 'Part-I';
        if (!acc[section]) acc[section] = [];
        acc[section].push(q);
        return acc;
    }, {});

    return (
        <div className="min-h-screen bg-gray-900 p-8">
            <div className="max-w-4xl mx-auto">
                <div className="flex justify-between items-center mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-white mb-2">Test Review: {review.test.title}</h1>
                        <p className="text-gray-400">Section-based performance analysis</p>
                    </div>
                    <button
                        onClick={() => navigate('/student/history')}
                        className="px-6 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition"
                    >
                        Back to History
                    </button>
                </div>

                <div className="space-y-12">
                    {Object.entries(groupedQuestions).map(([section, questions]) => (
                        <div key={section} className="space-y-6">
                            <div className="flex items-center gap-4">
                                <h2 className="text-xl font-bold text-blue-400 uppercase tracking-widest">{section}</h2>
                                <div className="h-px bg-gray-700 flex-1"></div>
                            </div>

                            <div className="space-y-6">
                                {questions.map((q, idx) => (
                                    <div key={idx} className="bg-gray-800 rounded-xl p-6 border border-gray-700 shadow-sm">
                                        <div className="flex justify-between items-start mb-4">
                                            <div className="flex items-center gap-2">
                                                <span className="text-gray-500 font-mono text-sm">Q{idx + 1}</span>
                                                <span className={`px-2 py-0.5 rounded text-[10px] uppercase font-bold text-white ${q.question.type === 'MCQ' ? 'bg-purple-600' : 'bg-blue-600'
                                                    }`}>
                                                    {q.question.type}
                                                </span>
                                            </div>
                                            <div className="bg-gray-900 px-3 py-1 rounded-lg border border-gray-700">
                                                <span className="text-gray-400 text-xs mr-2">Marks:</span>
                                                <span className={`font-bold ${q.correct ? 'text-green-400' : 'text-red-400'}`}>
                                                    {q.correct ? q.marks : 0} / {q.marks}
                                                </span>
                                            </div>
                                        </div>

                                        <p className="text-white text-lg mb-6 leading-relaxed">
                                            {q.question.questionText}
                                        </p>

                                        {q.question.type === 'MCQ' ? (
                                            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                                {['A', 'B', 'C', 'D'].map(opt => (
                                                    <div
                                                        key={opt}
                                                        className={`p-3 rounded-lg border text-sm flex items-center justify-between ${opt === q.correctAnswer
                                                            ? 'border-green-500 bg-green-900 bg-opacity-20 text-green-100'
                                                            : opt === q.studentAnswer && !q.correct
                                                                ? 'border-red-500 bg-red-900 bg-opacity-20 text-red-100'
                                                                : 'border-gray-700 bg-gray-750 text-gray-400'
                                                            }`}
                                                    >
                                                        <span>
                                                            <strong className="mr-2">{opt}.</strong>
                                                            {q.question[`option${opt}`]}
                                                        </span>
                                                        {opt === q.correctAnswer && <span className="text-green-400">✓</span>}
                                                        {opt === q.studentAnswer && !q.correct && <span className="text-red-400">✗</span>}
                                                    </div>
                                                ))}
                                            </div>
                                        ) : (
                                            <div className="space-y-4">
                                                <div className="bg-gray-900 rounded-lg p-4 font-mono text-sm overflow-x-auto border border-gray-700">
                                                    <label className="text-gray-500 text-[10px] uppercase mb-2 block">Your Code</label>
                                                    <pre className="text-gray-200">{q.studentAnswer || '// No code submitted'}</pre>
                                                </div>
                                                {q.executionResult && (
                                                    <div className="bg-gray-950 rounded-lg p-4 border border-gray-800">
                                                        <label className="text-gray-500 text-[10px] uppercase mb-2 block">Execution Result</label>
                                                        <pre className="text-green-400 text-xs whitespace-pre-wrap">
                                                            {q.executionResult.stdout || q.executionResult.stderr || 'No output'}
                                                        </pre>
                                                    </div>
                                                )}
                                            </div>
                                        )}

                                        {q.explanation && (
                                            <div className="mt-8 p-4 bg-blue-900 bg-opacity-10 border-l-4 border-blue-500 rounded-r-lg">
                                                <h4 className="text-blue-400 font-bold text-sm uppercase mb-2">Explanation</h4>
                                                <p className="text-gray-300 text-sm italic">{q.explanation}</p>
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
