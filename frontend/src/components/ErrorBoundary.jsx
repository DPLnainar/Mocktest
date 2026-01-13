import React from 'react';

class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null, errorInfo: null };
    }

    static getDerivedStateFromError(error) {
        // Update state so the next render will show the fallback UI.
        return { hasError: true, error };
    }

    componentDidCatch(error, errorInfo) {
        // You can also log the error to an error reporting service
        console.error("Uncaught error:", error, errorInfo);
        this.setState({ errorInfo });
    }

    handleReload = () => {
        window.location.reload();
    };

    render() {
        if (this.state.hasError) {
            // Custom Fallback UI
            return (
                <div className="min-h-screen bg-gray-900 flex items-center justify-center p-4">
                    <div className="max-w-md w-full bg-gray-800 rounded-lg shadow-xl p-8 border border-red-500/30">
                        <div className="flex items-center justify-center w-16 h-16 bg-red-500/10 rounded-full mx-auto mb-6">
                            <svg
                                className="w-8 h-8 text-red-500"
                                fill="none"
                                stroke="currentColor"
                                viewBox="0 0 24 24"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                                />
                            </svg>
                        </div>

                        <h2 className="text-2xl font-bold text-white text-center mb-2">
                            Something went wrong
                        </h2>

                        <p className="text-gray-400 text-center mb-6">
                            The application encountered an unexpected error. Don't worry, your progress is safe on the server.
                        </p>

                        <div className="bg-black/50 rounded p-4 mb-6 overflow-auto max-h-32">
                            <code className="text-red-400 text-sm font-mono block">
                                {this.state.error && this.state.error.toString()}
                            </code>
                        </div>

                        <div className="flex justify-center">
                            <button
                                onClick={this.handleReload}
                                className="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg transition-colors duration-200 flex items-center gap-2"
                            >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.058C6.38 3.78 9.5 2 13 2c5.523 0 10 4.477 10 10s-4.477 10-10 10S3 17.523 3 12m18 0c0 5.523-4.477 10-10 10S3 17.523 3 12m0 0c0-5.523 4.477-10 10-10" />
                                </svg>
                                Reload Application
                            </button>
                        </div>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
