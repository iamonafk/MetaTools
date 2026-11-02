package com.example

import com.example.data.IBackupConverterEngine
import com.example.model.ConversionMode
import com.example.model.IBackupInfo
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
class IBackupConverterEngineTest {

    private val sampleIBackup = """
    {
      "manifest_version": 2,
      "info": {
        "id": 295163811,
        "release_id": "899fb7bc-1063-4622-9b6b-378609e128ce",
        "name": "Askxtylishh Backup 🚀",
        "author": "Askxtylishh",
        "author_id": 50841662,
        "version": "40",
        "release_date": "2026-09-05T15:59:38.718Z",
        "changelog": "Backup changelog..."
      },
      "backup": {
        "50769:": ["23: : false"],
        "103969:": ["0: : true"],
        "73763:": [
          "0: : true",
          "4: : true",
          "5: : true"
        ],
        "qe_overrides": []
      }
    }
    """.trimIndent()

    @Test
    fun testConvertIbackupToJson() {
        val result = IBackupConverterEngine.convertIbackupToJson(sampleIBackup)
        assertTrue(result.isSuccess)

        val (jsonString, preview, suggestedFileName) = result.getOrThrow()

        // Verify JSON string parses as JSONObject
        val parsedJson = JSONObject(jsonString)
        assertFalse("Exported JSON should not contain manifest_version", parsedJson.has("manifest_version"))
        assertFalse("Exported JSON should not contain info", parsedJson.has("info"))
        assertTrue("Exported JSON must contain universal flag 50769:", parsedJson.has("50769:"))
        assertTrue("Exported JSON must contain universal flag 103969:", parsedJson.has("103969:"))
        assertTrue("Exported JSON must contain universal flag 73763:", parsedJson.has("73763:"))
        assertTrue("Exported JSON must contain qe_overrides", parsedJson.has("qe_overrides"))

        // Check preview calculations
        assertEquals("Askxtylishh Backup 🚀", preview.backupName)
        assertEquals(3, preview.totalUniversalFlags)
        assertEquals(5, preview.totalSubFlagEntries)
        assertEquals(4, preview.totalBackupEntries)
        assertEquals(ConversionMode.IBACKUP_TO_JSON, preview.mode)

        // Check suggested file name
        assertTrue(suggestedFileName.endsWith(".json"))
    }

    @Test
    fun testConvertJsonToIbackup() {
        val rawBackupJson = """
        {
          "50769:": ["23: : false"],
          "103969:": ["0: : true"],
          "73763:": [
            "0: : true",
            "4: : true",
            "5: : true"
          ],
          "qe_overrides": []
        }
        """.trimIndent()

        val info = IBackupInfo(
            id = 12345678L,
            releaseId = "custom-uuid-1234",
            name = "Test Backup Unit",
            author = "Tester",
            authorId = 9999L,
            version = "1.0",
            releaseDate = "2026-11-02T12:00:00.000Z",
            changelog = "Initial test release"
        )

        val result = IBackupConverterEngine.convertJsonToIbackup(rawBackupJson, info)
        assertTrue(result.isSuccess)

        val (ibackupString, preview, suggestedFileName) = result.getOrThrow()
        val parsed = JSONObject(ibackupString)

        assertEquals(2, parsed.getInt("manifest_version"))
        assertTrue(parsed.has("info"))
        assertTrue(parsed.has("backup"))

        val infoObj = parsed.getJSONObject("info")
        assertEquals(12345678L, infoObj.getLong("id"))
        assertEquals("custom-uuid-1234", infoObj.getString("release_id"))
        assertEquals("Test Backup Unit", infoObj.getString("name"))
        assertEquals("Tester", infoObj.getString("author"))
        assertEquals("1.0", infoObj.getString("version"))

        val backupObj = parsed.getJSONObject("backup")
        assertTrue(backupObj.has("50769:"))
        assertEquals(1, backupObj.getJSONArray("50769:").length())
        assertEquals("23: : false", backupObj.getJSONArray("50769:").getString(0))

        assertEquals("Test Backup Unit", preview.backupName)
        assertEquals(3, preview.totalUniversalFlags)
        assertEquals(5, preview.totalSubFlagEntries)
        assertEquals(4, preview.totalBackupEntries)
        assertEquals(ConversionMode.JSON_TO_IBACKUP, preview.mode)
        assertEquals("Test_Backup_Unit.ibackup", suggestedFileName)
    }

    @Test
    fun testMalformedJsonHandling() {
        val invalidJson = "{ this is not valid json : [[ "
        val result = IBackupConverterEngine.convertIbackupToJson(invalidJson)
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun testEmptyInputHandling() {
        val result = IBackupConverterEngine.convertIbackupToJson("")
        assertTrue(result.isFailure)
    }
}
