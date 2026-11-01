package com.example.data

import com.example.model.ComparisonReport
import com.example.model.ComparisonSummary
import com.example.model.NewFlagItem
import com.example.model.NewSubFlagItem
import com.example.model.RemovedFlagItem
import com.example.model.RemovedSubFlagItem
import com.example.model.SubFlag
import com.example.model.UniversalFlag
import com.example.model.UpdatedFlagCodeItem
import com.example.model.UpdatedSubFlagItem
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MappingCompareEngine {

    /**
     * Compares two lists of UniversalFlag parsed from the old and new mapping files.
     * Uses efficient O(N) indexing for fast comparison across thousands of flags.
     */
    fun compare(
        oldFlags: List<UniversalFlag>,
        newFlags: List<UniversalFlag>,
        oldSourceName: String,
        newSourceName: String
    ): ComparisonReport {
        // Build map by universal flag name, handling duplicates safely
        val oldMap = LinkedHashMap<String, UniversalFlag>(oldFlags.size)
        for (flag in oldFlags) {
            if (!oldMap.containsKey(flag.name)) {
                oldMap[flag.name] = flag
            }
        }

        val newMap = LinkedHashMap<String, UniversalFlag>(newFlags.size)
        for (flag in newFlags) {
            if (!newMap.containsKey(flag.name)) {
                newMap[flag.name] = flag
            }
        }

        val newUniversalFlags = ArrayList<NewFlagItem>()
        val newSubFlags = ArrayList<NewSubFlagItem>()
        val removedUniversalFlags = ArrayList<RemovedFlagItem>()
        val removedSubFlags = ArrayList<RemovedSubFlagItem>()
        val updatedFlagCodes = ArrayList<UpdatedFlagCodeItem>()
        val updatedSubFlags = ArrayList<UpdatedSubFlagItem>()
        var unchangedCount = 0

        // 1. Scan new flags against old flags
        for (newFlag in newMap.values) {
            val oldFlag = oldMap[newFlag.name]
            if (oldFlag == null) {
                // Completely new universal flag
                newUniversalFlags.add(NewFlagItem(flag = newFlag))
            } else {
                var flagHasChanges = false

                // Check if universal flag code changed
                if (oldFlag.code != newFlag.code) {
                    updatedFlagCodes.add(
                        UpdatedFlagCodeItem(
                            flagName = newFlag.name,
                            category = newFlag.category,
                            oldCode = oldFlag.code,
                            newCode = newFlag.code
                        )
                    )
                    flagHasChanges = true
                }

                // Sub-flag comparison within existing universal flag
                val oldSubMap = LinkedHashMap<String, SubFlag>(oldFlag.subFlags.size)
                for (sub in oldFlag.subFlags) {
                    if (!oldSubMap.containsKey(sub.name)) {
                        oldSubMap[sub.name] = sub
                    }
                }

                val newSubMap = LinkedHashMap<String, SubFlag>(newFlag.subFlags.size)
                for (sub in newFlag.subFlags) {
                    if (!newSubMap.containsKey(sub.name)) {
                        newSubMap[sub.name] = sub
                    }
                }

                // Identify new and updated sub-flags
                for (newSub in newSubMap.values) {
                    val oldSub = oldSubMap[newSub.name]
                    if (oldSub == null) {
                        // Newly added sub-flag inside existing flag
                        newSubFlags.add(
                            NewSubFlagItem(
                                parentFlagName = newFlag.name,
                                parentFlagCode = newFlag.code,
                                category = newFlag.category,
                                subFlag = newSub
                            )
                        )
                        flagHasChanges = true
                    } else if (oldSub.code != newSub.code) {
                        // Sub-flag code changed
                        updatedSubFlags.add(
                            UpdatedSubFlagItem(
                                parentFlagName = newFlag.name,
                                category = newFlag.category,
                                subFlagName = newSub.name,
                                oldCode = oldSub.code,
                                newCode = newSub.code
                            )
                        )
                        flagHasChanges = true
                    }
                }

                // Identify removed sub-flags in existing universal flag
                for (oldSub in oldSubMap.values) {
                    if (!newSubMap.containsKey(oldSub.name)) {
                        removedSubFlags.add(
                            RemovedSubFlagItem(
                                parentFlagName = oldFlag.name,
                                parentFlagCode = oldFlag.code,
                                category = oldFlag.category,
                                subFlag = oldSub
                            )
                        )
                        flagHasChanges = true
                    }
                }

                if (!flagHasChanges) {
                    unchangedCount++
                }
            }
        }

        // 2. Scan old flags for removed universal flags
        for (oldFlag in oldMap.values) {
            if (!newMap.containsKey(oldFlag.name)) {
                // Entire universal flag was removed (sub-flags are captured in oldFlag.subFlags)
                removedUniversalFlags.add(RemovedFlagItem(flag = oldFlag))
            }
        }

        val totalNew = newUniversalFlags.size + newSubFlags.size
        val totalRemoved = removedUniversalFlags.size + removedSubFlags.size
        val totalUpdated = updatedFlagCodes.size + updatedSubFlags.size

        val summary = ComparisonSummary(
            oldSourceName = oldSourceName,
            newSourceName = newSourceName,
            totalOldFlags = oldMap.size,
            totalNewFlags = newMap.size,
            newFlagsCount = totalNew,
            removedFlagsCount = totalRemoved,
            updatedFlagsCount = totalUpdated,
            unchangedFlagsCount = unchangedCount
        )

        return ComparisonReport(
            summary = summary,
            newFlags = newUniversalFlags,
            newSubFlags = newSubFlags,
            removedFlags = removedUniversalFlags,
            removedSubFlags = removedSubFlags,
            updatedFlagCodes = updatedFlagCodes,
            updatedSubFlags = updatedSubFlags
        )
    }

    /**
     * Formats the report as clean human-readable text.
     */
    fun formatAsText(report: ComparisonReport): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(report.summary.timestamp))

        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("       MetaTools - Mapping Comparison Report      ")
        sb.appendLine("==================================================")
        sb.appendLine("Comparison Date: $dateStr")
        sb.appendLine("Old Mapping:     ${report.summary.oldSourceName} (${report.summary.totalOldFlags} flags)")
        sb.appendLine("New Mapping:     ${report.summary.newSourceName} (${report.summary.totalNewFlags} flags)")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("SUMMARY:")
        sb.appendLine("  [+] Total New Flags:     ${report.totalNewCount} (${report.newFlags.size} universal, ${report.newSubFlags.size} sub-flags)")
        sb.appendLine("  [-] Total Removed Flags: ${report.totalRemovedCount} (${report.removedFlags.size} universal, ${report.removedSubFlags.size} sub-flags)")
        sb.appendLine("  [*] Total Updated Flags: ${report.totalUpdatedCount} (${report.updatedFlagCodes.size} codes, ${report.updatedSubFlags.size} sub-flag codes)")
        sb.appendLine("  [=] Unchanged Flags:     ${report.summary.unchangedFlagsCount}")
        sb.appendLine("==================================================")
        sb.appendLine()

        // 1. NEW
        sb.appendLine("--- NEW UNIVERSAL FLAGS (${report.newFlags.size}) ---")
        if (report.newFlags.isEmpty()) {
            sb.appendLine("  None")
        } else {
            for (item in report.newFlags) {
                sb.appendLine("  + [${item.flag.code}] ${item.flag.name} (${item.flag.category}, ${item.flag.subFlags.size} sub-flags)")
                for (sub in item.flag.subFlags) {
                    sb.appendLine("      • [${sub.code}] ${sub.name}")
                }
            }
        }
        sb.appendLine()

        sb.appendLine("--- NEW SUB-FLAGS IN EXISTING FLAGS (${report.newSubFlags.size}) ---")
        if (report.newSubFlags.isEmpty()) {
            sb.appendLine("  None")
        } else {
            for (item in report.newSubFlags) {
                sb.appendLine("  + Flag: ${item.parentFlagName} (Code: ${item.parentFlagCode})")
                sb.appendLine("      Added Sub-Flag: [${item.subFlag.code}] ${item.subFlag.name}")
            }
        }
        sb.appendLine()

        // 2. REMOVED
        sb.appendLine("--- REMOVED UNIVERSAL FLAGS (${report.removedFlags.size}) ---")
        if (report.removedFlags.isEmpty()) {
            sb.appendLine("  None")
        } else {
            for (item in report.removedFlags) {
                sb.appendLine("  - [${item.flag.code}] ${item.flag.name} (${item.flag.category}, had ${item.flag.subFlags.size} sub-flags)")
            }
        }
        sb.appendLine()

        sb.appendLine("--- REMOVED SUB-FLAGS (${report.removedSubFlags.size}) ---")
        if (report.removedSubFlags.isEmpty()) {
            sb.appendLine("  None")
        } else {
            for (item in report.removedSubFlags) {
                sb.appendLine("  - Flag: ${item.parentFlagName} (Code: ${item.parentFlagCode})")
                sb.appendLine("      Removed Sub-Flag: [${item.subFlag.code}] ${item.subFlag.name}")
            }
        }
        sb.appendLine()

        // 3. UPDATED
        sb.appendLine("--- UPDATED UNIVERSAL FLAG CODES (${report.updatedFlagCodes.size}) ---")
        if (report.updatedFlagCodes.isEmpty()) {
            sb.appendLine("  None")
        } else {
            for (item in report.updatedFlagCodes) {
                sb.appendLine("  * Flag: ${item.flagName}")
                sb.appendLine("      Old Code: ${item.oldCode}  ->  New Code: ${item.newCode}")
            }
        }
        sb.appendLine()

        sb.appendLine("--- UPDATED SUB-FLAG CODES (${report.updatedSubFlags.size}) ---")
        if (report.updatedSubFlags.isEmpty()) {
            sb.appendLine("  None")
        } else {
            for (item in report.updatedSubFlags) {
                sb.appendLine("  * Flag: ${item.parentFlagName}")
                sb.appendLine("      Sub-Flag: ${item.subFlagName}")
                sb.appendLine("      Old Code: ${item.oldCode}  ->  New Code: ${item.newCode}")
            }
        }
        sb.appendLine()
        sb.appendLine("==================================================")
        return sb.toString()
    }

    fun generateTextReport(report: ComparisonReport): String = formatAsText(report)

    fun generateJsonReport(report: ComparisonReport): String = formatAsJson(report)

    /**
     * Formats the report as structured JSON.
     */
    fun formatAsJson(report: ComparisonReport): String {
        val root = JSONObject()
        val summaryObj = JSONObject().apply {
            put("oldMapping", report.summary.oldSourceName)
            put("newMapping", report.summary.newSourceName)
            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(report.summary.timestamp)))
            put("totalOldFlags", report.summary.totalOldFlags)
            put("totalNewFlags", report.summary.totalNewFlags)
            put("totalNewCount", report.totalNewCount)
            put("totalRemovedCount", report.totalRemovedCount)
            put("totalUpdatedCount", report.totalUpdatedCount)
            put("unchangedCount", report.summary.unchangedFlagsCount)
        }
        root.put("summary", summaryObj)

        val newUniversalArray = JSONArray()
        for (item in report.newFlags) {
            val f = JSONObject().apply {
                put("name", item.flag.name)
                put("code", item.flag.code)
                put("category", item.flag.category)
                val subs = JSONArray()
                for (s in item.flag.subFlags) {
                    subs.put(JSONObject().apply {
                        put("name", s.name)
                        put("code", s.code)
                    })
                }
                put("subFlags", subs)
            }
            newUniversalArray.put(f)
        }
        root.put("newUniversalFlags", newUniversalArray)

        val newSubFlagsArray = JSONArray()
        for (item in report.newSubFlags) {
            newSubFlagsArray.put(JSONObject().apply {
                put("parentFlagName", item.parentFlagName)
                put("parentFlagCode", item.parentFlagCode)
                put("category", item.category)
                put("subFlagName", item.subFlag.name)
                put("subFlagCode", item.subFlag.code)
            })
        }
        root.put("newSubFlags", newSubFlagsArray)

        val removedUniversalArray = JSONArray()
        for (item in report.removedFlags) {
            removedUniversalArray.put(JSONObject().apply {
                put("name", item.flag.name)
                put("code", item.flag.code)
                put("category", item.flag.category)
            })
        }
        root.put("removedUniversalFlags", removedUniversalArray)

        val removedSubFlagsArray = JSONArray()
        for (item in report.removedSubFlags) {
            removedSubFlagsArray.put(JSONObject().apply {
                put("parentFlagName", item.parentFlagName)
                put("parentFlagCode", item.parentFlagCode)
                put("category", item.category)
                put("subFlagName", item.subFlag.name)
                put("subFlagCode", item.subFlag.code)
            })
        }
        root.put("removedSubFlags", removedSubFlagsArray)

        val updatedUniversalArray = JSONArray()
        for (item in report.updatedFlagCodes) {
            updatedUniversalArray.put(JSONObject().apply {
                put("flagName", item.flagName)
                put("category", item.category)
                put("oldCode", item.oldCode)
                put("newCode", item.newCode)
            })
        }
        root.put("updatedUniversalFlagCodes", updatedUniversalArray)

        val updatedSubFlagsArray = JSONArray()
        for (item in report.updatedSubFlags) {
            updatedSubFlagsArray.put(JSONObject().apply {
                put("parentFlagName", item.parentFlagName)
                put("category", item.category)
                put("subFlagName", item.subFlagName)
                put("oldCode", item.oldCode)
                put("newCode", item.newCode)
            })
        }
        root.put("updatedSubFlags", updatedSubFlagsArray)

        return root.toString(2)
    }
}
