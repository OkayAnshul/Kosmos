# Admin Operations Guide

> All admin operations are done from the **Supabase Dashboard** — no code deployment needed.
> Open: your project → **Table Editor** or **SQL Editor**

---

## Change Any App String / URL

1. Go to **Table Editor → app_config**
2. Find the row by `key`, click the value cell, edit, **Save**
3. Takes effect on **next app launch** for all users

| Key | Controls |
|---|---|
| `app_name` | App wordmark on Splash screen |
| `tagline` | Subtitle on Splash screen |
| `contact_email` | Email in Settings → Feedback → Send |
| `feedback_email` | Same (backup email) |
| `support_url` | Settings → Help & Support link |
| `terms_url` | Settings → Terms of Service link |
| `privacy_url` | Settings → Privacy Policy link |
| `logo_url` | Remote logo on Splash (empty = show text wordmark) |
| `app_description` | Description text in Settings → Developer card |
| `built_with` | Tech stack line in Settings → Developer card |
| `credits` | Credits line in Settings → Developer card |

---

## Upload and Set a New Splash Logo

1. Go to **Storage** → create a bucket called `branding` (set to **Public**)
2. Upload your logo image (PNG, ~256×256px recommended)
3. Click the file → **Get URL** → copy the public URL
4. Go to **Table Editor → app_config** → edit `logo_url` row → paste URL → Save
5. On next app launch the image is downloaded in the background and cached — shows instantly from that point

> **Note:** This changes the logo on the **Splash screen only**. The Android launcher icon (shown in the app drawer and search results) is built into the APK and can only be changed in a new app release.

---

## Send a Broadcast Notification to All Users

Run in **SQL Editor**:

```sql
INSERT INTO broadcast_notifications (title, body)
VALUES ('Your Title Here', 'Your message body here.');
```

The `fan_out_broadcast` trigger automatically creates one notification row per user. Each device receives it within seconds via Supabase Realtime.

---

## Show a One-Time Announcement Screen

1. Go to **Table Editor → announcements** → **Insert row**

| Field | Required | Example |
|---|---|---|
| `title` | ✅ | `"What's New in v2.0"` |
| `body` | ✅ | `"We've redesigned the task board..."` |
| `type` | ✅ | `info` / `warning` / `feature` |
| `cta_label` | Optional | `"See What's New"` |
| `cta_url` | Optional | `"https://kosmos.app/changelog"` |
| `is_active` | ✅ | `true` |
| `expires_at` | Optional | Leave empty = never expires |

2. Save. On next app launch every user who hasn't seen it gets a full-screen overlay.
3. Users tap **"Got it"** → dismissed forever for that user.

### Retire an Announcement

- **Immediate:** Set `is_active = false`
- **Scheduled:** Set `expires_at` to a future timestamp — auto-expires

---

## Mark a User as Admin

1. **Table Editor → users** → find the user row → set `is_admin = true` → Save
2. (No app-side effect yet — field reserved for future admin features)
