package com.goosethings.tools.client.nametag;

import com.goosethings.tools.nametag.NameTagSync;

/** Pure merge rules for icons that may arrive through private and snapshot state. */
final class NameTagRenderIconPolicy {
    private NameTagRenderIconPolicy() {
    }

    static boolean showWitchDoctorCurse(boolean privateTarget, int attachmentFlags) {
        return privateTarget
                || (attachmentFlags & NameTagSync.WITCH_DOCTOR_CURSE) != 0;
    }
}
