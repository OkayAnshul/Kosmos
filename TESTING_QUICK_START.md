# Kosmos - Testing Quick Start Guide

**Version:** 1.0.0
**Last Updated:** 2026-01-20

---

## 🚀 Quick Start (5 Minutes)

### 1. Install the APK

**Location:** `/home/anshul/1 - Work./Projects-Big-Three/File/Kosmos/app/build/outputs/apk/debug/app-debug.apk`

**Option A: Using ADB (Recommended)**
```bash
# Connect device via USB or start emulator
adb devices

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.example.kosmos/.MainActivity
```

**Option B: Manual Install**
```bash
# Copy APK to device
adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/

# Then on device: Settings → Security → Unknown Sources → Enable
# Navigate to Downloads → Tap app-debug.apk → Install
```

---

## 🧪 5-Minute Smoke Test

**Goal:** Verify core functionality works

### Step 1: Authentication (1 min)
```
1. Launch app
2. Tap "Sign Up"
3. Enter:
   - Email: test@kosmos.app
   - Username: tester
   - Password: TestPass123!
4. Tap "Sign Up"
✅ Should: Create account and redirect to home
```

### Step 2: Create Project (1 min)
```
1. Tap "+" FAB button
2. Enter:
   - Name: "Test Project"
   - Description: "My first project"
3. Tap "Create"
✅ Should: Project appears in list immediately
```

### Step 3: Create Task (1 min)
```
1. Tap on "Test Project"
2. Navigate to "Tasks" tab
3. Tap "+" button
4. Enter:
   - Title: "First Task"
   - Priority: High
5. Tap "Create"
✅ Should: Task appears in list
```

### Step 4: Send Message (1 min)
```
1. Navigate to "Chats" tab
2. Tap "+" button
3. Enter chat name: "Team Chat"
4. Tap "Create"
5. Type message: "Hello team!"
6. Tap send
✅ Should: Message appears immediately
```

### Step 5: Search Test (1 min)
```
1. Go back to Projects list
2. Tap search bar at top
3. Type: "Test"
✅ Should: Results appear as you type (300ms delay)
```

**✅ If all 5 steps pass → Core functionality works!**

---

## 🔥 15-Minute Essential Test

### Test 1: Offline Mode
```
1. Create a project online
2. Enable Airplane Mode
3. Create 2 tasks offline
4. Disable Airplane Mode
✅ Should: Tasks sync to server within 5 seconds
```

### Test 2: Real-Time Sync (Requires 2 devices)
```
Device A:
1. Create project "Sync Test"
2. Invite Device B user

Device B:
3. Accept invitation
4. Create task "Task from B"

Device A:
5. Check task list
✅ Should: See "Task from B" without refresh (< 5s)
```

### Test 3: Search Features
```
1. Create project "Search Test"
2. Create task "Find Me Please"
3. Go to Projects → Search: "Search"
   ✅ Should find "Search Test"
4. Go to Tasks → Search: "Find"
   ✅ Should find "Find Me Please"
5. Send message "Test message"
6. Use message search
   ✅ Should find "Test message"
```

### Test 4: Member Management
```
1. Create project
2. Tap Members tab
3. Tap "Invite Members"
4. Search for user
5. Select user, choose role
6. Tap "Send Invites"
✅ Should: Member added, appears in list
```

### Test 5: Settings Persistence
```
1. Navigate to Profile → Settings
2. Go to Privacy Settings
3. Toggle all options
4. Go to Notification Settings
5. Toggle all options
6. Force close app
7. Reopen app
8. Check settings
✅ Should: All toggles maintain their state
```

---

## 🐛 Known Issues to Verify

Based on deferred P0 items, these might not work:

### ❌ Expected Failures (OK for MVP)
1. **Photo Upload**
   - Tap Edit Profile → Upload Photo
   - Expected: Not wired to Supabase Storage yet
   - Impact: Users can't upload profile photos

2. **Data Migration**
   - Update app with schema changes
   - Expected: Data loss (fallbackToDestructiveMigration)
   - Impact: Don't test app updates yet

3. **No Automated Tests**
   - Run `./gradlew test`
   - Expected: No tests exist (0% coverage)

---

## 📱 Device Testing Matrix

### Minimum Test Coverage
Test on at least:
- ✅ 1 physical device (your phone)
- ✅ 1 emulator (Android Studio)
- ✅ Different screen sizes (phone + tablet)

### Recommended Devices
```
Priority 1 (Must test):
- Android 8.0 (Min SDK 26) - Pixel 2 emulator
- Android 14.0 (Latest) - Pixel 8 emulator or physical

Priority 2 (Should test):
- Android 11.0 (Common) - Pixel 5 emulator
- Different manufacturers (Samsung, OnePlus)

Priority 3 (Nice to have):
- Tablet (10" screen)
- Foldable device
```

---

## 🛠️ Testing Tools Setup

### 1. Enable USB Debugging
```
On Device:
1. Settings → About Phone
2. Tap "Build Number" 7 times
3. Settings → Developer Options
4. Enable "USB Debugging"
```

### 2. Setup ADB
```bash
# Verify ADB installed
adb version

# If not installed:
# Arch Linux:
sudo pacman -S android-tools

# Check device connected
adb devices
# Should see: [device-id]  device
```

### 3. Android Studio Profiler (Optional)
```
For performance testing:
1. Open Android Studio
2. View → Tool Windows → Profiler
3. Select running Kosmos app
4. Monitor CPU, Memory, Network
```

### 4. Logcat for Debugging
```bash
# View all logs
adb logcat

# Filter Kosmos logs only
adb logcat | grep "Kosmos\|com.example.kosmos"

# Clear logs
adb logcat -c

# Save logs to file
adb logcat > kosmos_logs.txt
```

---

## 📊 Test Result Tracking

### Quick Status Check
After each test session, update:

```
Date: ___________
Duration: _____ minutes
Device: ___________

Core Features:
- [ ] Auth (Login/Signup)
- [ ] Projects (CRUD)
- [ ] Tasks (CRUD)
- [ ] Chat (Send/Receive)
- [ ] Search (3 features)
- [ ] Offline Mode
- [ ] Settings

Bugs Found: ___
Critical: ___
High: ___
Medium: ___
Low: ___

Overall Status:
[ ] ✅ Ready for production
[ ] ⚠️ Minor issues, can ship
[ ] ❌ Critical issues, needs fixes
```

---

## 🚨 When to Stop Testing

### Red Flags (Stop and Fix)
- ❌ App crashes on startup
- ❌ Cannot login/signup
- ❌ Cannot create projects
- ❌ Data loss after app restart
- ❌ Network requests timeout
- ❌ Supabase connection fails

### Yellow Flags (Note but Continue)
- ⚠️ UI glitches (misaligned elements)
- ⚠️ Slow performance (> 5s load times)
- ⚠️ Search doesn't work
- ⚠️ Minor sync delays (> 10s)

### Green Flags (Good to Ship)
- ✅ Core features work smoothly
- ✅ No crashes in 30 min session
- ✅ Offline mode functional
- ✅ Real-time sync < 5s
- ✅ Search responsive
- ✅ Good performance

---

## 📞 Reporting Issues

### Where to Report
Create issues in the GitHub repo or document here:

**Template:**
```markdown
## Bug: [Short Description]

**Severity:** Critical / High / Medium / Low
**Reproducible:** Always / Sometimes / Rare

**Steps:**
1.
2.
3.

**Expected:**
**Actual:**

**Device:** [Model + Android Version]
**Build:** [APK version]

**Logs:**
```
[Paste logcat output]
```

**Screenshots:**
[Attach if available]
```

---

## ✅ Test Sign-Off

After completing testing:

```
I have tested Kosmos v1.0.0 on:
- Device: ___________
- Date: ___________
- Duration: ___________

Core Features Status:
- Authentication: ✅/❌
- Projects: ✅/❌
- Tasks: ✅/❌
- Chat: ✅/❌
- Search: ✅/❌
- Offline: ✅/❌

Critical Bugs: ___
High Bugs: ___
Medium/Low Bugs: ___

Recommendation:
[ ] ✅ Approved for production
[ ] ⚠️ Approved with known issues
[ ] ❌ Requires bug fixes before release

Tester: ___________
Signature: ___________
```

---

## 🎯 Next Steps After Testing

### If Tests Pass ✅
1. Deploy to internal testers (5-10 people)
2. Collect feedback (1 week)
3. Fix critical bugs
4. Deploy to beta (Google Play Internal Testing)
5. Monitor crash reports
6. Release to production

### If Tests Fail ❌
1. Document all bugs
2. Prioritize fixes (Critical → High → Medium)
3. Fix critical bugs
4. Rebuild APK
5. Retest from scratch
6. Repeat until pass

---

**Ready to test?** Start with the 5-Minute Smoke Test above! 🚀
