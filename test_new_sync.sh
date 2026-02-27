#!/bin/bash

# Test script for verifying the new sequential sync implementation
# This script helps verify that the FK constraint fixes are working

echo "=== Testing New Sequential Sync Implementation ==="
echo ""

# Step 1: Uninstall old app
echo "Step 1: Uninstall the old app"
echo "  Run: adb uninstall com.example.kosmos"
echo "  OR: Long press app icon → Uninstall"
echo ""
read -p "Press Enter after uninstalling..."

# Step 2: Install new build
echo "Step 2: Install the new build"
echo "  Run: ./gradlew installDebug"
echo "  OR: adb install app/build/outputs/apk/debug/app-debug.apk"
echo ""
read -p "Press Enter after installing..."

# Step 3: Clear Logcat
echo "Step 3: Clearing Logcat..."
adb logcat -c
echo "  ✅ Logcat cleared"
echo ""

# Step 4: Start logging
echo "Step 4: Starting Logcat monitoring..."
echo "  Watching for InitialSyncManager and FKErrorHandler logs"
echo "  Press Ctrl+C to stop"
echo ""
echo "=== LOGCAT OUTPUT STARTS HERE ==="
echo ""

# Monitor specific tags
adb logcat -s \
  InitialSyncManager:D \
  UserRepository:D \
  FKErrorHandler:E \
  ProjectRepository:D \
  ChatRepository:D \
  TaskRepository:D \
  *:E

# Instructions that appear after Ctrl+C
echo ""
echo "=== EXPECTED OUTPUT ==="
echo "If the new code is running, you should see:"
echo "  InitialSyncManager: 🔄 Starting sequential sync for user: ..."
echo "  UserRepository: Starting user sync from Supabase"
echo "  UserRepository: ✅ Synced X users to local cache"
echo "  InitialSyncManager: ✅ [1/6] Users synced"
echo "  InitialSyncManager: 📥 [2/6] Syncing projects..."
echo "  ... etc for all 6 steps"
echo ""
echo "If you see FK errors, note the missing user IDs and check Supabase."
