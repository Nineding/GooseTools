package com.goosethings.tools.intro;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Opening title definitions copied from the datapack's existing showrole translations. */
public final class RoleIntroCatalog {
    private static final List<Role> ROLES = load();
    private RoleIntroCatalog() {}

    public static List<Role> roles() { return ROLES; }
    public static Role byTag(String tag) {
        return ROLES.stream().filter(role -> role.tag().equals(tag)).findFirst().orElseGet(() ->
                "Duck".equals(tag) ? new Role("Duck", "role.evil", "Duck",
                        "intro.goosetools.duck_description", "Eliminate the Geese", 0xFF7373, 1)
                        : ROLES.stream().filter(role -> role.tag().equals("Goose")).findFirst().orElseThrow());
    }
    public static Role resolve(Set<String> tags) {
        // Preserve showrole's last-title-wins order, should a legacy player retain two tags.
        Role result = null;
        for (Role role : ROLES) if (tags.contains(role.tag())) result = role;
        if (result != null) return result;
        return byTag(tags.contains("evil") ? "Duck" : tags.contains("good") ? "Goose" : "Dodo");
    }

    private static List<Role> load() {
        var stream = RoleIntroCatalog.class.getResourceAsStream("/role_intro_roles.json");
        if (stream == null) throw new IllegalStateException("Missing opening role catalog");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Role[] roles = new Gson().fromJson(reader, Role[].class);
            if (roles == null || roles.length == 0) throw new IllegalStateException("Empty role catalog");
            for (Role role : roles) {
                if (role.tag() == null || role.key() == null || role.fallback() == null
                        || role.descriptionKey() == null || role.descriptionFallback() == null) {
                    throw new IllegalStateException("Incomplete role catalog entry");
                }
            }
            return List.copyOf(Arrays.asList(roles));
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }

    public record Role(String tag, String key, String fallback, String descriptionKey,
                       String descriptionFallback, int color, int faction) {}
}
