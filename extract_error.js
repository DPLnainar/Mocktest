
const fs = require('fs');
const fileName = "crash_analysis_full.txt";
try {
    const data = fs.readFileSync(fileName, 'utf8');
    const lines = data.split('\n');
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        if ((line.includes("Exception") || line.includes("Error")) && !line.includes("DEBUG") && !line.includes("INFO")) {
            console.log(`LINE ${i}: ${line.trim()}`);
            for (let j = 1; j < 15; j++) {
                if (i + j < lines.length) {
                    console.log(`  ${lines[i + j].trim()}`);
                }
            }
            console.log("-".repeat(20));
        }
    }
} catch (err) {
    console.error(err);
}
