package com.example.data

import com.example.model.SubFlag
import com.example.model.UniversalFlag
import java.util.Locale

object MetaConfigParser {

    /**
     * The 66 defined categories in exact order.
     */
    val CATEGORY_NAMES = listOf(
        "See All Flags",
        "Ads",
        "Analytics",
        "Aura",
        "Avatars",
        "Basel",
        "Bloks",
        "Branded Content",
        "Business",
        "Business Growth",
        "Camera",
        "Clips",
        "Company Account Access",
        "Content Appreciation",
        "Creation",
        "Creators",
        "Creator AI",
        "Cross Meta Experiences",
        "Direct",
        "Explore",
        "Feed",
        "Felix",
        "FBNS",
        "Friend Map",
        "GenAI",
        "Growth",
        "High Res",
        "Holdout",
        "Hatch/muse",
        "Instants",
        "Infra",
        "Japan",
        "MetaConfig Categories",
        "Live Video",
        "Logged Out",
        "Media Solutions",
        "Migrated",
        "Mocks",
        "Monetization",
        "Msys",
        "Notes",
        "Notification",
        "Other",
        "Oreo",
        "Payments",
        "Privacy",
        "Profile",
        "Quicksnap",
        "Realtime",
        "Reels App",
        "Rendering",
        "RTC",
        "Save",
        "Search",
        "Shopping",
        "Stories",
        "Stories Interactions",
        "Threads App/P92",
        "Trust",
        "UI Infra",
        "Unifex",
        "Vibes",
        "Video",
        "Video Call",
        "WhatsApp",
        "Wellbeing"
    )

    /**
     * Parses a single colon-separated mapping entry into a [UniversalFlag].
     *
     * Format: universal_code:universal_name:sub_code_1:sub_name_1:sub_code_2:sub_name_2...
     */
    fun parseEntry(entry: String): UniversalFlag? {
        val trimmed = entry.trim()
        if (trimmed.isEmpty()) return null

        val parts = trimmed.split(':')
        if (parts.size < 2) return null

        val universalCode = parts[0].trim()
        val universalName = parts[1].trim()
        if (universalCode.isEmpty() || universalName.isEmpty()) return null

        val category = detectCategory(universalName)

        val subFlags = ArrayList<SubFlag>()
        var i = 2
        while (i < parts.size) {
            val subCode = parts[i].trim()
            val subName = if (i + 1 < parts.size) parts[i + 1].trim() else ""
            if (subCode.isNotEmpty() || subName.isNotEmpty()) {
                subFlags.add(
                    SubFlag(
                        code = subCode,
                        name = subName,
                        parentFlagCode = universalCode,
                        parentFlagName = universalName,
                        category = category
                    )
                )
            }
            i += 2
        }

        return UniversalFlag(
            code = universalCode,
            name = universalName,
            subFlags = subFlags,
            category = category,
            originalEntry = trimmed
        )
    }

    /**
     * Automatic category detector using namespace, prefixes, and keyword rules.
     */
    fun detectCategory(flagName: String): String {
        val name = flagName.lowercase(Locale.ROOT)

        // Specific prefixes / namespaces first
        if (name.contains("threads") || name.startsWith("p92") || name.contains("_p92") || name.contains("p92_") ||
            name.contains("barcelona") || name.startsWith("bcn") || name.startsWith("b4a") ||
            name.contains("thrid") || name.contains("th4a")
        ) {
            return "Threads App/P92"
        }
        if (name.contains("basel") || name.startsWith("bsl")) {
            return "Basel"
        }
        if (name.contains("creator_ai") || name.contains("biz_ai_agent") || name.contains("ai_characters") ||
            name.contains("ugc_ai") || name.contains("ai_agents")
        ) {
            return "Creator AI"
        }
        if (name.contains("genai") || name.contains("gen_ai") || name.contains("magic_mod") ||
            name.contains("meta_ai") || name.contains("imagine") || name.contains("restyle") ||
            name.contains("cutoutai") || name.contains("wwai")
        ) {
            return "GenAI"
        }
        if (name.contains("whatsapp") || name.startsWith("wa_") || name.contains("_wa_") ||
            name.contains("ctwa") || name.startsWith("ptwa") || name.endsWith("_wa")
        ) {
            return "WhatsApp"
        }
        if (name.contains("friend_map") || name.contains("friends_map")) {
            return "Friend Map"
        }
        if (name.contains("quicksnap")) {
            return "Quicksnap"
        }
        if (name.contains("instants")) {
            return "Instants"
        }
        if (name.contains("vibes")) {
            return "Vibes"
        }
        if (name.contains("hatch") || name.contains("muse")) {
            return "Hatch/muse"
        }
        if (name.contains("aura")) {
            return "Aura"
        }
        if (name.contains("stories_interaction") || name.contains("story_interaction") ||
            name.contains("story_reply") || name.contains("story_likes")
        ) {
            return "Stories Interactions"
        }
        if (name.contains("story") || name.contains("stories")) {
            return "Stories"
        }
        if (name.contains("reels_app") || name.contains("reel_app")) {
            return "Reels App"
        }
        if (name.contains("clips") || name.contains("reel") || name.contains("reels")) {
            return "Clips"
        }
        if (name.contains("notes") || name.contains("note_") || name.contains("_note")) {
            return "Notes"
        }
        if (name.contains("feed") || name.contains("mainfeed") || name.contains("flash_feed") || name.contains("newsfeed")) {
            return "Feed"
        }
        if (name.contains("explore")) {
            return "Explore"
        }
        if (name.contains("profile")) {
            return "Profile"
        }
        if (name.contains("direct") || name.startsWith("igd_") || name.startsWith("igd") ||
            name.contains("inbox") || name.contains("thread_view") || name.contains("armadillo")
        ) {
            return "Direct"
        }
        if (name.contains("search") || name.contains("typeahead") || name.contains("serp")) {
            return "Search"
        }
        if (name.contains("camera") || name.contains("capture") || name.contains("onecamera")) {
            return "Camera"
        }
        if (name.contains("shop") || name.contains("commerce") || name.contains("storefront")) {
            return "Shopping"
        }
        if (name.contains("pay") || name.contains("checkout") || name.contains("fbpay") ||
            name.contains("wallet") || name.contains("billing")
        ) {
            return "Payments"
        }
        if (name.contains("video_call") || name.contains("videocall")) {
            return "Video Call"
        }
        if (name.contains("rtc") || name.contains("calling") || name.contains("rsys") ||
            name.contains("webrtc") || name.contains("call_")
        ) {
            return "RTC"
        }
        if (name.contains("realtime") || name.contains("real_time") || name.contains("mqtt") ||
            name.contains("iris") || name.contains("dgw")
        ) {
            return "Realtime"
        }
        if (name.contains("msys")) {
            return "Msys"
        }
        if (name.contains("fbns")) {
            return "FBNS"
        }
        if (name.contains("notif") || name.contains("notification") || name.contains("badge") ||
            name.contains("badging") || name.startsWith("ign_") || name.startsWith("igns_")
        ) {
            return "Notification"
        }
        if (name.contains("live") || name.contains("broadcast")) {
            return "Live Video"
        }
        if (name.contains("ads") || name.contains("ad_") || name.contains("_ad") ||
            name.contains("boost") || name.contains("lead_gen") || name.contains("sponsor")
        ) {
            return "Ads"
        }
        if (name.contains("analytics") || name.contains("logging") || name.contains("qpl") ||
            name.contains("logger") || name.contains("perf") || name.contains("latency") ||
            name.contains("ttrc") || name.contains("falco")
        ) {
            return "Analytics"
        }
        if (name.contains("avatar")) {
            return "Avatars"
        }
        if (name.contains("bloks") || name.startsWith("bk_")) {
            return "Bloks"
        }
        if (name.contains("branded_content") || name.contains("bc_") || name.contains("coauthor")) {
            return "Branded Content"
        }
        if (name.contains("business_growth")) {
            return "Business Growth"
        }
        if (name.contains("business") || name.contains("biz") || name.contains("smb") || name.contains("leadgen")) {
            return "Business"
        }
        if (name.contains("company_account") || name.contains("caa") || name.contains("account_linking") || name.contains("switcher")) {
            return "Company Account Access"
        }
        if (name.contains("appreciation") || name.contains("stars") || name.contains("gifts")) {
            return "Content Appreciation"
        }
        if (name.contains("creator")) {
            return "Creators"
        }
        if (name.contains("cross_meta") || name.contains("xplat") || name.contains("cross_app") ||
            name.contains("crosspost") || name.contains("xpost") || name.contains("xav")
        ) {
            return "Cross Meta Experiences"
        }
        if (name.contains("creation") || name.contains("composer") || name.contains("editor")) {
            return "Creation"
        }
        if (name.contains("felix") || name.contains("igtv")) {
            return "Felix"
        }
        if (name.contains("growth") || name.contains("su_") || name.contains("friending")) {
            return "Growth"
        }
        if (name.contains("high_res") || name.contains("hdr") || name.contains("4k")) {
            return "High Res"
        }
        if (name.contains("holdout") || name.contains("test") || name.contains("experiment")) {
            return "Holdout"
        }
        if (name.contains("japan")) {
            return "Japan"
        }
        if (name.contains("logged_out") || name.contains("login") || name.contains("auth") || name.contains("logout")) {
            return "Logged Out"
        }
        if (name.contains("media_solutions") || name.contains("tasos") || name.contains("bwe") || name.contains("bandwidth")) {
            return "Media Solutions"
        }
        if (name.contains("migrated") || name.contains("migration") || name.contains("deprecation") || name.contains("unship")) {
            return "Migrated"
        }
        if (name.contains("mock")) {
            return "Mocks"
        }
        if (name.contains("monetization") || name.contains("fan_club") || name.contains("subscriptions") || name.contains("subs")) {
            return "Monetization"
        }
        if (name.contains("oreo")) {
            return "Oreo"
        }
        if (name.contains("privacy") || name.contains("safety") || name.contains("guardian") || name.contains("supervision")) {
            return "Privacy"
        }
        if (name.contains("rendering") || name.contains("render") || name.contains("litho") || name.contains("compose")) {
            return "Rendering"
        }
        if (name.contains("save") || name.contains("saved") || name.contains("bookmark")) {
            return "Save"
        }
        if (name.contains("trust") || name.contains("integrity")) {
            return "Trust"
        }
        if (name.contains("ui_infra") || name.contains("actionbar") || name.contains("dialog") ||
            name.contains("bottomsheet") || name.contains("prism")
        ) {
            return "UI Infra"
        }
        if (name.contains("unifex")) {
            return "Unifex"
        }
        if (name.contains("video") || name.contains("exoplayer") || name.contains("player") ||
            name.contains("playback") || name.contains("codec")
        ) {
            return "Video"
        }
        if (name.contains("wellbeing") || name.contains("time_spent") || name.contains("screentime") || name.contains("sleep_mode")) {
            return "Wellbeing"
        }
        if (name.contains("metaconfig") || name.contains("mobileconfig") || name.contains("quickexperiments")) {
            return "MetaConfig Categories"
        }
        if (name.contains("infra") || name.contains("network") || name.contains("http") ||
            name.contains("storage") || name.contains("cache") || name.contains("database")
        ) {
            return "Infra"
        }

        return "Other"
    }
}
