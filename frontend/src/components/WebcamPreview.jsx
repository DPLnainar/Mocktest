import React from 'react';

export default function WebcamPreview({ videoRef, faceCount, detectedObjects, modelsLoaded, cameraStatus, headRotation }) {
    return (
        <div className="fixed bottom-4 left-4 z-50">
            <div className="bg-gray-800 rounded-lg shadow-2xl overflow-hidden border-2 border-gray-700">
                {/* Video Preview */}
                <div className="relative">
                    {/* Camera Status Overlays */}
                    {cameraStatus === 'error' && (
                        <div className="absolute inset-0 bg-gray-900 flex flex-col items-center justify-center text-center p-2 z-10">
                            <span className="text-2xl mb-1">🚫</span>
                            <p className="text-red-400 font-bold text-xs">Camera Error</p>
                            <p className="text-gray-400 text-[10px] leading-tight mt-1">
                                Check permissions or close other apps using camera.
                            </p>
                        </div>
                    )}

                    {cameraStatus === 'loading' && (
                        <div className="absolute inset-0 bg-gray-900 flex flex-col items-center justify-center text-center z-10">
                            <span className="text-2xl mb-1 animate-pulse">📷</span>
                            <p className="text-blue-400 font-bold text-xs">Starting Camera...</p>
                        </div>
                    )}

                    <video
                        ref={videoRef}
                        autoPlay
                        muted
                        playsInline
                        className="w-64 h-48 object-cover"
                    />

                    {/* Status Overlay */}
                    <div className="absolute top-2 left-2 right-2 flex justify-between items-start">
                        {/* AI Status */}
                        <div className={`px-2 py-1 rounded text-xs font-semibold ${modelsLoaded ? 'bg-green-600' : 'bg-yellow-600'
                            } text-white`}>
                            {modelsLoaded ? '🤖 AI Active' : '⏳ Loading...'}
                        </div>

                        {/* Face Count */}
                        <div className={`px-2 py-1 rounded text-xs font-semibold ${faceCount === 1 ? 'bg-green-600' :
                            faceCount === 0 ? 'bg-red-600' : 'bg-orange-600'
                            } text-white`}>
                            {faceCount === 0 ? '⚠️ No Face' :
                                faceCount === 1 ? '✓ 1 Face' :
                                    `⚠️ ${faceCount} Faces`}
                        </div>
                    </div>

                    {/* Detected Objects with Bounding Boxes (Violations Only) */}
                    {detectedObjects.map((obj, idx) => {
                        // Check if object is a violation
                        const isViolation = ['cell phone', 'mobile', 'remote'].some(c => obj.class.toLowerCase().includes(c));
                        if (!isViolation) return null;

                        // Calculate scaling factors
                        // Source is 640x480 (from useAIProctoring)
                        const sourceWidth = 640;
                        const sourceHeight = 480;

                        // Display size
                        const displayWidth = videoRef.current ? videoRef.current.clientWidth : 256;
                        const displayHeight = videoRef.current ? videoRef.current.clientHeight : 192;

                        const scaleX = displayWidth / sourceWidth;
                        const scaleY = displayHeight / sourceHeight;

                        return (
                            <div
                                key={idx}
                                className="absolute border-2 border-red-500 bg-red-500 bg-opacity-20 flex flex-col justify-end"
                                style={{
                                    left: `${obj.bbox[0] * scaleX}px`,
                                    top: `${obj.bbox[1] * scaleY}px`,
                                    width: `${obj.bbox[2] * scaleX}px`,
                                    height: `${obj.bbox[3] * scaleY}px`,
                                }}
                            >
                                <span className="bg-red-600 text-white text-[10px] px-1 w-max font-bold">
                                    {obj.class} {(obj.score * 100).toFixed(0)}%
                                </span>
                            </div>
                        )
                    })}
                    {/* HEAD ROTATION WARNING */}
                    {headRotation && (
                        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-20">
                            <div className="bg-red-600 text-white px-4 py-2 rounded-lg shadow-lg animate-bounce font-bold text-center border-2 border-white">
                                <span className="text-2xl block">👀</span>
                                LOOK AT SCREEN
                                <div className="text-xs opacity-75 mt-1">Head Turned</div>
                            </div>
                        </div>
                    )}
                </div>

                {/* Info Bar */}
                <div className="bg-gray-900 px-3 py-2 text-xs text-gray-400">
                    <div className="flex items-center justify-between">
                        <span>📹 Proctoring Active</span>
                        <span className="text-green-400">●</span>
                    </div>
                </div>
            </div>
        </div>
    );
}
