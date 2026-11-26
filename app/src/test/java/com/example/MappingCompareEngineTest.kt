package com.example

import com.example.data.MappingCompareEngine
import com.example.data.MappingDiagnosticsParser
import com.example.data.MappingIntelligenceAnalyzer
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

        // All 10 export types produce valid TXT & JSON
        for (exportType in ExportReportType.entries) {
            val txt = MappingCompareEngine.generateExportContent(report, exportType, ExportFileFormat.TXT)
            assertTrue("Expected non-blank TXT for $exportType", txt.isNotBlank())
            val js = MappingCompareEngine.generateExportContent(report, exportType, ExportFileFormat.JSON)
            val parsed = JSONObject(js)
            when (exportType) {
                ExportReportType.SINGLE_CATEGORY_EXPORT -> assertTrue(parsed.has("flags"))
                ExportReportType.MAPPING_HEALTH_REPORT -> assertTrue(parsed.has("newHealthScore"))
                else -> assertNotNull(parsed.getJSONObject("summary"))
            }
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

    @Test
    fun testParentChildSubFlagMovementDetection() {
        val oldFlags = listOf(
            makeFlag("100", "ig_old_parent", "Feed", listOf("0" to "shared_sub_enabled")),
            makeFlag("200", "ig_new_parent", "Reels", listOf("0" to "keep_me"))
        )
        val newFlags = listOf(
            makeFlag("100", "ig_old_parent", "Feed", emptyList()),
            makeFlag("200", "ig_new_parent", "Reels", listOf("0" to "keep_me", "4" to "shared_sub_enabled"))
        )

        val report = MappingCompareEngine.compare(
            oldFlags = oldFlags,
            newFlags = newFlags,
            oldSourceName = "v1.json",
            newSourceName = "v2.json"
        )

        assertEquals(1, report.parentChildChanges.size)
        val shift = report.parentChildChanges[0]
        assertEquals("shared_sub_enabled", shift.subFlagName)
        assertEquals("ig_old_parent", shift.oldParentFlagName)
        assertEquals("ig_new_parent", shift.newParentFlagName)
        assertEquals("0", shift.oldSubCode)
        assertEquals("4", shift.newSubCode)
        assertEquals(1, report.summary.parentChildChangesCount)
    }

    @Test
    fun testNamespaceStatsGrowthPercentAndThreadsP92Detection() {
        val oldFlags = listOf(
            makeFlag("100", "ig_feed_card", "Feed"),
            makeFlag("101", "p92_post_composer", "Threads App/P92")
        )
        val newFlags = listOf(
            makeFlag("100", "ig_feed_card", "Feed", listOf("0" to "a", "1" to "b")),
            makeFlag("101", "p92_post_composer", "Threads App/P92"),
            makeFlag("102", "barcelona_reply_thread", "Threads App/P92"),
            makeFlag("103", "ig_story_sticker", "Stories")
        )

        val report = MappingCompareEngine.compare(
            oldFlags = oldFlags,
            newFlags = newFlags,
            oldSourceName = "old.json",
            newSourceName = "new.json"
        )

        // 2 -> 4 universal flags = +100.0%
        assertEquals(100f, report.summary.mappingGrowthPercent, 0.01f)
        // 0 -> 2 subflags = +100.0%
        assertEquals(100f, report.summary.subflagGrowthPercent, 0.01f)
        assertEquals(2, report.summary.threadsP92FlagsCount)

        assertTrue(MappingIntelligenceAnalyzer.isThreadsOrP92("barcelona_reply_thread"))
        assertTrue(MappingIntelligenceAnalyzer.isThreadsOrP92("threads_composer"))
        assertFalse(MappingIntelligenceAnalyzer.isThreadsOrP92("ig_feed_card"))

        val p92Stat = report.namespaceStats.first { it.prefix == "p92_" }
        assertTrue(p92Stat.isThreadsP92)
        assertEquals(1, p92Stat.oldCount)
        assertEquals(1, p92Stat.newCount)

        val igStat = report.namespaceStats.first { it.prefix == "ig_" }
        assertEquals(1, igStat.oldCount)
        assertEquals(2, igStat.newCount)
        assertEquals(1, igStat.addedCount)
        assertEquals(100f, igStat.growthPercent, 0.01f)
    }

    @Test
    fun testEmptySubFlagDiagnosticsCodeConsistencyAndCleanupSuggestions() {
        val json = """
            [
              "100:ig_test_flag:0:sub_a:0:sub_b",
              "200:ig_empty_sub_flag::",
              "250:ig_incomplete_sub_flag:0:",
              "300:ig_dup_flag:1:enabled",
              "300:ig_dup_flag:1:enabled"
            ]
        """.trimIndent()

        val diag = MappingDiagnosticsParser.parseJsonString(json, "diag.json")
        assertTrue(diag.isValid)
        assertEquals(1, diag.emptySubFlagEntries.size)
        assertEquals(1, diag.missingNameEntries.size)
        assertEquals(1, diag.subFlagConsistencyIssues.size)
        assertTrue(diag.completenessPercent in 1..100)

        val suggestions = MappingIntelligenceAnalyzer.buildCleanupSuggestions(null, diag)
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.flagName == "ig_dup_flag" })
    }

    @Test
    fun testMultiFileComparisonMatrixAndExports() {
        val src1 = MappingDiagnosticsParser.parseJsonString(
            """["10:ig_common:0:a", "20:ig_only_one"]""",
            "v1.json"
        )
        val src2 = MappingDiagnosticsParser.parseJsonString(
            """["10:ig_common:0:a", "30:ig_divergent:0:x"]""",
            "v2.json"
        )
        val src3 = MappingDiagnosticsParser.parseJsonString(
            """["11:ig_common:0:a:1:b", "35:ig_divergent:0:x", "40:p92_new"]""",
            "v3.json"
        )

        val multi = MappingIntelligenceAnalyzer.compareMultipleMappings(
            listOf("v1.json" to src1, "v2.json" to src2, "v3.json" to src3)
        )

        assertEquals(4, multi.uniqueFlagsTotal)
        assertEquals(1, multi.commonInAllCount) // ig_common
        assertEquals(2, multi.onlyInOneFileCount) // ig_only_one, p92_new
        assertEquals(2, multi.codeDivergentCount) // ig_common (10 vs 11), ig_divergent (30 vs 35)
        assertEquals(2, multi.pairwiseDeltas.size)
        assertNotNull(multi.baselineVsLatestReport)

        val txtExport = MappingIntelligenceAnalyzer.generateMultiFileExport(multi, ExportFileFormat.TXT)
        assertTrue(txtExport.contains("Multi-File Comparison Matrix"))
        assertTrue(txtExport.contains("ig_common"))

        val jsonExport = JSONObject(
            MappingIntelligenceAnalyzer.generateMultiFileExport(multi, ExportFileFormat.JSON)
        )
        assertEquals(4, jsonExport.getInt("uniqueFlagsTotal"))
        assertEquals(1, jsonExport.getInt("commonInAllCount"))
    }

    @Test
    fun testSessionDiffSearchIndexAndSingleCategoryHealthExports() {
        val oldFlags = listOf(
            makeFlag("100", "ig_feed_video", "Feed", listOf("0" to "autoplay")),
            makeFlag("200", "ig_stories_music", "Stories")
        )
        val newFlagsA = listOf(
            makeFlag("100", "ig_feed_video", "Feed", listOf("0" to "autoplay")),
            makeFlag("300", "ig_reels_hdr", "Reels", listOf("1" to "hdr_enabled"))
        )
        val newFlagsB = listOf(
            makeFlag("105", "ig_feed_video", "Feed", listOf("0" to "autoplay")),
            makeFlag("300", "ig_reels_hdr", "Reels", listOf("1" to "hdr_enabled")),
            makeFlag("400", "p92_threads_badge", "Threads App/P92")
        )

        val reportA = MappingCompareEngine.compare(oldFlags, newFlagsA, "base", "relA")
        val reportB = MappingCompareEngine.compare(oldFlags, newFlagsB, "base", "relB")

        val sessionDiff = MappingIntelligenceAnalyzer.compareHistorySessions(
            sessionALabel = "Session A",
            reportA = reportA,
            sessionBLabel = "Session B",
            reportB = reportB
        )
        assertTrue(sessionDiff.newlyAppearedInB.contains("p92_threads_badge"))
        assertTrue(sessionDiff.statusTransitions.any { it.flagName == "ig_feed_video" })

        val index = MappingIntelligenceAnalyzer.buildSearchIndex(reportB)
        assertTrue(
            index.entries.any {
                MappingIntelligenceAnalyzer.matchesEntryAdvanced(it, "reels hdr")
            }
        )
        assertTrue(
            index.entries.any {
                MappingIntelligenceAnalyzer.matchesEntryAdvanced(it, "autoplay 105")
            }
        )
        assertFalse(
            index.entries.any {
                MappingIntelligenceAnalyzer.matchesEntryAdvanced(it, "nonexistent_token_xyz")
            }
        )

        val singleCatJson = JSONObject(
            MappingIntelligenceAnalyzer.generateSingleCategoryExport(
                report = reportB,
                categoryName = "Reels",
                format = ExportFileFormat.JSON
            )
        )
        assertEquals(1, singleCatJson.getInt("count"))

        val healthTxt = MappingIntelligenceAnalyzer.generateHealthReportExport(reportB, ExportFileFormat.TXT)
        assertTrue(healthTxt.contains("Mapping Health & Intelligence Report"))
        assertTrue(healthTxt.contains("Threads / P92 Flags:"))
    }
}
