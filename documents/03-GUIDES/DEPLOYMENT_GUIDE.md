# Kosmos Android - Deployment Guide

**Version**: 1.0
**Last Updated**: 2026-01-13
**Status**: Production Ready (pending RLS enablement)

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [🔴 CRITICAL: Enable Row Level Security](#critical-enable-row-level-security)
4. [Database Configuration](#database-configuration)
5. [Firebase Configuration](#firebase-configuration)
6. [Build Configuration](#build-configuration)
7. [Release Build](#release-build)
8. [Testing Checklist](#testing-checklist)
9. [Deployment Steps](#deployment-steps)
10. [Monitoring & Maintenance](#monitoring--maintenance)
11. [Rollback Procedures](#rollback-procedures)

---

## Prerequisites

### Required Tools
- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: 17 or later
- **Gradle**: 8.0+ (included in project)
- **Git**: Latest version
- **Supabase CLI**: (optional, for local testing)

### Required Accounts
- **Supabase Project**: Active project with database
- **Firebase Project**: Configured with Authentication + FCM
- **Google Cloud Console**: Speech API enabled (for voice features)
- **Google Play Console**: (for production deployment)

### Required Access
- Supabase database admin access
- Firebase project owner/editor access
- Google Play Console admin access
- GitHub repository access (if using CI/CD)

---

## Environment Setup

### 1. Clone Repository
```bash
git clone <repository-url>
cd Kosmos
```

### 2. Configure Environment Variables

**Create `local.properties`** (if not exists):
```properties
sdk.dir=/path/to/Android/sdk
```

**Update `app/build.gradle.kts`** with your credentials:

```kotlin
android {
    defaultConfig {
        // Supabase Configuration
        buildConfigField("String", "SUPABASE_URL", "\"YOUR_SUPABASE_URL\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"YOUR_SUPABASE_ANON_KEY\"")

        // Google Cloud Speech API
        buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"YOUR_SPEECH_API_KEY\"")
    }
}
```

**Add `google-services.json`**:
1. Download from Firebase Console
2. Place in `app/google-services.json`
3. Verify it's listed in `.gitignore`

### 3. Verify Dependencies
```bash
./gradlew dependencies
```

---

## 🔴 CRITICAL: Enable Row Level Security

**⚠️ WARNING**: RLS is currently DISABLED. This must be enabled before production deployment.

### Current Status
- All Supabase tables have RLS disabled (testing mode)
- Any authenticated user can access ANY data
- Complete data breach risk

### Enable RLS (REQUIRED)

**Step 1**: Access Supabase SQL Editor
1. Go to https://app.supabase.com
2. Select your project
3. Navigate to SQL Editor

**Step 2**: Execute RLS Script
1. Open `/RLS_ENABLE_PRODUCTION.sql` from project root
2. Copy entire script contents
3. Paste into Supabase SQL Editor
4. Click "Run" to execute

**Step 3**: Verify RLS Enabled
Run verification query:
```sql
SELECT
  tablename,
  CASE
    WHEN rowsecurity = true THEN '✅ ENABLED'
    ELSE '❌ STILL DISABLED'
  END as rls_status
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'projects', 'project_members')
ORDER BY tablename;
```

**Expected Result**: All 6 tables show "✅ ENABLED"

**Step 4**: Verify Policy Count
```sql
SELECT
  tablename,
  COUNT(*) as policy_count
FROM pg_policies
WHERE schemaname = 'public'
  AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'projects', 'project_members')
GROUP BY tablename
ORDER BY tablename;
```

**Expected Result**: Each table shows 4 policies (Total: 24 policies)

### Manual Testing (Required)

**Test 1: User Isolation** (10 mins)
1. Create test user A and user B
2. User A creates a project
3. Verify user B cannot see project via app
4. Check Supabase logs for RLS denials

**Test 2: Project Membership** (10 mins)
1. User A adds user B to project
2. Verify user B can now see project
3. User A removes user B
4. Verify user B loses access immediately

**Test 3: Message Privacy** (10 mins)
1. User A sends message in project chat
2. Verify user B (not in project) cannot see message
3. Add user B to project
4. Verify user B can now see messages

**Test 4: Task Permissions** (10 mins)
1. User A creates task in project
2. Verify user B (not in project) cannot see task
3. Add user B as project member
4. Verify user B can see tasks (based on role)

### RLS Rollback (if issues found)
```sql
-- ONLY USE IF CRITICAL ISSUES FOUND
ALTER TABLE users DISABLE ROW LEVEL SECURITY;
ALTER TABLE projects DISABLE ROW LEVEL SECURITY;
ALTER TABLE project_members DISABLE ROW LEVEL SECURITY;
ALTER TABLE chat_rooms DISABLE ROW LEVEL SECURITY;
ALTER TABLE messages DISABLE ROW LEVEL SECURITY;
ALTER TABLE tasks DISABLE ROW LEVEL SECURITY;
```

**Documentation**: See `/RLS_SECURITY_AUDIT.md` for detailed policy documentation

---

## Database Configuration

### Supabase Setup

**1. Database Schema**
- Schema file: `/SCHEMA_FIX_COMPLETE_V2.sql`
- Tables: 6 main tables (users, projects, project_members, chat_rooms, messages, tasks)
- Additional: 4 extended tables (milestones, task_dependencies, task_activity, time_entries)

**2. Storage Buckets** (if using Supabase Storage)
```sql
-- Create storage buckets
INSERT INTO storage.buckets (id, name, public)
VALUES
  ('avatars', 'avatars', true),
  ('attachments', 'attachments', false),
  ('voice-messages', 'voice-messages', false);

-- Set storage policies (customize as needed)
CREATE POLICY "Avatar images are publicly accessible"
  ON storage.objects FOR SELECT
  USING (bucket_id = 'avatars');

CREATE POLICY "Users can upload their own avatar"
  ON storage.objects FOR INSERT
  WITH CHECK (bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1]);
```

**3. Realtime Configuration**
Ensure Realtime is enabled for:
- `messages` table
- `tasks` table
- `project_members` table
- `chat_rooms` table

Go to: Database → Replication → Enable for tables

**4. Connection Pooling**
- Recommended: Enable connection pooler in Supabase settings
- Mode: Transaction mode
- Pool size: 15-20 connections

---

## Firebase Configuration

### Authentication Setup

**1. Enable Authentication Methods**
- Email/Password: ✅ Enable
- Google Sign-In: ✅ Enable
- Anonymous Sign-In: ❌ Disable (security)

**2. Configure OAuth Providers**
Google Sign-In:
1. Go to Firebase Console → Authentication → Sign-in method
2. Enable Google provider
3. Add SHA-1 and SHA-256 fingerprints:
   ```bash
   # Debug keystore
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

   # Release keystore
   keytool -list -v -keystore /path/to/release.keystore -alias <alias> -storepass <password>
   ```
4. Download updated `google-services.json`

**3. Configure Authorized Domains**
Add your domain (if using custom domain):
- Firebase Console → Authentication → Settings → Authorized domains

### Cloud Messaging (FCM) Setup

**1. Enable FCM**
- Already enabled if `google-services.json` is configured

**2. Configure Notification Channels** (optional)
- High priority: Task assignments, mentions
- Default: General notifications
- Low priority: Activity updates

**3. Server Key**
- Copy Server Key from Firebase Console → Cloud Messaging
- Add to Supabase Edge Functions (if using serverless notifications)

---

## Build Configuration

### Gradle Configuration

**1. Verify Build Configuration**
```bash
./gradlew clean
./gradlew assembleDebug
```

**2. Release Build Configuration**
Update `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release-keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

**3. ProGuard Rules**
Verify `app/proguard-rules.pro` contains necessary keep rules:

```proguard
# Supabase
-keep class io.github.jan.supabase.** { *; }
-keep class kotlinx.serialization.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Models (Kotlinx Serialization)
-keep class com.example.kosmos.core.models.** { *; }
```

### Version Management

**Update version in `app/build.gradle.kts`:**
```kotlin
android {
    defaultConfig {
        versionCode = 1  // Increment for each release
        versionName = "1.0.0"  // Semantic versioning
    }
}
```

---

## Release Build

### Generate Release APK/AAB

**1. Create Keystore** (if not exists)
```bash
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias kosmos-release
```

**2. Set Environment Variables**
```bash
export KEYSTORE_PASSWORD="your-keystore-password"
export KEY_ALIAS="kosmos-release"
export KEY_PASSWORD="your-key-password"
```

**3. Build Release**
```bash
# For APK
./gradlew assembleRelease

# For Play Store (AAB - recommended)
./gradlew bundleRelease
```

**4. Verify Build**
```bash
# Check APK
./gradlew assembleRelease --dry-run

# Verify signing
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

**5. Test Release Build**
```bash
# Install on device
adb install app/build/outputs/apk/release/app-release.apk

# Check for crashes
adb logcat | grep -i kosmos
```

---

## Testing Checklist

### Pre-Deployment Testing

**Security Testing**:
- [ ] RLS policies enabled and verified
- [ ] User isolation tested (cross-user data access blocked)
- [ ] Project membership enforcement tested
- [ ] Message privacy verified
- [ ] Task permissions verified
- [ ] No sensitive data in logs

**Authentication Testing**:
- [ ] Email/password signup works
- [ ] Email/password login works
- [ ] Google Sign-In works
- [ ] Logout works
- [ ] Session persistence works
- [ ] Token refresh works

**Core Features Testing**:
- [ ] Create project works
- [ ] Add project members works
- [ ] Create chat room works
- [ ] Send message works
- [ ] Create task works
- [ ] Assign task works
- [ ] Update task status works (with permissions)
- [ ] Real-time updates work (messages, tasks)

**Offline Testing**:
- [ ] App works offline (cached data)
- [ ] Syncs when back online
- [ ] No data loss during offline mode
- [ ] Conflict resolution works

**Performance Testing**:
- [ ] App launch time < 2 seconds
- [ ] Screen transitions smooth (60fps)
- [ ] No memory leaks
- [ ] No ANRs (Application Not Responding)
- [ ] Battery usage acceptable

**Build Testing**:
- [ ] Release APK/AAB builds successfully
- [ ] ProGuard doesn't break functionality
- [ ] No obfuscation errors
- [ ] APK size reasonable (<50MB)

---

## Deployment Steps

### Google Play Store Deployment

**1. Prepare Play Console**
1. Create app listing in Google Play Console
2. Fill in store listing details:
   - Title: Kosmos
   - Short description
   - Full description
   - Screenshots (phone + tablet)
   - Feature graphic
   - App icon

**2. Configure Release**
1. Production → Create new release
2. Upload AAB: `app/build/outputs/bundle/release/app-release.aab`
3. Set release name: v1.0.0
4. Add release notes

**3. Content Rating**
1. Complete content rating questionnaire
2. Get rating certificate

**4. Pricing & Distribution**
1. Set pricing (Free/Paid)
2. Select countries
3. Accept program policies

**5. Review & Publish**
1. Review all sections (must be green checkmarks)
2. Click "Start rollout to Production"
3. Wait for review (typically 1-3 days)

### Internal Testing Track (Recommended First)

**Before production:**
1. Create Internal Testing track
2. Add testers (email addresses)
3. Upload AAB to Internal track
4. Test for 1-2 weeks
5. Fix any issues found
6. Then promote to Production

---

## Monitoring & Maintenance

### Monitoring Tools

**1. Firebase Crashlytics**
- Monitor crashes in real-time
- View crash-free users percentage
- Set up alerts for crash rate spikes

**2. Firebase Analytics**
- Track user engagement
- Monitor retention rates
- Track feature usage

**3. Supabase Logs**
- Monitor database query performance
- Check for RLS policy violations
- Monitor API error rates

**4. Play Console**
- Monitor app ratings/reviews
- Check ANR rate (<0.47% threshold)
- Check crash rate (<1.09% threshold)

### Key Metrics to Monitor

- **Daily Active Users (DAU)**
- **Crash-free users** (target: >99%)
- **ANR rate** (target: <0.47%)
- **App size** (target: <50MB)
- **API response time** (target: <500ms p95)
- **User retention** (Day 1, Day 7, Day 30)

### Alerting Setup

**Critical Alerts** (page immediately):
- Crash rate > 5%
- RLS policy violations
- API error rate > 10%
- Database connection failures

**Warning Alerts** (check within 4 hours):
- Crash rate > 2%
- ANR rate > 1%
- API response time > 1s
- High memory usage

---

## Rollback Procedures

### Play Store Rollback

**If critical issues found:**
1. Go to Play Console → Production
2. Click "Create new release"
3. Select "Rollback" from previous releases
4. Choose last stable version
5. Click "Review release" → "Start rollout"

**Rollback takes:** 1-2 hours to reach all users

### Database Rollback

**If RLS causes issues:**
1. Execute RLS rollback script (see RLS section above)
2. Document issue
3. Fix policies
4. Re-test before re-enabling

**If schema issues:**
1. Restore from Supabase backup
2. Go to: Settings → Backups → Restore

### Hotfix Procedure

**For critical bugs:**
1. Create hotfix branch: `hotfix/v1.0.1`
2. Fix bug
3. Increment version: `versionCode = 2`, `versionName = "1.0.1"`
4. Build release
5. Deploy to Internal track first
6. Test quickly (1-2 hours)
7. Promote to Production
8. Monitor closely

---

## Environment Variables Summary

### Required Build Config Fields
```kotlin
SUPABASE_URL = "your-supabase-project-url"
SUPABASE_ANON_KEY = "your-supabase-anon-key"
GOOGLE_CLOUD_API_KEY = "your-speech-api-key"
```

### Required Files
- `app/google-services.json` - Firebase configuration
- `release-keystore.jks` - Release signing key
- `app/proguard-rules.pro` - ProGuard configuration

### Environment-Specific Settings

**Development:**
- `buildType = "debug"`
- Logging enabled
- ProGuard disabled
- Debuggable = true

**Staging/QA:**
- `buildType = "debug"` or separate `staging` build type
- Logging enabled
- ProGuard enabled (test obfuscation)
- Separate Supabase project (recommended)

**Production:**
- `buildType = "release"`
- Logging minimal (errors only)
- ProGuard enabled
- Debuggable = false
- Signed with release key

---

## Troubleshooting

### Common Issues

**Issue: Build fails with "google-services.json not found"**
- Solution: Download from Firebase Console and place in `app/` folder

**Issue: ProGuard breaks Supabase serialization**
- Solution: Add keep rules in `proguard-rules.pro` (see Build Configuration section)

**Issue: Google Sign-In fails on release build**
- Solution: Add SHA-256 fingerprint of release keystore to Firebase

**Issue: RLS blocks legitimate queries**
- Solution: Check Supabase logs, verify user authentication, check policy logic

**Issue: App crashes on startup (release only)**
- Solution: Check ProGuard rules, review crashlytics logs, test with ProGuard in debug mode

---

## Security Checklist

**Before Production:**
- [ ] RLS enabled on all tables (CRITICAL)
- [ ] RLS policies tested manually
- [ ] No API keys hardcoded in source
- [ ] `google-services.json` in `.gitignore`
- [ ] ProGuard enabled for release
- [ ] Certificate pinning considered (optional)
- [ ] Input validation on all forms
- [ ] SQL injection prevention (via Supabase)
- [ ] XSS prevention (via input sanitization)

---

## Support & Resources

### Documentation
- Project README: `/README.md`
- Phase Summaries: `/PHASE_1_COMPLETE_SUMMARY.md`, `/PHASE_2_COMPLETE_SUMMARY.md`, `/PHASE_3_DISCOVERY_SUMMARY.md`
- RLS Security: `/RLS_SECURITY_AUDIT.md`
- RLS SQL Script: `/RLS_ENABLE_PRODUCTION.sql`
- Codebase Documentation: `/documents/CODEBASE_MODULE_DOCS.md`

### External Resources
- Supabase Docs: https://supabase.com/docs
- Firebase Docs: https://firebase.google.com/docs
- Android Developers: https://developer.android.com
- Jetpack Compose: https://developer.android.com/jetpack/compose

### Supabase Resources
- Dashboard: https://app.supabase.com
- Status Page: https://status.supabase.com
- Support: https://supabase.com/support

---

## Deployment Checklist Summary

### Pre-Deployment (Required)
- [ ] RLS enabled and verified (CRITICAL - see section above)
- [ ] All tests passing
- [ ] Release build successful
- [ ] ProGuard enabled and tested
- [ ] Version numbers updated
- [ ] Release notes prepared

### Deployment
- [ ] Upload to Internal track
- [ ] Test with real users (1-2 weeks)
- [ ] Fix any issues found
- [ ] Upload to Production track
- [ ] Submit for review

### Post-Deployment
- [ ] Monitor crash rate (first 24 hours)
- [ ] Monitor user reviews
- [ ] Check RLS logs for violations
- [ ] Respond to critical issues within 4 hours
- [ ] Plan hotfix if needed

---

**Last Updated**: January 13, 2026
**Deployment Status**: Ready (pending RLS enablement)
**Critical Blocker**: RLS must be enabled before production deployment
