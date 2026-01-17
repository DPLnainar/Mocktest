import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Container, Row, Col, Card, Button, Badge, Form, Modal } from 'react-bootstrap';
import { Upload, ChevronLeft, Plus, Save, Trash2, Eye, Pencil, CheckCircle, Clipboard, FileText } from 'lucide-react';
import toast from 'react-hot-toast';
import QuestionForm from './QuestionForm';
import { testAPI, questionAPI } from "../services/testAPI";
import BulkUploadComponent from './BulkUploadComponent'; // New unified upload component

const TestBuilder = () => {
    const navigate = useNavigate();
    const { testId } = useParams();
    const isEditingMode = !!testId;

    // 1. Test Configuration State
    const [testDetails, setTestDetails] = useState({
        title: '',
        description: '',
        durationMinutes: 60,
        startDateTime: '',
        endDateTime: '',
        type: 'MCQ_ONLY', // Default to MCQ
        instructions: 'Please answer all questions carefully.',
    });

    // 2. The "Master List" of questions for this test
    const [questions, setQuestions] = useState([]);

    // UI State
    const [showImportModal, setShowImportModal] = useState(false); // Unified Import Modal
    const [uploadMode, setUploadMode] = useState('file'); // 'file' or 'paste'
    const [isSubmitting, setIsSubmitting] = useState(false);

    const [showQuestionModal, setShowQuestionModal] = useState(false);
    const [isLoadingData, setIsLoadingData] = useState(false);

    // Modal State for Question Details (View/Edit)
    const [selectedQuestion, setSelectedQuestion] = useState(null);
    const [isEditing, setIsEditing] = useState(false);

    // --- FETCH TEST DATA FOR EDITING ---
    useEffect(() => {
        if (isEditingMode) {
            const fetchTestDetails = async () => {
                setIsLoadingData(true);
                try {
                    const response = await testAPI.getTestById(testId);
                    const data = response.data;

                    setTestDetails({
                        title: data.title || '',
                        description: data.description || '',
                        durationMinutes: data.durationMinutes || 60,
                        startDateTime: data.startDateTime ? data.startDateTime.substring(0, 16) : '',
                        endDateTime: data.endDateTime ? data.endDateTime.substring(0, 16) : '',
                        type: data.type || 'MCQ_ONLY',
                        instructions: data.instructions || '',
                        status: data.status || 'PUBLISHED'
                    });

                    if (data.testQuestions) {
                        const loadedQuestions = data.testQuestions.map((tq, idx) => ({
                            ...(tq.question || {}),
                            id: tq.questionId,  // Add this so we know it's an existing question
                            questionId: tq.questionId,
                            tempId: Date.now() + Math.random() + idx,
                            type: tq.question?.type || 'MCQ',
                            marks: tq.marks || tq.question?.marks || 1,
                        }));
                        setQuestions(loadedQuestions);
                    }
                } catch (error) {
                    console.error("Error fetching test details:", error);
                    toast.error("Failed to load test details");
                    navigate('/moderator/tests');
                } finally {
                    setIsLoadingData(false);
                }
            };
            fetchTestDetails();
        }
    }, [testId, isEditingMode, navigate]);

    // --- BULK IMPORT HANDLER ---
    const handleBulkUploadSuccess = (result) => {
        if (!result || !result.questionIds || result.questionIds.length === 0) {
            toast.error('No questions were uploaded');
            return;
        }

        // Create question objects from uploaded IDs
        // Create question objects from uploaded IDs by merging with parsed data
        const newQuestions = result.questionIds.map((id, idx) => {
            const parsedData = result.parsedQuestions && result.parsedQuestions[idx] ? result.parsedQuestions[idx] : {};
            return {
                ...parsedData, // Spread all parsed fields (questionText, marks, options, etc.)
                questionId: id,
                tempId: Date.now() + Math.random() + idx,
                type: parsedData.type || 'UPLOADED',
                marks: parsedData.marks || 1,
                questionText: parsedData.questionText || `Question ${id}`, // Fallback only if text is missing
                // Ensure coding specific fields are preserved if present
                testCases: parsedData.testCases || [],
                allowedLanguages: parsedData.allowedLanguageIds || [62, 71, 54, 63],
                starterCode: parsedData.starterCode || "",
                constraints: parsedData.constraints || { banLoops: false, requireRecursion: false }
            };
        });

        setQuestions(prev => {
            // Deduplicate: Compare new questions against existing ones by text content
            const existingTexts = new Set(prev.map(q => q.questionText?.trim()));
            const uniqueNewQuestions = newQuestions.filter(nq => !existingTexts.has(nq.questionText?.trim()));

            if (uniqueNewQuestions.length < newQuestions.length) {
                // Silently skip duplicates as requested by user
                console.log(`Skipped ${newQuestions.length - uniqueNewQuestions.length} duplicate questions.`);
            }

            return [...prev, ...uniqueNewQuestions];
        });
        setShowImportModal(false);
        // Toast handled by BulkUploadComponent, so removed here to avoid duplicates

        if (result.failed > 0) {
            toast.error(`${result.failed} questions failed to upload. Check the error report.`);
        }
    };

    // --- Question Management Handlers ---

    const handleAddNewClick = () => {
        const newTemplate = { type: testDetails.type === 'CODING_ONLY' ? 'CODING' : 'MCQ', tempId: null };
        setSelectedQuestion(newTemplate);
        setIsEditing(true);
        setShowQuestionModal(true);
    };

    const handleViewQuestion = (question) => {
        setSelectedQuestion(question);
        setIsEditing(false);
        setShowQuestionModal(true);
    };

    const handleEditQuestion = () => {
        setIsEditing(true);
    };

    const handleSaveQuestion = (updatedQuestion) => {
        if (selectedQuestion.tempId) {
            // "Copy-on-Write": Validation strategy
            // If we edit a question, we remove its ID to treat it as a "new candidate".
            // - If the text is unchanged, backend dedupe will return the old ID (Reuse).
            // - If text changed, backend creates a NEW question (Fork/Copy).
            // This prevents edits in Test A from breaking Test B, while allowing reuse.
            const dirtyQuestion = {
                ...updatedQuestion,
                id: null, // <--- Key change: Force re-verification/creation
                tempId: selectedQuestion.tempId
            };

            setQuestions(questions.map(q => q.tempId === selectedQuestion.tempId ? dirtyQuestion : q));
            toast.success("Question updated (will be saved as custom version)");
        } else {
            const newQ = { ...updatedQuestion, tempId: Date.now() + Math.random(), id: null };
            setQuestions([...questions, newQ]);
            toast.success("Question added");
        }
        setShowQuestionModal(false);
        setIsEditing(false);
        setSelectedQuestion(null);
    };

    const handleDeleteQuestion = (tempId, e) => {
        e.stopPropagation();
        if (window.confirm("Are you sure you want to delete this question?")) {
            setQuestions(questions.filter(q => q.tempId !== tempId));
            toast.success("Question deleted");
        }
    };

    // --- Finalize Test ---

    const finalizeTest = async () => {
        if (!testDetails.title.trim()) return toast.error("Please enter a test title");
        if (!testDetails.startDateTime || !testDetails.endDateTime) return toast.error("Please set test dates");
        if (questions.length === 0) return toast.error("Add at least one question!");

        if (isSubmitting) return;
        setIsSubmitting(true);
        const loadingToast = toast.loading("Creating test...");

        try {
            // 1. Identify unsaved questions
            const unsavedQuestions = questions.filter(q => !q.id);
            const savedQuestionsMap = new Map(); // tempId -> savedQuestion

            if (unsavedQuestions.length > 0) {
                const loadingSave = toast.loading(`Saving ${unsavedQuestions.length} new questions...`);
                try {
                    // Prepare questions for bulk create
                    const questionsToCreate = unsavedQuestions.map(q => ({
                        ...q,
                        languageId: q.languageId || (q.type === 'CODING' ? 62 : null)
                    }));

                    const { data: bulkResult } = await questionAPI.bulkCreate(questionsToCreate);

                    if (bulkResult.errorCount > 0 && bulkResult.successCount === 0) {
                        throw new Error(`Failed to save questions: ${bulkResult.errors[0]}`);
                    }

                    unsavedQuestions.forEach((q, index) => {
                        if (bulkResult.questionIds[index]) {
                            savedQuestionsMap.set(q.tempId, { ...q, id: bulkResult.questionIds[index] });
                        }
                    });

                    toast.success(`Saved ${bulkResult.successCount} questions!`);
                    toast.dismiss(loadingSave);

                } catch (err) {
                    console.error("Bulk save failed:", err);
                    toast.error("Failed to save new questions. Please try again.");
                    toast.dismiss(loadingSave);
                    setIsSubmitting(false);
                    toast.dismiss(loadingToast);
                    return;
                }
            }

            // 2. Merge saved questions back into the main list to get a fully persisted list
            const finalQuestions = questions.map(q => {
                if (q.id) return q;
                return savedQuestionsMap.get(q.tempId) || q;
            });

            // Verify all have IDs now
            const invalidQuestions = finalQuestions.filter(q => !q.id);
            if (invalidQuestions.length > 0) {
                toast.error(`Error: ${invalidQuestions.length} questions could not be saved.`);
                setIsSubmitting(false);
                toast.dismiss(loadingToast);
                return;
            }

            // Optional: Update local state
            setQuestions(finalQuestions);

            // 3. Format dates and create payload
            const formatDateTime = (dt) => dt && dt.length === 16 ? `${dt}:00` : dt;

            const payload = {
                id: isEditingMode ? parseInt(testId) : null,
                title: testDetails.title,
                description: testDetails.description,
                instructions: testDetails.instructions,
                durationMinutes: parseInt(testDetails.durationMinutes) || 60,
                type: testDetails.type,
                status: testDetails.status || 'PUBLISHED',
                testType: testDetails.testType || 'Placement Drive',
                startDateTime: formatDateTime(testDetails.startDateTime),
                endDateTime: formatDateTime(testDetails.endDateTime),
                testQuestions: finalQuestions.map((q, index) => ({
                    questionId: q.id,
                    marks: q.marks || 1,
                    orderIndex: index + 1,
                    sectionName: "General"
                }))
            };

            console.log("Finalizing test with payload:", JSON.stringify(payload, null, 2));

            if (isEditingMode) {
                await testAPI.updateTest(testId, payload);
                toast.success("Test updated successfully!");
            } else {
                await testAPI.createTest(payload);
                toast.success("Test created successfully!");
            }

            toast.dismiss(loadingToast);
            setTimeout(() => navigate('/moderator/tests'), 1000);
        } catch (error) {
            console.error("Failed to save test:", error);
            console.log("Error response data:", error.response?.data);
            toast.dismiss(loadingToast);
            const errorMsg = error.response?.data?.message || error.response?.data?.error || "Failed to save test";
            toast.error(errorMsg);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-900 text-white p-4">
            <Container style={{ maxWidth: '900px' }}>

                {/* Header */}
                <div className="d-flex align-items-center mb-4">
                    <Button variant="link" className="p-0 me-3 text-white" onClick={() => navigate('/moderator/tests')}>
                        <ChevronLeft size={24} />
                    </Button>
                    <h4 className="mb-0 text-white fw-bold">{isEditingMode ? 'Edit Test' : 'Create New Test'}</h4>
                </div>

                {/* 1. Test Configuration Card */}
                <Card className="border-0 shadow-sm mb-4 bg-gray-800 text-white">
                    <Card.Body className="p-4">
                        <h6 className="fw-bold mb-3 text-primary">1. Test Details</h6>
                        <Row className="g-3">
                            <Col md={12}>
                                <Form.Group>
                                    <Form.Label className="text-gray-300">Test Title *</Form.Label>
                                    <Form.Control
                                        type="text"
                                        className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500 placeholder-gray-400"
                                        placeholder="e.g. Java Mid-Term Exam"
                                        value={testDetails.title}
                                        onChange={e => setTestDetails({ ...testDetails, title: e.target.value })}
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group>
                                    <Form.Label className="text-gray-300">Start Time *</Form.Label>
                                    <Form.Control
                                        type="datetime-local"
                                        className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500 placeholder-gray-400"
                                        value={testDetails.startDateTime}
                                        onChange={e => setTestDetails({ ...testDetails, startDateTime: e.target.value })}
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group>
                                    <Form.Label className="text-gray-300">End Time *</Form.Label>
                                    <Form.Control
                                        type="datetime-local"
                                        className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500 placeholder-gray-400"
                                        value={testDetails.endDateTime}
                                        onChange={e => setTestDetails({ ...testDetails, endDateTime: e.target.value })}
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group>
                                    <Form.Label className="text-gray-300">Duration (mins)</Form.Label>
                                    <Form.Control
                                        type="number"
                                        className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500 placeholder-gray-400"
                                        value={testDetails.durationMinutes}
                                        onChange={e => {
                                            const val = parseInt(e.target.value);
                                            setTestDetails({
                                                ...testDetails,
                                                durationMinutes: isNaN(val) ? '' : val
                                            });
                                        }}
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group>
                                    <Form.Label className="text-gray-300">Test Type</Form.Label>
                                    <Form.Select
                                        className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500"
                                        value={testDetails.type}
                                        onChange={e => setTestDetails({ ...testDetails, type: e.target.value })}
                                    >
                                        <option value="MCQ_ONLY">MCQ Only</option>
                                        <option value="CODING_ONLY">Coding Only</option>
                                        <option value="HYBRID">Hybrid</option>
                                    </Form.Select>
                                </Form.Group>
                            </Col>
                        </Row>
                    </Card.Body>
                </Card>

                {/* 2. Questions Section */}
                <Card className="border-0 shadow-sm mb-4 bg-gray-800 text-white">
                    <Card.Body className="p-4">
                        <div className="d-flex justify-content-between align-items-center mb-4">
                            <h6 className="fw-bold m-0 text-primary">2. Questions ({questions.length})</h6>

                            {/* Toolbar */}
                            <div className="d-flex gap-2">

                                {/* Separate Buttons as requested */}
                                {/* Separate Buttons as requested */}
                                <Button size="sm" variant="outline-primary" onClick={() => { setUploadMode('file'); setShowImportModal(true); }}>
                                    <FileText size={16} className="me-1" /> Upload Excel/CSV
                                </Button>
                                <Button size="sm" variant="outline-info" onClick={() => { setUploadMode('paste'); setShowImportModal(true); }}>
                                    <Clipboard size={16} className="me-1" /> Smart Paste
                                </Button>
                            </div>
                        </div>

                        {/* Questions List */}
                        <div className="d-flex flex-column gap-2">
                            {questions.length === 0 ? (
                                <div className="text-center py-5 border border-dashed border-gray-700 rounded bg-gray-900">
                                    <p className="text-gray-400 mb-0">No questions added yet.</p>

                                </div>
                            ) : (
                                questions.map((q, index) => (
                                    <div key={q.tempId} className="d-flex align-items-center justify-content-between p-3 rounded bg-gray-700 border border-gray-600 hover:border-gray-500 transition cursor-pointer" onClick={() => handleViewQuestion(q)}>
                                        <div className="d-flex align-items-center gap-3 overflow-hidden">
                                            <Badge bg="secondary" className="rounded-circle p-2" style={{ width: 30, height: 30, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                                {index + 1}
                                            </Badge>
                                            <div className="d-flex flex-column" style={{ minWidth: 0 }}>
                                                <div className="d-flex align-items-center gap-2 mb-1">
                                                    <Badge bg={q.type === 'MCQ' ? 'info' : 'warning'} className="text-xs">
                                                        {q.type}
                                                    </Badge>
                                                    <span className="fw-bold text-truncate text-white d-block" style={{ maxWidth: '400px' }}>
                                                        {q.questionText || q.question || "Untitled Question"}
                                                    </span>
                                                </div>
                                                <small className="text-gray-400 text-truncate">
                                                    {q.type === 'MCQ' ? `Correct: ${q.correctOption}` : 'Coding Challenge'} • {q.marks} Marks
                                                </small>
                                            </div>
                                        </div>
                                        <div className="d-flex align-items-center gap-2">
                                            <Button variant="dark" size="sm" className="text-gray-300" title="View/Edit"><Eye size={16} /></Button>
                                            <Button variant="dark" size="sm" className="text-danger hover:bg-danger hover:text-white" onClick={(e) => handleDeleteQuestion(q.tempId, e)} title="Delete"><Trash2 size={16} /></Button>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </Card.Body>
                </Card>

                {/* Finalize Button */}
                <div className="d-grid gap-2 mb-5">
                    <Button variant="success" size="lg" onClick={finalizeTest} disabled={questions.length === 0 || isSubmitting || isLoadingData} className="fw-bold py-3">
                        {isSubmitting ? (
                            <><span className="spinner-border spinner-border-sm me-2" /> {isEditingMode ? 'Updating...' : 'Creating...'}</>
                        ) : (
                            <><Save size={20} className="me-2" /> {isEditingMode ? 'Save & Update Test' : 'Finalize & Create Test'}</>
                        )}
                    </Button>
                </div>

                {/* --- MODALS --- */}

                {/* Question Detail Modal (View/Edit) */}
                <Modal show={showQuestionModal} onHide={() => setShowQuestionModal(false)} size="lg" centered contentClassName="bg-gray-800 text-white border-0 shadow-lg" backdrop="static">
                    <Modal.Header closeButton closeVariant="white" className="border-gray-700">
                        <Modal.Title>{isEditing ? (selectedQuestion?.tempId ? "Edit Question" : "New Question") : "Question Preview"}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body className="p-0">
                        {showQuestionModal && (
                            isEditing ? (
                                <div className="p-4">
                                    <QuestionForm onSubmit={(data) => handleSaveQuestion(data)} initialData={selectedQuestion} testType={testDetails.type} />
                                    <div className="mt-3 text-end"><Button variant="secondary" size="sm" onClick={() => setIsEditing(false)}>Cancel Edit</Button></div>
                                </div>
                            ) : (
                                <div className="p-4">
                                    <div className="d-flex justify-content-between mb-3">
                                        <Badge bg={selectedQuestion?.type === 'MCQ' ? 'info' : 'warning'}>{selectedQuestion?.type}</Badge>
                                        <Badge bg="success">{selectedQuestion?.marks} Marks</Badge>
                                    </div>
                                    <h5 className="mb-4">{selectedQuestion?.questionText || selectedQuestion?.question}</h5>
                                    {selectedQuestion?.type === 'MCQ' && (
                                        <div className="d-flex flex-column gap-2">
                                            {['A', 'B', 'C', 'D'].map(opt => (
                                                <div key={opt} className={`p-3 rounded border ${selectedQuestion.correctOption === opt ? 'border-success bg-success bg-opacity-10' : 'border-gray-600 bg-gray-700'}`}>
                                                    <span className="fw-bold me-2">{opt})</span> {selectedQuestion[`option${opt}`] || "Option Text"}
                                                    {selectedQuestion.correctOption === opt && <CheckCircle size={16} className="text-success float-end" />}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                    {selectedQuestion?.type === 'CODING' && (
                                        <div className="bg-black p-3 rounded font-monospace text-sm text-gray-300">
                                            <div className="text-gray-500 mb-2">// Starter Code (Language ID: {selectedQuestion?.languageId})</div>
                                            <pre className="m-0">{selectedQuestion?.starterCode || "// No starter code provided"}</pre>
                                        </div>
                                    )}
                                </div>
                            )
                        )}
                    </Modal.Body>
                    <Modal.Footer className="border-gray-700">
                        {!isEditing && (
                            <>
                                <Button variant="secondary" onClick={() => setShowQuestionModal(false)}>Close</Button>
                                <Button variant="primary" onClick={handleEditQuestion}><Pencil size={16} className="me-2" /> Edit Question</Button>
                            </>
                        )}
                    </Modal.Footer>
                </Modal>

                {/* UNIFIED BULK IMPORT MODAL */}
                <Modal
                    show={showImportModal}
                    onHide={() => setShowImportModal(false)}
                    size="xl" // Extra large for the robust uploader
                    centered
                    contentClassName="bg-transparent border-0 shadow-none" // QuestionUploader has its own styling
                >
                    <Modal.Header closeButton closeVariant="white" className="border-gray-700 bg-gray-800 rounded-t-lg">
                        <Modal.Title className="text-white">
                            {uploadMode === 'paste' ? 'Smart Paste Questions' : 'Upload Excel/CSV File'}
                        </Modal.Title>
                    </Modal.Header>
                    <Modal.Body className="p-0">
                        {/* New BulkUploadComponent with unified feedback */}
                        <BulkUploadComponent
                            onSuccess={handleBulkUploadSuccess}
                            testType={testDetails.type === 'MCQ_ONLY' ? 'MCQ' : testDetails.type === 'CODING_ONLY' ? 'CODING' : 'HYBRID'}
                            initialTab={uploadMode}
                            hideTabs={true}
                        />
                    </Modal.Body>
                </Modal>

            </Container>
        </div>
    );
};

export default TestBuilder;
