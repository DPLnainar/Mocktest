import { useRef, useState } from 'react'
import Editor from '@monaco-editor/react'
import { useExamStore } from '../store/examStore'
import { executionAPI, parserAPI } from '../services/api'
import toast from 'react-hot-toast'
import { Play, Loader2 } from 'lucide-react'

// Updated Props: CodeEditor now needs attemptId and questionId to track execution
export default function CodeEditor({ attemptId, questionId }) {
  const { code, language, setCode } = useExamStore()
  const editorRef = useRef(null)
  const [isRunning, setIsRunning] = useState(false)
  const [executionOutput, setExecutionOutput] = useState(null)

  const handleEditorMount = (editor) => {
    editorRef.current = editor
  }

  const handleRunCode = async () => {
    if (!code.trim()) {
      toast.error('Please write some code first')
      return
    }

    setIsRunning(true)
    setExecutionOutput(null)

    try {
      // Step 1: Verify logic (ANTLR parser)
      toast.loading('Verifying logic...', { id: 'run-process' })
      const verifyResponse = await parserAPI.verify(code, language, [])

      const { violations } = verifyResponse.data
      if (violations.length > 0) {
        toast.error('Logic violations detected!', { id: 'run-process' })
        violations.forEach(v => toast.error(`Line ${v.line}: ${v.message}`))
        setIsRunning(false)
        return
      }

      // Step 2: Queue Execution
      toast.loading('Queuing execution...', { id: 'run-process' })

      // Execute request now returns executionId immediately (Async)
      const executeResponse = await executionAPI.execute(attemptId, {
        questionId: questionId,
        code: code,
        languageId: language === 'java' ? 62 : 71, // Example mapping, ideally fetched from config
        stdin: ""
      })

      // If backend returns status=QUEUED, we poll. 
      // Current SubmissionProducer returns plain executionId string or ExecutionResult object? 
      // Controller returns ResponseEntity<ExecutionResult>.
      const initialResult = executeResponse.data

      if (initialResult.status === 'QUEUED') {
        toast.loading('Running in queue...', { id: 'run-process' })
        await pollResult(initialResult.executionId)
      } else {
        // Immediate result (fallback or cached)
        handleExecutionComplete(initialResult)
      }

    } catch (error) {
      console.error('Run failed:', error)
      toast.error('Run failed: ' + (error.response?.data?.message || error.message), { id: 'run-process' })
      setIsRunning(false)
    }
  }

  const pollResult = async (executionId) => {
    const { exponentialBackoffPoll, getStatusMessage } = await import('../utils/pollingUtils')

    const pollFunction = async () => {
      const response = await executionAPI.getResult(attemptId, questionId)
      return response.data
    }

    const onUpdate = (result, attemptNumber, nextDelay) => {
      const message = getStatusMessage(result?.status, attemptNumber)

      if (attemptNumber % 2 === 0) {
        toast.loading(
          `${message} (${attemptNumber}/30, next: ${Math.round(nextDelay / 1000)}s)`,
          { id: 'run-process' }
        )
      }
    }

    const onComplete = (result) => {
      setIsRunning(false)
      toast.dismiss('run-process')

      if (result.passed) {
        toast.success('Test Cases Passed! ✓', { duration: 3000 })
      } else if (result.status?.includes('Error')) {
        toast.error(`Execution failed: ${result.error || result.status}`, { duration: 5000 })
      } else {
        toast.error('Test Cases Failed', { duration: 4000 })
      }

      setExecutionOutput(result.stdout || result.error || "Execution finished")
    }

    const onTimeout = () => {
      setIsRunning(false)
      toast.dismiss('run-process')
      toast.error('Execution timed out - please try again', { duration: 6000 })
      setExecutionOutput('Execution timed out after 30 polling attempts')
    }

    exponentialBackoffPoll(pollFunction, onUpdate, onComplete, onTimeout)
  }

  const handleExecutionComplete = (result) => {
    setIsRunning(false)
    if (result.passed) {
      toast.success('Test Cases Passed!', { id: 'run-process' })
    } else {
      toast.error('Test Cases Failed', { id: 'run-process' })
    }
    setExecutionOutput(result.stdout || result.error || "Execution finished")
  }

  return (
    <div className="h-full flex flex-col">
      {/* Toolbar */}
      <div className="bg-gray-900 px-4 py-3 flex items-center justify-between border-b border-gray-800">
        <div className="flex items-center space-x-4">
          <span className="text-sm text-gray-400">Language:</span>
          <select
            value={language}
            onChange={(e) => useExamStore.getState().setLanguage(e.target.value)}
            className="bg-gray-800 px-3 py-1 rounded text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
          >
            <option value="java">Java</option>
            <option value="python">Python</option>
            <option value="cpp">C++</option>
          </select>
        </div>

        <button
          onClick={handleRunCode}
          disabled={isRunning}
          className={`flex items-center space-x-2 px-4 py-2 rounded-lg transition ${isRunning ? 'bg-gray-700 cursor-not-allowed' : 'bg-green-600 hover:bg-green-700'
            }`}
        >
          {isRunning ? <Loader2 className="animate-spin" size={16} /> : <Play size={16} />}
          <span>{isRunning ? 'Running...' : 'Run Code'}</span>
        </button>
      </div>

      {/* Editor & Output Split */}
      <div className="flex-1 flex flex-col">
        <div className="flex-1 monaco-container relative">
          <Editor
            height="100%"
            language={language}
            value={code}
            onChange={(value) => setCode(value || '')}
            onMount={handleEditorMount}
            theme="vs-dark"
            options={{
              fontSize: 14,
              minimap: { enabled: true },
              scrollBeyondLastLine: false,
              automaticLayout: true,
              tabSize: 2,
              wordWrap: 'on',
            }}
          />
        </div>

        {/* Output Console Component */}
        {executionOutput && (
          <div className="h-48 bg-black border-t border-gray-700 p-4 overflow-auto font-mono text-sm">
            <div className="text-gray-400 mb-2">Console Output:</div>
            <pre className="text-green-400 whitespace-pre-wrap">{executionOutput}</pre>
          </div>
        )}
      </div>
    </div>
  )
}

