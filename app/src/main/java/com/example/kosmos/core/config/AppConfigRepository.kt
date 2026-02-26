package com.example.kosmos.core.config

import android.content.Context
import android.content.SharedPreferences
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_APP_NAME        = "cfg_app_name"
private const val PREF_TAGLINE         = "cfg_tagline"
private const val PREF_CONTACT_EMAIL   = "cfg_contact_email"
private const val PREF_FEEDBACK_EMAIL  = "cfg_feedback_email"
private const val PREF_SUPPORT_URL     = "cfg_support_url"
private const val PREF_TERMS_URL       = "cfg_terms_url"
private const val PREF_PRIVACY_URL     = "cfg_privacy_url"
private const val PREF_LOGO_URL        = "cfg_logo_url"
private const val PREF_APP_DESCRIPTION = "cfg_app_description"
private const val PREF_BUILT_WITH      = "cfg_built_with"
private const val PREF_CREDITS         = "cfg_credits"

@Singleton
class AppConfigRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val prefs: SharedPreferences,
    private val context: Context
) {
    private val defaults = AppConfig()

    private val _config = MutableStateFlow(loadFromPrefs())
    val config: StateFlow<AppConfig> = _config

    /** Called once at app startup (background thread). Updates StateFlow + pre-warms logo cache. */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        try {
            val rows = supabase.from("app_config")
                .select()
                .decodeList<AppConfigEntry>()

            val map = rows.associate { it.key to it.value }

            val newConfig = AppConfig(
                appName        = map["app_name"]        ?: defaults.appName,
                tagline        = map["tagline"]         ?: defaults.tagline,
                contactEmail   = map["contact_email"]   ?: defaults.contactEmail,
                feedbackEmail  = map["feedback_email"]  ?: defaults.feedbackEmail,
                supportUrl     = map["support_url"]     ?: defaults.supportUrl,
                termsUrl       = map["terms_url"]       ?: defaults.termsUrl,
                privacyUrl     = map["privacy_url"]     ?: defaults.privacyUrl,
                logoUrl        = map["logo_url"]        ?: defaults.logoUrl,
                appDescription = map["app_description"] ?: defaults.appDescription,
                builtWith      = map["built_with"]      ?: defaults.builtWith,
                credits        = map["credits"]         ?: defaults.credits
            )

            saveToPrefs(newConfig)

            val oldLogoUrl = _config.value.logoUrl
            _config.value = newConfig

            // Pre-warm Coil disk cache if logo URL changed or was newly set
            if (newConfig.logoUrl.isNotEmpty() && newConfig.logoUrl != oldLogoUrl) {
                prewarmLogo(newConfig.logoUrl)
            }
        } catch (_: Exception) {
            // Network unavailable — SharedPrefs values (already loaded into StateFlow) remain valid
        }
    }

    private fun prewarmLogo(url: String) {
        val imageLoader = ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.10)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()

        val request = ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()

        imageLoader.enqueue(request)
    }

    // ── SharedPreferences helpers ──────────────────────────────────────────

    private fun loadFromPrefs(): AppConfig = AppConfig(
        appName        = prefs.getString(PREF_APP_NAME,        defaults.appName)!!,
        tagline        = prefs.getString(PREF_TAGLINE,         defaults.tagline)!!,
        contactEmail   = prefs.getString(PREF_CONTACT_EMAIL,   defaults.contactEmail)!!,
        feedbackEmail  = prefs.getString(PREF_FEEDBACK_EMAIL,  defaults.feedbackEmail)!!,
        supportUrl     = prefs.getString(PREF_SUPPORT_URL,     defaults.supportUrl)!!,
        termsUrl       = prefs.getString(PREF_TERMS_URL,       defaults.termsUrl)!!,
        privacyUrl     = prefs.getString(PREF_PRIVACY_URL,     defaults.privacyUrl)!!,
        logoUrl        = prefs.getString(PREF_LOGO_URL,        defaults.logoUrl)!!,
        appDescription = prefs.getString(PREF_APP_DESCRIPTION, defaults.appDescription)!!,
        builtWith      = prefs.getString(PREF_BUILT_WITH,      defaults.builtWith)!!,
        credits        = prefs.getString(PREF_CREDITS,         defaults.credits)!!
    )

    private fun saveToPrefs(config: AppConfig) {
        prefs.edit()
            .putString(PREF_APP_NAME,        config.appName)
            .putString(PREF_TAGLINE,         config.tagline)
            .putString(PREF_CONTACT_EMAIL,   config.contactEmail)
            .putString(PREF_FEEDBACK_EMAIL,  config.feedbackEmail)
            .putString(PREF_SUPPORT_URL,     config.supportUrl)
            .putString(PREF_TERMS_URL,       config.termsUrl)
            .putString(PREF_PRIVACY_URL,     config.privacyUrl)
            .putString(PREF_LOGO_URL,        config.logoUrl)
            .putString(PREF_APP_DESCRIPTION, config.appDescription)
            .putString(PREF_BUILT_WITH,      config.builtWith)
            .putString(PREF_CREDITS,         config.credits)
            .apply()
    }
}
