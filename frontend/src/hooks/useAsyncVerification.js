import { useState, useEffect } from 'react';
import { moderatorAPI } from '../services/testAPI';
import toast from 'react-hot-toast';

/**
 * Hook for handling async solution verification
 * Submits verification request and polls for results
 */
export const useAsyncVerification = () => {
    const [verificationId, setVerificationId] = useState(null);
    const [status, setStatus] = useState('IDLE');
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);
    const [pollingInterval, setPollingInterval] = useState(null);

    /**
     * Start verification process
     */
    const startVerification = async (verificationRequest) => {
        try {
            setStatus('SUBMITTING');
            setError(null);
            setResult(null);

            const { data } = await moderatorAPI.verifySolution(verificationRequest);

            setVerificationId(data.verificationId);
            setStatus(data.status);

            toast.success('Verification queued successfully!');

        } catch (err) {
            console.error('Verification submission failed:', err);
            setError(err.response?.data?.message || err.message || 'Failed to submit verification');
            setStatus('ERROR');
            toast.error('Failed to submit verification');
        }
    };

    /**
     * Poll for verification status
     */
    useEffect(() => {
        if (!verificationId || status === 'SUCCESS' || status === 'FAILED' || status === 'ERROR' || status === 'IDLE') {
            if (pollingInterval) {
                clearInterval(pollingInterval);
                setPollingInterval(null);
            }
            return;
        }

        const interval = setInterval(async () => {
            try {
                const { data } = await moderatorAPI.getVerificationStatus(verificationId);

                setStatus(data.status);

                if (data.status === 'SUCCESS') {
                    setResult(data.result);
                    toast.success('✅ Solution verified successfully!');
                    clearInterval(interval);
                } else if (data.status === 'FAILED') {
                    setResult(data.result);
                    toast.error('❌ Solution verification failed');
                    clearInterval(interval);
                } else if (data.status === 'ERROR') {
                    setError(data.message);
                    toast.error('⚠️ Verification error');
                    clearInterval(interval);
                }

            } catch (err) {
                console.error('Failed to poll verification status:', err);
                setError(err.message);
                setStatus('ERROR');
                clearInterval(interval);
            }
        }, 2000); // Poll every 2 seconds

        setPollingInterval(interval);

        return () => {
            if (interval) {
                clearInterval(interval);
            }
        };
    }, [verificationId, status]);

    /**
     * Reset verification state
     */
    const reset = () => {
        setVerificationId(null);
        setStatus('IDLE');
        setResult(null);
        setError(null);
        if (pollingInterval) {
            clearInterval(pollingInterval);
            setPollingInterval(null);
        }
    };

    return {
        startVerification,
        reset,
        verificationId,
        status,
        result,
        error,
        isVerifying: status === 'QUEUED' || status === 'PROCESSING' || status === 'SUBMITTING',
        isSuccess: status === 'SUCCESS',
        isFailed: status === 'FAILED',
        isError: status === 'ERROR'
    };
};

export default useAsyncVerification;
