package com.goosethings.tools.nametag;

import java.util.Set;

/** Role checks that keep only a Seagull's borrowed nametag visibility after the skill is spent. */
final class NameTagRolePolicy {
    private static final String BORROWED_PREFIX = "seagullNametag";

    private NameTagRolePolicy() {
    }

    static boolean hasRole(Set<String> tags, String role) {
        return tags.contains(role) || hasBorrowedRole(tags, role);
    }

    static boolean hasBorrowedRole(Set<String> tags, String role) {
        return tags.contains("Seagull") && tags.contains(BORROWED_PREFIX + role);
    }
}
