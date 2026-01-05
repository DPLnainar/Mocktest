import React from 'react';

export default function WebcamPreview({ videoRef, faceCount, detectedObjects, modelsLoaded }) {
    return (
        <div className="fixed bottom-4 left-4 z-50">
            <div className="bg-gray-800 rounded-lg shadow-2xl overflow-hidden border-2 border-gray-700">
                {/* Video Preview */}
                <div className="relative">
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

                    {/* Detected Objects */}
                    {detectedObjects.length > 0 && (
                        <div className="absolute bottom-2 left-2 right-2">
                            <div className="bg-black bg-opacity-70 rounded px-2 py-1">
                                {detectedObjects.slice(0, 3).map((obj, idx) => (
                                    <div key={idx} className="text-xs text-white">
                                        {obj.class}: {(obj.score * 100).toFixed(0)}%
                                    </div>
                                ))}
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
