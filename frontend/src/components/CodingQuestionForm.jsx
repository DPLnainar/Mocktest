import React, { useState, useEffect } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { Button, Form, Spinner, Alert, ButtonGroup, ToggleButton } from 'react-bootstrap';
import { Upload, CheckCircle, XCircle, Play } from 'lucide-react';
import { moderatorAPI } from '../services/testAPI';
import toast from 'react-hot-toast';

export default function CodingQuestionForm({ onSubmit, defaultValues }) {
    const { register, control, handleSubmit, watch, setValue, formState: { errors } } = useForm({
        defaultValues: defaultValues || {
            type: 'CODING',
            questionText: '',
            description: '',
            constraints: '',
            marks: 10,
            allowedLanguages: [62, 71, 54, 63], // Default Java, Python, C++, JS
            constraints_config: { forbidLoops: false, requireRecursion: false },
            testCaseMode: 'text', // 'text' or 'file'
            testCaseFile: '',
            referenceSolution: '', // Code to verify
            testCases: [{ input: '', output: '' }]
        }
    });

    const { fields, append, remove } = useFieldArray({
        control,
        name: "testCases"
    });

    // Watchers
    const testCaseMode = watch('testCaseMode');
    const constraintsConfig = watch('constraints_config');
    const questionText = watch('questionText');
    const referenceSolution = watch('referenceSolution');
    const testCases = watch('testCases');

    // UI State
    const [verifying, setVerifying] = useState(false);
    const [verificationStatus, setVerificationStatus] = useState(null); // 'QUEUED', 'PROCESSING', 'SUCCESS', 'FAILED', 'ERROR'
    const [verificationResult, setVerificationResult] = useState(null);

    // File Upload Handler
    const handleFileUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const toastId = toast.loading("Uploading test cases...");
        try {
            const response = await moderatorAPI.uploadTestCase(file, questionText || 'temp');
            setValue('testCaseFile', response.data.s3Key);
            toast.success("File uploaded successfully", { id: toastId });
        } catch (error) {
            console.error(error);
            toast.error("Upload failed", { id: toastId });
        }
    };

    // Async Verification Handler
    const handleVerify = async () => {
        if (!referenceSolution) {
            toast.error("Please provide a Reference Solution to verify.");
            return;
        }

        setVerifying(true);
        setVerificationStatus('QUEUED');
        setVerificationResult(null);

        try {
            // 1. Submit Verification Request
            const payload = {
                code: referenceSolution,
                languageId: 62, // Defaulting to Java for verification (or make selectable)
                testCases: testCases,
                constraints: constraintsConfig
            };

            const response = await moderatorAPI.verifySolution(payload);
            const { verificationId } = response.data;

            // 2. Start Polling
            pollVerificationStatus(verificationId);

        } catch (error) {
            console.error("Verification failed:", error);
            toast.error("Failed to start verification");
            setVerifying(false);
            setVerificationStatus('ERROR');
        }
    };

    const pollVerificationStatus = async (id) => {
        const pollInterval = setInterval(async () => {
            try {
                const res = await moderatorAPI.getVerificationStatus(id);
                const status = res.data.status;
                const result = res.data.result;

                setVerificationStatus(status);

                if (status === 'SUCCESS' || status === 'FAILED' || status === 'ERROR') {
                    clearInterval(pollInterval);
                    setVerifying(false);
                    setVerificationResult(result);

                    if (status === 'SUCCESS') {
                        toast.success("Solution Verified: Passed!");
                    } else if (status === 'FAILED') {
                        toast.error("Solution Verified: Failed!");
                    } else {
                        toast.error("Verification Error: " + res.data.message);
                    }
                }
            } catch (error) {
                console.error("Polling error:", error);
                // Don't stop polling on transient errors, but maybe limit retries?
                // For now, keep polling.
            }
        }, 2000);
    };

    const isSaveEnabled = verificationStatus === 'SUCCESS';

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            {/* Title */}
            <div>
                <label className="block text-sm font-medium text-gray-700">Question Title</label>
                <input
                    {...register("questionText", { required: "Title is required" })}
                    type="text"
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm p-2 border"
                    placeholder="e.g. Reverse a String"
                />
                {errors.questionText && <p className="text-red-500 text-xs mt-1">{errors.questionText.message}</p>}
            </div>

            {/* Description */}
            <div>
                <label className="block text-sm font-medium text-gray-700">Description</label>
                <textarea
                    {...register("description", { required: "Description is required" })}
                    rows={4}
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm p-2 border"
                    placeholder="Detailed problem statement..."
                />
                {errors.description && <p className="text-red-500 text-xs mt-1">{errors.description.message}</p>}
            </div>

            {/* Reference Solution (For Verification) */}
            <div>
                <label className="block text-sm font-medium text-gray-700">Reference Solution (Java) *</label>
                <div className="mt-1">
                    <textarea
                        {...register("referenceSolution", { required: "Reference solution is required for verification" })}
                        rows={6}
                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm p-2 border font-mono bg-gray-50"
                        placeholder="public class Solution { public static void main(String[] args) { ... } }"
                    />
                    <p className="text-xs text-gray-500 mt-1">Provide a correct Java solution to verify test cases and constraints.</p>
                </div>
            </div>

            {/* Logic Constraints Panel */}
            <div className="bg-gray-50 p-4 rounded-lg border border-gray-200">
                <h6 className="font-semibold text-gray-700 mb-3">Logic Constraints</h6>
                <div className="flex gap-6">
                    <label className="inline-flex items-center cursor-pointer">
                        <input
                            type="checkbox"
                            {...register("constraints_config.forbidLoops")}
                            className="rounded border-gray-300 text-indigo-600 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                        />
                        <span className="ml-2 text-sm text-gray-700">Forbid Loops (Force Recursion)</span>
                    </label>
                    <label className="inline-flex items-center cursor-pointer">
                        <input
                            type="checkbox"
                            {...register("constraints_config.requireRecursion")}
                            className="rounded border-gray-300 text-indigo-600 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                        />
                        <span className="ml-2 text-sm text-gray-700">Require Recursion</span>
                    </label>
                </div>
            </div>

            {/* Constraints Text */}
            <div>
                <label className="block text-sm font-medium text-gray-700">Text Constraints (Display Only)</label>
                <textarea
                    {...register("constraints")}
                    rows={2}
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm p-2 border"
                    placeholder="e.g. 1 <= N <= 1000"
                />
            </div>

            {/* Marks */}
            <div>
                <label className="block text-sm font-medium text-gray-700">Marks</label>
                <input
                    {...register("marks", { required: true, min: 1 })}
                    type="number"
                    className="mt-1 block w-24 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm p-2 border"
                />
            </div>

            {/* Test Cases Toggle */}
            <div>
                <div className="flex justify-between items-center mb-2">
                    <label className="block text-sm font-medium text-gray-700">Test Cases</label>
                    <div className="flex bg-gray-200 rounded p-1">
                        <button
                            type="button"
                            onClick={() => setValue('testCaseMode', 'text')}
                            className={`px-3 py-1 text-xs rounded transition-all ${testCaseMode === 'text' ? 'bg-white shadow text-indigo-600 font-bold' : 'text-gray-600'}`}
                        >
                            Text Mode
                        </button>
                        <button
                            type="button"
                            onClick={() => setValue('testCaseMode', 'file')}
                            className={`px-3 py-1 text-xs rounded transition-all ${testCaseMode === 'file' ? 'bg-white shadow text-indigo-600 font-bold' : 'text-gray-600'}`}
                        >
                            File Mode
                        </button>
                    </div>
                </div>

                {testCaseMode === 'file' ? (
                    <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center bg-gray-50">
                        <Upload size={32} className="mx-auto text-gray-400 mb-2" />
                        <label className="cursor-pointer block">
                            <span className="mt-2 block text-sm font-medium text-indigo-600 hover:text-indigo-500">
                                Upload Test Case File
                            </span>
                            <input type="file" className="hidden" onChange={handleFileUpload} />
                        </label>
                        <p className="text-xs text-gray-500 mt-1">Upload .zip or .json containing inputs/outputs</p>
                        {watch('testCaseFile') && (
                            <div className="mt-2 text-xs text-green-600 flex items-center justify-center gap-1">
                                <CheckCircle size={12} /> File Uploaded: {watch('testCaseFile')}
                            </div>
                        )}
                    </div>
                ) : (
                    <div>
                        <div className="space-y-3">
                            {fields.map((field, index) => (
                                <div key={field.id} className="flex gap-4 items-start bg-gray-50 p-3 rounded-md border">
                                    <div className="flex-1">
                                        <label className="block text-xs font-medium text-gray-500">Input</label>
                                        <textarea
                                            {...register(`testCases.${index}.input`, { required: "Input is required (or switch to File Mode)" })}
                                            rows={2}
                                            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-xs p-1 border font-mono"
                                            placeholder="Input data"
                                        />
                                    </div>
                                    <div className="flex-1">
                                        <label className="block text-xs font-medium text-gray-500">Output</label>
                                        <textarea
                                            {...register(`testCases.${index}.output`, { required: "Output is required" })}
                                            rows={2}
                                            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-xs p-1 border font-mono"
                                            placeholder="Expected output"
                                        />
                                    </div>
                                    <div className="pt-5">
                                        <button
                                            type="button"
                                            onClick={() => remove(index)}
                                            className="text-red-600 hover:text-red-800"
                                        >
                                            <XCircle size={20} />
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                        <button
                            type="button"
                            onClick={() => append({ input: '', output: '' })}
                            className="mt-3 inline-flex items-center px-3 py-1.5 border border-transparent text-xs font-medium rounded-md text-indigo-700 bg-indigo-100 hover:bg-indigo-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                        >
                            + Add Test Case
                        </button>
                    </div>
                )}
            </div>

            {/* Allowed Languages */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Allowed Languages</label>
                <div className="flex flex-wrap gap-4">
                    {[
                        { id: 62, name: 'Java' },
                        { id: 71, name: 'Python' },
                        { id: 50, name: 'C' },
                        { id: 54, name: 'C++' },
                        { id: 63, name: 'JavaScript' }
                    ].map(lang => (
                        <label key={lang.id} className="inline-flex items-center">
                            <input
                                type="checkbox"
                                value={lang.id}
                                {...register("allowedLanguages")}
                                className="rounded border-gray-300 text-indigo-600 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
                            />
                            <span className="ml-2 text-sm text-gray-700">{lang.name}</span>
                        </label>
                    ))}
                </div>
            </div>

            {/* Verification Status & Save */}
            <div className="pt-5 border-t border-gray-200">
                <div className="flex justify-between items-center">

                    {/* Verify Button */}
                    <div className="flex items-center gap-4">
                        <Button
                            variant="warning"
                            onClick={handleVerify}
                            disabled={verifying}
                            className="d-flex align-items-center gap-2 text-white font-bold"
                        >
                            {verifying ? (
                                <><Spinner size="sm" animation="border" /> Verifying in Queue...</>
                            ) : (
                                <><Play size={18} /> Verify Solution</>
                            )}
                        </Button>

                        {/* Status Indicators */}
                        {verificationStatus === 'SUCCESS' && <span className="text-green-600 font-bold flex items-center gap-1"><CheckCircle size={18} /> PASSED</span>}
                        {verificationStatus === 'FAILED' && <span className="text-red-600 font-bold flex items-center gap-1"><XCircle size={18} /> FAILED</span>}
                    </div>

                    {/* Save Button */}
                    <Button
                        type="submit"
                        disabled={!isSaveEnabled}
                        variant={isSaveEnabled ? "success" : "secondary"}
                        className="font-bold px-6"
                        title={!isSaveEnabled ? "Verify solution first" : "Save Question"}
                    >
                        Save Question
                    </Button>
                </div>

                {/* Verification Error/Output Display */}
                {verificationResult && verificationStatus === 'FAILED' && (
                    <Alert variant="danger" className="mt-3">
                        <h6 className="font-bold">Verification Failed:</h6>
                        <ul className="mb-0 pl-4 text-sm">
                            {verificationResult.testCaseResults?.map((res, idx) => (
                                !res.passed && <li key={idx}>Test Case #{idx + 1}: Expected "{res.expectedOutput}" but got "{res.actualOutput}"</li>
                            ))}
                            {verificationResult.constraintViolations?.map((violation, idx) => (
                                <li key={idx}>Constraint: {violation}</li>
                            ))}
                        </ul>
                    </Alert>
                )}
            </div>
        </form>
    );
}
