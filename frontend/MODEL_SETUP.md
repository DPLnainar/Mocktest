# Face-API.js Model Setup

The AI proctoring feature requires face detection models to be placed in the `public/models` directory.

## Option 1: Download from CDN (Recommended)

Run this PowerShell script to download the required model:

```powershell
# Download tiny_face_detector model
$modelUrl = "https://raw.githubusercontent.com/justadudewhohacks/face-api.js/master/weights/tiny_face_detector_model-weights_manifest.json"
$modelBin = "https://raw.githubusercontent.com/justadudewhohacks/face-api.js/master/weights/tiny_face_detector_model-shard1"

Invoke-WebRequest -Uri $modelUrl -OutFile "public/models/tiny_face_detector_model-weights_manifest.json"
Invoke-WebRequest -Uri $modelBin -OutFile "public/models/tiny_face_detector_model-shard1"
```

## Option 2: Copy from node_modules

If you have internet issues, copy from the installed package:

```powershell
Copy-Item "node_modules/face-api.js/weights/tiny_face_detector_*" -Destination "public/models/"
```

## Option 3: Use CDN (No download needed)

Update `useAIProctoring.js` line 23 to use CDN:

```javascript
const MODEL_URL = 'https://justadudewhohacks.github.io/face-api.js/models';
```

## Verify Installation

After setup, you should have these files:
- `public/models/tiny_face_detector_model-weights_manifest.json`
- `public/models/tiny_face_detector_model-shard1`

## Testing

1. Start a test as a student
2. Allow webcam access when prompted
3. You should see a webcam preview in the bottom-right corner
4. The preview will show face count and detected objects
