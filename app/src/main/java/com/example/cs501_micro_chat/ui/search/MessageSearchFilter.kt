package com.example.cs501_micro_chat.ui.search

import androidx.annotation.StringRes
import com.example.cs501_micro_chat.R

/**
 * Filters for chat search quick actions (photos/videos, files, audio, links).
 */
enum class MessageSearchFilter(
    val arg: String,
    @StringRes val titleRes: Int
    ) {
    Photos("photos", R.string.search_chip_photos_videos),
    Files("files", R.string.search_chip_files),
    Audio("audio", R.string.search_chip_audio),
    Links("links", R.string.search_chip_links);

    companion object {
        fun fromArg(value: String?): MessageSearchFilter {
            return entries.firstOrNull { it.arg.equals(value, ignoreCase = true) } ?: Photos
        }
    }
}
