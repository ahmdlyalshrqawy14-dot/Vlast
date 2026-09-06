package com.example.core.model

/**
 * Section 4: Priority hierarchy resolution.
 * When multiple services are active simultaneously, clarifies who is in control.
 *
 * Precedence Rule:
 * 1. Kill Switch (Absolute highest priority)
 * 2. Today Temporary Limit (Supersedes recurring limit for active day)
 * 3. Recurring Limit (Baseline)
 * 4. Normal / Unlimited (No active limit)
 */
sealed class ServicePriorityStatus(
    val titleAr: String,
    val descriptionAr: String,
    val priorityLevel: Int
) {
    data object KillSwitchInControl : ServicePriorityStatus(
        titleAr = "القطع الكامل هو المتحكم الآن",
        descriptionAr = "تم حظر كافة حركات البيانات بالكامل بقرار يدوي ويتفوق على الحدود اليومية والدائمة.",
        priorityLevel = 1
    )

    data class TodayOverrideInControl(
        val networkName: String,
        val limitDisplay: String
    ) : ServicePriorityStatus(
        titleAr = "الحد اليومي المؤقت هو المتحكم الآن ($networkName)",
        descriptionAr = "تم تطبيق الحد المؤقت لليوم ($limitDisplay) وهو يتفوق على الحد الدائم حتى منتصف الليل.",
        priorityLevel = 2
    )

    data class RecurringLimitInControl(
        val networkName: String,
        val limitDisplay: String
    ) : ServicePriorityStatus(
        titleAr = "الحد الدائم هو المتحكم الآن ($networkName)",
        descriptionAr = "الحركة مقيدة بالحد اليومي الدائم المجدد تلقائيًا ($limitDisplay).",
        priorityLevel = 3
    )

    data object UnlimitedInControl : ServicePriorityStatus(
        titleAr = "لا يوجد حد نشط",
        descriptionAr = "يتم قياس الاستهلاك وتدفق البيانات يعمل بكامل حرية الاتصال.",
        priorityLevel = 4
    )
}
