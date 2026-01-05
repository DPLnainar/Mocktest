import React from 'react';

export default function ViolationTimeline({ violations }) {
    if (!violations || violations.length === 0) {
        return (
            <div className="bg-gray-800 rounded-lg p-6 text-center">
                <p className="text-gray-400">No violations recorded</p>
            </div>
        );
    }

    const getViolationIcon = (eventType) => {
        const icons = {
            'TAB_SWITCH': '🔄',
            'FULLSCREEN_EXIT': '⛔',
            'COPY_PASTE': '📋',
            'MULTIPLE_FACES_DETECTED': '👥',
            'NO_FACE_DETECTED': '❌',
            'UNAUTHORIZED_OBJECT_DETECTED': '📱',
            'SUSPICIOUS_EXTENSION_DETECTED': '🔌',
            'LARGE_PASTE': '📝',
            'SUSPICIOUS_TYPING_SPEED': '⌨️',
            'RAPID_ANSWER_CHANGES': '⚡',
            'IP_ADDRESS_CHANGED': '🌐',
            'IP_TRACKED': '📍'
        };
        return icons[eventType] || '⚠️';
    };

    const getViolationColor = (eventType) => {
        const highSeverity = ['MULTIPLE_FACES_DETECTED', 'UNAUTHORIZED_OBJECT_DETECTED', 'IP_ADDRESS_CHANGED'];
        const mediumSeverity = ['TAB_SWITCH', 'FULLSCREEN_EXIT', 'LARGE_PASTE', 'SUSPICIOUS_EXTENSION_DETECTED'];

        if (highSeverity.includes(eventType)) return 'border-red-500 bg-red-900 bg-opacity-20';
        if (mediumSeverity.includes(eventType)) return 'border-yellow-500 bg-yellow-900 bg-opacity-20';
        return 'border-blue-500 bg-blue-900 bg-opacity-20';
    };

    const formatEventType = (eventType) => {
        return eventType.replace(/_/g, ' ').toLowerCase()
            .split(' ')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1))
            .join(' ');
    };

    const formatTime = (timestamp) => {
        const date = new Date(timestamp);
        return date.toLocaleTimeString('en-US', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    };

    const formatMetadata = (metadata) => {
        if (!metadata) return null;

        const entries = Object.entries(metadata);
        if (entries.length === 0) return null;

        return (
            <div className="mt-2 text-xs text-gray-400">
                {entries.map(([key, value]) => (
                    <div key={key}>
                        <span className="font-semibold">{key}:</span> {JSON.stringify(value)}
                    </div>
                ))}
            </div>
        );
    };

    // Sort violations by timestamp
    const sortedViolations = [...violations].sort((a, b) =>
        new Date(a.timestamp) - new Date(b.timestamp)
    );

    return (
        <div className="bg-gray-800 rounded-lg p-6">
            <h3 className="text-xl font-bold text-white mb-4">Violation Timeline</h3>

            <div className="relative">
                {/* Timeline line */}
                <div className="absolute left-6 top-0 bottom-0 w-0.5 bg-gray-700"></div>

                {/* Violations */}
                <div className="space-y-4">
                    {sortedViolations.map((violation, index) => (
                        <div key={violation.id || index} className="relative pl-16">
                            {/* Timeline dot */}
                            <div className="absolute left-4 top-2 w-4 h-4 rounded-full bg-gray-700 border-2 border-gray-600"></div>

                            {/* Violation card */}
                            <div className={`border-l-4 rounded-lg p-4 ${getViolationColor(violation.eventType)}`}>
                                <div className="flex items-start justify-between">
                                    <div className="flex items-start gap-3">
                                        <span className="text-2xl">{getViolationIcon(violation.eventType)}</span>
                                        <div>
                                            <h4 className="font-semibold text-white">
                                                {formatEventType(violation.eventType)}
                                            </h4>
                                            <p className="text-sm text-gray-400 mt-1">
                                                {formatTime(violation.timestamp)}
                                            </p>
                                            {formatMetadata(violation.metadata)}
                                        </div>
                                    </div>
                                    <span className="text-xs text-gray-500">
                                        #{index + 1}
                                    </span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Summary */}
            <div className="mt-6 pt-4 border-t border-gray-700">
                <div className="grid grid-cols-3 gap-4 text-center">
                    <div>
                        <p className="text-2xl font-bold text-white">{violations.length}</p>
                        <p className="text-sm text-gray-400">Total Violations</p>
                    </div>
                    <div>
                        <p className="text-2xl font-bold text-red-400">
                            {violations.filter(v =>
                                ['MULTIPLE_FACES_DETECTED', 'UNAUTHORIZED_OBJECT_DETECTED', 'IP_ADDRESS_CHANGED'].includes(v.eventType)
                            ).length}
                        </p>
                        <p className="text-sm text-gray-400">High Severity</p>
                    </div>
                    <div>
                        <p className="text-2xl font-bold text-yellow-400">
                            {violations.filter(v =>
                                ['TAB_SWITCH', 'FULLSCREEN_EXIT', 'LARGE_PASTE'].includes(v.eventType)
                            ).length}
                        </p>
                        <p className="text-sm text-gray-400">Medium Severity</p>
                    </div>
                </div>
            </div>
        </div>
    );
}
