import { useEffect, useState } from 'react';

export default function ProctoringNotice({ violationCount }) {
    const [borderColor, setBorderColor] = useState('border-green-500');

    useEffect(() => {
        if (violationCount === 0) {
            setBorderColor('border-green-500');
        } else if (violationCount <= 2) {
            setBorderColor('border-yellow-500');
        } else {
            setBorderColor('border-red-500');
        }
    }, [violationCount]);

    return (
        <div className={`fixed bottom-4 right-4 bg-gray-800 rounded-lg p-4 border-2 ${borderColor} shadow-lg z-50`}>
            <div className="flex items-center gap-3">
                <div className="w-3 h-3 rounded-full bg-green-500 animate-pulse"></div>
                <div className="text-white">
                    <p className="text-sm font-semibold">Proctoring Active</p>
                    <p className="text-xs text-gray-400">
                        {violationCount === 0 ? 'No violations' : `${violationCount} violation(s) detected`}
                    </p>
                </div>
            </div>
            {violationCount >= 3 && (
                <p className="mt-2 text-xs text-red-400">
                    Warning: High violation count!
                </p>
            )}
        </div>
    );
}
