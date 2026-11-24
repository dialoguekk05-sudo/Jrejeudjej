package com.example

import com.example.model.HayatActionType
import com.example.model.HayatLanguage
import com.example.model.HayatMood
import com.example.model.ReminderCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun language_enum_resolves_safely() {
        assertEquals(HayatLanguage.HINDI, HayatLanguage.fromCode("hi"))
        assertEquals(HayatLanguage.ENGLISH, HayatLanguage.fromCode("en"))
        assertEquals(HayatLanguage.HINGLISH, HayatLanguage.fromCode("unknown"))
    }

    @Test
    fun mood_and_action_enums_resolve_safely() {
        assertEquals(HayatMood.STUDY, HayatMood.fromLabel("Study"))
        assertEquals(HayatMood.FOCUSED, HayatMood.fromLabel("bad_value"))
        assertEquals(HayatActionType.SET_ALARM, HayatActionType.fromName("SET_ALARM"))
        assertEquals(HayatActionType.NONE, HayatActionType.fromName("INVALID"))
        assertEquals(ReminderCategory.ALARM, ReminderCategory.fromLabel("Alarm"))
    }
}
