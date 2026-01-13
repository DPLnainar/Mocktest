import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTestStore } from '../store/testStore';
import { testAPI } from '../services/testAPI';
import { useAIProctoring } from '../hooks/useAIProctoring';
import { useExtensionDetection } from '../hooks/useExtensionDetection';
import { useKeystrokeAnalysis } from '../hooks/useKeystrokeAnalysis';
import { useIPTracking } from '../hooks/useIPTracking';
import useAntiCheat from '../hooks/useAntiCheat';
import ProctoringNotice from '../components/ProctoringNotice';
import WebcamPreview from '../components/WebcamPreview';
import toast from 'react-hot-toast';

export default function TestTakingPage() {
    const { testId } = useParams();
    const navigate = useNavigate();
    const { startTest, getAttempt, submitAnswer, executeCode, submitTest, violations } = useTestStore();

    const [test, setTest] = useState(null);
    const [attempt, setAttempt] = useState(null);
    const [questions, setQuestions] = useState([]);
    const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
    const [answers, setAnswers] = useState({});

    const [codeAnswers, setCodeAnswers] = useState({});
    const [selectedLanguages, setSelectedLanguages] = useState({}); // Map<qId, langId>
    const [timeRemaining, setTimeRemaining] = useState(0);
    const [loading, setLoading] = useState(true);
    const [testStarted, setTestStarted] = useState(false);
    const [showSubmitModal, setShowSubmitModal] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [executionResults, setExecutionResults] = useState({});
    const [isExecuting, setIsExecuting] = useState(false);
    const [isFrozen, setIsFrozen] = useState(false);

    const autoSaveInterval = useRef(null);

    // AI Proctoring
    const { videoRef, modelsLoaded, faceCount, detectedObjects, aiViolationCount } = useAIProctoring(
        attempt?.id,
        !loading && attempt !== null
    );

    // Extension Detection
    const { detectedExtensions, hasExtensions } = useExtensionDetection(
        attempt?.id,
        testStarted
    );

    // Keystroke Analysis
    const { copyPasteCount } = useKeystrokeAnalysis(
        attempt?.id,
        questions[currentQuestionIndex]?.id,
        testStarted
    );

    // IP Tracking
    const { ipAddress, ipChanged } = useIPTracking(
        attempt?.id,
        testStarted
    );

    // General Anti-Cheat (Tab switch, Fullscreen, Copy/Paste)
    useAntiCheat(attempt?.id, testStarted);

    const initialized = useRef(false);

    useEffect(() => {
        if (!initialized.current) {
            initialized.current = true;
            initializeTest();
        }
        return () => {
            if (autoSaveInterval.current) {
                clearInterval(autoSaveInterval.current);
            }
        };
    }, [testId]);

    const initializeTest = async () => {
        try {
            console.log('=== INIT TEST START ===', testId);

            // 1. Try to get existing attempt
            let attemptData = null;
            try {
                console.log('Trying to get existing attempt...');
                attemptData = await getAttempt(testId);
                console.log('Found existing attempt:', attemptData);
                toast.success('Resumed previous session');
            } catch (e) {
                console.log('No existing attempt, will create new one');
            }

            // 2. If no existing attempt, start new one
            if (!attemptData) {
                console.log('Starting new test...');
                attemptData = await startTest(testId);
                console.log('Created new attempt:', attemptData);
            }

            setAttempt(attemptData);

            if (attemptData.status === 'SUBMITTED') {
                toast.error('You have already submitted this test');
                navigate('/student/history');
                return;
            }

            if (attemptData.status === 'FROZEN') {
                setIsFrozen(true);
            }
            console.log('Attempt set:', attemptData);

            // Fetch test details
            console.log('Fetching test details...');
            const { data: testData } = await testAPI.getStudentTest(testId);

            console.log('Test data received:', testData);
            setTest(testData);

            // Setup questions from testQuestions
            if (testData.testQuestions) {
                const mappedQuestions = testData.testQuestions
                    .sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0))
                    .map(tq => ({
                        ...tq.question,
                        id: tq.questionId,
                        marks: tq.marks,
                        sectionName: tq.sectionName
                    }));
                setQuestions(mappedQuestions);
            } else {
                setQuestions([]);
            }

            // Set timer
            setTimeRemaining(testData.durationMinutes * 60);

            // Auto-save setup
            autoSaveInterval.current = setInterval(() => {
                saveCurrentAnswer();
            }, 30000);

            console.log('=== INIT TEST COMPLETE ===');
            setLoading(false);
        } catch (error) {
            console.error('=== TEST INIT ERROR ===', error);
            toast.error(error.message || 'Failed to start test');
            setLoading(false); // Make sure to stop loading even on error
            navigate('/student/tests');
        }
    };

    // Timer countdown
    useEffect(() => {
        if (timeRemaining <= 0) {
            handleSubmitTest();
            return;
        }

        const timer = setInterval(() => {
            setTimeRemaining((prev) => prev - 1);
        }, 1000);

        return () => clearInterval(timer);
    }, [timeRemaining]);

    const formatTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    const saveCurrentAnswer = async () => {
        const question = questions[currentQuestionIndex];
        if (!question || !attempt) return;

        const answer = question.type === 'MCQ'
            ? answers[question.id]
            : codeAnswers[question.id];

        if (answer) {
            try {
                await submitAnswer(attempt.id, question.id, answer);
            } catch (error) {
                console.error('Auto-save failed:', error);
            }
        }
    };

    const handleAnswerChange = (questionId, answer) => {
        setAnswers({ ...answers, [questionId]: answer });
    };

    const handleCodeChange = (questionId, code) => {
        setCodeAnswers({ ...codeAnswers, [questionId]: code });
    };

    const handleRunCode = async (questionId) => {
        const code = codeAnswers[questionId];
        if (!code || !attempt) {
            toast.error('Please write some code first');
            return;
        }

        setIsExecuting(true);
        // Clear previous result display while running
        setExecutionResults({ ...executionResults, [questionId]: { status: { description: 'Running...' } } });

        try {
            // Determine language (Default to first allowed or Java)
            const question = questions.find(q => q.id === questionId);
            const defaultLang = question?.allowedLanguageIds?.[0] || 62;
            const langId = selectedLanguages[questionId] || defaultLang;

            // Execute Request (Returns ID immediately)
            const result = await executeCode(attempt.id, questionId, code, langId, '');

            if (result.status === 'QUEUED') {
                toast.success('Submitted to queue...');
                // Poll for result
                pollExecutionResult(attempt.id, questionId, result.executionId);
            } else {
                // Synchronous/Cached result
                setExecutionResults(prev => ({ ...prev, [questionId]: result }));
                toast.success('Code executed successfully');
                setIsExecuting(false);
            }
        } catch (error) {
            toast.error('Code execution failed');
            setIsExecuting(false);
        }
    };

    const pollExecutionResult = async (attemptId, questionId, executionId) => {
        const { exponentialBackoffPoll, getStatusMessage } = await import('../utils/pollingUtils');

        const pollFunction = async () => {
            const { data: result } = await testAPI.getExecutionResult(attemptId, questionId);
            return result;
        };

        const onUpdate = (result, attemptNumber, nextDelay) => {
            const message = getStatusMessage(result?.status, attemptNumber);

            setExecutionResults(prev => ({
                ...prev,
                [questionId]: {
                    ...result,
                    _pollingInfo: {
                        attempt: attemptNumber,
                        nextDelay: Math.round(nextDelay / 1000),
                        message
                    }
                }
            }));

            if (attemptNumber % 3 === 0) {
                toast.loading(
                    `${message} (attempt ${attemptNumber}, next check in ${Math.round(nextDelay / 1000)}s)`,
                    { id: 'execution-poll' }
                );
            }
        };

        const onComplete = (result) => {
            setExecutionResults(prev => ({ ...prev, [questionId]: result }));
            setIsExecuting(false);
            toast.dismiss('execution-poll');

            if (result.status === 'INTERNAL_ERROR') {
                toast.error(`Execution Error: ${result.error || 'Unknown error'}`, { duration: 5000 });
            } else if (result.passed) {
                toast.success('All test cases passed! ✓', { duration: 3000 });
            } else if (result.status?.includes('Compilation Error')) {
                toast.error('Compilation failed - check your code', { duration: 5000 });
            } else {
                toast.error('Some test cases failed', { duration: 4000 });
            }
        };

        const onTimeout = () => {
            setIsExecuting(false);
            toast.dismiss('execution-poll');
            toast.error(
                'Execution timed out after 30 attempts. Please try again.',
                { duration: 6000 }
            );

            setExecutionResults(prev => ({
                ...prev,
                [questionId]: {
                    status: 'TIMEOUT',
                    error: 'Execution timed out - please try again later',
                    executionId
                }
            }));
        };

        exponentialBackoffPoll(pollFunction, onUpdate, onComplete, onTimeout);
    };

    const handleSubmitTest = async () => {
        if (submitting) return; // Prevent double submission

        try {
            setSubmitting(true);
            await saveCurrentAnswer();
            await submitTest(attempt.id);
            toast.success('Test submitted successfully!');
            navigate('/student/history');
        } catch (error) {
            toast.error(error.message || 'Failed to submit test');
            setSubmitting(false); // Re-enable if failed
        }
    };

    const goToQuestion = (index) => {
        saveCurrentAnswer();
        setCurrentQuestionIndex(index);
    };

    const handleBeginTest = async () => {
        try {
            // Request fullscreen with user gesture
            await document.documentElement.requestFullscreen();

            setTestStarted(true);
            toast.success('Test started - Good luck!');
        } catch (error) {
            console.error('Fullscreen error:', error);
            toast.error('Please allow fullscreen to start the test');
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-900 flex items-center justify-center">
                <div className="text-white text-xl">Initializing test...</div>
            </div>
        );
    }

    // Show Begin Test screen before starting
    if (!testStarted) {
        return (
            <div className="min-h-screen bg-gradient-to-br from-gray-900 via-blue-900 to-gray-900 flex items-center justify-center p-4">
                <div className="max-w-2xl w-full bg-gray-800 rounded-2xl shadow-2xl p-8 border border-gray-700">
                    <div className="text-center">
                        <h1 className="text-4xl font-bold text-white mb-4">{test?.title}</h1>
                        <p className="text-gray-300 mb-8">{test?.description}</p>

                        <div className="bg-gray-700 rounded-lg p-6 mb-8">
                            <h2 className="text-xl font-semibold text-white mb-4">Test Information</h2>
                            <div className="grid grid-cols-2 gap-4 text-left">
                                <div>
                                    <p className="text-gray-400 text-sm">Duration</p>
                                    <p className="text-white font-semibold">{test?.durationMinutes} minutes</p>
                                </div>
                                <div>
                                    <p className="text-gray-400 text-sm">Questions</p>
                                    <p className="text-white font-semibold">{questions.length}</p>
                                </div>
                            </div>
                        </div>

                        <div className="bg-yellow-900 bg-opacity-50 border border-yellow-600 rounded-lg p-4 mb-8">
                            <h3 className="text-yellow-400 font-semibold mb-2">⚠️ Important Instructions</h3>
                            <ul className="text-yellow-200 text-sm text-left space-y-2">
                                <li>• The test will enter fullscreen mode</li>
                                <li>• Your webcam will be activated for proctoring</li>
                                <li>• Tab switching and copy/paste are disabled</li>
                                <li>• Violations will be logged and may result in auto-submission</li>
                            </ul>
                        </div>

                        <button
                            onClick={handleBeginTest}
                            className="w-full px-8 py-4 bg-gradient-to-r from-green-600 to-blue-600 hover:from-green-700 hover:to-blue-700 text-white text-xl font-bold rounded-lg transition-all transform hover:scale-105 shadow-lg"
                        >
                            🚀 Begin Test
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    const currentQuestion = questions[currentQuestionIndex];
    const isLastQuestion = currentQuestionIndex === questions.length - 1;

    if (isFrozen) {
        return (
            <div className="min-h-screen bg-gray-900 flex items-center justify-center p-4">
                <div className="max-w-md w-full bg-red-900 bg-opacity-20 border border-red-600 rounded-2xl p-8 text-center backdrop-blur-sm">
                    <div className="text-6xl mb-4">❄️</div>
                    <h1 className="text-3xl font-bold text-white mb-4">Test Frozen</h1>
                    <p className="text-gray-300 mb-8">
                        This test session has been suspended due to multiple proctoring violations (e.g., switching tabs or browsers).
                    </p>
                    <div className="bg-red-600 bg-opacity-20 rounded-lg p-4 mb-8 text-red-200 text-sm">
                        You can no longer continue this test. Please contact your moderator for assistance.
                    </div>
                    <button
                        onClick={() => navigate('/student/history')}
                        className="w-full py-3 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition font-semibold"
                    >
                        Back to My History
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-900 p-4">
            {/* Header */}
            <div className="bg-gray-800 rounded-lg p-4 mb-4 flex justify-between items-center">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => {
                            if (window.confirm("Are you sure you want to exit? Your progress will be saved but the timer will continue.")) {
                                navigate('/student/tests');
                            }
                        }}
                        className="p-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition"
                        title="Go Home / Exit Test"
                    >
                        🏠 Home
                    </button>
                    <div>
                        <h2 className="text-2xl font-bold text-white">{test?.title}</h2>
                        <p className="text-gray-400 text-sm">Question {currentQuestionIndex + 1} of {questions.length}</p>
                    </div>
                </div>
                <div className="text-right">
                    <div className={`text-3xl font-bold ${timeRemaining < 300 ? 'text-red-400' : 'text-green-400'}`}>
                        {formatTime(timeRemaining)}
                    </div>
                    <p className="text-gray-400 text-sm">Time Remaining</p>
                </div>
            </div>

            <div className="grid grid-cols-12 gap-4">
                {/* Question Navigator */}
                <div className="col-span-2">
                    <div className="bg-gray-800 rounded-lg p-4">
                        <h3 className="text-white font-semibold mb-3">Questions</h3>
                        <div className="space-y-6">
                            {Object.entries(questions.reduce((acc, q, idx) => {
                                const section = q.sectionName || 'Part-I';
                                if (!acc[section]) acc[section] = [];
                                acc[section].push({ q, idx });
                                return acc;
                            }, {})).map(([section, items]) => (
                                <div key={section}>
                                    <h3 className="text-gray-500 text-[10px] uppercase font-bold mb-2 tracking-widest">{section}</h3>
                                    <div className="grid grid-cols-3 gap-2">
                                        {items.map(({ q, idx }) => (
                                            <button
                                                key={q.id}
                                                onClick={() => goToQuestion(idx)}
                                                className={`w-10 h-10 rounded font-semibold text-xs transition-all ${idx === currentQuestionIndex
                                                    ? 'bg-blue-600 text-white shadow-lg shadow-blue-900/40'
                                                    : answers[q.id] || codeAnswers[q.id]
                                                        ? 'bg-green-700 text-white'
                                                        : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                                                    }`}
                                            >
                                                {idx + 1}
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Question Content */}
                <div className="col-span-10">
                    <div className="bg-gray-800 rounded-lg p-6">
                        {currentQuestion && (
                            <>
                                <div className="mb-6">
                                    <div className="flex items-center gap-3 mb-4">
                                        <span className={`px-3 py-1 rounded-full text-sm ${currentQuestion.type === 'MCQ' ? 'bg-purple-600' : 'bg-blue-600'
                                            } text-white`}>
                                            {currentQuestion.type}
                                        </span>
                                        <span className="text-gray-400">{currentQuestion.marks} marks</span>
                                    </div>
                                    <p className="text-white text-lg mb-4">{currentQuestion.questionText}</p>
                                </div>

                                {/* MCQ Options */}
                                {currentQuestion.type === 'MCQ' && (
                                    <div className="space-y-3">
                                        {['A', 'B', 'C', 'D'].map((option) => (
                                            <label
                                                key={option}
                                                className={`flex items-center p-4 rounded-lg border-2 cursor-pointer transition ${answers[currentQuestion.id] === option
                                                    ? 'border-blue-500 bg-blue-900 bg-opacity-30'
                                                    : 'border-gray-700 bg-gray-700 hover:border-gray-600'
                                                    }`}
                                            >
                                                <input
                                                    type="radio"
                                                    name={`question-${currentQuestion.id}`}
                                                    value={option}
                                                    checked={answers[currentQuestion.id] === option}
                                                    onChange={(e) => handleAnswerChange(currentQuestion.id, e.target.value)}
                                                    className="mr-3"
                                                />
                                                <span className="text-white">
                                                    <strong>{option}.</strong> {currentQuestion[`option${option}`]}
                                                </span>
                                            </label>
                                        ))}
                                    </div>
                                )}

                                {/* Coding Question */}
                                {currentQuestion.type === 'CODING' && (
                                    <div>
                                        <div className="mb-4">
                                            <div className="flex justify-between items-center mb-2">
                                                <label className="block text-gray-300">Your Code:</label>
                                                <select
                                                    className="bg-gray-700 text-white border-gray-600 rounded px-2 py-1 text-sm"
                                                    value={selectedLanguages[currentQuestion.id] || currentQuestion.allowedLanguageIds?.[0] || 62}
                                                    onChange={(e) => setSelectedLanguages({
                                                        ...selectedLanguages,
                                                        [currentQuestion.id]: parseInt(e.target.value)
                                                    })}
                                                >
                                                    {(!currentQuestion.allowedLanguageIds || currentQuestion.allowedLanguageIds.includes(62)) && <option value="62">Java</option>}
                                                    {(!currentQuestion.allowedLanguageIds || currentQuestion.allowedLanguageIds.includes(71)) && <option value="71">Python</option>}
                                                    {(!currentQuestion.allowedLanguageIds || currentQuestion.allowedLanguageIds.includes(54)) && <option value="54">C++</option>}
                                                    {(!currentQuestion.allowedLanguageIds || currentQuestion.allowedLanguageIds.includes(63)) && <option value="63">JavaScript</option>}
                                                </select>
                                            </div>
                                            <textarea
                                                value={codeAnswers[currentQuestion.id] || currentQuestion.starterCode || ''}
                                                onChange={(e) => handleCodeChange(currentQuestion.id, e.target.value)}
                                                className="w-full h-64 p-4 bg-gray-900 text-white font-mono text-sm rounded border border-gray-700 focus:border-blue-500 focus:outline-none"
                                                placeholder="Write your code here..."
                                            />
                                        </div>

                                        <button
                                            onClick={() => handleRunCode(currentQuestion.id)}
                                            disabled={isExecuting}
                                            className="px-6 py-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-600 text-white rounded-lg transition mb-4"
                                        >
                                            {isExecuting ? 'Running...' : '▶ Run Code'}
                                        </button>

                                        {executionResults[currentQuestion.id] && (
                                            <div className="mt-4 p-4 bg-gray-900 rounded-lg">
                                                {executionResults[currentQuestion.id].error && executionResults[currentQuestion.id].error.startsWith("LOGIC BLOCK:") ? (
                                                    <div className="bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-4 rounded shadow-md">
                                                        <h5 className="font-bold flex items-center gap-2">
                                                            🚫 Submission Blocked
                                                        </h5>
                                                        <p className="mt-1 font-semibold">
                                                            {executionResults[currentQuestion.id].error.replace("LOGIC BLOCK:", "").trim()}
                                                        </p>
                                                    </div>
                                                ) : (
                                                    <>
                                                        <h4 className="text-white font-semibold mb-2">Output:</h4>
                                                        <pre className="text-green-400 text-sm overflow-x-auto">
                                                            {executionResults[currentQuestion.id].stdout ||
                                                                executionResults[currentQuestion.id].error || // Normal runtime error
                                                                executionResults[currentQuestion.id].stderr ||
                                                                'No output'}
                                                        </pre>
                                                    </>
                                                )}

                                                {executionResults[currentQuestion.id].status && !executionResults[currentQuestion.id].error?.startsWith("LOGIC BLOCK:") && (
                                                    <p className="text-gray-400 text-sm mt-2">
                                                        Status: {executionResults[currentQuestion.id].status.description}
                                                    </p>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                )}

                                {/* Navigation */}
                                <div className="flex justify-between mt-8 pt-6 border-t border-gray-700">
                                    <button
                                        onClick={() => goToQuestion(currentQuestionIndex - 1)}
                                        disabled={currentQuestionIndex === 0}
                                        className="px-6 py-2 bg-gray-700 hover:bg-gray-600 disabled:bg-gray-800 disabled:text-gray-600 text-white rounded-lg transition"
                                    >
                                        ← Previous
                                    </button>

                                    {!isLastQuestion ? (
                                        <button
                                            onClick={() => goToQuestion(currentQuestionIndex + 1)}
                                            className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition"
                                        >
                                            Next →
                                        </button>
                                    ) : (
                                        <button
                                            onClick={() => setShowSubmitModal(true)}
                                            className="px-8 py-3 bg-green-600 hover:bg-green-700 text-white rounded-lg transition font-bold"
                                        >
                                            Submit Test
                                        </button>
                                    )}
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </div>

            {/* Submit Confirmation Modal */}
            {showSubmitModal && (
                <div className="fixed inset-0 bg-black bg-opacity-70 flex items-center justify-center z-50">
                    <div className="bg-gray-800 rounded-lg p-8 max-w-md">
                        <h3 className="text-2xl font-bold text-white mb-4">Submit Test?</h3>
                        <p className="text-gray-300 mb-6">
                            Are you sure you want to submit? You won't be able to change your answers after submission.
                        </p>
                        <div className="flex gap-4">
                            <button
                                onClick={handleSubmitTest}
                                className="flex-1 px-6 py-3 bg-green-600 hover:bg-green-700 text-white rounded-lg transition font-semibold"
                            >
                                Yes, Submit
                            </button>
                            <button
                                onClick={() => setShowSubmitModal(false)}
                                className="flex-1 px-6 py-3 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition"
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Proctoring Notice */}
            <ProctoringNotice violationCount={violationCount + aiViolationCount + violations.length} />

            {/* Webcam Preview */}
            {!loading && attempt && (
                <WebcamPreview
                    videoRef={videoRef}
                    faceCount={faceCount}
                    detectedObjects={detectedObjects}
                    modelsLoaded={modelsLoaded}
                />
            )}
        </div>
    );
}
