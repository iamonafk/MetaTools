package com.example

import com.example.data.FlagInformationParser
import com.example.model.DetectedFlagType
import org.json.JSONArray
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
class FlagInformationParserTest {

    @Test
    fun testParseExampleInput() {
        val result = FlagInformationParser.parse(FlagInformationParser.EXAMPLE_INPUT)
        assertEquals(1, result.size)

        val flag = result[0]
        assertEquals("Instagram", flag.application)
        assertEquals("modern_ui_edge_to_edge", flag.universe)
        assertEquals("126024", flag.stableId)

        assertEquals(1, flag.parameters.size)
        val param = flag.parameters[0]
        assertEquals("is_bottom_enabled_for_main_activity", param.name)
        assertEquals("false", param.value)
        assertTrue(param.isSubFlag)

        assertTrue(flag.useDescription.startsWith("Disable this subflag to fix the Reels playback bug"))
        assertEquals("—435.0.0.37.76", flag.appVersion)
        assertEquals("Instagram Version", flag.versionLabel)

        // Type detection
        assertEquals(DetectedFlagType.SUB_FLAG, flag.detectedType)
        assertTrue(flag.missingFields.isEmpty())
    }

    @Test
    fun testUniversalFlagAndMultipleParamsDetection() {
        // Universal Flag with single non-subflag param
        val universalText = """
            Application: Instagram
            Universe: test_universal_flag
            Stable Id: 998877
            Parameters:
            sample_config: true
            Use: Testing universal flag
            Instagram Version: 1.0
        """.trimIndent()

        val uResult = FlagInformationParser.parse(universalText)
        assertEquals(1, uResult.size)
        assertEquals(DetectedFlagType.UNIVERSAL_FLAG, uResult[0].detectedType)
        assertFalse(uResult[0].parameters[0].isSubFlag)

        // Flag with multiple parameters
        val multiParamText = """
            Application: Threads
            Universe: threads_composer_v2
            Stable Id: 445566
            Parameters:
            is_enabled: true (Sub Flag)
            max_media_count: 10
            cache_duration_ms: 3000
            Use: Multi-param testing
            Threads Version: 100.0
        """.trimIndent()

        val mResult = FlagInformationParser.parse(multiParamText)
        assertEquals(1, mResult.size)
        assertEquals(DetectedFlagType.MULTIPLE_PARAMETERS, mResult[0].detectedType)
        assertEquals(3, mResult[0].parameters.size)
        assertEquals("Threads", mResult[0].application)
        assertEquals("Threads Version", mResult[0].versionLabel)
        assertEquals("100.0", mResult[0].appVersion)
    }

    @Test
    fun testMissingFieldsDetection() {
        val partialText = """
            Universe: partial_flag
            Parameters:
            some_param: false
        """.trimIndent()

        val result = FlagInformationParser.parse(partialText)
        assertEquals(1, result.size)
        val flag = result[0]
        val missing = flag.missingFields

        assertTrue("Should report missing Application", missing.contains("Application"))
        assertTrue("Should report missing Stable Id", missing.contains("Stable Id"))
        assertTrue("Should report missing Use", missing.contains("Use"))
        assertTrue("Should report missing Version", missing.contains("Version"))
        assertFalse("Universe should not be missing", missing.contains("Universe"))
    }

    @Test
    fun testMultipleFlagsAndDuplicateDetection() {
        val multiFlagsText = """
            Application: Instagram
            Universe: duplicate_universe
            Stable Id: 111111
            Parameters:
            flag_a: true
            Use: First instance
            Instagram Version: 1.0

            ---

            Application: Instagram
            Universe: duplicate_universe
            Stable Id: 222222
            Parameters:
            flag_b: false
            Use: Second instance with duplicate universe
            Instagram Version: 1.0
        """.trimIndent()

        val result = FlagInformationParser.parse(multiFlagsText)
        assertEquals(2, result.size)

        assertTrue(result[0].isDuplicateUniverse)
        assertTrue(result[1].isDuplicateUniverse)
        assertFalse(result[0].isDuplicateStableId)
        assertFalse(result[1].isDuplicateStableId)
    }

    @Test
    fun testStandardFormatAndJsonExport() {
        val result = FlagInformationParser.parse(FlagInformationParser.EXAMPLE_INPUT)
        val flag = result[0]

        val standardText = flag.toStandardTextFormat()
        assertTrue(standardText.contains("Application: Instagram"))
        assertTrue(standardText.contains("Universe: modern_ui_edge_to_edge"))
        assertTrue(standardText.contains("Stable Id: 126024"))
        assertTrue(standardText.contains("is_bottom_enabled_for_main_activity: false (Sub Flag)"))
        assertTrue(standardText.contains("Instagram Version: —435.0.0.37.76"))

        // JSON export
        val json = FlagInformationParser.exportToJson(result)
        val array = JSONArray(json)
        assertEquals(1, array.length())
        val obj = array.getJSONObject(0)
        assertEquals("Instagram", obj.getString("application"))
        assertEquals("modern_ui_edge_to_edge", obj.getString("universe"))
        assertEquals("126024", obj.getString("stable_id"))
        assertEquals("Sub Flag", obj.getString("flag_type"))
    }
}
