import { useState, useEffect } from 'react';
import { Form, Button, Row, Col, Card } from 'react-bootstrap';

export default function QuestionForm({ onSubmit, loading, initialData = null, testType = 'HYBRID' }) {
    // Determine allowed question types based on Test Type
    const allowedTypes = [];
    if (testType === 'MCQ_ONLY' || testType === 'HYBRID') allowedTypes.push('MCQ');
    if (testType === 'CODING_ONLY' || testType === 'HYBRID') allowedTypes.push('CODING');

    // Determine default type
    const defaultType = allowedTypes.includes('MCQ') ? 'MCQ' : 'CODING';

    const [formData, setFormData] = useState(initialData || {
        type: initialData?.type || defaultType,
        questionText: '',
        marks: 1,
        optionA: '',
        optionB: '',
        optionC: '',
        optionD: '',
        correctOption: 'A',
        languageId: 62, // Default (can receive from drop-down if needed)
        starterCode: '',
        constraints: initialData?.constraints || { banLoops: false, requireRecursion: false },
        allowedLanguages: initialData?.allowedLanguages || [62, 71, 54, 63], // Default ALL
    });

    const toggleLanguage = (id) => {
        setFormData(prev => {
            const current = prev.allowedLanguages || [];
            if (current.includes(id)) {
                return { ...prev, allowedLanguages: current.filter(x => x !== id) };
            } else {
                return { ...prev, allowedLanguages: [...current, id] };
            }
        });
    };

    useEffect(() => {
        if (!initialData && !allowedTypes.includes(formData.type)) {
            setFormData(prev => ({ ...prev, type: defaultType }));
        }
    }, [testType, allowedTypes, formData.type, defaultType, initialData]);

    const handleSubmit = (e) => {
        e.preventDefault();

        if (!formData.questionText.trim()) {
            alert("Please enter a question text!");
            return;
        }

        if (formData.type === 'MCQ') {
            const selectedOptionValue = formData[`option${formData.correctOption}`];
            if (!selectedOptionValue || !selectedOptionValue.trim()) {
                alert("The selected correct option cannot be empty!");
                return;
            }
        }

        onSubmit(formData);
    };

    return (
        <Form onSubmit={handleSubmit}>
            <Row className="g-3">
                <Col md={12}>
                    <Form.Group>
                        <Form.Label className="text-gray-300">Question Type</Form.Label>
                        <Form.Select
                            value={formData.type}
                            onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                            className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500"
                        >
                            {allowedTypes.includes('MCQ') && <option value="MCQ">Multiple Choice (MCQ)</option>}
                            {allowedTypes.includes('CODING') && <option value="CODING">Coding Question</option>}
                        </Form.Select>
                    </Form.Group>
                </Col>

                <Col md={12}>
                    <Form.Group>
                        <Form.Label className="text-gray-300">Question Text *</Form.Label>
                        <Form.Control
                            as="textarea"
                            required
                            rows={3}
                            value={formData.questionText}
                            onChange={(e) => setFormData({ ...formData, questionText: e.target.value })}
                            className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500 placeholder-gray-400"
                        />
                    </Form.Group>
                </Col>

                <Col md={12}>
                    <Form.Group>
                        <Form.Label className="text-gray-300">Marks *</Form.Label>
                        <Form.Control
                            type="number"
                            required
                            min="1"
                            value={formData.marks}
                            onChange={(e) => setFormData({ ...formData, marks: parseInt(e.target.value) })}
                            className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500 placeholder-gray-400"
                        />
                    </Form.Group>
                </Col>

                {formData.type === 'MCQ' && (
                    <>
                        <Col md={6}>
                            <Form.Group>
                                <Form.Label className="text-gray-300">Option A *</Form.Label>
                                <Form.Control
                                    type="text"
                                    required
                                    value={formData.optionA}
                                    onChange={(e) => setFormData({ ...formData, optionA: e.target.value })}
                                    className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500 placeholder-gray-400"
                                />
                            </Form.Group>
                        </Col>
                        <Col md={6}>
                            <Form.Group>
                                <Form.Label className="text-gray-300">Option B *</Form.Label>
                                <Form.Control
                                    type="text"
                                    required
                                    value={formData.optionB}
                                    onChange={(e) => setFormData({ ...formData, optionB: e.target.value })}
                                    className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500 placeholder-gray-400"
                                />
                            </Form.Group>
                        </Col>
                        <Col md={6}>
                            <Form.Group>
                                <Form.Label className="text-gray-300">Option C *</Form.Label>
                                <Form.Control
                                    type="text"
                                    required
                                    value={formData.optionC}
                                    onChange={(e) => setFormData({ ...formData, optionC: e.target.value })}
                                    className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500 placeholder-gray-400"
                                />
                            </Form.Group>
                        </Col>
                        <Col md={6}>
                            <Form.Group>
                                <Form.Label className="text-gray-300">Option D *</Form.Label>
                                <Form.Control
                                    type="text"
                                    required
                                    value={formData.optionD}
                                    onChange={(e) => setFormData({ ...formData, optionD: e.target.value })}
                                    className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500 placeholder-gray-400"
                                />
                            </Form.Group>
                        </Col>
                        <Col md={12}>
                            <Form.Group>
                                <Form.Label className="text-gray-300">Correct Answer *</Form.Label>
                                <Form.Select
                                    value={formData.correctOption}
                                    onChange={(e) => setFormData({ ...formData, correctOption: e.target.value })}
                                    className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500"
                                >
                                    <option value="A">A</option>
                                    <option value="B">B</option>
                                    <option value="C">C</option>
                                    <option value="D">D</option>
                                </Form.Select>
                            </Form.Group>
                        </Col>
                    </>
                )}

                {formData.type === 'CODING' && (
                    <>
                        <Col md={12}>
                            <Form.Group>
                                <Form.Label className="text-gray-300">Allowed Languages *</Form.Label>
                                <div className="d-flex gap-3 flex-wrap">
                                    <Form.Check
                                        type="checkbox"
                                        label="Java (62)"
                                        className="text-white"
                                        checked={formData.allowedLanguages?.includes(62)}
                                        onChange={() => toggleLanguage(62)}
                                    />
                                    <Form.Check
                                        type="checkbox"
                                        label="Python (71)"
                                        className="text-white"
                                        checked={formData.allowedLanguages?.includes(71)}
                                        onChange={() => toggleLanguage(71)}
                                    />
                                    <Form.Check
                                        type="checkbox"
                                        label="C++ (54)"
                                        className="text-white"
                                        checked={formData.allowedLanguages?.includes(54)}
                                        onChange={() => toggleLanguage(54)}
                                    />
                                    <Form.Check
                                        type="checkbox"
                                        label="JavaScript (63)"
                                        className="text-white"
                                        checked={formData.allowedLanguages?.includes(63)}
                                        onChange={() => toggleLanguage(63)}
                                    />
                                </div>
                            </Form.Group>
                        </Col>
                        {/* Optional: Default Start Language Selector could go here if needed, but not critical */}
                        <Col md={12}>
                            <Form.Group>
                                <Form.Label className="text-gray-300">Starter Code (optional)</Form.Label>
                                <Form.Control
                                    as="textarea"
                                    rows={5}
                                    value={formData.starterCode}
                                    onChange={(e) => setFormData({ ...formData, starterCode: e.target.value })}
                                    className="!bg-gray-700 !text-white !border-gray-600 focus:!bg-gray-700 focus:!text-white focus:!border-blue-500 placeholder-gray-400 font-monospace"
                                />
                            </Form.Group>
                        </Col>
                    </>
                )}

                {formData.type === 'CODING' && (
                    <Col md={12}>
                        <Card className="bg-gray-800 border-gray-700">
                            <Card.Body className="p-3">
                                <h6 className="text-white mb-2 fw-bold">Logic Constraints (Static Analysis)</h6>
                                <Form.Check
                                    type="checkbox"
                                    label="Ban Loops (Force Recursion)"
                                    id="banLoops"
                                    className="text-white mb-2"
                                    checked={formData.constraints?.banLoops || false}
                                    onChange={(e) => setFormData({
                                        ...formData,
                                        constraints: { ...formData.constraints, banLoops: e.target.checked }
                                    })}
                                />
                                <Form.Check
                                    type="checkbox"
                                    label="Require Recursion (Check for self-calls)"
                                    id="requireRecursion"
                                    className="text-white"
                                    checked={formData.constraints?.requireRecursion || false}
                                    onChange={(e) => setFormData({
                                        ...formData,
                                        constraints: { ...formData.constraints, requireRecursion: e.target.checked }
                                    })}
                                />
                            </Card.Body>
                        </Card>
                    </Col>
                )}

                <Col md={12} className="pt-3">
                    <Button
                        type="submit"
                        variant="success"
                        className="w-100 fw-bold"
                        disabled={loading}
                    >
                        {loading ? 'Saving...' : initialData ? 'Update Question' : 'Add to Test'}
                    </Button>
                </Col>
            </Row>
        </Form>
    );
}
