package com.example

import com.example.data.MetaConfigParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MetaConfigParserTest {

    @Test
    fun testParseMultipleSubFlags() {
        val entry = "130058:ig_loot_android:0:is_enabled:1:profile_toolbar_entrypoint_enabled:3:canvas_item_scale_enabled"
        val flag = MetaConfigParser.parseEntry(entry)

        assertNotNull(flag)
        assertEquals("130058", flag!!.code)
        assertEquals("ig_loot_android", flag.name)
        assertEquals(3, flag.subFlags.size)

        assertEquals("0", flag.subFlags[0].code)
        assertEquals("is_enabled", flag.subFlags[0].name)

        assertEquals("1", flag.subFlags[1].code)
        assertEquals("profile_toolbar_entrypoint_enabled", flag.subFlags[1].name)

        assertEquals("3", flag.subFlags[2].code)
        assertEquals("canvas_item_scale_enabled", flag.subFlags[2].name)
    }

    @Test
    fun testParseSingleSubFlag() {
        val entry = "142330:ig4a_maa_server_driven_headers:0:is_killswitched"
        val flag = MetaConfigParser.parseEntry(entry)

        assertNotNull(flag)
        assertEquals("142330", flag!!.code)
        assertEquals("ig4a_maa_server_driven_headers", flag.name)
        assertEquals(1, flag.subFlags.size)
        assertEquals("0", flag.subFlags[0].code)
        assertEquals("is_killswitched", flag.subFlags[0].name)
    }

    @Test
    fun testMalformedEntryGracefulHandling() {
        assertNull(MetaConfigParser.parseEntry(""))
        assertNull(MetaConfigParser.parseEntry("    "))
        assertNull(MetaConfigParser.parseEntry("invalid_entry_no_colon"))

        // Entry with odd number of parts should not crash
        val oddEntry = "1000:some_flag:0"
        val flag = MetaConfigParser.parseEntry(oddEntry)
        assertNotNull(flag)
        assertEquals("1000", flag!!.code)
        assertEquals("some_flag", flag.name)
        assertEquals(1, flag.subFlags.size)
        assertEquals("0", flag.subFlags[0].code)
        assertEquals("", flag.subFlags[0].name)
    }

    @Test
    fun testCategoryDetection() {
        assertEquals("Basel", MetaConfigParser.detectCategory("basel_android_caption_style_editor_redesign"))
        assertEquals("Threads App/P92", MetaConfigParser.detectCategory("p92_android_best_of_threads"))
        assertEquals("WhatsApp", MetaConfigParser.detectCategory("ig_to_wa_posts_reels_crossposting_linked"))
        assertEquals("Quicksnap", MetaConfigParser.detectCategory("ig_android_quicksnap_halloween"))
        assertEquals("Friend Map", MetaConfigParser.detectCategory("ig_android_friend_map_visits"))
        assertEquals("Aura", MetaConfigParser.detectCategory("ig_android_aura_superlike"))
        assertEquals("Direct", MetaConfigParser.detectCategory("igd_android_message_island_listener_fix"))
    }

    @Test
    fun testExactCopyValuesIntegrity() {
        val entry = "130058:ig_loot_android:1:profile_toolbar_entrypoint_enabled"
        val flag = MetaConfigParser.parseEntry(entry)!!

        // Universal Flag Name
        assertEquals("ig_loot_android", flag.name)
        // Universal Flag Code
        assertEquals("130058", flag.code)
        // Sub-flag Name
        assertEquals("profile_toolbar_entrypoint_enabled", flag.subFlags[0].name)
        // Sub-flag Code
        assertEquals("1", flag.subFlags[0].code)
    }
}
