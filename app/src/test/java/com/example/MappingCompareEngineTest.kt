package com.example

import com.example.data.MappingCompareEngine
import com.example.model.SubFlag
import com.example.model.UniversalFlag
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MappingCompareEngineTest {

    private fun makeFlag(
        code: String,
        name: String,
        category: String = "General",
        subFlags: List<Pair<String, String>> = emptyList()
    ): UniversalFlag {
        val subs = subFlags.map { (subCode, subName) ->
            SubFlag(
                code = subCode,
                name = subName,
                parentFlagCode = code,
                parentFlagName = name,
                category = category
            )
        }
        return UniversalFlag(
            code = code,
            name = name,
            subFlags = subs,
            category = category,
            originalEntry = "$code:$name"
        )
    }

    @Test
    fun testDetectNewAndRemovedUniversalFlags() {
        val oldFlags = listOf(
            makeFlag("100", "flag_alpha", "Alpha", listOf("0" to "is_enabled")),
            makeFlag("200", "flag_beta", "Beta", emptyList())
        )

        val newFlags = listOf(
            makeFlag("100", "flag_alpha", "Alpha", listOf("0" to "is_enabled")),
            makeFlag("300", "flag_gamma", "Gamma", listOf("1" to "v2_enabled"))
        )

        val report = MappingCompareEngine.compare(
            oldFlags = oldFlags,
            newFlags = newFlags,
            oldSourceName = "v1.json",
            newSourceName = "v2.json"
        )

        assertEquals(1, report.newFlags.size)
        assertEquals("flag_gamma", report.newFlags[0].flag.name)
        assertEquals("300", report.newFlags[0].flag.code)

        assertEquals(1, report.removedFlags.size)
        assertEquals("flag_beta", report.removedFlags[0].flag.name)

        assertEquals(1, report.summary.unchangedFlagsCount)
        assertEquals(0, report.updatedFlagCodes.size)
        assertEquals(0, report.newSubFlags.size)
        assertEquals(0, report.removedSubFlags.size)
    }

    @Test
    fun testDetectUpdatedFlagCodeAndSubFlagChanges() {
        val oldFlags = listOf(
            makeFlag(
                "100",
                "flag_alpha",
                "Alpha",
                listOf("0" to "is_enabled", "1" to "legacy_mode")
            )
        )

        val newFlags = listOf(
            makeFlag(
                "105", // updated flag code
                "flag_alpha",
                "Alpha",
                listOf("9" to "is_enabled", "2" to "modern_mode") // "0"->"9" updated, "2" added, "1" removed
            )
        )

        val report = MappingCompareEngine.compare(
            oldFlags = oldFlags,
            newFlags = newFlags,
            oldSourceName = "old",
            newSourceName = "new"
        )

        // Universal code update
        assertEquals(1, report.updatedFlagCodes.size)
        assertEquals("flag_alpha", report.updatedFlagCodes[0].flagName)
        assertEquals("100", report.updatedFlagCodes[0].oldCode)
        assertEquals("105", report.updatedFlagCodes[0].newCode)

        // Sub-flag code update
        assertEquals(1, report.updatedSubFlags.size)
        assertEquals("is_enabled", report.updatedSubFlags[0].subFlagName)
        assertEquals("0", report.updatedSubFlags[0].oldCode)
        assertEquals("9", report.updatedSubFlags[0].newCode)

        // New sub-flag
        assertEquals(1, report.newSubFlags.size)
        assertEquals("modern_mode", report.newSubFlags[0].subFlag.name)
        assertEquals("2", report.newSubFlags[0].subFlag.code)

        // Removed sub-flag
        assertEquals(1, report.removedSubFlags.size)
        assertEquals("legacy_mode", report.removedSubFlags[0].subFlag.name)

        // No new/removed entire flags
        assertEquals(0, report.newFlags.size)
        assertEquals(0, report.removedFlags.size)
        assertEquals(0, report.summary.unchangedFlagsCount)
    }

    @Test
    fun testGenerateTextAndJsonReports() {
        val oldFlags = listOf(
            makeFlag("100", "flag_alpha", "Alpha", listOf("0" to "is_enabled"))
        )

        val newFlags = listOf(
            makeFlag(
                "100",
                "flag_alpha",
                "Alpha",
                listOf("0" to "is_enabled", "1" to "new_feature")
            ),
            makeFlag("200", "flag_beta", "Beta", emptyList())
        )

        val report = MappingCompareEngine.compare(
            oldFlags = oldFlags,
            newFlags = newFlags,
            oldSourceName = "old_mapping.json",
            newSourceName = "new_mapping.json"
        )

        // Text Report
        val textReport = MappingCompareEngine.generateTextReport(report)
        assertTrue(textReport.contains("Mapping Comparison Report"))
        assertTrue(textReport.contains("flag_beta"))
        assertTrue(textReport.contains("new_feature"))

        // JSON Report
        val jsonReport = MappingCompareEngine.generateJsonReport(report)
        val jsonObj = JSONObject(jsonReport)
        assertNotNull(jsonObj.getJSONObject("summary"))
        assertEquals("old_mapping.json", jsonObj.getJSONObject("summary").getString("oldMapping"))
        assertEquals("new_mapping.json", jsonObj.getJSONObject("summary").getString("newMapping"))
        assertEquals(1, jsonObj.getJSONArray("newUniversalFlags").length())
        assertEquals(1, jsonObj.getJSONArray("newSubFlags").length())
    }
}
