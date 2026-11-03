package com.pdp.evaluation;

import java.util.Iterator;
import java.util.stream.Stream;

/** Three-valued logic for targets and conditions: a processing error yields {@link #UNKNOWN} (XACML Indeterminate). */
enum Truth {
    TRUE, FALSE, UNKNOWN;

    static Truth of(boolean value) {
        return value ? TRUE : FALSE;
    }

    /** False if any is false; otherwise unknown if any is unknown; otherwise true. Stops at the first false. */
    static Truth all(Stream<Truth> values) {
        boolean unknown = false;
        for (Iterator<Truth> it = values.iterator(); it.hasNext(); ) {
            Truth t = it.next();
            if (t == FALSE) return FALSE;
            if (t == UNKNOWN) unknown = true;
        }
        return unknown ? UNKNOWN : TRUE;
    }

    /** True if any is true; otherwise unknown if any is unknown; otherwise false. Stops at the first true. */
    static Truth any(Stream<Truth> values) {
        boolean unknown = false;
        for (Iterator<Truth> it = values.iterator(); it.hasNext(); ) {
            Truth t = it.next();
            if (t == TRUE) return TRUE;
            if (t == UNKNOWN) unknown = true;
        }
        return unknown ? UNKNOWN : FALSE;
    }
}
