#!/bin/bash

# ================================================================
# Quick Test Script for Kosmos App
# Date: 2025-11-01
# Purpose: Automated verification of fixes
# ================================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Kosmos App Quick Test${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# ================================================================
# Phase 1: Pre-flight Checks
# ================================================================

echo -e "${YELLOW}Phase 1: Pre-flight Checks${NC}"
echo "---"

# Check ADB
echo -n "Checking ADB connection... "
if adb devices | grep -q "device$"; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ No device connected${NC}"
    exit 1
fi

# Check app installed
echo -n "Checking app installation... "
if adb shell pm list packages | grep -q "com.example.kosmos"; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ App not installed${NC}"
    echo "Run: adb install -r app/build/outputs/apk/debug/app-debug.apk"
    exit 1
fi

# Check APK exists
echo -n "Checking APK file... "
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ APK not found${NC}"
    echo "Run: ./gradlew assembleDebug"
    exit 1
fi

echo ""

# ================================================================
# Phase 2: Installation
# ================================================================

echo -e "${YELLOW}Phase 2: Fresh Installation${NC}"
echo "---"

read -p "Install latest APK? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Installing APK..."
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    echo -e "${GREEN}✓ Installation complete${NC}"
fi

read -p "Clear app data for fresh start? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Clearing app data..."
    adb shell pm clear com.example.kosmos
    echo -e "${GREEN}✓ Data cleared${NC}"
fi

echo ""

# ================================================================
# Phase 3: Start Monitoring
# ================================================================

echo -e "${YELLOW}Phase 3: Start Log Monitoring${NC}"
echo "---"

echo "Starting logcat monitor..."
echo "Monitor will show errors in real-time"
echo ""
echo -e "${BLUE}Look for these SUCCESS indicators:${NC}"
echo "  ✓ Task inserted successfully"
echo "  ✓ Task updated successfully"
echo "  ✓ No 'Serializer for class Any' errors"
echo "  ✓ No 'PGRST204' errors"
echo ""
echo -e "${RED}Look for these ERROR indicators:${NC}"
echo "  ✗ Error inserting task"
echo "  ✗ Error updating task"
echo "  ✗ kotlinx.serialization.SerializationException"
echo "  ✗ PGRST204"
echo ""

read -p "Start logcat monitoring? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${BLUE}Press Ctrl+C to stop monitoring${NC}"
    echo "================================"
    echo ""

    # Clear old logs
    adb logcat -c

    # Monitor specific tags
    adb logcat -s \
        SupabaseTaskDataSource:* \
        TaskRepository:* \
        SupabaseUserDataSource:* \
        UserRepository:* \
        SupabaseChatDataSource:* \
        ChatRepository:* \
        SupabaseProjectDataSource:* \
        ProjectRepository:* \
        SupabaseConfig:* \
        | grep --line-buffered -E "(successfully|Error|FAILED|SUCCESS|Serializ|PGRST)" \
        | while read line; do
            if echo "$line" | grep -q -E "(successfully|SUCCESS)"; then
                echo -e "${GREEN}$line${NC}"
            elif echo "$line" | grep -q -E "(Error|FAILED|Serializ|PGRST)"; then
                echo -e "${RED}$line${NC}"
            else
                echo "$line"
            fi
        done
fi

echo ""
echo -e "${BLUE}Test complete!${NC}"
