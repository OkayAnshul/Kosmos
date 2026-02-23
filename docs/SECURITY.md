# Security Model

## Security Posture Summary
Kosmos uses client-safe key distribution (Supabase anon key), local secret injection via non-tracked properties files, and release logging reduction in production builds.

## Secrets and Key Management
Do not commit runtime credentials or signing keys.

Allowed local secret locations:
- `local.properties`
- `~/.gradle/gradle.properties`

Required runtime keys:
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `GOOGLE_WEB_CLIENT_ID`

Required release signing keys:
- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

## Build-Time Security Controls
- `ENABLE_LOGGING=false` in release build type
- resource shrinking and code minification enabled in release
- signed-bundle verification script required in release runbook

## App Permissions Surface
Declared sensitive permissions include:
- network access
- notifications
- microphone
- camera
- legacy storage compatibility permissions

Risk note:
- Storage/media and microphone permission usage should stay tightly coupled to user actions and visible UI context.

## Authentication and Session
- OAuth callback via custom scheme (`kosmos://auth-callback`)
- Supabase auth integration
- role/permission checks enforced in app logic for critical actions

## Data Protection
- local persistence with Room
- remote data integrity depends on Supabase RLS and schema policy quality
- backup configuration files exist and should be finalized with explicit include/exclude rules before full production rollout

## Security Checklist Before Public Release
1. No secrets in git-tracked files
2. Release AAB is signed and verified
3. Privacy and Terms URLs are live and reachable
4. Runtime permissions audited against actual feature usage
5. RLS policies reviewed for project/task/chat/member paths
6. Incident response owner and rotation documented
