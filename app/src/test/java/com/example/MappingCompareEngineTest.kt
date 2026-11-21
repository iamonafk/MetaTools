package com.example

import com.example.data.MappingCompareEngine
import com.example.data.MappingDiagnosticsParser
import com.example.data.MetaConfigParser
import com.example.model.CatalogAvailability
import com.example.model.ExportFileFormat
import com.example.model.ExportReportType
import com.example.model.FlagChangeStatus
import com.example.model.SubFlag
import com.example.model.UniversalFlag
import org.json.JSONObject
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
class MappingCompareEngineTest {

    private fun makeFlag(
        code: String,
        name: String,
        category: String = "Other",
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
            makeFlag("100", "flag_alpha", "Feed", listOf("0" to "is_enabled")),
            makeFlag("200", "flag_beta", "Stories", emptyList())
        )

        val newFlags = listOf(
            makeFlag("100", "flag_alpha", "Feed", listOf("0" to "is_enabled")),
            makeFlag("300", "flag_gamma", "Profile", listOf("1" to "v2_enabled"))
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
        assertEquals(1, report.unchangedFlags.size)
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
                "Feed",
                listOf("0" to "is_enabled", "1" to "legacy_mode")
            )
        )

        val newFlags = listOf(
            makeFlag(
                "105",
                "flag_alpha",
                "Feed",
                listOf("9" to "is_enabled", "2" to "modern_mode")
            )
        )

        val report = MappingCompareEngine.compare(
            oldFlags = oldFlags,
            newFlags = newFlags,
            oldSourceName = "old",
            newSourceName = "new"
        )

        assertEquals(1, report.updatedFlagCodes.size)
        assertEquals("flag_alpha", report.updatedFlagCodes[0].flagName)
        assertEquals("100", report.updatedFlagCodes[0].oldCode)
        assertEquals("105", report.updatedFlagCodes[0].newCode)

        assertEquals(1, report.updatedSubFlags.size)
        assertEquals("is_enabled", report.updatedSubFlags[0].subFlagName)
        assertEquals("0", report.updatedSubFlags[0].oldCode)
        assertEquals("9", report.updatedSubFlags[0].newCode)

        assertEquals(1, report.newSubFlags.size)
        assertEquals("modern_mode", report.newSubFlags[0].subFlag.name)
        assertEquals("2", report.newSubFlags[0].subFlag.code)

        assertEquals(1, report.removedSubFlags.size)
        assertEquals("legacy_mode", report.removedSubFlags[0].subFlag.name)

        assertEquals(0, report.newFlags.size)
        assertEquals(0, report.removedFlags.size)
        assertEquals(0, report.summary.unchangedFlagsCount)
        assertEquals(1, report.summary.updatedUniversalFlagsCount)
    }

    @Test
    fun testDiagnosticsParserValidatesDuplicatesMalformedAndP92() {
        val json = """
            [
              "142330:ig4a_maa_server_driven_headers:0:is_killswitched",
              "130058:ig_loot_android:0:is_enabled:1:profile_toolbar_entrypoint_enabled:3:canvas_item_scale_enabled",
              "112851:p92_custom_threads_flag",
              "142330:ig4a_maa_server_driven_headers:0:is_killswitched",
              "invalid_no_colon_entry",
              "999999:"
            ]
        """.trimIndent()

        val diag = MappingDiagnosticsParser.parseJsonString(json, "test.json")
        assertTrue(diag.isValid)
        assertEquals(6, diag.rawEntryCount)
        assertEquals(4, diag.validEntryCount)
        assertEquals(5, diag.totalSubFlagsCount) // 1 + 3 + 0 + 1
        assertEquals(1, diag.malformedEntries.size)
        assertEquals(1, diag.missingNameEntries.size)
        assertEquals(2, diag.duplicateUniversalEntries.size) // dup name + dup code

        val p92Flag = diag.validFlags.first { it.name == "p92_custom_threads_flag" }
        assertEquals("Threads App/P92", p92Flag.category)

        val lootFlag = diag.validFlags.first { it.name == "ig_loot_android" }
        assertEquals("130058", lootFlag.code)
        assertEquals(3, lootFlag.subFlags.size)
        assertEquals("1", lootFlag.subFlags[1].code)
        assertEquals("profile_toolbar_entrypoint_enabled", lootFlag.subFlags[1].name)
    }

    @Test
    fun testCatalogAnalyticsRenamesAndNormalizedSearch() {
        val oldJson = """
            [
              "1000:ig_old_removed_name:0:enabled",
              "130058:ig_loot_android:0:is_enabled:1:profile_toolbar_entrypoint_enabled"
            ]
        """.trimIndent()
        val newJson = """
            [
              "1000:ig_new_candidate_name:0:enabled",
              "130058:ig_loot_android:0:is_enabled:1:profile_toolbar_entrypoint_enabled:2:extra_flag",
              "5555:p92_barcelona_switch"
            ]
        """.trimIndent()

        val oldDiag = MappingDiagnosticsParser.parseJsonString(oldJson, "old.json")
        val newDiag = MappingDiagnosticsParser.parseJsonString(newJson, "new.json")

        val report = MappingCompareEngine.compareWithDiagnostics(
            oldFlags = oldDiag.validFlags,
            newFlags = newDiag.validFlags,
            oldSourceName = oldDiag.sourceName,
            newSourceName = newDiag.sourceName,
            oldDiagnostics = oldDiag,
            newDiagnostics = newDiag
        )

        // Adding subflag keeps ig_loot_android as UPDATED (not New/Removed)
        assertEquals(1, report.summary.updatedUniversalFlagsCount)
        val lootCatalog = report.catalogFlags.first { it.name == "ig_loot_android" }
        assertEquals(CatalogAvailability.BOTH_UPDATED, lootCatalog.availability)
        assertEquals(FlagChangeStatus.UPDATED, lootCatalog.status)
        assertEquals(3, lootCatalog.subFlags.size)

        // Code 1000 appears in removed & new -> potential rename candidate without merging
        assertEquals(1, report.potentialRenames.size)
        assertEquals("1000", report.potentialRenames[0].sharedCode)
        assertEquals("ig_old_removed_name", report.potentialRenames[0].oldName)
        assertEquals("ig_new_candidate_name", report.potentialRenames[0].newName)

        // 65 feature categories in analytics (all MetaConfigParser.CATEGORY_NAMES minus "See All Flags")
        assertEquals(MetaConfigParser.CATEGORY_NAMES.size - 1, report.categoryAnalytics.size)
        val p92Analytics = report.categoryAnalytics.first { it.categoryName == "Threads App/P92" }
        assertEquals(1, p92Analytics.newFlagsCount)

        // Normalized search ignores underscores vs spaces
        assertTrue(
            MappingCompareEngine.matchesQuery(
                "profile toolbar entrypoint",
                "profile_toolbar_entrypoint_enabled"
            )
        )
        assertFalse(MappingCompareEngine.matchesQuery("nonexistent_flag", "ig_loot_android"))
    }

    @Test
    fun testGenerateTextJsonAndAllExportFormatsAndSnapshotRoundTrip() {
        val oldFlags = listOf(
            makeFlag("100", "flag_alpha", "Feed", listOf("0" to "is_enabled"))
        )
        val newFlags = listOf(
            makeFlag("100", "flag_alpha", "Feed", listOf("0" to "is_enabled", "1" to "new_feature")),
            makeFlag("200", "flag_beta", "Stories", emptyList())
        )

        val report = MappingCompareEngine.compare(
            oldFlags = oldFlags,
            newFlags = newFlags,
            oldSourceName = "old_mapping.json",
            newSourceName = "new_mapping.json"
        )

        // Existing text/json checks
        val textReport = MappingCompareEngine.generateTextReport(report)
        assertTrue(textReport.contains("Mapping Comparison Report"))
        assertTrue(textReport.contains("flag_beta"))
        assertTrue(textReport.contains("new_feature"))

        val jsonReport = MappingCompareEngine.generateJsonReport(report)
        val jsonObj = JSONObject(jsonReport)
        assertNotNull(jsonObj.getJSONObject("summary"))
        assertEquals("old_mapping.json", jsonObj.getJSONObject("summary").getString("oldMapping"))
        assertEquals("new_mapping.json", jsonObj.getJSONObject("summary").getString("newMapping"))
        assertEquals(1, jsonObj.getJSONArray("newUniversalFlags").length())
        assertEquals(1, jsonObj.getJSONArray("newSubFlags").length())

        // All 7 export types produce valid TXT & JSON
        for (exportType in ExportReportType.entries) {
            val txt = MappingCompareEngine.generateExportContent(report, exportType, ExportFileFormat.TXT)
            assertTrue(txt.isNotBlank())
            val js = MappingCompareEngine.generateExportContent(report, exportType, ExportFileFormat.JSON)
            val parsed = JSONObject(js)
            assertNotNull(parsed.getJSONObject("summary"))
        }

        // Snapshot serialize / deserialize roundtrip
        val snapshot = MappingCompareEngine.serializeSnapshot(report)
        val restored = MappingCompareEngine.deserializeSnapshot(snapshot)
        assertNotNull(restored)
        assertEquals(report.summary.oldSourceName, restored!!.summary.oldSourceName)
        assertEquals(report.summary.newSourceName, restored.summary.newSourceName)
        assertEquals(report.catalogFlags.size, restored.catalogFlags.size)
        assertEquals(report.newFlags.size, restored.newFlags.size)
        assertEquals(report.newSubFlags.size, restored.newSubFlags.size)
    }
}
