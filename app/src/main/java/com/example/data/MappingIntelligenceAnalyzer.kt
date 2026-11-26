package com.example.data

import com.example.model.CatalogFlagItem
import com.example.model.ComparisonReport
import com.example.model.DiagnosticIssueType
import com.example.model.DuplicateCleanupSuggestion
import com.example.model.ExportFileFormat
import com.example.model.FlagChangeStatus
import com.example.model.MappingParseDiagnostics
import com.example.model.MultiFileComparisonReport
import com.example.model.MultiFileFlagPresence
import com.example.model.NamespaceStatItem
import com.example.model.NewSubFlagItem
import com.example.model.ParentChildChangeItem
import com.example.model.RemovedSubFlagItem
import com.example.model.SessionDiffItem
import com.example.model.SessionDiffReport
import com.example.model.SourcePreviewInfo
import com.example.model.UniversalFlag
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CompareSearchIndexEntry(
    val item: CatalogFlagItem,
    val combinedNormText: String,
    val prefixLower: String
)

data class CompareSearchIndex(
    val reportTimestamp: Long,
    val entries: List<CompareSearchIndexEntry>,
    val byName: Map<String, CatalogFlagItem>
)

object MappingIntelligenceAnalyzer {

    fun calcGrowthPercent(oldVal: Int, newVal: Int): Float {
        if (oldVal == 0) return if (newVal == 0) 0f else 100f
        return ((newVal - oldVal).toFloat() / oldVal.toFloat()) * 100f
    }

    fun formatGrowthPercent(pct: Float): String {
        val sign = if (pct > 0f) "+" else ""
        return String.format(Locale.US, "%s%.1f%%", sign, pct)
    }

    fun extractNamespacePrefix(flagName: String): String {
        val idx = flagName.indexOf('_')
        return if (idx in 1..14) "${flagName.substring(0, idx).lowercase(Locale.ROOT)}_" else "other"
    }

    fun isThreadsOrP92(flagName: String, category: String = ""): Boolean {
        val lower = flagName.lowercase(Locale.ROOT)
        return lower.startsWith("p92_") ||
            lower.startsWith("barcelona_") ||
            lower.startsWith("threads_") ||
            lower.contains("_p92_") ||
            category.contains("Threads", ignoreCase = true) ||
            category.contains("P92", ignoreCase = true)
    }

    /**
     * Detects when a subflag name moved from one universal flag parent in Old
     * to a different universal flag parent in New.
     */
    fun detectParentChildChanges(
        removedSubFlags: List<RemovedSubFlagItem>,
        newSubFlags: List<NewSubFlagItem>
    ): List<ParentChildChangeItem> {
        if (removedSubFlags.isEmpty() || newSubFlags.isEmpty()) return emptyList()
        val addedBySubName = newSubFlags.groupBy { it.subFlag.name }
        val result = ArrayList<ParentChildChangeItem>()
        for (rem in removedSubFlags) {
            val matches = addedBySubName[rem.subFlag.name].orEmpty()
            for (added in matches) {
                if (rem.parentFlagName != added.parentFlagName) {
                    result.add(
                        ParentChildChangeItem(
                            subFlagName = rem.subFlag.name,
                            oldParentFlagName = rem.parentFlagName,
                            oldParentCode = rem.parentFlagCode,
                            newParentFlagName = added.parentFlagName,
                            newParentCode = added.parentFlagCode,
                            oldSubCode = rem.subFlag.code,
                            newSubCode = added.subFlag.code,
                            category = added.category
                        )
                    )
                }
            }
        }
        return result
    }

    /**
     * Builds namespace & prefix breakdown (ig_, ig4a_, igd_, p92_, barcelona_, fb_, etc.).
     */
    fun buildNamespaceStats(
        oldFlags: Map<String, UniversalFlag>,
        newFlags: Map<String, UniversalFlag>
    ): List<NamespaceStatItem> {
        val oldCounts = HashMap<String, Int>()
        val newCounts = HashMap<String, Int>()
        val allPrefixes = HashSet<String>()

        for (flag in oldFlags.values) {
            val prefix = extractNamespacePrefix(flag.name)
            allPrefixes.add(prefix)
            oldCounts[prefix] = (oldCounts[prefix] ?: 0) + 1
        }
        for (flag in newFlags.values) {
            val prefix = extractNamespacePrefix(flag.name)
            allPrefixes.add(prefix)
            newCounts[prefix] = (newCounts[prefix] ?: 0) + 1
        }

        val addedCounts = HashMap<String, Int>()
        for (flag in newFlags.values) {
            if (!oldFlags.containsKey(flag.name)) {
                val p = extractNamespacePrefix(flag.name)
                addedCounts[p] = (addedCounts[p] ?: 0) + 1
            }
        }
        val removedCounts = HashMap<String, Int>()
        for (flag in oldFlags.values) {
            if (!newFlags.containsKey(flag.name)) {
                val p = extractNamespacePrefix(flag.name)
                removedCounts[p] = (removedCounts[p] ?: 0) + 1
            }
        }

        return allPrefixes.map { prefix ->
            val oldC = oldCounts[prefix] ?: 0
            val newC = newCounts[prefix] ?: 0
            val isP92 = prefix == "p92_" || prefix == "barcelona_" || prefix == "threads_"
            NamespaceStatItem(
                prefix = prefix,
                oldCount = oldC,
                newCount = newC,
                addedCount = addedCounts[prefix] ?: 0,
                removedCount = removedCounts[prefix] ?: 0,
                growthPercent = calcGrowthPercent(oldC, newC),
                isThreadsP92 = isP92
            )
        }.sortedByDescending { maxOf(it.oldCount, it.newCount) }
    }

    /**
     * Produces actionable duplicate cleanup suggestions without auto-mutating original files.
     */
    fun buildCleanupSuggestions(
        oldDiagnostics: MappingParseDiagnostics?,
        newDiagnostics: MappingParseDiagnostics?
    ): List<DuplicateCleanupSuggestion> {
        val list = ArrayList<DuplicateCleanupSuggestion>()

        fun collect(side: String, diag: MappingParseDiagnostics?) {
            if (diag == null) return
            val seenRaw = HashSet<String>()
            for (issue in diag.duplicateUniversalEntries) {
                val isExactRepeat = !seenRaw.add(issue.rawEntry)
                val type = when {
                    isExactRepeat -> "EXACT_DUPLICATE_REMOVE"
                    issue.type == DiagnosticIssueType.DUPLICATE_UNIVERSAL_CODE -> "CODE_CONFLICT_MERGE"
                    else -> "DUPLICATE_NAME_DEDUP"
                }
                val rec = when (type) {
                    "EXACT_DUPLICATE_REMOVE" -> "Identical duplicate line detected — safe to remove redundant occurrence."
                    "CODE_CONFLICT_MERGE" -> "Code ${issue.flagCode} appears on multiple flags — verify universal code ownership."
                    else -> "Flag '${issue.flagName}' declared multiple times — keep the latest subflag set and remove earlier copy."
                }
                list.add(
                    DuplicateCleanupSuggestion(
                        sourceSide = side,
                        flagName = issue.flagName,
                        flagCode = issue.flagCode,
                        suggestionType = type,
                        recommendation = rec,
                        rawEntry = issue.rawEntry
                    )
                )
            }
            for (issue in diag.duplicateSubFlagEntries) {
                list.add(
                    DuplicateCleanupSuggestion(
                        sourceSide = side,
                        flagName = issue.flagName,
                        flagCode = issue.flagCode,
                        suggestionType = "SUBFLAG_DUPLICATE_CLEAN",
                        recommendation = "Remove duplicate subflag entry inside '${issue.flagName}' (${issue.reason}).",
                        rawEntry = issue.rawEntry
                    )
                )
            }
        }

        collect("New", newDiagnostics)
        collect("Old", oldDiagnostics)
        return list
    }

    /**
     * Builds a cached searchable index for fast background/filtered lookups.
     */
    fun buildSearchIndex(report: ComparisonReport): CompareSearchIndex {
        val entries = report.catalogFlags.map { item ->
            val sb = StringBuilder()
            sb.append(item.name).append(' ')
            item.oldCode?.let { sb.append(it).append(' ') }
            item.newCode?.let { sb.append(it).append(' ') }
            sb.append(item.category).append(' ')
            sb.append(item.namespacePrefix).append(' ')
            sb.append(item.status.label).append(' ')
            for (sub in item.subFlags) {
                sb.append(sub.name).append(' ')
                sub.oldCode?.let { sb.append(it).append(' ') }
                sub.newCode?.let { sb.append(it).append(' ') }
            }
            CompareSearchIndexEntry(
                item = item,
                combinedNormText = MappingCompareEngine.normalizeForSearch(sb.toString()),
                prefixLower = item.namespacePrefix.lowercase(Locale.ROOT)
            )
        }
        return CompareSearchIndex(
            reportTimestamp = report.summary.timestamp,
            entries = entries,
            byName = report.catalogFlags.associateBy { it.name }
        )
    }

    /**
     * Multi-keyword, partial, case-insensitive, underscore-tolerant query check across combined fields.
     */
    fun matchesEntryAdvanced(
        indexEntry: CompareSearchIndexEntry,
        query: String
    ): Boolean {
        val rawTrim = query.trim().lowercase(Locale.ROOT)
        if (rawTrim.isEmpty()) return true
        val normQuery = MappingCompareEngine.normalizeForSearch(rawTrim)
        val tokens = normQuery.split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return true
        return tokens.all { token -> indexEntry.combinedNormText.contains(token) }
    }

    /**
     * Compares N >= 2 mapping files in 3-File or Multi-File mode.
     */
    fun compareMultipleMappings(
        sources: List<Pair<String, MappingParseDiagnostics>>
    ): MultiFileComparisonReport {
        require(sources.size >= 2) { "At least 2 valid mappings are required for multi-file comparison" }

        val sourceNames = sources.map { it.first }
        val previews = sources.map { MappingDiagnosticsParser.toSourcePreview(it.second) }

        val mapsBySource = sources.map { (_, diag) ->
            val map = LinkedHashMap<String, UniversalFlag>(diag.validFlags.size)
            for (flag in diag.validFlags) {
                if (!map.containsKey(flag.name)) {
                    map[flag.name] = flag
                }
            }
            map
        }

        val allFlagNames = LinkedHashSet<String>()
        for (m in mapsBySource) {
            allFlagNames.addAll(m.keys)
        }

        val presenceEntries = ArrayList<MultiFileFlagPresence>(allFlagNames.size)
        var commonInAll = 0
        var onlyInOne = 0
        var codeDivergent = 0

        for (flagName in allFlagNames) {
            val codesBySource = LinkedHashMap<String, String?>(sources.size)
            val subCountsBySource = LinkedHashMap<String, Int>(sources.size)
            val presentLabels = ArrayList<String>()
            val distinctCodes = HashSet<String>()
            val subNamesSet = LinkedHashSet<String>()
            var resolvedCategory = "Other"

            sources.forEachIndexed { idx, (label, _) ->
                val flag = mapsBySource[idx][flagName]
                if (flag != null) {
                    codesBySource[label] = flag.code
                    subCountsBySource[label] = flag.subFlags.size
                    presentLabels.add(label)
                    distinctCodes.add(flag.code)
                    if (resolvedCategory == "Other" || idx == sources.lastIndex) {
                        resolvedCategory = flag.category
                    }
                    flag.subFlags.forEach { subNamesSet.add(it.name) }
                } else {
                    codesBySource[label] = null
                    subCountsBySource[label] = 0
                }
            }

            if (presentLabels.size == sources.size) commonInAll++
            if (presentLabels.size == 1) onlyInOne++
            if (distinctCodes.size > 1) codeDivergent++

            presenceEntries.add(
                MultiFileFlagPresence(
                    flagName = flagName,
                    category = resolvedCategory,
                    namespacePrefix = extractNamespacePrefix(flagName),
                    codesBySource = codesBySource,
                    subflagCountsBySource = subCountsBySource,
                    presentInSourceLabels = presentLabels,
                    distinctCodesCount = distinctCodes.size,
                    subFlagNames = subNamesSet.toList()
                )
            )
        }

        // Pairwise deltas along sequence: File[i] -> File[i+1]
        val pairwiseDeltas = ArrayList<String>()
        for (i in 0 until sources.size - 1) {
            val (nameA, diagA) = sources[i]
            val (nameB, diagB) = sources[i + 1]
            val pairReport = MappingCompareEngine.compareWithDiagnostics(
                oldFlags = diagA.validFlags,
                newFlags = diagB.validFlags,
                oldSourceName = nameA,
                newSourceName = nameB,
                oldDiagnostics = diagA,
                newDiagnostics = diagB
            )
            pairwiseDeltas.add(
                "$nameA → $nameB: +${pairReport.newFlags.size} flags (+${pairReport.newSubFlags.size} subs), " +
                    "-${pairReport.removedFlags.size} flags (-${pairReport.removedSubFlags.size} subs), " +
                    "~${pairReport.updatedFlagCodes.size} flag codes (~${pairReport.updatedSubFlags.size} sub codes)"
            )
        }

        // Also produce baseline (first) vs latest (last) full report so all 15 sections populate seamlessly!
        val (firstName, firstDiag) = sources.first()
        val (lastName, lastDiag) = sources.last()
        val baselineVsLatest = MappingCompareEngine.compareWithDiagnostics(
            oldFlags = firstDiag.validFlags,
            newFlags = lastDiag.validFlags,
            oldSourceName = firstName,
            newSourceName = lastName,
            oldDiagnostics = firstDiag,
            newDiagnostics = lastDiag
        )

        return MultiFileComparisonReport(
            sourceNames = sourceNames,
            sourcePreviews = previews,
            uniqueFlagsTotal = allFlagNames.size,
            commonInAllCount = commonInAll,
            onlyInOneFileCount = onlyInOne,
            codeDivergentCount = codeDivergent,
            entries = presenceEntries,
            baselineVsLatestReport = baselineVsLatest,
            pairwiseDeltas = pairwiseDeltas
        )
    }

    /**
     * Compares two previously saved comparison history sessions.
     */
    fun compareHistorySessions(
        sessionALabel: String,
        reportA: ComparisonReport,
        sessionBLabel: String,
        reportB: ComparisonReport
    ): SessionDiffReport {
        val mapA = reportA.catalogFlags.associateBy { it.name }
        val mapB = reportB.catalogFlags.associateBy { it.name }

        val newlyAppeared = ArrayList<String>()
        val disappeared = ArrayList<String>()
        val transitions = ArrayList<SessionDiffItem>()

        for ((name, itemB) in mapB) {
            val itemA = mapA[name]
            if (itemA == null) {
                newlyAppeared.add(name)
            } else if (itemA.status != itemB.status) {
                transitions.add(
                    SessionDiffItem(
                        flagName = name,
                        category = itemB.category,
                        statusInA = itemA.status.label,
                        statusInB = itemB.status.label
                    )
                )
            }
        }
        for (name in mapA.keys) {
            if (!mapB.containsKey(name)) {
                disappeared.add(name)
            }
        }

        return SessionDiffReport(
            sessionALabel = sessionALabel,
            sessionBLabel = sessionBLabel,
            newlyAppearedInB = newlyAppeared.sorted(),
            disappearedInB = disappeared.sorted(),
            statusTransitions = transitions,
            deltaNewCount = reportB.totalNewCount - reportA.totalNewCount,
            deltaRemovedCount = reportB.totalRemovedCount - reportA.totalRemovedCount,
            deltaUpdatedCount = reportB.totalUpdatedCount - reportA.totalUpdatedCount
        )
    }

    fun generateSingleCategoryExport(
        report: ComparisonReport,
        categoryName: String,
        format: ExportFileFormat
    ): String {
        val flags = if (categoryName == "See All Flags") {
            report.catalogFlags
        } else {
            report.catalogFlags.filter { it.category == categoryName }
        }
        return generateCatalogListExport(
            title = "Category Export: $categoryName",
            items = flags,
            format = format
        )
    }

    fun generateCatalogListExport(
        title: String,
        items: List<CatalogFlagItem>,
        format: ExportFileFormat
    ): String {
        if (format == ExportFileFormat.JSON) {
            return JSONObject().apply {
                put("title", title)
                put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("count", items.size)
                put("flags", JSONArray().apply {
                    items.forEach { item ->
                        put(JSONObject().apply {
                            put("name", item.name)
                            put("oldCode", item.oldCode ?: JSONObject.NULL)
                            put("newCode", item.newCode ?: JSONObject.NULL)
                            put("category", item.category)
                            put("status", item.status.name)
                            put("originalEntry", item.newOriginalEntry ?: item.oldOriginalEntry ?: JSONObject.NULL)
                            put("subFlags", JSONArray().apply {
                                item.subFlags.forEach { sub ->
                                    put(JSONObject().apply {
                                        put("name", sub.name)
                                        put("oldCode", sub.oldCode ?: JSONObject.NULL)
                                        put("newCode", sub.newCode ?: JSONObject.NULL)
                                        put("status", sub.status.name)
                                    })
                                }
                            })
                        })
                    }
                })
            }.toString(2)
        }
        return buildString {
            appendLine("=== $title (${items.size} flags) ===")
            items.forEach { item ->
                appendLine("[${item.status.label.uppercase(Locale.ROOT)}] ${item.name} | Old=${item.oldCode ?: "-"} New=${item.newCode ?: "-"} | Cat=${item.category} (${item.totalDistinctSubFlags} subs)")
                item.subFlags.forEach { s ->
                    appendLine("    • ${s.name} [${s.status.label}] Old=${s.oldCode ?: "-"} New=${s.newCode ?: "-"}")
                }
            }
        }
    }

    fun generateHealthReportExport(
        report: ComparisonReport,
        format: ExportFileFormat
    ): String {
        if (format == ExportFileFormat.JSON) {
            return JSONObject().apply {
                put("reportType", "Mapping Health Report")
                put("oldHealthScore", report.summary.oldHealthScore)
                put("newHealthScore", report.summary.newHealthScore)
                put("oldCompletenessPercent", report.oldDiagnostics?.completenessPercent ?: 100)
                put("newCompletenessPercent", report.newDiagnostics?.completenessPercent ?: 100)
                put("mappingGrowthPercent", report.summary.mappingGrowthPercent.toDouble())
                put("subflagGrowthPercent", report.summary.subflagGrowthPercent.toDouble())
                put("threadsP92FlagsCount", report.summary.threadsP92FlagsCount)
                put("parentChildChangesCount", report.summary.parentChildChangesCount)
                put("namespaces", JSONArray().apply {
                    report.namespaceStats.forEach { ns ->
                        put(JSONObject().apply {
                            put("prefix", ns.prefix)
                            put("oldCount", ns.oldCount)
                            put("newCount", ns.newCount)
                            put("addedCount", ns.addedCount)
                            put("removedCount", ns.removedCount)
                            put("growthPercent", ns.growthPercent.toDouble())
                            put("isThreadsP92", ns.isThreadsP92)
                        })
                    }
                })
                put("cleanupSuggestions", JSONArray().apply {
                    report.cleanupSuggestions.forEach { sug ->
                        put(JSONObject().apply {
                            put("sourceSide", sug.sourceSide)
                            put("flagName", sug.flagName)
                            put("flagCode", sug.flagCode)
                            put("suggestionType", sug.suggestionType)
                            put("recommendation", sug.recommendation)
                        })
                    }
                })
            }.toString(2)
        }

        return buildString {
            appendLine("=== MetaTools Mapping Health & Intelligence Report ===")
            appendLine("Old Mapping: ${report.summary.oldSourceName} (Health ${report.summary.oldHealthScore}%, Completeness ${report.oldDiagnostics?.completenessPercent ?: 100}%)")
            appendLine("New Mapping: ${report.summary.newSourceName} (Health ${report.summary.newHealthScore}%, Completeness ${report.newDiagnostics?.completenessPercent ?: 100}%)")
            appendLine("Mapping Growth: ${formatGrowthPercent(report.summary.mappingGrowthPercent)} | Subflag Growth: ${formatGrowthPercent(report.summary.subflagGrowthPercent)}")
            appendLine("Threads / P92 Flags: ${report.summary.threadsP92FlagsCount} | Parent-Child Shifts: ${report.summary.parentChildChangesCount}")
            appendLine()
            appendLine("--- NAMESPACE / PREFIX ANALYSIS ---")
            report.namespaceStats.take(25).forEach { ns ->
                appendLine("  ${ns.prefix.padEnd(14)} Old=${ns.oldCount} New=${ns.newCount} (${formatGrowthPercent(ns.growthPercent)})${if (ns.isThreadsP92) " [Threads/P92]" else ""}")
            }
            appendLine()
            appendLine("--- DUPLICATE CLEANUP SUGGESTIONS (${report.cleanupSuggestions.size}) ---")
            report.cleanupSuggestions.forEach { sug ->
                appendLine("  [${sug.sourceSide}] ${sug.flagName} (${sug.flagCode}): ${sug.recommendation}")
            }
        }
    }

    fun generateMultiFileExport(
        multiReport: MultiFileComparisonReport,
        format: ExportFileFormat
    ): String {
        if (format == ExportFileFormat.JSON) {
            return JSONObject().apply {
                put("reportType", "Multi-File Matrix Report")
                put("sources", JSONArray(multiReport.sourceNames))
                put("uniqueFlagsTotal", multiReport.uniqueFlagsTotal)
                put("commonInAllCount", multiReport.commonInAllCount)
                put("onlyInOneFileCount", multiReport.onlyInOneFileCount)
                put("codeDivergentCount", multiReport.codeDivergentCount)
                put("pairwiseDeltas", JSONArray(multiReport.pairwiseDeltas))
                put("flags", JSONArray().apply {
                    multiReport.entries.forEach { entry ->
                        put(JSONObject().apply {
                            put("flagName", entry.flagName)
                            put("category", entry.category)
                            put("namespacePrefix", entry.namespacePrefix)
                            put("distinctCodesCount", entry.distinctCodesCount)
                            put("codesBySource", JSONObject(entry.codesBySource.mapValues { it.value ?: JSONObject.NULL }))
                        })
                    }
                })
            }.toString(2)
        }

        return buildString {
            appendLine("=== MetaTools Multi-File Comparison Matrix ===")
            appendLine("Sources (${multiReport.sourceNames.size}): ${multiReport.sourceNames.joinToString(" | ")}")
            appendLine("Unique Flags: ${multiReport.uniqueFlagsTotal} | Shared In All: ${multiReport.commonInAllCount} | Single-File Only: ${multiReport.onlyInOneFileCount} | Code Divergent: ${multiReport.codeDivergentCount}")
            appendLine()
            appendLine("--- PAIRWISE DELTAS ---")
            multiReport.pairwiseDeltas.forEach { appendLine("  • $it") }
            appendLine()
            appendLine("--- MATRIX ENTRIES (FIRST 500) ---")
            multiReport.entries.take(500).forEach { e ->
                val codesStr = multiReport.sourceNames.joinToString(" | ") { src -> "$src=${e.codesBySource[src] ?: "-"}" }
                appendLine("  ${e.flagName} [${e.category}] -> $codesStr")
            }
        }
    }
}
