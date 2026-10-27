package com.dimowner.audiorecorder.v2.data.model

/**
 * Where new recordings are saved.
 */
enum class StorageLocation {
    /** The app's own folder under Android/data, which other apps cannot read. */
    PRIVATE,

    /** A fixed shared folder, Recordings/R-Recorder, that other apps can read. */
    PUBLIC,

    /**
     * A folder the user picked with the system folder picker. Recordings are made in the
     * private folder and copied there once finished, because the picked folder can only be
     * reached through a content Uri, not through the file paths the rest of the app works with.
     */
    CUSTOM,
}
