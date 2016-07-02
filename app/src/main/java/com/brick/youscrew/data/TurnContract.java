package com.brick.youscrew.data;

import android.provider.BaseColumns;

/**
 * Created by windows on 04.02.2016.
 */
public class TurnContract {


    public static final class RatEntry implements BaseColumns {

        /*
        The RAT table holds one entry for each rat in the database
         */

        public static final String TABLE_NAME = "rat";

        // Rat number
        public static final String COLUMN_CODE = "code";

        // Rat name
        public static final String COLUMN_NAME = "name";

        // Number of tetrodes to be logged
        public static final String COLUMN_NUM_TETRODES = "num_tetrodes";

        // Number of micrometers a tetrode is advanced with one turn
        public static final String COLUMN_TURN_MICROMETERS = "turn_micrometers";

        public static final String COLUMN_DISPLAY_INDEX = "display_index";

        public static final String COLUMN_SCREW_THREAD_DIR = "screw_thread_dir";

        public static final String COLUMN_TETRODES_INDEPENDENT = "tetrodes_independent";

    }

    public static final class TetrodeEntry implements BaseColumns {

        /*
        The TETRODE table holds one entry for each tetrode of each rat. It links
        back to the rat table via a foreign key.
         */

        public static final String TABLE_NAME = "tetrode";

        public static final String COLUMN_RAT_KEY = "rat_id";

        // Tetrode index in (i.e. from 1 to 14 in a 14TT hyperdrive)
        public static final String COLUMN_INDEX = "idx";

        // Tetrode name (e.g. TT4; optionally user modifiable)
        public static final String COLUMN_NAME = "name";

        public static final String COLUMN_INITIAL_ANGLE = "initial_angle";

        // Log of total turns (can just add up turns from all turn events, but
        // this will often be more convenient). This will have to be updated after
        // each turn event

        public static final String COLUMN_IS_REF = "is_reference";

        public static final String COLUMN_IN_USE = "in_use";

        public static final String COLUMN_COLOUR = "colour";

        public static final String COLUMN_INITIAL_ANGLE_SET = "initial_angle_set";

        public static final String COLUMN_EVER_TURNED = "ever_turned";

        public static final String COLUMN_TAGGABLE = "taggable";

        public static final String COLUMN_COMMENT = "comment";
    }

    public static final class SessionEntry implements BaseColumns {

        /*
        The SESSION table holds on entry for each turning session. Each entry is bound
        to a rat via a foreign key
         */

        public static final String TABLE_NAME = "session";

        //Foreign key links each session to the rat table
        public static final String COLUMN_RAT_KEY = "rat_id";

        // Time of day session created
        public static final String COLUMN_TIME_START = "time_start";

        // Time of day ended
        public static final String COLUMN_TIME_END = "time_end";

        // String tags optionally given to session by user
        public static final String COLUMN_TAGS = "tags";

        // On session close, the number of tetrodes turned is logged
        public static final String COLUMN_NUM_TTS_TURNED = "num_tts_turned";

        // Boolean indicator of whether the session is still open
        public static final String COLUMN_IS_OPEN = "is_open";

        // Session time mode (TIME_REAL or TIME_PICKED)
        public static final String COLUMN_TIME_MODE = "time_mode";

        public static final String COLUMN_SESSION_KEY_LAST = "turn_id_last";

        // Boolean, true if the session is the first for a given rat
        public static final String COLUMN_IS_FIRST_SESSION = "is_first_session";

        // Single comment string
        public static final String COLUMN_COMMENT = "comment";

    }

    public static final class TurnEntry implements BaseColumns {

        public static final String TABLE_NAME = "turn";

        // Foreign key links each turn to the session table
        public static final String COLUMN_SESSION_KEY = "session_id";

        // Foreign key links each turn to the tetrode (and thus rat) table
        public static final String COLUMN_TETRODE_KEY = "tetrode_id";

        // Time turned
        public static final String COLUMN_TIME = "time";

        // Start angle
        public static final String COLUMN_START_ANGLE = "start_angle";

        // End angle
        public static final String COLUMN_END_ANGLE = "end_angle";

        // CSV list of tag IDs
        public static final String COLUMN_TAG_ID_PRE = "tag_id_pre";

        public static final String COLUMN_TAG_ID_POST = "tag_id_post";

        // CSV list of persistent tagIDs. This specifies the IDs of the tags
        // that will be automatically added when the TT is next turned
        public static final String COLUMN_TAG_ID_PERSISTENT_PRE = "tag_id_persistent_pre";

        public static final String COLUMN_TAG_ID_PERSISTENT_POST = "tag_id_persistent_post";

        // Boolean, true if the tt angle changed during the session
        public static final String COLUMN_WAS_TURNED = "was_turned";

        // Boolean, true if any non-mandatory fields were updated
        public static final String COLUMN_WAS_EDITED = "was_edited";

        public static final String COLUMN_COMMENT = "comment";

//        public static final String COLUMN_TURN_KEY_LAST = "turn_id_last";

    }

    public static final class TagGroupEntry implements BaseColumns {

        public static final String TABLE_NAME = "tag_group";

        public static final String COLUMN_NAME = "name";

        public static final String COLUMN_MANDATORY = "mandatory";

        public static final String COLUMN_COLOUR = "colour";

        // Determines when a tag group is displayed (never, pre/post only, or always)
        public static final String COLUMN_IN_USE = "in_use";

        public static final String COLUMN_DISPLAY_INDEX = "display_index";

        public static final String COLUMN_SINGLE_CONSTRAINT = "single_constraint";

        public static final String COLUMN_HINT_TEXT = "hint_text";

        // If the user deletes a tag group, this field is set to true. The tag group will no longer
        // be visible to the user but will remain in the database so that old records can keep
        // their references to it
        public static final String COLUMN_DELETED = "deleted";

    }

    public static final class TagEntry implements BaseColumns {

        public static final String TABLE_NAME = "tag";

        public static final String COLUMN_TAG_GROUP_KEY = "tag_group_id";

        public static final String COLUMN_NAME = "name";

        // Field set by user in the tag settings activity
        public static final String COLUMN_IN_USE = "in_use";

        public static final String COLUMN_DISPLAY_INDEX = "display_index";

        // True if the user has deleted the tag (makes it invisible to the user thereafter,
        // but the tag will remain in the database such that old records can still reference it)
        public static final String COLUMN_DELETED = "deleted";

    }

}

