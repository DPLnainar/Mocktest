import { useRef } from 'react'
import Editor from '@monaco-editor/react'
import { useExamStore } from '../store/examStore'
import { executionAPI, parserAPI } from '../services/api'
import toast from 'react-hot-toast'
import { Play, Check, X } from 'lucide-react'

export default function CodeEditor({ sessionId, examId }) {
  const { code, language, setCode } = useExamStore()
  const editorRef = useRef(null)

  const handleEditorMount = (editor) => {
    editorRef.current = editor
  }

  const handleRunCode = async () => {
    if (!code.trim()) {
      toast.error('Please write some code first')
      return
    }

    try {
      toast.loading('Verifying code...', { id: 'verify' })

      // Step 1: Verify logic (ANTLR parser)
      const verifyResponse = await parserAPI.verify(code, language, [
        { construct: 'FORBIDDEN_LIBRARY', value: 'Arrays.sort' },
        { construct: 'FORBIDDEN_LIBRARY', value: 'Collections.sort' },
      ])

      const { violations } = verifyResponse.data

      if (violations.length > 0) {
        toast.error('Forbidden constructs detected!', { id: 'verify' })
        violations.forEach((v) => {
          toast.error(`Line ${v.line}: ${v.message}`, { duration: 5000 })
        })
        return
      }

      toast.success('Logic verified ✓', { id: 'verify' })

      // Step 2: Execute code (Judge0)
      toast.loading('Executing code...', { id: 'execute' })

      const executeResponse = await executionAPI.execute({
        sessionId,
        code,
        language,
        testCases: [
          { input: '5\n3\n1\n4\n2', expectedOutput: '1\n2\n3\n4\n5' },
        ],
      })

      const { submissionId } = executeResponse.data

      // Poll for result
      await pollResult(submissionId)
    } catch (error) {
      console.error('Execution failed:', error)
      toast.error('Execution failed', { id: 'execute' })
    }
  }

  const pollResult = async (submissionId, maxAttempts = 10) => {
    for (let i = 0; i < maxAttempts; i++) {
      await new Promise((resolve) => setTimeout(resolve, 1000))

      try {
        const response = await executionAPI.getResult(submissionId)
        const { status, output, error } = response.data

        if (status === 'COMPLETED') {
          toast.success('Execution completed!', { id: 'execute' })
          console.log('Output:', output)
          return
        } else if (status === 'FAILED') {
          toast.error(`Error: ${error}`, { id: 'execute', duration: 5000 })
          return
        }
      } catch (error) {
        console.error('Polling failed:', error)
      }
    }

    toast.error('Execution timeout', { id: 'execute' })
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
          className="flex items-center space-x-2 px-4 py-2 bg-green-600 hover:bg-green-700 rounded-lg transition"
        >
          <Play size={16} />
          <span>Run Code</span>
        </button>
      </div>

      {/* Editor */}
      <div className="flex-1 monaco-container">
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
    </div>
  )
}
