#!/bin/bash

# Clean script for Unify AI project
# Removes all build artifacts to reclaim disk space

echo "🧹 Cleaning Unify AI build artifacts..."

# Get initial size
INITIAL_SIZE=$(du -sh . | cut -f1)
echo "Current project size: $INITIAL_SIZE"

# Remove build directories
echo "Removing build directories..."
rm -rf app/build
rm -rf build
rm -rf .gradle

# Find and remove all nested build directories
find . -type d -name "build" -exec rm -rf {} + 2>/dev/null || true
find . -type d -name ".gradle" -exec rm -rf {} + 2>/dev/null || true

# Remove gradle cache in home directory (optional - uncomment if needed)
# rm -rf ~/.gradle/caches/

# Remove other temporary files
echo "Removing temporary files..."
find . -name "*.apk" -type f -delete 2>/dev/null || true
find . -name "*.aar" -type f -delete 2>/dev/null || true
find . -name "*.dex" -type f -delete 2>/dev/null || true
find . -name "*.class" -type f -delete 2>/dev/null || true
find . -name "*.log" -type f -delete 2>/dev/null || true
find . -name "*.hprof" -type f -delete 2>/dev/null || true

# Get final size
FINAL_SIZE=$(du -sh . | cut -f1)
echo "✅ Cleanup complete!"
echo "Final project size: $FINAL_SIZE"
echo ""
echo "Note: The remaining ~2.6GB is from the Phi-3.5 AI model in app/src/main/assets/models/"
echo "This is required for the app to function offline."