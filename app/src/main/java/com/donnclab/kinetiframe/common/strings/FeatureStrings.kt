package com.donnclab.kinetiframe.common.strings

/**
 * User-facing strings for the Library, Home and a few studio surfaces, grouped by screen.
 * New strings belong in Android resources so they can be translated; this object holds
 * existing copy until it is migrated.
 */
object FeatureStrings {

    /** Labels shared by more than one screen (top tabs, bottom nav, generic actions). */
    object Common {
        const val TAB_PROJECTS = "Projects"
        const val TAB_ACTORS = "Actors"
        const val TAB_PACKS = "Packs"
        const val TAB_AUDIO = "Audio"
        const val SEARCH = "Search"
        const val FILTER = "Filter"
        const val BACK = "Back"
        const val SETTINGS_COMING_SOON = "Settings coming soon"
    }

    object Library {
        const val TITLE = "Library"
        const val SUBTITLE = "Your local collection -- ready to drop into any project"
        const val PACKS_FLAT_NOTE = "All your saved packs, one continuous scroll -- no sub-categories to dig through."
        const val ADD_TO_PROJECT = "Add to Current Project"
        const val NO_PROJECT_HELPER = "Open a project first to use this asset"
        const val ADDED_TO_PROJECT_MESSAGE = "Added to your current project"
        const val FILTER_FULL_PROJECTS = "Full Projects"
        const val FILTER_BACKGROUND_IMAGES = "Background Images"
        const val FILTER_STATIC_OVERLAYS = "Static Overlays"
        const val FILTER_RIGS = "Rigs / Blueprints"
        const val FILTER_PERFORMANCES = "Performances"
        const val FILTER_SOUND_EFFECTS = "Sound Effects"
        const val FILTER_MUSIC_TRACKS = "Music Tracks"
        const val FILTER_VOICE_CLIPS = "Voice Clips"
    }

    object AssetDetails {
        const val LIVE_PREVIEW_LOOPS = "Live preview -- loops"
        const val LIVE_PREVIEW = "Live preview"
        const val STATIC_PREVIEW = "Static preview"
        const val AUDIO_PREVIEW = "Preview"
    }

    object PackContents {
        fun itemCount(count: Int) = "$count Items in this pack"
        const val ADD_PACK_TO_LIBRARY = "Add pack to Library"
    }

    /**
     * Actor Studio's single-line workflow coach. Deliberately phrased as "what to do next" at each
     * step of the blueprint flow (pick a part -> sketch -> fit joints -> pose), because the studio
     * has no other affordance telling a first-time animator where to begin.
     */
    object ActorStudio {
        const val NEEDS_SKELETON = "Add a reference skeleton to start this actor"
        const val PICK_A_PART = "Pick a body part below, then sketch it -- it binds automatically"
        const val SKETCH_PART = "Sketching binds this part automatically -- switch to the bone tool to fit its joints"
        const val FIT_JOINTS = "Drag the joint pins to match your drawing, then switch back to the pencil"
        const val TRACE_ARTWORK = "Artwork -- trace the sketch you drew for this part, then confirm its pivot"
        const val POSE = "Pose -- drag a joint, then lift your finger to drop a keyframe"
    }

    /** Voice-over capture and placed audio clips. */
    object Audio {
        const val RECORDER_TITLE = "Audio Recorder"
        const val START_HINT = "Tap the button to start recording"
        const val RECORDING = "Recording"
        const val TAKE_READY = "Take ready"
        const val RECORD = "Start recording"
        const val STOP = "Stop recording"
        const val DISCARD = "Discard take"
        const val RETAKE = "Record again"
        const val USE_TAKE = "Add to timeline"
        const val CLOSE = "Close"
        const val MUTE_LANE = "Mute this audio lane"
        const val UNMUTE_LANE = "Unmute this audio lane"
        const val LOCK_LANE = "Lock this audio lane"
        const val UNLOCK_LANE = "Unlock this audio lane"
        const val DUPLICATE_CLIP = "Duplicate clip"
        const val SPLIT_CLIP = "Split clip at playhead"
        const val DELETE_CLIP = "Delete clip"
        const val TRIM_TITLE = "Trim audio"
        const val PREVIEW = "Preview selection"
        const val STOP_PREVIEW = "Stop preview"
        const val ADD_TO_TIMELINE = "Add to timeline"
        const val TRIM_IN = "In"
        const val TRIM_OUT = "Out"
        const val TRIM_LENGTH = "Length"
    }

    /** Home's Packs tab and the saved-pack drill-down it opens. Distinct from [PackContents],
     *  which belongs to the Library browse surface. */
    object Packs {
        const val CLOSE = "Close"
        const val EDIT_PACK = "Edit Pack"
        const val EDIT = "Edit"
        const val SHARE = "Share (.kpack)"
        const val DELETE = "Delete"
        const val PACK_ACTIONS = "Pack actions"
        const val EMPTY_MESSAGE = "Build a pack of stamp-ready drawings to see it here."
        const val NEW_PACK = "New pack"
        const val COULD_NOT_OPEN = "Could not open this pack"
        const val COULD_NOT_READ_DRAWING = "One of this pack's drawings could not be read"
        fun itemCount(count: Int) = if (count == 1) "1 item" else "$count items"
    }

    /** Rows of Home's Import sheet. */
    object Import {
        const val FROM_FILE_TITLE = "From File"
        const val FROM_FILE_SUBTITLE = "A .kframe file saved on your device"
        const val FROM_LIBRARY_TITLE = "From Library"
        const val FROM_LIBRARY_SUBTITLE = "Your saved packs, actors and assets"
    }
}
