import { useEffect, useRef, useState } from 'react';
import * as faceapi from '@vladmandic/face-api/dist/face-api.esm-nobundle.js';
import * as cocoSsd from '@tensorflow-models/coco-ssd';

import api from '../services/api';
import toast from 'react-hot-toast';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const useAIProctoring = (attemptId, isActive = true, onCriticalViolation = null) => {
    const [modelsLoaded, setModelsLoaded] = useState(false);
    const [faceCount, setFaceCount] = useState(0);
    const [detectedObjects, setDetectedObjects] = useState([]);
    const [aiViolationCount, setAiViolationCount] = useState(0);
    const [headRotation, setHeadRotation] = useState(false); // New state for Head Rotation

    const videoRef = useRef(null);
    const faceDetectionModel = useRef(null);
    const objectDetectionModel = useRef(null);
    const detectionInterval = useRef(null);
    const lastViolationTime = useRef({});

    const [cameraStatus, setCameraStatus] = useState('pending'); // pending, active, error

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

                // Load Face Landmark model for Head Rotation/Gaze
                if (!faceapi.nets.faceLandmark68TinyNet.isLoaded) {
                    console.log('Loading face landmark model...');
                    await faceapi.nets.faceLandmark68TinyNet.loadFromUri(MODEL_URL);
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

        let retryCount = 0;
        const maxRetries = 10; // 5 seconds total

        const startWebcam = async () => {
            try {
                if (!videoRef.current) {
                    if (retryCount < maxRetries) {
                        retryCount++;
                        console.log(`Video ref not ready, retrying (${retryCount}/${maxRetries})...`);
                        setTimeout(startWebcam, 500);
                        return;
                    } else {
                        throw new Error('Video element not found');
                    }
                }

                setCameraStatus('loading');
                console.log('Requesting webcam access...');
                const stream = await navigator.mediaDevices.getUserMedia({
                    video: { width: 640, height: 480 }
                });

                videoRef.current.srcObject = stream;
                // Wait for video to be ready
                videoRef.current.onloadedmetadata = () => {
                    videoRef.current.play();
                    setCameraStatus('active');
                    console.log('Webcam started successfully');
                };
            } catch (error) {
                console.error('Webcam access denied:', error);
                setCameraStatus('error');
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
                // Face detection with landmarks
                const faceDetections = await faceapi.detectAllFaces(
                    videoRef.current,
                    new faceapi.TinyFaceDetectorOptions()
                ).withFaceLandmarks(true);  // Enable landmarks

                const currentFaceCount = faceDetections.length;
                setFaceCount(currentFaceCount);

                const now = Date.now();

                // Head Rotation / Looking Away Logic
                if (currentFaceCount === 1) {
                    const face = faceDetections[0];
                    const landmarks = face.landmarks;
                    const nose = landmarks.getNose()[3]; // Tip of the nose
                    const box = face.detection.box;

                    // Calculate relative nose position (0 to 1)
                    const noseRelX = (nose.x - box.x) / box.width;
                    const noseRelY = (nose.y - box.y) / box.height;

                    // If nose is too far left (<0.3) or right (>0.7), consider it looking away
                    const isLookingAway = noseRelX < 0.3 || noseRelX > 0.7 || noseRelY < 0.2 || noseRelY > 0.8;
                    setHeadRotation(isLookingAway);

                    if (isLookingAway) {
                        const key = 'head_rotated';
                        // Debounce 3 seconds
                        if (!lastViolationTime.current[key] ||
                            now - lastViolationTime.current[key] > 3000) {

                            await logViolation('HEAD_ROTATED', {
                                direction: noseRelX < 0.3 ? 'right' : noseRelX > 0.7 ? 'left' : 'vertical'
                            });
                            lastViolationTime.current[key] = now;
                        }
                    }
                }

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

                // DEBUG: Log all detections to debug phone issue
                if (predictions.length > 0) {
                    console.log('AI Objects:', predictions.map(p => `${p.class} (${Math.round(p.score * 100)}%)`));
                }

                // Enhanced phone camera detection
                const phoneDetections = predictions.filter(pred => {
                    const className = pred.class.toLowerCase();
                    return className.includes('cell phone') ||
                        className.includes('mobile') ||
                        className.includes('remote'); // Phones often misidentified as remotes
                });

                // Check for phone ANYWHERE in frame
                for (const phone of phoneDetections) {
                    // SENSITIVE DETECTION: > 25% confidence
                    // Tuned for high recall to prevent cheating
                    if (phone.score > 0.25) {
                        const bbox = phone.bbox;
                        const key = 'camera_detected';

                        // Reduced debounce to 3 seconds
                        if (!lastViolationTime.current[key] ||
                            now - lastViolationTime.current[key] > 3000) {

                            // CRITICAL: Immediate Frontend Freeze
                            if (onCriticalViolation) {
                                console.log(`CRITICAL: Phone detected (${Math.round(phone.score * 100)}%) - Invoking callback`);
                                onCriticalViolation();
                            }

                            await logViolation('CAMERA_DETECTED', {
                                object: 'phone',
                                confidence: phone.score,
                                position: 'detected-in-frame', // Position check removed
                                likelyTakingPhoto: true,
                                bbox: bbox
                            });
                            lastViolationTime.current[key] = now;
                        }
                        // Stop after first phone found
                        break;
                    }
                }

                // Check for other unauthorized objects
                const otherSuspiciousObjects = predictions.filter(pred => {
                    const className = pred.class.toLowerCase();
                    return className.includes('book') || className.includes('laptop');
                });

                for (const obj of otherSuspiciousObjects) {
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

        // Recursive detection function
        const detectionLoop = async () => {
            if (!videoRef.current || videoRef.current.paused || videoRef.current.ended) {
                // Wait and retry if video not ready
                detectionInterval.current = requestAnimationFrame(detectionLoop);
                return;
            }

            const startTime = Date.now();

            await runDetection();

            // Calculate delay to maintain ~500ms equivalent interval
            // If detection took 100ms, wait 400ms. If it took 600ms, wait 0ms (immediate).
            const elapsed = Date.now() - startTime;
            const delay = Math.max(10, 500 - elapsed);

            detectionInterval.current = setTimeout(detectionLoop, delay);
        };

        // Start loop
        detectionLoop();

        return () => {
            if (detectionInterval.current) {
                clearTimeout(detectionInterval.current);
                cancelAnimationFrame(detectionInterval.current);
            }
        };
    }, [attemptId, isActive, modelsLoaded]);

    const logViolation = async (eventType, metadata) => {
        try {
            const messages = {
                'MULTIPLE_FACES_DETECTED': 'Multiple people detected!',
                'NO_FACE_DETECTED': 'No face detected - stay in view!',
                'CAMERA_DETECTED': '📱 Phone detected! Do not take photos of the screen.',
                'UNAUTHORIZED_OBJECT_DETECTED': `Unauthorized object detected: ${metadata.object}`
            };

            // Determine severity
            let severity = 'MAJOR';
            if (eventType === 'NO_FACE_DETECTED') severity = 'MINOR';
            if (eventType === 'CAMERA_DETECTED') severity = 'CRITICAL';

            const response = await api.post(
                '/violations/report',
                {
                    sessionId: attemptId,
                    examId: null,
                    violationType: eventType,
                    severity: severity,
                    message: messages[eventType] || 'Proctoring violation detected',
                    evidence: metadata,
                    consecutiveFrames: 1,
                    confidence: metadata.confidence || 1.0,
                    confirmed: true
                }
            );

            setAiViolationCount(prev => prev + 1);
            toast.error(messages[eventType] || 'Proctoring violation detected');

            // CRITICAL FIX: Check for termination
            if (response.data?.terminated || response.data?.status === 'FROZEN') {
                console.log('Violation triggered termination/freeze. Redirecting...');
                toast.error('Test terminated due to violations');
                window.location.href = '/exam-terminated';
            }

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
        aiViolationCount,
        cameraStatus, // Expose status
        headRotation // Expose rotation state
    };
};
