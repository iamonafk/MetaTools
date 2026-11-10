package com.example

import com.example.data.JsonCodeToNameEngine
import com.example.model.JsonFormatStyle
import com.example.model.SubFlag
import com.example.model.UniversalFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JsonCodeToNameEngineTest {

    private val sampleMapping = listOf(
        UniversalFlag(
            code = "69718",
            name = "ig_android_camera_waterfall",
            subFlags = listOf(
                SubFlag("4", "is_recording_waterfall_enabled", "69718", "ig_android_camera_waterfall", "Camera"),
                SubFlag("1", "is_postcapture_waterfall_enabled", "69718", "ig_android_camera_waterfall", "Camera")
            ),
            category = "Camera",
            originalEntry = "69718:ig_android_camera_waterfall:4:is_recording_waterfall_enabled:1:is_postcapture_waterfall_enabled"
        ),
        UniversalFlag(
            code = "53982",
            name = "ig_android_reels_swipe_opt",
            subFlags = listOf(
                SubFlag("0", "is_enabled", "53982", "ig_android_reels_swipe_opt", "Reels"),
                SubFlag("5", "cache_depth", "53982", "ig_android_reels_swipe_opt", "Reels")
            ),
            category = "Reels",
            originalEntry = "53982:ig_android_reels_swipe_opt:0:is_enabled:5:cache_depth"
        ),
        UniversalFlag(
            code = "89103",
            name = "ig_android_long_press",
            subFlags = listOf(
                SubFlag("3", "direct_tab", "89103", "ig_android_long_press", "Direct")
            ),
            category = "Direct",
            originalEntry = "89103:ig_android_long_press:3:direct_tab"
        ),
        UniversalFlag(
            code = "24298",
            name = "ig_android_reels_clips_video_launching_bitrate",
            subFlags = listOf(
                SubFlag("0", "transcoding_bitrate", "24298", "ig_android_reels_clips_video_launching_bitrate", "Video")
            ),
            category = "Video",
            originalEntry = "24298:ig_android_reels_clips_video_launching_bitrate:0:transcoding_bitrate"
        )
    )

    private val sampleCodeJson = """
    {
      "69718:": ["4: : true", "1: : true"],
      "53982:": ["0: : true"],
      "89103:": ["3: : "],
      "24298:": ["0: : 8000000"],
      "99999:": ["7: : false"],
      "_qe_overrides_": []
    }
    """.trimIndent()

    @Test
    fun testConvertCodeToName() {
        val result = JsonCodeToNameEngine.convert(sampleCodeJson, sampleMapping)
        assertTrue(result.isSuccess)

        val res = result.getOrThrow()
        val pretty = res.convertedJsonPretty

        // 1. Verify mapped universal flags format: code:name
        assertTrue("Output should contain mapped key 69718:ig_android_camera_waterfall",
            pretty.contains("\"69718:ig_android_camera_waterfall\":"))
        assertTrue("Output should contain mapped key 53982:ig_android_reels_swipe_opt",
            pretty.contains("\"53982:ig_android_reels_swipe_opt\":"))
        assertTrue("Output should contain mapped key 89103:ig_android_long_press",
            pretty.contains("\"89103:ig_android_long_press\":"))
        assertTrue("Output should contain mapped key 24298:ig_android_reels_clips_video_launching_bitrate",
            pretty.contains("\"24298:ig_android_reels_clips_video_launching_bitrate\":"))

        // 2. Verify subflag output format: "code: subflag_name: value"
        assertTrue("Output should contain '4: is_recording_waterfall_enabled: true'",
            pretty.contains("\"4: is_recording_waterfall_enabled: true\""))
        assertTrue("Output should contain '1: is_postcapture_waterfall_enabled: true'",
            pretty.contains("\"1: is_postcapture_waterfall_enabled: true\""))
        assertTrue("Output should contain '0: is_enabled: true'",
            pretty.contains("\"0: is_enabled: true\""))
        assertTrue("Output should contain '0: transcoding_bitrate: 8000000'",
            pretty.contains("\"0: transcoding_bitrate: 8000000\""))
        assertTrue("Output should contain '3: direct_tab: '",
            pretty.contains("\"3: direct_tab: \""))

        // 3. Verify unmapped universal code retained
        assertTrue("Unmapped code 99999: should be preserved",
            pretty.contains("\"99999:\":"))

        // 4. Verify _qe_overrides_ preserved
        assertTrue("_qe_overrides_ must be preserved exactly",
            pretty.contains("\"_qe_overrides_\": []"))
    }

    @Test
    fun testStatsAndCoverageCalculation() {
        val result = JsonCodeToNameEngine.convert(sampleCodeJson, sampleMapping)
        val res = result.getOrThrow()
        val stats = res.stats

        // Total universal flags = 5 (69718, 53982, 89103, 24298, 99999) - _qe_overrides_ is non-flag
        assertEquals(5, stats.totalUniversalFlags)
        assertEquals(4, stats.mappedUniversalFlags)
        assertEquals(1, stats.unmappedUniversalCodes)

        // Total subflags = 6 ("4", "1", "0", "3", "0", "7")
        assertEquals(6, stats.totalSubFlags)
        assertEquals(5, stats.mappedSubFlags)
        assertEquals(1, stats.unmappedSubflagCodes)

        // Coverage: 9 of 11 codes mapped = 81.8%
        assertTrue(stats.mappingCoveragePercentage > 80f)
        assertEquals(1, res.unmappedItems.size)
        assertEquals("99999", res.unmappedItems[0].universalCode)
    }

    @Test
    fun testParseSubFlagCodeAndValueHelper() {
        val pair1 = JsonCodeToNameEngine.parseSubFlagCodeAndValue("4: : true")
        assertEquals("4", pair1.first)
        assertEquals("true", pair1.second)

        val pair2 = JsonCodeToNameEngine.parseSubFlagCodeAndValue("0: transcoding_bitrate: 8000000")
        assertEquals("0", pair2.first)
        assertEquals("8000000", pair2.second)

        val pair3 = JsonCodeToNameEngine.parseSubFlagCodeAndValue("3: : ")
        assertEquals("3", pair3.first)
        assertEquals("", pair3.second)

        val pair4 = JsonCodeToNameEngine.parseSubFlagCodeAndValue("15: false")
        assertEquals("15", pair4.first)
        assertEquals("false", pair4.second)
    }

    @Test
    fun testCodeLookup() {
        val lookupUniversal = JsonCodeToNameEngine.lookupCode("89103", sampleMapping)
        assertFalse(lookupUniversal.isEmpty())
        assertTrue(lookupUniversal[0].first.contains("ig_android_long_press"))

        val lookupSub = JsonCodeToNameEngine.lookupCode("4", sampleMapping)
        assertFalse(lookupSub.isEmpty())
        assertTrue(lookupSub.any { it.first.contains("is_recording_waterfall_enabled") })
    }

    @Test
    fun testCompactAndPrettyJsonSerialization() {
        val result = JsonCodeToNameEngine.convert(sampleCodeJson, sampleMapping)
        val res = result.getOrThrow()

        val pretty = res.convertedJsonPretty
        assertTrue(pretty.contains("\n"))
        assertTrue(pretty.contains("  "))

        val compact = res.convertedJsonCompact
        assertFalse(compact.contains("\n"))
    }
}
