import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Container, Row, Col, Card, Button, Badge, Form, Modal } from 'react-bootstrap';
import { Upload, ChevronLeft, Plus, Save, Trash2, Eye, Pencil, CheckCircle, Clipboard, FileText } from 'lucide-react';
import toast from 'react-hot-toast';
import QuestionForm from './QuestionForm';
import { testAPI } from "../services/testAPI";
import BulkUploadComponent from './BulkUploadComponent'; // New unified upload component

const TestBuilder = () => {
    const navigate = useNavigate();

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

    // Modal State for Question Details (View/Edit)
    const [selectedQuestion, setSelectedQuestion] = useState(null);
    const [isEditing, setIsEditing] = useState(false);
    const [showQuestionModal, setShowQuestionModal] = useState(false);

    // --- BULK IMPORT HANDLER ---
    const handleBulkUploadSuccess = (result) => {
        if (!result || !result.questionIds || result.questionIds.length === 0) {
            toast.error('No questions were uploaded');
            return;
        }

        // Create question objects from uploaded IDs
        const newQuestions = result.questionIds.map((id, idx) => ({
            questionId: id,
            tempId: Date.now() + Math.random() + idx,
            type: 'UPLOADED', // Mark as uploaded from backend
            marks: 1,
            questionText: `Question ${id}` // Placeholder - will be loaded from backend
        }));

        setQuestions(prev => [...prev, ...newQuestions]);
        setShowImportModal(false);
        toast.success(`Successfully uploaded ${result.saved} questions!`);

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
            setQuestions(questions.map(q => q.tempId === selectedQuestion.tempId ? { ...updatedQuestion, tempId: selectedQuestion.tempId } : q));
            toast.success("Question updated");
        } else {
            const newQ = { ...updatedQuestion, tempId: Date.now() + Math.random() };
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
            const payload = {
                ...testDetails,
                testQuestions: questions.map((q, index) => ({
                    question: {
                        ...q,
                        marks: q.marks || 1,
                        languageId: q.languageId || (q.type === 'CODING' ? 62 : null)
                    },
                    questionOrder: index + 1
                }))
            };
            await testAPI.createTest(payload);
            toast.dismiss(loadingToast);
            toast.success("Test created successfully!");
            navigate('/moderator/tests');
        } catch (error) {
            console.error("Failed to create test:", error);
            toast.dismiss(loadingToast);
            toast.error(error.response?.data?.message || "Failed to upload test");
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
                    <h4 className="mb-0 text-white fw-bold">Create New Test</h4>
                </div>

                {/* 1. Test Configuration Card */}
                <Card className="border-0 shadow-sm mb-4 bg-gray-800 text-white">
                    <Card.Body className="p-4">
                        <h6 className="fw-bold mb-3 text-primary">1. Test Details</h6>
                        <Row className="g-3">
                            <Col md={12}>
                                <Form.Group>
                                    <Form.Label className="text-gray-300">Test Title *</Form.Label>
                                    <Form.Control type="text" className="bg-gray-700 text-white border-gray-600" placeholder="e.g. Java Mid-Term Exam" value={testDetails.title} onChange={e => setTestDetails({ ...testDetails, title: e.target.value })} />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group>
                                    <Form.Label className="text-gray-300">Start Time *</Form.Label>
                                    <Form.Control type="datetime-local" className="bg-gray-700 text-white border-gray-600" value={testDetails.startDateTime} onChange={e => setTestDetails({ ...testDetails, startDateTime: e.target.value })} />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group>
                                    <Form.Label className="text-gray-300">End Time *</Form.Label>
                                    <Form.Control type="datetime-local" className="bg-gray-700 text-white border-gray-600" value={testDetails.endDateTime} onChange={e => setTestDetails({ ...testDetails, endDateTime: e.target.value })} />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group>
                                    <Form.Label className="text-gray-300">Duration (mins)</Form.Label>
                                    <Form.Control type="number" className="bg-gray-700 text-white border-gray-600" value={testDetails.durationMinutes} onChange={e => setTestDetails({ ...testDetails, durationMinutes: parseInt(e.target.value) })} />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group>
                                    <Form.Label className="text-gray-300">Test Type</Form.Label>
                                    <Form.Select className="bg-gray-700 text-white border-gray-600" value={testDetails.type} onChange={e => setTestDetails({ ...testDetails, type: e.target.value })}>
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
                    <Button variant="success" size="lg" onClick={finalizeTest} disabled={questions.length === 0 || isSubmitting} className="fw-bold py-3">
                        {isSubmitting ? (<><span className="spinner-border spinner-border-sm me-2" /> Creating Test...</>) : (<><Save size={20} className="me-2" /> Finalize & Publish Test</>)}
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
