# Security Model

Status: Mixed (verified controls + required release checks)

## Security Posture
Kosmos uses local-only secret provisioning, release logging reduction, and explicit signing verification for artifacts.

## Secret Handling Rules
Never commit secrets, keystores, or local property files.

Use local-only properties for:
- `SUPABASE_URL`, `SUPABASE_ANON_KEY`
- `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_CLOUD_API_KEY`
- `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`

## Verified Controls
- `.gitignore` excludes `local.properties` and key-store formats.
- Release script validates presence of required properties.
- `verify_bundle_signature.sh` checks signed AAB state.

## Required Before Public Rollout
1. Signed release bundle verification passes.
2. Privacy policy and terms links are live and correct.
3. Runtime permission usage is audited against feature entry points.
4. Supabase RLS policies are reviewed for project/task/chat/member boundaries.
