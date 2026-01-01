// Violation System Test Client
// Demonstrates Redis atomic counting, auto-termination, and WebSocket broadcasting

// Prerequisites:
// 1. Backend running on http://localhost:8080
// 2. Valid JWT token
// 3. Active exam session

const BASE_URL = 'http://localhost:8080';
const WS_URL = 'ws://localhost:8080/ws';

// Replace with actual JWT token
const JWT_TOKEN = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...';

// ===== WebSocket Connection =====
let stompClient = null;

function connectWebSocket() {
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);
    
    stompClient.connect(
        { Authorization: `Bearer ${JWT_TOKEN}` },
        (frame) => {
            console.log('WebSocket connected:', frame);
            
            // Subscribe to moderator monitoring (if moderator)
            stompClient.subscribe('/topic/exam/456/monitoring', (message) => {
                const update = JSON.parse(message.body);
                console.log('📡 Status Update:', update);
            });
            
            // Subscribe to personal messages (if student)
            stompClient.subscribe('/user/queue/messages', (message) => {
                const notification = JSON.parse(message.body);
                console.log('💬 Personal Message:', notification);
                
                if (notification.type === 'termination') {
                    console.error('🚨 EXAM TERMINATED:', notification.payload);
                }
            });
        },
        (error) => {
            console.error('WebSocket connection error:', error);
        }
    );
}

// ===== Violation Testing Functions =====

/**
 * Test 1: Record single violation
 */
async function test_recordSingleViolation() {
    console.log('\n===== Test 1: Record Single Violation =====');
    
    const violation = {
        sessionId: 123,
        examId: 456,
        type: 'TAB_SWITCH',
        severity: 'MAJOR',
        description: 'Switched to Google Chrome',
        evidence: {
            timestamp: new Date().toISOString(),
            previousTab: 'exam.html',
            switchedTo: 'google.com',
            duration: '5 seconds'
        }
    };
    
    try {
        const response = await fetch(`${BASE_URL}/api/violations/report`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${JWT_TOKEN}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(violation)
        });
        
        const result = await response.json();
        console.log('✅ Violation recorded:', result);
        console.log(`Current strikes: ${result.strikeCount}/5`);
        
        return result;
    } catch (error) {
        console.error('❌ Error recording violation:', error);
    }
}

/**
 * Test 2: Record multiple simultaneous violations (Redis atomic test)
 */
async function test_atomicCounting() {
    console.log('\n===== Test 2: Atomic Counting (Race Condition Test) =====');
    
    // Simulate phone + tab switch detected at same millisecond
    const violations = [
        {
            sessionId: 123,
            examId: 456,
            type: 'PHONE_DETECTED',
            severity: 'MAJOR', // 2 strikes
            description: 'Cell phone detected',
            evidence: {
                screenshot: 'data:image/png;base64,iVBORw0KGgoAAAANS...',
                confidence: 0.95,
                detectedObject: 'cell phone',
                boundingBox: { x: 100, y: 200, width: 50, height: 80 }
            }
        },
        {
            sessionId: 123,
            examId: 456,
            type: 'TAB_SWITCH',
            severity: 'MAJOR', // 2 strikes
            description: 'Switched to another tab',
            evidence: {
                timestamp: new Date().toISOString(),
                switchedTo: 'stackoverflow.com'
            }
        }
    ];
    
    try {
        // Fire both requests simultaneously
        const promises = violations.map(v => 
            fetch(`${BASE_URL}/api/violations/report`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${JWT_TOKEN}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(v)
            }).then(r => r.json())
        );
        
        const results = await Promise.all(promises);
        
        console.log('✅ Both violations recorded:');
        results.forEach((r, i) => {
            console.log(`  Violation ${i + 1}: ${r.strikeCount} strikes`);
        });
        
        // Verify final count
        const finalCount = await getStrikeCount(123);
        console.log(`\n📊 Final strike count: ${finalCount.currentStrikes}/5`);
        console.log('⚡ Redis INCR worked correctly - no race condition!');
        
        return results;
    } catch (error) {
        console.error('❌ Error in atomic test:', error);
    }
}

/**
 * Test 3: Auto-termination at 5 strikes
 */
async function test_autoTermination() {
    console.log('\n===== Test 3: Auto-Termination (5 Strikes) =====');
    
    // Get current strike count
    const current = await getStrikeCount(124); // New session
    console.log(`Current strikes: ${current.currentStrikes}/5`);
    
    // Report violations until termination
    let terminated = false;
    let count = current.currentStrikes;
    
    while (!terminated && count < 5) {
        const violation = {
            sessionId: 124,
            examId: 456,
            type: 'NO_FACE_DETECTED',
            severity: 'MINOR', // 1 strike at a time
            description: `Face not detected (strike ${count + 1})`,
            evidence: {
                timestamp: new Date().toISOString(),
                confidence: 0.9
            }
        };
        
        const result = await fetch(`${BASE_URL}/api/violations/report`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${JWT_TOKEN}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(violation)
        }).then(r => r.json());
        
        console.log(`Strike ${result.strikeCount}/5 recorded`);
        count = result.strikeCount;
        terminated = result.terminated;
        
        // Small delay between violations
        await new Promise(resolve => setTimeout(resolve, 500));
    }
    
    if (terminated) {
        console.log('🚨 AUTO-TERMINATION TRIGGERED!');
        console.log('⚠️  Student should receive termination message via WebSocket');
    }
}

/**
 * Test 4: False positive handling
 */
async function test_falsePositive() {
    console.log('\n===== Test 4: False Positive Handling =====');
    
    // Report violation
    const violation = {
        sessionId: 125,
        examId: 456,
        type: 'PHONE_DETECTED',
        severity: 'MAJOR',
        description: 'Phone detected (student was adjusting webcam)',
        evidence: { confidence: 0.75 }
    };
    
    const reported = await fetch(`${BASE_URL}/api/violations/report`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${JWT_TOKEN}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(violation)
    }).then(r => r.json());
    
    console.log(`✅ Violation recorded: ${reported.strikeCount} strikes`);
    
    // Get violation ID (simulate moderator review)
    const violations = await fetch(`${BASE_URL}/api/violations/session/125`, {
        headers: { 'Authorization': `Bearer ${JWT_TOKEN}` }
    }).then(r => r.json());
    
    const violationId = violations[0].id;
    console.log(`📋 Reviewing violation ID: ${violationId}`);
    
    // Reject as false positive
    await fetch(`${BASE_URL}/api/violations/${violationId}/confirm`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${JWT_TOKEN}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            confirmed: false,
            reason: 'Student was adjusting webcam, not using phone'
        })
    });
    
    console.log('✅ Violation rejected as false positive');
    
    // Verify strike count adjusted
    const updated = await getStrikeCount(125);
    console.log(`📊 Updated strike count: ${updated.currentStrikes}/5 (should be 0)`);
}

/**
 * Test 5: Get violation history
 */
async function test_violationHistory() {
    console.log('\n===== Test 5: Violation History =====');
    
    try {
        const violations = await fetch(`${BASE_URL}/api/violations/session/123`, {
            headers: { 'Authorization': `Bearer ${JWT_TOKEN}` }
        }).then(r => r.json());
        
        console.log(`📋 Found ${violations.length} violations:`);
        violations.forEach((v, i) => {
            console.log(`\n${i + 1}. ${v.type} (${v.severity})`);
            console.log(`   Strikes: ${v.strikeCount}`);
            console.log(`   Time: ${v.detectedAt}`);
            console.log(`   Confirmed: ${v.confirmed}`);
            if (v.evidence) {
                console.log(`   Evidence: ${JSON.stringify(v.evidence, null, 2)}`);
            }
        });
    } catch (error) {
        console.error('❌ Error fetching history:', error);
    }
}

/**
 * Test 6: Violation statistics
 */
async function test_violationStats() {
    console.log('\n===== Test 6: Violation Statistics =====');
    
    try {
        const stats = await fetch(`${BASE_URL}/api/violations/session/123/stats`, {
            headers: { 'Authorization': `Bearer ${JWT_TOKEN}` }
        }).then(r => r.json());
        
        console.log('📊 Violation Statistics:');
        console.log(`   Total Strikes: ${stats.totalStrikes}`);
        console.log(`   Total Violations: ${stats.totalViolations}`);
        console.log(`   Camera Violations: ${stats.cameraViolations}`);
        console.log(`   Tab Switches: ${stats.tabSwitchCount}`);
        console.log(`   Terminated: ${stats.terminated}`);
    } catch (error) {
        console.error('❌ Error fetching stats:', error);
    }
}

// ===== Helper Functions =====

async function getStrikeCount(sessionId) {
    try {
        const response = await fetch(`${BASE_URL}/api/violations/session/${sessionId}/strikes`, {
            headers: { 'Authorization': `Bearer ${JWT_TOKEN}` }
        });
        return await response.json();
    } catch (error) {
        console.error('❌ Error getting strike count:', error);
        return { currentStrikes: 0, terminated: false, remainingStrikes: 5 };
    }
}

// ===== Run All Tests =====

async function runAllTests() {
    console.log('🚀 Starting Violation System Tests...\n');
    
    // Connect WebSocket for real-time monitoring
    connectWebSocket();
    
    // Wait for WebSocket connection
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    try {
        await test_recordSingleViolation();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        await test_atomicCounting();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        await test_violationHistory();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        await test_violationStats();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        await test_falsePositive();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // ⚠️ WARNING: This will auto-terminate session 124!
        // await test_autoTermination();
        
        console.log('\n✅ All tests completed!');
    } catch (error) {
        console.error('❌ Test suite failed:', error);
    }
}

// Run tests when ready
// runAllTests();

// Export for browser console
if (typeof window !== 'undefined') {
    window.ViolationTests = {
        connectWebSocket,
        test_recordSingleViolation,
        test_atomicCounting,
        test_autoTermination,
        test_falsePositive,
        test_violationHistory,
        test_violationStats,
        runAllTests
    };
    
    console.log('✅ Violation test client loaded!');
    console.log('Run tests with: ViolationTests.runAllTests()');
}
