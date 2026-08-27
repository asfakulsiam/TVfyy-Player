package com.example.domain.model

enum class PlaylistSourceType(val displayName: String) {
    LOCAL_FILE("Local File"),
    REMOTE_URL("Remote URL"),
    GITHUB_RAW("GitHub Raw"),
    PASTED_TEXT("Pasted Text"),
    MANUAL("Manual")
}

data class Playlist(
    val id: Long = 0,
    val name: String,
    val sourceType: PlaylistSourceType = PlaylistSourceType.MANUAL,
    val sourceUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null,
    val categoryCount: Int = 0,
    val channelCount: Int = 0,
    val isActive: Boolean = false
)

data class PlaylistCategory(
    val id: Long = 0,
    val playlistId: Long,
    val name: String,
    val position: Int = 0,
    val isVisible: Boolean = true,
    val channelCount: Int = 0
)

data class PlaylistChannel(
    val id: Long = 0,
    val playlistId: Long,
    val categoryId: Long? = null,
    val categoryName: String = "Uncategorized",
    val name: String,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgLogo: String? = null,
    val streamUrl: String,
    val position: Int = 0,
    val isFavorite: Boolean = false,
    val isUserEdited: Boolean = false,
    val knownAttributes: Map<String, String> = emptyMap(),
    val unknownAttributes: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ParsedEntry(
    val name: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgLogo: String? = null,
    val groupTitle: String? = null,
    val durationSeconds: Int = -1,
    val knownAttributes: Map<String, String> = emptyMap(),
    val unknownAttributes: Map<String, String> = emptyMap(),
    val rawLineNumber: Int = 0
)

data class ParserWarning(
    val lineNumber: Int,
    val rawContent: String,
    val reason: String
)

data class ParsedPlaylist(
    val defaultName: String? = null,
    val entries: List<ParsedEntry> = emptyList(),
    val categories: List<String> = emptyList(),
    val warnings: List<ParserWarning> = emptyList(),
    val totalFound: Int = 0,
    val totalValid: Int = 0
)

enum class ImportMode {
    REPLACE_EXISTING,
    MERGE_EXISTING,
    ADD_AS_NEW
}

data class ChannelDiff(
    val channel: PlaylistChannel,
    val changeType: DiffType,
    val details: String = ""
)

enum class DiffType {
    ADDED,
    REMOVED,
    MODIFIED,
    UNCHANGED
}

data class PlaylistChangeSummary(
    val playlistId: Long,
    val playlistName: String,
    val existingCount: Int,
    val remoteCount: Int,
    val addedCount: Int,
    val removedCount: Int,
    val modifiedCount: Int,
    val unchangedCount: Int,
    val diffList: List<ChannelDiff> = emptyList()
)

enum class ChannelSortOrder(val displayName: String) {
    CUSTOM("Custom Order"),
    NAME_ASC("Name (A → Z)"),
    NAME_DESC("Name (Z → A)"),
    RECENTLY_ADDED("Recently Added")
}
