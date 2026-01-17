import { useEffect, useState } from 'react';
import { useAuthStore } from '../store/authStore';

/**
 * AntiPhotoWatermark Component
 * 
 * Displays a dynamic, semi-transparent watermark overlay that:
 * - Shows student name, email, and timestamp
 * - Rotates position every few seconds
 * - Makes any photographed content traceable
 */
export default function AntiPhotoWatermark({ attemptId }) {
    const { user } = useAuthStore();
    const [position, setPosition] = useState({ top: '10%', left: '10%' });
    const [rotation, setRotation] = useState(0);

    useEffect(() => {
        // Change watermark position every 5 seconds
        const interval = setInterval(() => {
            const positions = [
                { top: '10%', left: '10%' },
                { top: '10%', right: '10%' },
                { top: '50%', left: '50%' },
                { bottom: '10%', left: '10%' },
                { bottom: '10%', right: '10%' },
                { top: '30%', right: '30%' },
                { bottom: '30%', left: '30%' },
            ];

            const randomPos = positions[Math.floor(Math.random() * positions.length)];
            const randomRotation = Math.floor(Math.random() * 360);

            setPosition(randomPos);
            setRotation(randomRotation);
        }, 5000);

        return () => clearInterval(interval);
    }, []);

    const currentTime = new Date().toLocaleString();

    return (
        <>
            {/* Main Watermark */}
            <div
                className="fixed pointer-events-none z-50 transition-all duration-1000 ease-in-out"
                style={{
                    ...position,
                    transform: `rotate(${rotation}deg)`,
                }}
            >
                <div className="bg-red-600 bg-opacity-20 backdrop-blur-sm border border-red-500 border-opacity-30 rounded-lg px-4 py-2 text-red-300 text-sm font-mono">
                    <div className="font-bold">{user?.name || 'Student'}</div>
                    <div className="text-xs">{user?.email}</div>
                    <div className="text-xs">ID: {attemptId}</div>
                    <div className="text-xs">{currentTime}</div>
                </div>
            </div>

            {/* Diagonal Watermark Grid (Subtle) */}
            <div className="fixed inset-0 pointer-events-none z-40 overflow-hidden">
                <div className="absolute inset-0" style={{
                    backgroundImage: `repeating-linear-gradient(
                        45deg,
                        transparent,
                        transparent 200px,
                        rgba(255, 0, 0, 0.03) 200px,
                        rgba(255, 0, 0, 0.03) 202px
                    )`,
                }}>
                    <div className="absolute inset-0 flex items-center justify-center">
                        <div className="text-red-900 text-opacity-10 text-6xl font-bold transform -rotate-45 select-none">
                            {user?.email} • {attemptId}
                        </div>
                    </div>
                </div>
            </div>

            {/* Corner Timestamps */}
            <div className="fixed top-2 left-2 text-red-400 text-xs font-mono opacity-30 pointer-events-none z-50">
                {currentTime}
            </div>
            <div className="fixed top-2 right-2 text-red-400 text-xs font-mono opacity-30 pointer-events-none z-50">
                {user?.email}
            </div>
            <div className="fixed bottom-2 left-2 text-red-400 text-xs font-mono opacity-30 pointer-events-none z-50">
                Attempt: {attemptId}
            </div>
            <div className="fixed bottom-2 right-2 text-red-400 text-xs font-mono opacity-30 pointer-events-none z-50">
                {currentTime}
            </div>
        </>
    );
}
