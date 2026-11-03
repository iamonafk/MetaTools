package com.example

import com.example.data.FlagAnalysisEngine
import com.example.model.BackupFileType
import com.example.model.SubFlag
import com.example.model.UniversalFlag
import org.json.JSONArray
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
class FlagAnalysisEngineTest {

    private val sampleMapping = listOf(
        UniversalFlag(
            code = "130058",
            name = "ig_loot_android",
            subFlags = listOf(
                SubFlag("0", "is_enabled", "130058", "ig_loot_android", "Direct"),
                SubFlag("1", "profile_toolbar_entrypoint_enabled", "130058", "ig_loot_android", "Direct")
            ),
            category = "Direct",
            originalEntry = "130058:ig_loot_android:0:is_enabled:1:profile_toolbar_entrypoint_enabled"
        ),
        UniversalFlag(
            code = "80737",
            name = "ig_camera_android_gl_canvas_input_preview",
            subFlags = listOf(
                SubFlag("0", "preview_canvas_overlay_enabled", "80737", "ig_camera_android_gl_canvas_input_preview", "Camera"),
                SubFlag("8", "enable_drawable_as_media_composition_track_basel", "80737", "ig_camera_android_gl_canvas_input_preview", "Camera")
            ),
            category = "Camera",
            originalEntry = "80737:ig_camera_android_gl_canvas_input_preview:0:preview_canvas_overlay_enabled:8:enable_drawable_as_media_composition_track_basel"
        )
    )

    private val sampleIBackup = """
    {
      "manifest_version": 2,
      "info": {
        "id": 998877,
        "release_id": "test-uuid-1234",
        "name": "My Custom Backup",
        "author": "Tester",
        "author_id": 12345,
        "version": "42",
        "release_date": "2026-11-03T00:00:00.000Z",
        "changelog": "Initial backup"
      },
      "backup": {
        "130058:": [
          "0: : true",
          "1: : false"
        ],
        "80737:": [
          "0: : false",
          "8: : true"
        ],
        "999999:": [
          "7: : true"
        ],
        "qe_overrides": []
      }
    }
    """.trimIndent()

    private val sampleJsonBackup = """
    {
      "130058:": [
        "0: : false"
      ]
    }
    """.trimIndent()

    @Test
    fun testParseIBackup() {
        val result = FlagAnalysisEngine.parseBackup(sampleIBackup, "test.ibackup", sampleMapping)
        assertTrue(result.isSuccess)

        val state = result.getOrThrow()
        assertEquals(BackupFileType.IBACKUP, state.fileType)
        assertEquals("test.ibackup", state.fileName)
        assertEquals("My Custom Backup", state.info?.name)
        assertEquals(2, state.manifestVersion)

        // 3 universal flags: 130058, 80737, and 999999 (qe_overrides should not be counted as a flag)
        assertEquals(3, state.flags.size)

        // Verify resolved flag
        val lootFlag = state.flags.firstOrNull { it.code == "130058" }
        assertNotNull(lootFlag)
        assertEquals("ig_loot_android", lootFlag!!.name)
        assertEquals("Direct", lootFlag.category)
        assertFalse(lootFlag.isUnknown)
        assertEquals(2, lootFlag.subFlags.size)

        val sub0 = lootFlag.subFlags.first { it.subCode == "0" }
        assertEquals("is_enabled", sub0.name)
        assertTrue(sub0.isEnabled)
        assertFalse(sub0.isUnknown)

        val sub1 = lootFlag.subFlags.first { it.subCode == "1" }
        assertEquals("profile_toolbar_entrypoint_enabled", sub1.name)
        assertFalse(sub1.isEnabled)

        // Verify unmapped flag
        val unknownFlag = state.flags.firstOrNull { it.code == "999999" }
        assertNotNull(unknownFlag)
        assertTrue(unknownFlag!!.isUnknown)
        assertEquals("Unknown Flag (999999)", unknownFlag.name)
        val unknownSub = unknownFlag.subFlags.first()
        assertTrue(unknownSub.isUnknown)
        assertEquals("Unknown Sub-Flag (7)", unknownSub.name)
    }

    @Test
    fun testParseJsonBackup() {
        val result = FlagAnalysisEngine.parseBackup(sampleJsonBackup, "normal.json", sampleMapping)
        assertTrue(result.isSuccess)

        val state = result.getOrThrow()
        assertEquals(BackupFileType.JSON, state.fileType)
        assertEquals(1, state.flags.size)
        assertEquals("ig_loot_android", state.flags[0].name)
        assertFalse(state.flags[0].subFlags[0].isEnabled)
    }

    @Test
    fun testToggleSubFlagAndTracking() {
        val result = FlagAnalysisEngine.parseBackup(sampleIBackup, "test.ibackup", sampleMapping)
        val state = result.getOrThrow()

        // Toggle subflag 1 of 130058 from false to true
        val updatedFlags = FlagAnalysisEngine.toggleSubFlagState(state.flags, "130058", "1")
        val lootFlag = updatedFlags.first { it.code == "130058" }
        val sub1 = lootFlag.subFlags.first { it.subCode == "1" }

        assertTrue("Subflag 1 should now be enabled", sub1.isEnabled)
        assertTrue("Subflag 1 should be marked modified", sub1.isModified)

        val modifiedList = FlagAnalysisEngine.getModifiedFlags(updatedFlags)
        assertEquals(1, modifiedList.size)
        assertEquals("130058", modifiedList[0].universalCode)
        assertEquals("1", modifiedList[0].subCode)
        assertFalse("Original state was false", modifiedList[0].originalState)
        assertTrue("Current state is true", modifiedList[0].currentState)

        // Revert change
        val revertedFlags = FlagAnalysisEngine.resetSubFlagToOriginal(updatedFlags, "130058", "1")
        val revertedSub1 = revertedFlags.first { it.code == "130058" }.subFlags.first { it.subCode == "1" }
        assertFalse("Subflag should be reverted back to false", revertedSub1.isEnabled)
        assertFalse("Subflag should not be modified after revert", revertedSub1.isModified)
        assertEquals(0, FlagAnalysisEngine.getModifiedFlags(revertedFlags).size)
    }

    @Test
    fun testCategoryBatchEnableAndDisable() {
        val result = FlagAnalysisEngine.parseBackup(sampleIBackup, "test.ibackup", sampleMapping)
        val state = result.getOrThrow()

        // Disable all flags in Direct category
        val disabledDirect = FlagAnalysisEngine.setCategoryFlagsState(state.flags, "Direct", false)
        val loot = disabledDirect.first { it.code == "130058" }
        assertTrue(loot.subFlags.all { !it.isEnabled })

        // Enable all flags in Direct category
        val enabledDirect = FlagAnalysisEngine.setCategoryFlagsState(state.flags, "Direct", true)
        val lootEnabled = enabledDirect.first { it.code == "130058" }
        assertTrue(lootEnabled.subFlags.all { it.isEnabled })
    }

    @Test
    fun testLosslessExportPreservesStructure() {
        val result = FlagAnalysisEngine.parseBackup(sampleIBackup, "test.ibackup", sampleMapping)
        val state = result.getOrThrow()

        // Toggle subflag 1 from false to true
        val updatedFlags = FlagAnalysisEngine.updateSubFlagState(state.flags, "130058", "1", true)
        val updatedState = state.copy(flags = updatedFlags)

        val exportResult = FlagAnalysisEngine.generateExportContent(updatedState)
        assertTrue(exportResult.isSuccess)
        val exportJson = exportResult.getOrThrow()

        val root = JSONObject(exportJson)
        // Verify manifest_version & info preserved exactly
        assertEquals(2, root.getInt("manifest_version"))
        val info = root.getJSONObject("info")
        assertEquals(998877L, info.getLong("id"))
        assertEquals("test-uuid-1234", info.getString("release_id"))
        assertEquals("My Custom Backup", info.getString("name"))

        // Verify backup object
        val backup = root.getJSONObject("backup")
        assertTrue("qe_overrides must be preserved", backup.has("qe_overrides"))

        val lootArray = backup.getJSONArray("130058:")
        assertEquals(2, lootArray.length())
        assertEquals("0: : true", lootArray.getString(0))
        assertEquals("1: : true", lootArray.getString(1)) // was false, now true!

        // Verify unmapped flag preserved
        val unmappedArray = backup.getJSONArray("999999:")
        assertEquals("7: : true", unmappedArray.getString(0))
    }
}
