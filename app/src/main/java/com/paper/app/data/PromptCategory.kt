package com.paper.app.data

import android.content.Context

/**
 * An opt-in prompt category. `id` and `slot` are persisted (SharedPreferences keys and
 * notification/alarm request codes) and must never change or be reused once shipped;
 * `displayName` and `prompts` are free to edit.
 */
enum class PromptCategory(
    val id: String,
    val displayName: String,
    val slot: Int,
    val prompts: List<String>
) {
    GOALS(
        id = "goals",
        displayName = "Goals",
        slot = 1,
        prompts = listOf(
            "What's one thing you're working toward right now?",
            "What would progress look like this week?",
            "What's a goal you haven't told anyone about?",
            "What's holding you back from something you want?",
            "What's a small step you could take today?",
            "What goal feels furthest away right now?",
            "What's something you finished recently that you're proud of?",
            "If nothing could go wrong, what would you try next?",
            "What do you keep putting off, and why?",
            "What does \"enough\" look like for you right now?"
        )
    ),
    DREAMS(
        id = "dreams",
        displayName = "Dreams",
        slot = 2,
        prompts = listOf(
            "What's a dream you've had for a long time?",
            "If you could try anything without fear of failing, what would it be?",
            "What did you dream about doing when you were young?",
            "What's something you'd love to do someday?",
            "What would your life look like if things went exactly how you hoped?",
            "Is there a dream you've quietly let go of? What happened?",
            "What's a place you've always wanted to go?",
            "What would you attempt if you knew you had ten more years to try?",
            "What's a version of your life you sometimes imagine?",
            "What's something you want, even if it feels far off?"
        )
    ),
    PERSONAL_GROWTH(
        id = "personal_growth",
        displayName = "Personal growth",
        slot = 3,
        prompts = listOf(
            "What's something you learned about yourself this month?",
            "What's a habit you're trying to build or break?",
            "When did you last feel proud of how you handled something hard?",
            "What's something you'd like to understand better about yourself?",
            "What's a mistake that taught you something?",
            "How have you changed in the last year?",
            "What's something you're slowly getting better at?",
            "What do you need more of in your life right now?",
            "What's a belief you've reconsidered recently?",
            "What's one thing you'd tell yourself from a year ago?"
        )
    ),
    WORK(
        id = "work",
        displayName = "Work",
        slot = 4,
        prompts = listOf(
            "What went well at work today?",
            "What's something at work that's been on your mind?",
            "What's a small win you had recently?",
            "What's frustrating you right now, and why?",
            "Who made your day better at work recently?",
            "What would make tomorrow feel easier?",
            "What have you been avoiding?",
            "What's something you're quietly proud of?",
            "What do you need to let go of?",
            "What would a good day look like this week?"
        )
    ),
    RELATIONSHIPS(
        id = "relationships",
        displayName = "Relationships",
        slot = 5,
        prompts = listOf(
            "Who have you been thinking about lately?",
            "Is there someone you'd like to reach out to?",
            "What's a relationship you're grateful for right now?",
            "Who made you feel understood recently?",
            "Is there something you've been meaning to say to someone?",
            "What's a moment with someone that's stuck with you this week?",
            "Who do you wish you spent more time with?",
            "What's something you appreciate about someone close to you?",
            "Is there a relationship that could use your attention right now?",
            "Who has shaped who you are?"
        )
    );

    companion object {
        fun byId(id: String?): PromptCategory? = id?.let { target -> entries.find { it.id == target } }
    }
}

/** Persists which prompt categories the user has opted into. Empty by default — none opted in. */
object PromptCategoryStore {
    private const val PREFS = "paper_prompt_categories"
    private const val KEY_ENABLED = "enabled"

    fun loadEnabled(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_ENABLED, emptySet())
            .orEmpty()

    fun setEnabled(context: Context, categoryId: String, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_ENABLED, emptySet()).orEmpty()
        val updated = if (enabled) current + categoryId else current - categoryId
        prefs.edit().putStringSet(KEY_ENABLED, updated).apply()
    }
}
