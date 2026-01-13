import { useEffect, useRef, useState } from 'react';
import * as faceapi from '@vladmandic/face-api/dist/face-api.esm-nobundle.js';
import * as cocoSsd from '@tensorflow-models/coco-ssd';

import api from '../services/api';
import toast from 'react-hot-toast';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const useAIProctoring = (attemptId, isActive = true) => {
    const [modelsLoaded, setModelsLoaded] = useState(false);
    const [faceCount, setFaceCount] = useState(0);
    const [detectedObjects, setDetectedObjects] = useState([]);
    const [aiViolationCount, setAiViolationCount] = useState(0);

    const videoRef = useRef(null);
    const faceDetectionModel = useRef(null);
    const objectDetectionModel = useRef(null);
    const detectionInterval = useRef(null);
    const lastViolationTime = useRef({});

    // Load AI models
    useEffect(() => {
        if (!isActive || !attemptId) return;

        const loadModels = async () => {
            try {
                // Check if models are already loaded to prevent reloading
                if (faceapi.nets.tinyFaceDetector.isLoaded && objectDetectionModel.current) {
                    setModelsLoaded(true);
                    return;
                }

                console.log('Loading AI proctoring models...');

                // Load face-api.js models from CDN
                const MODEL_URL = 'https://justadudewhohacks.github.io/face-api.js/models';

                if (!faceapi.nets.tinyFaceDetector.isLoaded) {
                    await faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL);
                }

                // Load COCO-SSD model
                if (!objectDetectionModel.current) {
                    objectDetectionModel.current = await cocoSsd.load();
                }

                setModelsLoaded(true);
                console.log('AI models loaded successfully');
                toast.success('AI proctoring activated');
            } catch (error) {
                console.error('Failed to load AI models:', error);

                // Don't show error if it's just a backend re-registration warning that threw an error
                // (though usually those are just warnings, actual errors should be shown)
                if (!error.message?.includes('already registered')) {
                    toast.error('AI proctoring unavailable');
                }
            }
        };

        loadModels();
    }, [attemptId, isActive]);

    // Initialize webcam (don't wait for models to load)
    useEffect(() => {
        if (!isActive || !attemptId) return;

        const startWebcam = async () => {
            try {
                console.log('Requesting webcam access...');
                const stream = await navigator.mediaDevices.getUserMedia({
                    video: { width: 640, height: 480 }
                });

                if (videoRef.current) {
                    videoRef.current.srcObject = stream;
                    // Wait for video to be ready
                    videoRef.current.onloadedmetadata = () => {
                        videoRef.current.play();
                        console.log('Webcam started successfully');
                    };
                }
            } catch (error) {
                console.error('Webcam access denied:', error);
                toast.error('Please enable webcam for proctoring');
            }
        };

        startWebcam();

        return () => {
            if (videoRef.current?.srcObject) {
                const tracks = videoRef.current.srcObject.getTracks();
                tracks.forEach(track => track.stop());
            }
        };
    }, [attemptId, isActive]);

    // Run AI detection
    useEffect(() => {
        if (!isActive || !attemptId || !modelsLoaded || !videoRef.current) return;

        const runDetection = async () => {
            try {
                // Face detection
                const faceDetections = await faceapi.detectAllFaces(
                    videoRef.current,
                    new faceapi.TinyFaceDetectorOptions()
                );

                const currentFaceCount = faceDetections.length;
                setFaceCount(currentFaceCount);

                // Check for violations
                const now = Date.now();

                // Multiple faces detected
                if (currentFaceCount > 1) {
                    if (!lastViolationTime.current.multipleFaces ||
                        now - lastViolationTime.current.multipleFaces > 10000) {
                        await logViolation('MULTIPLE_FACES_DETECTED', {
                            faceCount: currentFaceCount
                        });
                        lastViolationTime.current.multipleFaces = now;
                    }
                }

                // No face detected
                if (currentFaceCount === 0) {
                    if (!lastViolationTime.current.noFace ||
                        now - lastViolationTime.current.noFace > 10000) {
                        await logViolation('NO_FACE_DETECTED', {});
                        lastViolationTime.current.noFace = now;
                    }
                }

                // Object detection
                const predictions = await objectDetectionModel.current.detect(videoRef.current);
                setDetectedObjects(predictions);

                // Check for unauthorized objects
                const suspiciousObjects = predictions.filter(pred =>
                    ['cell phone', 'book', 'laptop'].includes(pred.class.toLowerCase())
                );

                for (const obj of suspiciousObjects) {
                    const key = `object_${obj.class}`;
                    if (!lastViolationTime.current[key] ||
                        now - lastViolationTime.current[key] > 15000) {
                        await logViolation('UNAUTHORIZED_OBJECT_DETECTED', {
                            object: obj.class,
                            confidence: obj.score
                        });
                        lastViolationTime.current[key] = now;
                    }
                }

            } catch (error) {
                console.error('Detection error:', error);
            }
        };

        // Run detection every 5 seconds
        detectionInterval.current = setInterval(runDetection, 5000);

        return () => {
            if (detectionInterval.current) {
                clearInterval(detectionInterval.current);
            }
        };
    }, [attemptId, isActive, modelsLoaded]);

    const logViolation = async (eventType, metadata) => {
        try {
            const response = await api.post(
                '/proctor/violation',
                {
                    attemptId,
                    eventType,
                    metadata,
                }
            );

            setAiViolationCount(prev => prev + 1);

            // Show warning to user
            const messages = {
                'MULTIPLE_FACES_DETECTED': 'Multiple people detected!',
                'NO_FACE_DETECTED': 'No face detected - stay in view!',
                'UNAUTHORIZED_OBJECT_DETECTED': `Unauthorized object detected: ${metadata.object}`
            };

            toast.error(messages[eventType] || 'Proctoring violation detected');

            return response.data;
        } catch (error) {
            console.error('Failed to log AI violation:', error);
        }
    };

    return {
        videoRef,
        modelsLoaded,
        faceCount,
        detectedObjects,
        aiViolationCount
    };
};
