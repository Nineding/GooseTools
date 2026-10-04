package com.goosethings.tools.client.csl;

import java.util.Objects;
import java.util.function.Predicate;

/** Detects supported CustomSkinLoader mod IDs without linking to CSL classes. */
public final class CslModDetector {
    static final String BOOTSTRAP_MOD_ID = "customskinloader-bootstrap";
    static final String LEGACY_MOD_ID = "customskinloader";

    private CslModDetector() {
    }

    public static boolean isLoaded(Predicate<String> isModLoaded) {
        Objects.requireNonNull(isModLoaded, "isModLoaded");
        return isModLoaded.test(BOOTSTRAP_MOD_ID)
                || isModLoaded.test(LEGACY_MOD_ID);
    }
}
