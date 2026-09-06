package com.example.core.model

/**
 * Activity Log Entry for Section 11 & Section 17.
 * Explicitly records connection cuts, restorations, kill switch toggles, and Airplane Mode changes.
 */
data class ActivityLogEntry(
    val id: Long = 0L,
    val timestamp: Long,
    val eventType: EventType,
    val specificReason: SpecificCutReason,
    val description: String,
    val details: String? = null
) {
    enum class EventType {
        DISCONNECTED,
        RESTORED,
        CONFIG_CHANGED,
        SYSTEM_EVENT
    }

    /**
     * Item 10 & 17: Precise identification of the reason.
     * Never generic.
     */
    enum class SpecificCutReason(val titleAr: String, val titleEn: String) {
        MANUAL_KILL_SWITCH("القطع الكامل اليدوي", "Manual Kill Switch"),
        WIFI_TODAY_LIMIT("حد الواي فاي اليومي", "Wi-Fi Today Limit"),
        WIFI_RECURRING_LIMIT("حد الواي فاي الدائم", "Wi-Fi Recurring Limit"),
        MOBILE_TODAY_LIMIT("حد الموبايل اليومي", "Mobile Today Limit"),
        MOBILE_RECURRING_LIMIT("حد الموبايل الدائم", "Mobile Recurring Limit"),
        MOBILE_SIM2_LIMIT("حد الشريحة الثانية (SIM 2)", "Mobile SIM 2 Limit"),
        AIRPLANE_MODE("تفعيل وضع الطيران", "Airplane Mode Enabled"),
        CONNECTION_RESTORED("استعادة الاتصال", "Connection Restored"),
        LIMIT_EXPANDED("زيادة الحد واستئناف البيانات", "Limit Expanded & Resumed"),
        APP_FULLY_BLOCKED("حظر التطبيق بالكامل", "App Fully Blocked"),
        APP_LIMIT_EXCEEDED("تجاوز حد التطبيق المخصص", "App Limit Exceeded"),
        APP_UNBLOCKED("إلغاء حظر التطبيق", "App Unblocked")
    }
}
