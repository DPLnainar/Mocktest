#!/bin/bash

# Quick deployment script for free-tier cloud services
# Prerequisites: Git, GitHub account, Vercel CLI, Render account

set -e

echo "🚀 Free-Tier Cloud Deployment"
echo "============================"
echo ""

# Check if git is initialized
if [ ! -d .git ]; then
    echo "📦 Initializing Git repository..."
    git init
    git add .
    git commit -m "Initial commit for cloud deployment"
fi

# Check for GitHub remote
if ! git remote | grep -q origin; then
    echo "⚠️  No GitHub remote found"
    echo "Please create a GitHub repository and add it:"
    echo "  git remote add origin https://github.com/yourusername/exam-portal.git"
    echo "  git push -u origin main"
    exit 1
fi

echo "✅ Git repository ready"
echo ""

# Push to GitHub
echo "📤 Pushing to GitHub..."
git push origin main

echo "✅ Code pushed to GitHub"
echo ""

# Deploy frontend to Vercel
echo "🌐 Deploying frontend to Vercel..."
echo ""
echo "Next steps:"
echo "  1. Go to https://vercel.com"
echo "  2. Click 'Import Project'"
echo "  3. Select your GitHub repository"
echo "  4. Set build settings:"
echo "     - Framework: Vite"
echo "     - Build Command: npm run build"
echo "     - Output Directory: dist"
echo "  5. Add environment variables:"
echo "     - VITE_API_URL (will be set after backend deployment)"
echo "     - VITE_WS_URL (will be set after backend deployment)"
echo ""
read -p "Press Enter when frontend is deployed..."

# Get Vercel URL
read -p "Enter your Vercel URL (e.g., https://exam-portal.vercel.app): " VERCEL_URL
echo "✅ Frontend URL: $VERCEL_URL"
echo ""

# Deploy backend to Render
echo "⚙️  Deploying backend to Render..."
echo ""
echo "Next steps:"
echo "  1. Go to https://render.com"
echo "  2. Click 'New +' → 'Web Service'"
echo "  3. Connect your GitHub repository"
echo "  4. Render will detect render.yaml automatically"
echo "  5. Add environment variables (see .env.cloud):"
echo "     - SPRING_DATASOURCE_URL (from Neon.tech)"
echo "     - SPRING_DATASOURCE_USERNAME"
echo "     - SPRING_DATASOURCE_PASSWORD"
echo "     - SPRING_REDIS_HOST (from Upstash)"
echo "     - SPRING_REDIS_PASSWORD"
echo "     - RAPIDAPI_KEY (from RapidAPI)"
echo "  6. Click 'Create Web Service'"
echo ""
read -p "Press Enter when backend is deployed..."

# Get Render URL
read -p "Enter your Render URL (e.g., https://exam-portal-backend.onrender.com): " RENDER_URL
echo "✅ Backend URL: $RENDER_URL"
echo ""

# Update Vercel environment variables
echo "🔧 Final step: Update Vercel environment variables"
echo ""
echo "Go to Vercel Dashboard → Settings → Environment Variables"
echo "Update these values:"
echo "  - VITE_API_URL=$RENDER_URL"
echo "  - VITE_WS_URL=${RENDER_URL/https/wss}/ws"
echo ""
echo "Then redeploy: Vercel Dashboard → Deployments → Redeploy"
echo ""

echo "✅ Deployment Complete!"
echo ""
echo "📊 Your Application:"
echo "  - Frontend: $VERCEL_URL"
echo "  - Backend: $RENDER_URL"
echo "  - Health Check: $RENDER_URL/actuator/health"
echo ""
echo "⚠️  Important:"
echo "  1. Setup UptimeRobot to ping $RENDER_URL/actuator/health every 5 min"
echo "  2. Monitor Render logs for first startup (may take 2-3 minutes)"
echo "  3. Test login at $VERCEL_URL"
echo ""
