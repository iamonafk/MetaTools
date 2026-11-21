package com.example.data

import com.example.model.CatalogAvailability
import com.example.model.CatalogFlagItem
import com.example.model.CatalogSubFlagDetail
import com.example.model.CategoryAnalyticsItem
import com.example.model.ComparisonReport
import com.example.model.ComparisonSummary
import com.example.model.DiagnosticIssue
import com.example.model.DiagnosticIssueType
import com.example.model.ExportFileFormat
import com.example.model.ExportReportType
import com.example.model.FlagChangeStatus
import com.example.model.MappingParseDiagnostics
import com.example.model.NewFlagItem
import com.example.model.NewSubFlagItem
import com.example.model.PotentialRenameCandidate
import com.example.model.RemovedFlagItem
import com.example.model.RemovedSubFlagItem
import com.example.model.SubFlag
import com.example.model.UnchangedFlagItem
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
     * Backwards-compatible comparison entrypoint.
     */
    fun compare(
        oldFlags: List<UniversalFlag>,
        newFlags: List<UniversalFlag>,
        oldSourceName: String,
        newSourceName: String
    ): ComparisonReport {
        return compareWithDiagnostics(
            oldFlags = oldFlags,
            newFlags = newFlags,
            oldSourceName = oldSourceName,
            newSourceName = newSourceName,
            oldDiagnostics = null,
            newDiagnostics = null
        )
    }

    /**
     * Full-featured comparison engine producing diff lists, unchanged flags,
     * potential rename candidates, unified 66-category catalog, category analytics,
     * and validation/health metrics.
     */
    fun compareWithDiagnostics(
        oldFlags: List<UniversalFlag>,
        newFlags: List<UniversalFlag>,
        oldSourceName: String,
        newSourceName: String,
        oldDiagnostics: MappingParseDiagnostics? = null,
        newDiagnostics: MappingParseDiagnostics? = null
    ): ComparisonReport {
        val oldMap = LinkedHashMap<String, UniversalFlag>(oldFlags.size)
        val duplicateOldNames = HashSet<String>()
        for (flag in oldFlags) {
            if (oldMap.containsKey(flag.name)) {
                duplicateOldNames.add(flag.name)
            } else {
                oldMap[flag.name] = flag
            }
        }

        val newMap = LinkedHashMap<String, UniversalFlag>(newFlags.size)
        val duplicateNewNames = HashSet<String>()
        for (flag in newFlags) {
            if (newMap.containsKey(flag.name)) {
                duplicateNewNames.add(flag.name)
            } else {
                newMap[flag.name] = flag
            }
        }

        val newUniversalFlags = ArrayList<NewFlagItem>()
        val newSubFlags = ArrayList<NewSubFlagItem>()
        val removedUniversalFlags = ArrayList<RemovedFlagItem>()
        val removedSubFlags = ArrayList<RemovedSubFlagItem>()
        val updatedFlagCodes = ArrayList<UpdatedFlagCodeItem>()
        val updatedSubFlags = ArrayList<UpdatedSubFlagItem>()
        val unchangedFlags = ArrayList<UnchangedFlagItem>()
        val potentialRenames = ArrayList<PotentialRenameCandidate>()
        val catalogFlags = ArrayList<CatalogFlagItem>(maxOf(oldMap.size, newMap.size))

        var updatedUniversalFlagParentsCount = 0
        var oldSubFlagsTotal = 0
        for (f in oldMap.values) oldSubFlagsTotal += f.subFlags.size
        var newSubFlagsTotal = 0
        for (f in newMap.values) newSubFlagsTotal += f.subFlags.size

        // 1. Process all flags present in New Mapping
        for (newFlag in newMap.values) {
            val oldFlag = oldMap[newFlag.name]
            if (oldFlag == null) {
                newUniversalFlags.add(NewFlagItem(flag = newFlag))
                catalogFlags.add(
                    CatalogFlagItem(
                        name = newFlag.name,
                        oldCode = null,
                        newCode = newFlag.code,
                        category = newFlag.category,
                        availability = CatalogAvailability.NEW_ONLY,
                        status = FlagChangeStatus.NEW,
                        subFlags = newFlag.subFlags.map { sub ->
                            CatalogSubFlagDetail(
                                name = sub.name,
                                oldCode = null,
                                newCode = sub.code,
                                status = FlagChangeStatus.NEW
                            )
                        },
                        oldSubFlagCount = 0,
                        newSubFlagCount = newFlag.subFlags.size,
                        isDuplicateInOld = false,
                        isDuplicateInNew = duplicateNewNames.contains(newFlag.name),
                        isUnknownCategory = newFlag.category == "Other"
                    )
                )
            } else {
                var flagHasChanges = false

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

                val oldSubMap = LinkedHashMap<String, SubFlag>(oldFlag.subFlags.size)
                for (sub in oldFlag.subFlags) {
                    if (!oldSubMap.containsKey(sub.name)) oldSubMap[sub.name] = sub
                }

                val newSubMap = LinkedHashMap<String, SubFlag>(newFlag.subFlags.size)
                for (sub in newFlag.subFlags) {
                    if (!newSubMap.containsKey(sub.name)) newSubMap[sub.name] = sub
                }

                val mergedSubDetails = ArrayList<CatalogSubFlagDetail>(maxOf(oldSubMap.size, newSubMap.size))
                val addedSubsInParent = ArrayList<SubFlag>()
                val removedSubsInParent = ArrayList<SubFlag>()

                for (newSub in newSubMap.values) {
                    val oldSub = oldSubMap[newSub.name]
                    if (oldSub == null) {
                        newSubFlags.add(
                            NewSubFlagItem(
                                parentFlagName = newFlag.name,
                                parentFlagCode = newFlag.code,
                                category = newFlag.category,
                                subFlag = newSub
                            )
                        )
                        addedSubsInParent.add(newSub)
                        mergedSubDetails.add(
                            CatalogSubFlagDetail(
                                name = newSub.name,
                                oldCode = null,
                                newCode = newSub.code,
                                status = FlagChangeStatus.NEW
                            )
                        )
                        flagHasChanges = true
                    } else if (oldSub.code != newSub.code) {
                        updatedSubFlags.add(
                            UpdatedSubFlagItem(
                                parentFlagName = newFlag.name,
                                category = newFlag.category,
                                subFlagName = newSub.name,
                                oldCode = oldSub.code,
                                newCode = newSub.code
                            )
                        )
                        mergedSubDetails.add(
                            CatalogSubFlagDetail(
                                name = newSub.name,
                                oldCode = oldSub.code,
                                newCode = newSub.code,
                                status = FlagChangeStatus.UPDATED
                            )
                        )
                        flagHasChanges = true
                    } else {
                        mergedSubDetails.add(
                            CatalogSubFlagDetail(
                                name = newSub.name,
                                oldCode = oldSub.code,
                                newCode = newSub.code,
                                status = FlagChangeStatus.UNCHANGED
                            )
                        )
                    }
                }

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
                        removedSubsInParent.add(oldSub)
                        mergedSubDetails.add(
                            CatalogSubFlagDetail(
                                name = oldSub.name,
                                oldCode = oldSub.code,
                                newCode = null,
                                status = FlagChangeStatus.REMOVED
                            )
                        )
                        flagHasChanges = true
                    }
                }

                // Detect potential subflag rename candidates within this parent flag (same subflag code)
                if (addedSubsInParent.isNotEmpty() && removedSubsInParent.isNotEmpty()) {
                    val removedByCode = removedSubsInParent.groupBy { it.code }
                    for (addedSub in addedSubsInParent) {
                        val matches = removedByCode[addedSub.code].orEmpty()
                        for (rem in matches) {
                            potentialRenames.add(
                                PotentialRenameCandidate(
                                    scope = "SUBFLAG",
                                    sharedCode = addedSub.code,
                                    oldName = rem.name,
                                    newName = addedSub.name,
                                    parentFlagName = newFlag.name,
                                    category = newFlag.category
                                )
                            )
                        }
                    }
                }

                if (flagHasChanges) {
                    updatedUniversalFlagParentsCount++
                } else {
                    unchangedFlags.add(UnchangedFlagItem(flag = newFlag))
                }

                catalogFlags.add(
                    CatalogFlagItem(
                        name = newFlag.name,
                        oldCode = oldFlag.code,
                        newCode = newFlag.code,
                        category = newFlag.category,
                        availability = if (flagHasChanges) CatalogAvailability.BOTH_UPDATED else CatalogAvailability.BOTH_IDENTICAL,
                        status = if (flagHasChanges) FlagChangeStatus.UPDATED else FlagChangeStatus.UNCHANGED,
                        subFlags = mergedSubDetails,
                        oldSubFlagCount = oldSubMap.size,
                        newSubFlagCount = newSubMap.size,
                        isDuplicateInOld = duplicateOldNames.contains(oldFlag.name),
                        isDuplicateInNew = duplicateNewNames.contains(newFlag.name),
                        isUnknownCategory = newFlag.category == "Other"
                    )
                )
            }
        }

        // 2. Process flags only in Old Mapping (Removed Universal Flags)
        for (oldFlag in oldMap.values) {
            if (!newMap.containsKey(oldFlag.name)) {
                removedUniversalFlags.add(RemovedFlagItem(flag = oldFlag))
                catalogFlags.add(
                    CatalogFlagItem(
                        name = oldFlag.name,
                        oldCode = oldFlag.code,
                        newCode = null,
                        category = oldFlag.category,
                        availability = CatalogAvailability.OLD_ONLY,
                        status = FlagChangeStatus.REMOVED,
                        subFlags = oldFlag.subFlags.map { sub ->
                            CatalogSubFlagDetail(
                                name = sub.name,
                                oldCode = sub.code,
                                newCode = null,
                                status = FlagChangeStatus.REMOVED
                            )
                        },
                        oldSubFlagCount = oldFlag.subFlags.size,
                        newSubFlagCount = 0,
                        isDuplicateInOld = duplicateOldNames.contains(oldFlag.name),
                        isDuplicateInNew = false,
                        isUnknownCategory = oldFlag.category == "Other"
                    )
                )
            }
        }

        // 3. Detect potential universal flag rename candidates (same universal code between removed & new)
        if (removedUniversalFlags.isNotEmpty() && newUniversalFlags.isNotEmpty()) {
            val removedByCode = removedUniversalFlags.groupBy { it.flag.code }
            for (newItem in newUniversalFlags) {
                val matchedOlds = removedByCode[newItem.flag.code].orEmpty()
                for (remItem in matchedOlds) {
                    potentialRenames.add(
                        PotentialRenameCandidate(
                            scope = "UNIVERSAL",
                            sharedCode = newItem.flag.code,
                            oldName = remItem.flag.name,
                            newName = newItem.flag.name,
                            parentFlagName = null,
                            category = newItem.flag.category
                        )
                    )
                }
            }
        }

        // 4. Category Analytics across the 66 MetaTools categories (without double-counting duplicates)
        val categoryAnalytics = buildCategoryAnalytics(catalogFlags)

        // 5. Combine diagnostics counts
        val oldUnknownCount = oldDiagnostics?.unknownCategoryCount
            ?: oldMap.values.count { it.category == "Other" }
        val newUnknownCount = newDiagnostics?.unknownCategoryCount
            ?: newMap.values.count { it.category == "Other" }
        val unknownCategoryUniqueCount = catalogFlags.count { it.isUnknownCategory }

        val duplicateEntriesCount = if (oldDiagnostics != null || newDiagnostics != null) {
            (oldDiagnostics?.duplicateCount ?: 0) + (newDiagnostics?.duplicateCount ?: 0)
        } else {
            duplicateOldNames.size + duplicateNewNames.size
        }
        val invalidEntriesCount = (oldDiagnostics?.invalidCount ?: 0) + (newDiagnostics?.invalidCount ?: 0)

        val totalNew = newUniversalFlags.size + newSubFlags.size
        val totalRemoved = removedUniversalFlags.size + removedSubFlags.size
        val totalUpdated = updatedFlagCodes.size + updatedSubFlags.size

        val summary = ComparisonSummary(
            oldSourceName = oldSourceName,
            newSourceName = newSourceName,
            totalOldFlags = oldMap.size,
            totalNewFlags = newMap.size,
            totalOldSubFlags = oldSubFlagsTotal,
            totalNewSubFlags = newSubFlagsTotal,
            newFlagsCount = totalNew,
            removedFlagsCount = totalRemoved,
            updatedFlagsCount = totalUpdated,
            unchangedFlagsCount = unchangedFlags.size,
            newUniversalFlagsCount = newUniversalFlags.size,
            removedUniversalFlagsCount = removedUniversalFlags.size,
            updatedUniversalFlagsCount = updatedUniversalFlagParentsCount,
            newSubFlagsCount = newSubFlags.size,
            removedSubFlagsCount = removedSubFlags.size,
            updatedSubFlagsCount = updatedSubFlags.size,
            duplicateEntriesCount = duplicateEntriesCount,
            invalidEntriesCount = invalidEntriesCount,
            unknownCategoryCount = maxOf(unknownCategoryUniqueCount, maxOf(oldUnknownCount, newUnknownCount)),
            potentialRenamesCount = potentialRenames.size,
            oldByteSize = oldDiagnostics?.byteSize ?: 0L,
            newByteSize = newDiagnostics?.byteSize ?: 0L,
            oldHealthScore = oldDiagnostics?.healthScore ?: 100,
            newHealthScore = newDiagnostics?.healthScore ?: 100
        )

        return ComparisonReport(
            summary = summary,
            newFlags = newUniversalFlags,
            newSubFlags = newSubFlags,
            removedFlags = removedUniversalFlags,
            removedSubFlags = removedSubFlags,
            updatedFlagCodes = updatedFlagCodes,
            updatedSubFlags = updatedSubFlags,
            unchangedFlags = unchangedFlags,
            potentialRenames = potentialRenames,
            catalogFlags = catalogFlags,
            categoryAnalytics = categoryAnalytics,
            oldDiagnostics = oldDiagnostics,
            newDiagnostics = newDiagnostics
        )
    }

    private fun buildCategoryAnalytics(catalogFlags: List<CatalogFlagItem>): List<CategoryAnalyticsItem> {
        val byCat = catalogFlags.groupBy { it.category }
        val orderedNames = LinkedHashSet<String>(MetaConfigParser.CATEGORY_NAMES.size + byCat.size)
        for (cat in MetaConfigParser.CATEGORY_NAMES) {
            if (cat != "See All Flags") {
                orderedNames.add(cat)
            }
        }
        for (cat in byCat.keys) {
            orderedNames.add(cat)
        }

        return orderedNames.map { category ->
            val items = byCat[category].orEmpty()
            val oldCount = items.count { it.oldCode != null }
            val newCount = items.count { it.newCode != null }
            val added = items.count { it.status == FlagChangeStatus.NEW }
            val removed = items.count { it.status == FlagChangeStatus.REMOVED }
            val updated = items.count { it.status == FlagChangeStatus.UPDATED }
            val unchanged = items.count { it.status == FlagChangeStatus.UNCHANGED }
            val subCount = items.sumOf { it.totalDistinctSubFlags }
            val total = items.size
            val unchangedPct = if (total > 0) ((unchanged * 100f) / total).toInt() else 100

            CategoryAnalyticsItem(
                categoryName = category,
                totalUniqueFlags = total,
                oldCount = oldCount,
                newCount = newCount,
                newFlagsCount = added,
                removedFlagsCount = removed,
                updatedFlagsCount = updated,
                unchangedFlagsCount = unchanged,
                totalSubFlags = subCount,
                unchangedPercent = unchangedPct
            )
        }
    }

    /**
     * Normalizes queries & fields by treating underscores, hyphens, and whitespace interchangeably.
     */
    fun normalizeForSearch(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun matchesQuery(query: String, vararg fields: String?): Boolean {
        val rawTrim = query.trim().lowercase(Locale.ROOT)
        if (rawTrim.isEmpty()) return true
        val normQuery = normalizeForSearch(rawTrim)
        val tokens = normQuery.split(' ').filter { it.isNotEmpty() }

        for (field in fields) {
            if (field.isNullOrEmpty()) continue
            val fieldLower = field.lowercase(Locale.ROOT)
            if (fieldLower.contains(rawTrim)) return true
            val fieldNorm = normalizeForSearch(fieldLower)
            if (fieldNorm.contains(normQuery)) return true
            if (tokens.size > 1 && tokens.all { fieldNorm.contains(it) }) return true
        }
        return false
    }

    fun generateExportContent(
        report: ComparisonReport,
        type: ExportReportType,
        format: ExportFileFormat
    ): String {
        return when (format) {
            ExportFileFormat.TXT -> generateTypedTextReport(report, type)
            ExportFileFormat.JSON -> generateTypedJsonReport(report, type)
        }
    }

    fun generateTextReport(report: ComparisonReport): String = formatAsText(report)

    fun generateJsonReport(report: ComparisonReport): String = formatAsJson(report)

    private fun generateTypedTextReport(report: ComparisonReport, type: ExportReportType): String {
        return when (type) {
            ExportReportType.COMPLETE_COMPARISON -> formatAsText(report)
            ExportReportType.NEW_ONLY -> buildString {
                appendHeader(this, report, "New Flags Report")
                appendNewSections(this, report)
            }
            ExportReportType.REMOVED_ONLY -> buildString {
                appendHeader(this, report, "Removed Flags Report")
                appendRemovedSections(this, report)
            }
            ExportReportType.UPDATED_ONLY -> buildString {
                appendHeader(this, report, "Updated Flags Report")
                appendUpdatedSections(this, report)
            }
            ExportReportType.COMPLETE_CATALOG -> buildString {
                appendHeader(this, report, "Complete Flag Catalog")
                appendLine("CATALOG FLAGS (${report.catalogFlags.size}):")
                for (item in report.catalogFlags) {
                    appendLine("  [${item.status.label.uppercase(Locale.ROOT)}] ${item.name} | Old=${item.oldCode ?: "-"} New=${item.newCode ?: "-"} | Cat=${item.category} | Subflags=${item.totalDistinctSubFlags}")
                    for (sub in item.subFlags) {
                        appendLine("      • ${sub.name} [${sub.status.label}] Old=${sub.oldCode ?: "-"} New=${sub.newCode ?: "-"}")
                    }
                }
            }
            ExportReportType.CATEGORY_CATALOG -> buildString {
                appendHeader(this, report, "Category-Wise Catalog")
                val byCat = report.catalogFlags.groupBy { it.category }
                for (catAnalytics in report.categoryAnalytics.filter { it.totalUniqueFlags > 0 }) {
                    appendLine("=== ${catAnalytics.categoryName} (${catAnalytics.totalUniqueFlags} flags | +${catAnalytics.newFlagsCount} / -${catAnalytics.removedFlagsCount} / ~${catAnalytics.updatedFlagsCount} | ${catAnalytics.totalSubFlags} subflags) ===")
                    byCat[catAnalytics.categoryName].orEmpty().forEach { flag ->
                        appendLine("  • [${flag.status.label}] ${flag.name} (Old=${flag.oldCode ?: "-"}, New=${flag.newCode ?: "-"})")
                    }
                    appendLine()
                }
            }
            ExportReportType.UNMAPPED_INVALID -> buildString {
                appendHeader(this, report, "Unmapped & Invalid Entries")
                appendDiagnosticsSection(this, "OLD MAPPING DIAGNOSTICS", report.oldDiagnostics)
                appendDiagnosticsSection(this, "NEW MAPPING DIAGNOSTICS", report.newDiagnostics)
                appendLine("--- POTENTIAL RENAME CANDIDATES (${report.potentialRenames.size}) ---")
                if (report.potentialRenames.isEmpty()) {
                    appendLine("  None")
                } else {
                    for (cand in report.potentialRenames) {
                        appendLine("  ? [${cand.scope}] Code=${cand.sharedCode}: ${cand.oldName} -> ${cand.newName} (Not confirmed rename)")
                    }
                }
            }
        }
    }

    private fun generateTypedJsonReport(report: ComparisonReport, type: ExportReportType): String {
        return when (type) {
            ExportReportType.COMPLETE_COMPARISON -> formatAsJson(report)
            ExportReportType.NEW_ONLY -> JSONObject().apply {
                put("reportType", type.title)
                put("summary", buildSummaryJson(report))
                put("newUniversalFlags", buildNewUniversalJsonArray(report.newFlags))
                put("newSubFlags", buildNewSubFlagsJsonArray(report.newSubFlags))
            }.toString(2)
            ExportReportType.REMOVED_ONLY -> JSONObject().apply {
                put("reportType", type.title)
                put("summary", buildSummaryJson(report))
                put("removedUniversalFlags", buildRemovedUniversalJsonArray(report.removedFlags))
                put("removedSubFlags", buildRemovedSubFlagsJsonArray(report.removedSubFlags))
            }.toString(2)
            ExportReportType.UPDATED_ONLY -> JSONObject().apply {
                put("reportType", type.title)
                put("summary", buildSummaryJson(report))
                put("updatedUniversalFlagCodes", buildUpdatedCodesJsonArray(report.updatedFlagCodes))
                put("updatedSubFlags", buildUpdatedSubFlagsJsonArray(report.updatedSubFlags))
                put("updatedCatalogFlags", JSONArray().apply {
                    report.catalogFlags.filter { it.status == FlagChangeStatus.UPDATED }.forEach {
                        put(catalogItemToJson(it))
                    }
                })
            }.toString(2)
            ExportReportType.COMPLETE_CATALOG -> JSONObject().apply {
                put("reportType", type.title)
                put("summary", buildSummaryJson(report))
                put("catalog", JSONArray().apply {
                    report.catalogFlags.forEach { put(catalogItemToJson(it)) }
                })
            }.toString(2)
            ExportReportType.CATEGORY_CATALOG -> JSONObject().apply {
                put("reportType", type.title)
                put("summary", buildSummaryJson(report))
                val categoriesObj = JSONObject()
                val grouped = report.catalogFlags.groupBy { it.category }
                for (cat in report.categoryAnalytics) {
                    val arr = JSONArray()
                    grouped[cat.categoryName].orEmpty().forEach { arr.put(catalogItemToJson(it)) }
                    categoriesObj.put(cat.categoryName, JSONObject().apply {
                        put("totalUniqueFlags", cat.totalUniqueFlags)
                        put("oldCount", cat.oldCount)
                        put("newCount", cat.newCount)
                        put("newFlagsCount", cat.newFlagsCount)
                        put("removedFlagsCount", cat.removedFlagsCount)
                        put("updatedFlagsCount", cat.updatedFlagsCount)
                        put("totalSubFlags", cat.totalSubFlags)
                        put("unchangedPercent", cat.unchangedPercent)
                        put("flags", arr)
                    })
                }
                put("categories", categoriesObj)
            }.toString(2)
            ExportReportType.UNMAPPED_INVALID -> JSONObject().apply {
                put("reportType", type.title)
                put("summary", buildSummaryJson(report))
                put("oldDiagnostics", diagnosticsToJson(report.oldDiagnostics))
                put("newDiagnostics", diagnosticsToJson(report.newDiagnostics))
                put("potentialRenames", JSONArray().apply {
                    report.potentialRenames.forEach { cand ->
                        put(JSONObject().apply {
                            put("scope", cand.scope)
                            put("sharedCode", cand.sharedCode)
                            put("oldName", cand.oldName)
                            put("newName", cand.newName)
                            put("parentFlagName", cand.parentFlagName ?: JSONObject.NULL)
                            put("category", cand.category)
                        })
                    }
                })
            }.toString(2)
        }
    }

    private fun appendHeader(sb: StringBuilder, report: ComparisonReport, title: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(report.summary.timestamp))
        sb.appendLine("==================================================")
        sb.appendLine("  MetaTools - $title")
        sb.appendLine("==================================================")
        sb.appendLine("Date:            $dateStr")
        sb.appendLine("Old Mapping:     ${report.summary.oldSourceName} (${report.summary.totalOldFlags} flags, ${report.summary.totalOldSubFlags} sub-flags)")
        sb.appendLine("New Mapping:     ${report.summary.newSourceName} (${report.summary.totalNewFlags} flags, ${report.summary.totalNewSubFlags} sub-flags)")
        sb.appendLine("Health Score:    Old=${report.summary.oldHealthScore}% | New=${report.summary.newHealthScore}%")
        sb.appendLine("--------------------------------------------------")
    }

    private fun appendNewSections(sb: StringBuilder, report: ComparisonReport) {
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
    }

    private fun appendRemovedSections(sb: StringBuilder, report: ComparisonReport) {
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
    }

    private fun appendUpdatedSections(sb: StringBuilder, report: ComparisonReport) {
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
    }

    private fun appendDiagnosticsSection(
        sb: StringBuilder,
        title: String,
        diag: MappingParseDiagnostics?
    ) {
        sb.appendLine("--- $title ---")
        if (diag == null) {
            sb.appendLine("  No diagnostics collected")
            sb.appendLine()
            return
        }
        sb.appendLine("  Source: ${diag.sourceName} | Valid=${diag.validEntryCount}/${diag.rawEntryCount} | Health=${diag.healthScore}%")
        sb.appendLine("  Duplicates: ${diag.duplicateCount} | Invalid/Malformed: ${diag.invalidCount} | Unknown Category: ${diag.unknownCategoryCount}")
        diag.malformedEntries.forEach { sb.appendLine("  [MALFORMED #${it.entryIndex}] ${it.reason} :: ${it.rawEntry}") }
        diag.missingNameEntries.forEach { sb.appendLine("  [MISSING #${it.entryIndex}] ${it.reason} :: ${it.rawEntry}") }
        diag.duplicateUniversalEntries.forEach { sb.appendLine("  [DUP #${it.entryIndex}] ${it.reason}") }
        diag.duplicateSubFlagEntries.forEach { sb.appendLine("  [DUP SUB #${it.entryIndex}] ${it.reason}") }
        diag.unknownCategoryEntries.take(200).forEach { sb.appendLine("  [OTHER CAT] ${it.flagName} (${it.flagCode})") }
        sb.appendLine()
    }

    /**
     * Formats the complete report as clean human-readable text.
     */
    fun formatAsText(report: ComparisonReport): String {
        val sb = StringBuilder()
        appendHeader(sb, report, "Mapping Comparison Report")
        sb.appendLine("SUMMARY:")
        sb.appendLine("  [+] Total New Flags:     ${report.totalNewCount} (${report.newFlags.size} universal, ${report.newSubFlags.size} sub-flags)")
        sb.appendLine("  [-] Total Removed Flags: ${report.totalRemovedCount} (${report.removedFlags.size} universal, ${report.removedSubFlags.size} sub-flags)")
        sb.appendLine("  [*] Total Updated Flags: ${report.totalUpdatedCount} (${report.updatedFlagCodes.size} universal codes, ${report.updatedSubFlags.size} sub-flag codes | ${report.summary.updatedUniversalFlagsCount} universal parents modified)")
        sb.appendLine("  [=] Unchanged Flags:     ${report.summary.unchangedFlagsCount}")
        sb.appendLine("  [!] Duplicates/Invalid:  ${report.summary.duplicateEntriesCount} dup / ${report.summary.invalidEntriesCount} invalid / ${report.summary.unknownCategoryCount} Other category")
        sb.appendLine("==================================================")
        sb.appendLine()

        appendNewSections(sb, report)
        appendRemovedSections(sb, report)
        appendUpdatedSections(sb, report)

        if (report.potentialRenames.isNotEmpty()) {
            sb.appendLine("--- POTENTIAL RENAME CANDIDATES (${report.potentialRenames.size}) ---")
            for (cand in report.potentialRenames) {
                sb.appendLine("  ? [${cand.scope}] Same Code ${cand.sharedCode}: ${cand.oldName} -> ${cand.newName}")
            }
            sb.appendLine()
        }

        sb.appendLine("==================================================")
        return sb.toString()
    }

    /**
     * Formats the complete report as structured JSON (preserving all existing keys).
     */
    fun formatAsJson(report: ComparisonReport): String {
        val root = JSONObject()
        root.put("summary", buildSummaryJson(report))
        root.put("newUniversalFlags", buildNewUniversalJsonArray(report.newFlags))
        root.put("newSubFlags", buildNewSubFlagsJsonArray(report.newSubFlags))
        root.put("removedUniversalFlags", buildRemovedUniversalJsonArray(report.removedFlags))
        root.put("removedSubFlags", buildRemovedSubFlagsJsonArray(report.removedSubFlags))
        root.put("updatedUniversalFlagCodes", buildUpdatedCodesJsonArray(report.updatedFlagCodes))
        root.put("updatedSubFlags", buildUpdatedSubFlagsJsonArray(report.updatedSubFlags))
        root.put("unchangedFlagsCount", report.summary.unchangedFlagsCount)
        root.put("potentialRenames", JSONArray().apply {
            report.potentialRenames.forEach { cand ->
                put(JSONObject().apply {
                    put("scope", cand.scope)
                    put("sharedCode", cand.sharedCode)
                    put("oldName", cand.oldName)
                    put("newName", cand.newName)
                    put("parentFlagName", cand.parentFlagName ?: JSONObject.NULL)
                    put("category", cand.category)
                })
            }
        })
        return root.toString(2)
    }

    private fun buildSummaryJson(report: ComparisonReport): JSONObject {
        return JSONObject().apply {
            put("oldMapping", report.summary.oldSourceName)
            put("newMapping", report.summary.newSourceName)
            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(report.summary.timestamp)))
            put("totalOldFlags", report.summary.totalOldFlags)
            put("totalNewFlags", report.summary.totalNewFlags)
            put("oldSubFlagsCount", report.summary.totalOldSubFlags)
            put("newSubFlagsCount", report.summary.totalNewSubFlags)
            put("totalNewCount", report.totalNewCount)
            put("totalRemovedCount", report.totalRemovedCount)
            put("totalUpdatedCount", report.totalUpdatedCount)
            put("newUniversalFlagsCount", report.summary.newUniversalFlagsCount)
            put("removedUniversalFlagsCount", report.summary.removedUniversalFlagsCount)
            put("updatedUniversalFlagsCount", report.summary.updatedUniversalFlagsCount)
            put("unchangedCount", report.summary.unchangedFlagsCount)
            put("duplicateEntriesCount", report.summary.duplicateEntriesCount)
            put("invalidEntriesCount", report.summary.invalidEntriesCount)
            put("unknownCategoryCount", report.summary.unknownCategoryCount)
            put("oldHealthScore", report.summary.oldHealthScore)
            put("newHealthScore", report.summary.newHealthScore)
        }
    }

    private fun buildNewUniversalJsonArray(items: List<NewFlagItem>): JSONArray {
        return JSONArray().apply {
            for (item in items) {
                put(JSONObject().apply {
                    put("name", item.flag.name)
                    put("code", item.flag.code)
                    put("category", item.flag.category)
                    put("subFlags", JSONArray().apply {
                        for (s in item.flag.subFlags) {
                            put(JSONObject().apply {
                                put("name", s.name)
                                put("code", s.code)
                            })
                        }
                    })
                })
            }
        }
    }

    private fun buildNewSubFlagsJsonArray(items: List<NewSubFlagItem>): JSONArray {
        return JSONArray().apply {
            for (item in items) {
                put(JSONObject().apply {
                    put("parentFlagName", item.parentFlagName)
                    put("parentFlagCode", item.parentFlagCode)
                    put("category", item.category)
                    put("subFlagName", item.subFlag.name)
                    put("subFlagCode", item.subFlag.code)
                })
            }
        }
    }

    private fun buildRemovedUniversalJsonArray(items: List<RemovedFlagItem>): JSONArray {
        return JSONArray().apply {
            for (item in items) {
                put(JSONObject().apply {
                    put("name", item.flag.name)
                    put("code", item.flag.code)
                    put("category", item.flag.category)
                })
            }
        }
    }

    private fun buildRemovedSubFlagsJsonArray(items: List<RemovedSubFlagItem>): JSONArray {
        return JSONArray().apply {
            for (item in items) {
                put(JSONObject().apply {
                    put("parentFlagName", item.parentFlagName)
                    put("parentFlagCode", item.parentFlagCode)
                    put("category", item.category)
                    put("subFlagName", item.subFlag.name)
                    put("subFlagCode", item.subFlag.code)
                })
            }
        }
    }

    private fun buildUpdatedCodesJsonArray(items: List<UpdatedFlagCodeItem>): JSONArray {
        return JSONArray().apply {
            for (item in items) {
                put(JSONObject().apply {
                    put("flagName", item.flagName)
                    put("category", item.category)
                    put("oldCode", item.oldCode)
                    put("newCode", item.newCode)
                })
            }
        }
    }

    private fun buildUpdatedSubFlagsJsonArray(items: List<UpdatedSubFlagItem>): JSONArray {
        return JSONArray().apply {
            for (item in items) {
                put(JSONObject().apply {
                    put("parentFlagName", item.parentFlagName)
                    put("category", item.category)
                    put("subFlagName", item.subFlagName)
                    put("oldCode", item.oldCode)
                    put("newCode", item.newCode)
                })
            }
        }
    }

    private fun catalogItemToJson(item: CatalogFlagItem): JSONObject {
        return JSONObject().apply {
            put("name", item.name)
            put("oldCode", item.oldCode ?: JSONObject.NULL)
            put("newCode", item.newCode ?: JSONObject.NULL)
            put("category", item.category)
            put("availability", item.availability.label)
            put("status", item.status.name)
            put("subFlags", JSONArray().apply {
                for (s in item.subFlags) {
                    put(JSONObject().apply {
                        put("name", s.name)
                        put("oldCode", s.oldCode ?: JSONObject.NULL)
                        put("newCode", s.newCode ?: JSONObject.NULL)
                        put("status", s.status.name)
                    })
                }
            })
        }
    }

    private fun diagnosticsToJson(diag: MappingParseDiagnostics?): JSONObject {
        if (diag == null) return JSONObject()
        return JSONObject().apply {
            put("sourceName", diag.sourceName)
            put("rawEntryCount", diag.rawEntryCount)
            put("validEntryCount", diag.validEntryCount)
            put("totalSubFlagsCount", diag.totalSubFlagsCount)
            put("byteSize", diag.byteSize)
            put("healthScore", diag.healthScore)
            put("duplicates", issuesToJsonArray(diag.duplicateUniversalEntries + diag.duplicateSubFlagEntries))
            put("malformed", issuesToJsonArray(diag.malformedEntries))
            put("missingNames", issuesToJsonArray(diag.missingNameEntries))
            put("unknownCategory", issuesToJsonArray(diag.unknownCategoryEntries))
        }
    }

    private fun issuesToJsonArray(issues: List<DiagnosticIssue>): JSONArray {
        return JSONArray().apply {
            for (issue in issues) {
                put(JSONObject().apply {
                    put("type", issue.type.name)
                    put("index", issue.entryIndex)
                    put("flagName", issue.flagName)
                    put("flagCode", issue.flagCode)
                    put("reason", issue.reason)
                    put("rawEntry", issue.rawEntry)
                })
            }
        }
    }

    /**
     * Serializes a full ComparisonReport snapshot to JSON for local Comparison History persistence.
     */
    fun serializeSnapshot(report: ComparisonReport): String {
        val root = JSONObject()
        root.put("summary", JSONObject().apply {
            val s = report.summary
            put("oldSourceName", s.oldSourceName)
            put("newSourceName", s.newSourceName)
            put("totalOldFlags", s.totalOldFlags)
            put("totalNewFlags", s.totalNewFlags)
            put("oldSubFlagsCount", s.totalOldSubFlags)
            put("newSubFlagsCount", s.totalNewSubFlags)
            put("newFlagsCount", s.newFlagsCount)
            put("removedFlagsCount", s.removedFlagsCount)
            put("updatedFlagsCount", s.updatedFlagsCount)
            put("unchangedFlagsCount", s.unchangedFlagsCount)
            put("newUniversalFlagsCount", s.newUniversalFlagsCount)
            put("removedUniversalFlagsCount", s.removedUniversalFlagsCount)
            put("updatedUniversalFlagsCount", s.updatedUniversalFlagsCount)
            put("newSubFlagsCountSummary", s.newSubFlagsCount)
            put("removedSubFlagsCount", s.removedSubFlagsCount)
            put("updatedSubFlagsCount", s.updatedSubFlagsCount)
            put("duplicateEntriesCount", s.duplicateEntriesCount)
            put("invalidEntriesCount", s.invalidEntriesCount)
            put("unknownCategoryCount", s.unknownCategoryCount)
            put("potentialRenamesCount", s.potentialRenamesCount)
            put("oldByteSize", s.oldByteSize)
            put("newByteSize", s.newByteSize)
            put("oldHealthScore", s.oldHealthScore)
            put("newHealthScore", s.newHealthScore)
            put("timestamp", s.timestamp)
        })

        // Save catalog entries — compact yet lossless reconstruction for reopening sessions
        val catalogArr = JSONArray()
        for (cat in report.catalogFlags) {
            catalogArr.put(JSONObject().apply {
                put("n", cat.name)
                put("oc", cat.oldCode ?: JSONObject.NULL)
                put("nc", cat.newCode ?: JSONObject.NULL)
                put("c", cat.category)
                put("do", cat.isDuplicateInOld)
                put("dn", cat.isDuplicateInNew)
                put("uc", cat.isUnknownCategory)
                val sArr = JSONArray()
                for (sf in cat.subFlags) {
                    sArr.put(JSONObject().apply {
                        put("n", sf.name)
                        put("oc", sf.oldCode ?: JSONObject.NULL)
                        put("nc", sf.newCode ?: JSONObject.NULL)
                    })
                }
                put("sf", sArr)
            })
        }
        root.put("catalog", catalogArr)

        if (report.oldDiagnostics != null) {
            root.put("oldDiagnostics", diagnosticsSnapshotToJson(report.oldDiagnostics))
        }
        if (report.newDiagnostics != null) {
            root.put("newDiagnostics", diagnosticsSnapshotToJson(report.newDiagnostics))
        }
        return root.toString()
    }

    fun deserializeSnapshot(jsonStr: String): ComparisonReport? {
        return try {
            val root = JSONObject(jsonStr)
            val sObj = root.getJSONObject("summary")
            val oldSourceName = sObj.getString("oldSourceName")
            val newSourceName = sObj.getString("newSourceName")

            val oldReconstructed = ArrayList<UniversalFlag>()
            val newReconstructed = ArrayList<UniversalFlag>()
            val catalogArr = root.getJSONArray("catalog")
            for (i in 0 until catalogArr.length()) {
                val obj = catalogArr.getJSONObject(i)
                val flagName = obj.getString("n")
                val oldCode = if (obj.isNull("oc")) null else obj.getString("oc")
                val newCode = if (obj.isNull("nc")) null else obj.getString("nc")
                val cat = obj.getString("c")
                val sfArr = obj.getJSONArray("sf")
                val oldSubs = ArrayList<SubFlag>()
                val newSubs = ArrayList<SubFlag>()
                for (j in 0 until sfArr.length()) {
                    val sObjItem = sfArr.getJSONObject(j)
                    val sName = sObjItem.getString("n")
                    val sOld = if (sObjItem.isNull("oc")) null else sObjItem.getString("oc")
                    val sNew = if (sObjItem.isNull("nc")) null else sObjItem.getString("nc")
                    if (sOld != null && oldCode != null) {
                        oldSubs.add(SubFlag(sOld, sName, oldCode, flagName, cat))
                    }
                    if (sNew != null && newCode != null) {
                        newSubs.add(SubFlag(sNew, sName, newCode, flagName, cat))
                    }
                }
                if (oldCode != null) {
                    oldReconstructed.add(UniversalFlag(oldCode, flagName, oldSubs, cat, "$oldCode:$flagName"))
                }
                if (newCode != null) {
                    newReconstructed.add(UniversalFlag(newCode, flagName, newSubs, cat, "$newCode:$flagName"))
                }
            }

            val oldDiag = if (root.has("oldDiagnostics")) jsonToDiagnosticsSnapshot(root.getJSONObject("oldDiagnostics"), oldReconstructed) else null
            val newDiag = if (root.has("newDiagnostics")) jsonToDiagnosticsSnapshot(root.getJSONObject("newDiagnostics"), newReconstructed) else null

            val report = compareWithDiagnostics(
                oldFlags = oldReconstructed,
                newFlags = newReconstructed,
                oldSourceName = oldSourceName,
                newSourceName = newSourceName,
                oldDiagnostics = oldDiag,
                newDiagnostics = newDiag
            )
            // Restore original timestamp & sizes
            val origTs = sObj.optLong("timestamp", System.currentTimeMillis())
            report.copy(
                summary = report.summary.copy(
                    timestamp = origTs,
                    oldByteSize = sObj.optLong("oldByteSize", report.summary.oldByteSize),
                    newByteSize = sObj.optLong("newByteSize", report.summary.newByteSize),
                    oldHealthScore = sObj.optInt("oldHealthScore", report.summary.oldHealthScore),
                    newHealthScore = sObj.optInt("newHealthScore", report.summary.newHealthScore),
                    duplicateEntriesCount = sObj.optInt("duplicateEntriesCount", report.summary.duplicateEntriesCount),
                    invalidEntriesCount = sObj.optInt("invalidEntriesCount", report.summary.invalidEntriesCount)
                )
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun diagnosticsSnapshotToJson(d: MappingParseDiagnostics): JSONObject {
        return JSONObject().apply {
            put("sourceName", d.sourceName)
            put("rawEntryCount", d.rawEntryCount)
            put("validEntryCount", d.validEntryCount)
            put("totalSubFlagsCount", d.totalSubFlagsCount)
            put("byteSize", d.byteSize)
            put("dupUniversal", issuesToJsonArray(d.duplicateUniversalEntries))
            put("dupSub", issuesToJsonArray(d.duplicateSubFlagEntries))
            put("malformed", issuesToJsonArray(d.malformedEntries))
            put("missing", issuesToJsonArray(d.missingNameEntries))
            put("unknown", issuesToJsonArray(d.unknownCategoryEntries))
        }
    }

    private fun jsonToDiagnosticsSnapshot(obj: JSONObject, flags: List<UniversalFlag>): MappingParseDiagnostics {
        fun parseArr(key: String): List<DiagnosticIssue> {
            val arr = obj.optJSONArray(key) ?: return emptyList()
            val list = ArrayList<DiagnosticIssue>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val type = runCatching { DiagnosticIssueType.valueOf(o.getString("type")) }
                    .getOrDefault(DiagnosticIssueType.MALFORMED_ENTRY)
                list.add(
                    DiagnosticIssue(
                        type = type,
                        entryIndex = o.optInt("index", 0),
                        rawEntry = o.optString("rawEntry", ""),
                        flagName = o.optString("flagName", ""),
                        flagCode = o.optString("flagCode", ""),
                        reason = o.optString("reason", "")
                    )
                )
            }
            return list
        }
        return MappingParseDiagnostics(
            sourceName = obj.optString("sourceName", ""),
            validFlags = flags,
            rawEntryCount = obj.optInt("rawEntryCount", flags.size),
            validEntryCount = obj.optInt("validEntryCount", flags.size),
            totalSubFlagsCount = obj.optInt("totalSubFlagsCount", 0),
            byteSize = obj.optLong("byteSize", 0L),
            previewEntries = emptyList(),
            duplicateUniversalEntries = parseArr("dupUniversal"),
            duplicateSubFlagEntries = parseArr("dupSub"),
            malformedEntries = parseArr("malformed"),
            missingNameEntries = parseArr("missing"),
            unknownCategoryEntries = parseArr("unknown"),
            isValid = true,
            validationError = null
        )
    }
}
