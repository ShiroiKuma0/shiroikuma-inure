package app.simple.inure.preferences

import android.annotation.SuppressLint
import app.simple.inure.util.AppUtils
import app.simple.inure.util.CalendarUtils
import java.util.Date

@Suppress("NOTHING_TO_INLINE", "UseKtx")
object TrialPreferences {

    const val MAX_TRIAL_DAYS = 0xF

    private const val FIRST_LAUNCH = "first_launch_"
    const val IS_APP_FULL_VERSION_ENABLED = "is_full_version_"
    private const val IS_LEGACY_MIGRATED = "is_legacy_migrated_"
    private const val IS_UNLOCKER_VERIFICATION_REQUIRED = "is_unlocker_verification_required_"
    private const val LAST_VERIFICATION_DATE = "last_verification_date_"

    const val HAS_LICENSE_KEY = "has_license_key"

    // ---------------------------------------------------------------------------------------------------------- //

    @SuppressLint("UseKtx")
    fun setFirstLaunchDate(date: Long) {
        SharedPreferences.getEncryptedSharedPreferences().edit().putLong(FIRST_LAUNCH, date).apply()
    }

    fun getFirstLaunchDate(): Long {
        return SharedPreferences.getEncryptedSharedPreferences().getLong(FIRST_LAUNCH, -1)
    }

    // ---------------------------------------------------------------------------------------------------------- //

    fun getDaysLeft(): Int {
        return kotlin.runCatching {
            MAX_TRIAL_DAYS - CalendarUtils.getDaysBetweenTwoDates(Date(getFirstLaunchDate()), CalendarUtils.getToday())
                .coerceAtLeast(0).coerceAtMost(MAX_TRIAL_DAYS)
        }.getOrElse {
            -1
        }
    }

    fun getMaxDays(): Int {
        return MAX_TRIAL_DAYS
    }

    // ---------------------------------------------------------------------------------------------------------- //


    fun setFullVersion(value: Boolean): Boolean {
        return SharedPreferences.getEncryptedSharedPreferences().edit()
            .putBoolean(IS_APP_FULL_VERSION_ENABLED, value).commit()
    }

    inline fun isAppFullVersionEnabled(): Boolean {
        return true // Fork: full version always enabled (the Play unlocker is package/signature-bound to the official app.simple.inure and can't activate this renamed fork).
    }

    fun isWithinTrialPeriod(): Boolean {
        return CalendarUtils.getDaysBetweenTwoDates(Date(getFirstLaunchDate()), CalendarUtils.getToday()) <= MAX_TRIAL_DAYS
    }

    fun isTrialWithoutFull(): Boolean {
        return CalendarUtils.getDaysBetweenTwoDates(Date(getFirstLaunchDate()), CalendarUtils.getToday()) <= MAX_TRIAL_DAYS
                && !isAppFullVersionEnabled()
    }

    fun isFullVersion(): Boolean {
        return true // Fork: full version always enabled (see isAppFullVersionEnabled).
    }

    // ---------------------------------------------------------------------------------------------------------- //

    fun reset() {
        setFirstLaunchDate(-1)
        setFullVersion(false)
    }

    fun migrateLegacy() {
        if (!isLegacyMigrated()) {
            setFirstLaunchDate(MainPreferences.getFirstLaunchDateLegacy())
            setFullVersion(MainPreferences.isFullVersionEnabledLegacy())

            setLegacyMigrated(true)
            MainPreferences.removeLegacyPreferences()
        }
    }

    // ---------------------------------------------------------------------------------------------------------- //

    fun setLegacyMigrated(value: Boolean) {
        SharedPreferences.getEncryptedSharedPreferences().edit().putBoolean(IS_LEGACY_MIGRATED, value).apply()
    }

    private fun isLegacyMigrated(): Boolean {
        return SharedPreferences.getEncryptedSharedPreferences().getBoolean(IS_LEGACY_MIGRATED, false)
    }

    // ---------------------------------------------------------------------------------------------------------- //

    fun setHasLicenceKey(hasLicence: Boolean) {
        SharedPreferences.getEncryptedSharedPreferences().edit().putBoolean(HAS_LICENSE_KEY, hasLicence).apply()
    }

    fun hasLicenceKey(): Boolean {
        return true // Fork: take the clean "licence key mode" UI path on the splash screen; no unlocker dependency, no deactivation warning.
    }

    // ---------------------------------------------------------------------------------------------------------- //

    fun setUnlockerVerificationRequired(value: Boolean): Boolean {
        return SharedPreferences.getEncryptedSharedPreferences().edit().putBoolean(IS_UNLOCKER_VERIFICATION_REQUIRED, value).commit()
    }

    fun isUnlockerVerificationRequired(): Boolean {
        return false // Fork: never round-trip to the unlocker (it won't answer a renamed package -> "Checking license" hang).
    }

    // ---------------------------------------------------------------------------------------------------------- //

    fun setLastVerificationDate(date: Long) {
        SharedPreferences.getEncryptedSharedPreferences().edit().putLong(LAST_VERIFICATION_DATE, date).apply()
    }

    fun getLastVerificationDate(): Long {
        return SharedPreferences.getEncryptedSharedPreferences().getLong(LAST_VERIFICATION_DATE, -1L)
    }
}
