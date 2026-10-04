package com.goosethings.tools.marker;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable marker codes shared by the server snapshot and client renderers. */
public final class PlayerMarkerCatalog {
    private static final Map<Integer, Definition> BY_CODE = createDefinitions();

    private PlayerMarkerCatalog() {
    }

    public static Definition byCode(int code) {
        return BY_CODE.get(code);
    }

    public static List<Definition> definitions() {
        return List.copyOf(BY_CODE.values());
    }

    private static Map<Integer, Definition> createDefinitions() {
        Map<Integer, Definition> definitions = new LinkedHashMap<>();
        add(definitions, 1, Style.GOOSE, "menu.ggd.marker.additional.goose", "Goose",
                "minecraft:textures/item/ggd/goose.png", false);
        add(definitions, 2, Style.DUCK, "menu.ggd.marker.additional.duck", "Duck",
                "minecraft:textures/item/ggd/duck.png", false);
        add(definitions, 3, Style.BIRD, "menu.ggd.marker.additional.bird", "Bird",
                "minecraft:textures/item/ggd/dodo.png", false);
        add(definitions, 4, Style.GROUP_ONE, "menu.ggd.marker.additional.group_one", "Group 1",
                "minecraft:textures/item/group_one.png", false);
        add(definitions, 5, Style.GROUP_TWO, "menu.ggd.marker.additional.group_two", "Group 2",
                "minecraft:textures/item/group_two.png", false);
        add(definitions, 6, Style.GROUP_THREE, "menu.ggd.marker.additional.group_three", "Group 3",
                "minecraft:textures/item/group_three.png", false);
        add(definitions, 7, Style.KILL, "menu.ggd.marker.additional.kill", "Kill Card",
                "minecraft:textures/item/kill.png", false);
        add(definitions, 8, Style.INFO, "menu.ggd.marker.additional.info", "Information Card",
                "minecraft:textures/item/focus_mode.png", false);
        add(definitions, 9, Style.PROTECT, "menu.ggd.marker.additional.protect", "Protection Card",
                "minecraft:textures/item/guard_shield_nametag.png", false);
        add(definitions, 10, Style.SOLO, "menu.ggd.marker.additional.solo", "Solo Card",
                "minecraft:textures/item/rush.png", false);

        addRole(definitions, 101, Faction.GOOSE, "goose", "Goose", "goose");
        addRole(definitions, 102, Faction.GOOSE, "canadian", "Canadian Goose", "canadian");
        addRole(definitions, 103, Faction.GOOSE, "sheriff", "Sheriff", "sheriff");
        addRole(definitions, 104, Faction.GOOSE, "mimic", "Mimic", "mimic");
        addRole(definitions, 105, Faction.GOOSE, "vigilante", "Vigilante", "vigilante");
        addRole(definitions, 106, Faction.GOOSE, "medium", "Medium", "medium");
        addRole(definitions, 107, Faction.GOOSE, "detective", "Detective", "detective");
        addRole(definitions, 108, Faction.GOOSE, "mortician", "Mortician", "mortician");
        addRole(definitions, 109, Faction.GOOSE, "celebrity", "Celebrity", "celebrity");
        addRole(definitions, 110, Faction.GOOSE, "avenger", "Avenger", "avenger");
        addRole(definitions, 111, Faction.GOOSE, "engineer", "Engineer", "engineer");
        addRole(definitions, 112, Faction.GOOSE, "neptune", "Neptune", "neptune");
        addRole(definitions, 113, Faction.GOOSE, "stalker", "Stalker", "stalker");
        addRole(definitions, 114, Faction.GOOSE, "sensor", "Sensor", "sensor");
        addRole(definitions, 115, Faction.GOOSE, "gravy", "Gravy", "gravy");
        addRole(definitions, 116, Faction.GOOSE, "bodyguard", "Bodyguard", "bodyguard");
        addRole(definitions, 117, Faction.GOOSE, "soldier", "Soldier", "soldier");
        addRole(definitions, 118, Faction.GOOSE, "lobbyist", "Lobbyist", "lobbyist");
        addRole(definitions, 119, Faction.GOOSE, "fortuneteller", "Fortune Teller", "fortuneteller");
        addRole(definitions, 120, Faction.GOOSE, "scientist", "Scientist", "scientist");
        addRole(definitions, 121, Faction.GOOSE, "coroner", "Coroner", "coroner");
        addRole(definitions, 122, Faction.GOOSE, "birdwatcher", "Birdwatcher", "birdwatcher");
        addRole(definitions, 123, Faction.GOOSE, "correspondent", "Correspondent", "correspondent");
        addRole(definitions, 124, Faction.GOOSE, "survivalist", "Survivalist", "survivalist");
        addRole(definitions, 125, Faction.GOOSE, "politician", "Politician", "politician");
        addRole(definitions, 126, Faction.GOOSE, "locksmith", "Locksmith", "locksmith");
        addRole(definitions, 127, Faction.GOOSE, "astral", "Astral", "astral");
        addRole(definitions, 128, Faction.GOOSE, "lucid_dreamer", "Lucid Dreamer", "luciddreamer");
        addRole(definitions, 129, Faction.GOOSE, "guard", "Guard", "guard");
        addRole(definitions, 130, Faction.GOOSE, "broker", "Broker", "broker");
        addRole(definitions, 131, Faction.GOOSE, "spook", "Spook", "spook");

        addRole(definitions, 201, Faction.DUCK, "cannibal", "Cannibal", "cannibal");
        addRole(definitions, 202, Faction.DUCK, "morphling", "Morphling", "morphling");
        addRole(definitions, 203, Faction.DUCK, "professional", "Professional", "professional");
        addRole(definitions, 204, Faction.DUCK, "spy", "Spy", "spy");
        addRole(definitions, 205, Faction.DUCK, "assassin", "Assassin", "assassin");
        addRole(definitions, 206, Faction.DUCK, "demolitionist", "Demolitionist", "demolitionist");
        addRole(definitions, 207, Faction.DUCK, "identitythief", "Identity Thief", "identitythief");
        addRole(definitions, 208, Faction.DUCK, "ninja", "Ninja", "ninja");
        addRole(definitions, 209, Faction.DUCK, "invisibility", "Invisible Duck", "invisibility");
        addRole(definitions, 210, Faction.DUCK, "party", "Party Duck", "party");
        addRole(definitions, 211, Faction.DUCK, "clown", "Clown", "clown");
        addRole(definitions, 212, Faction.DUCK, "silencer", "Silencer", "silencer");
        addRole(definitions, 213, Faction.DUCK, "sniper", "Sniper", "sniper");
        addRole(definitions, 214, Faction.DUCK, "hitman", "Hitman", "hitman");
        addRole(definitions, 215, Faction.DUCK, "cupid", "Cupid", "cupid");
        addRole(definitions, 216, Faction.DUCK, "marauder", "Marauder", "marauder");
        addRole(definitions, 217, Faction.DUCK, "witch_doctor", "Witch Doctor", "witchdoctor");
        addRole(definitions, 218, Faction.DUCK, "serialkiller", "Serial Killer", "serialkiller");
        addRole(definitions, 219, Faction.DUCK, "esper", "Esper", "esper");
        addRole(definitions, 220, Faction.DUCK, "snitch", "Snitch", "snitch");
        addRole(definitions, 221, Faction.DUCK, "carrier", "Carrier", "carrier");
        addRole(definitions, 222, Faction.DUCK, "parasite", "Parasite", "parasite");

        addRole(definitions, 301, Faction.BIRD, "dodo", "Dodo", "dodo");
        addRole(definitions, 302, Faction.BIRD, "duelingdodos", "Dueling Dodos", "dueling_dodo");
        addRole(definitions, 303, Faction.BIRD, "vulture", "Vulture", "vulture");
        addRole(definitions, 304, Faction.BIRD, "pigeon", "Pigeon", "pigeon");
        addRole(definitions, 305, Faction.BIRD, "falcon", "Falcon", "falcon");
        addRole(definitions, 306, Faction.BIRD, "pelican", "Pelican", "pelican");
        addRole(definitions, 307, Faction.BIRD, "raven", "Raven", "raven");
        addRole(definitions, 308, Faction.BIRD, "cuckoo", "Cuckoo", "cuckoo");
        addRole(definitions, 309, Faction.BIRD, "phoenix", "Phoenix", "phoenix");
        return Map.copyOf(definitions);
    }

    private static void addRole(Map<Integer, Definition> definitions, int code, Faction faction,
                                String role, String fallback, String textureName) {
        add(definitions, code, faction.style, "role." + faction.translationSegment + "." + role,
                fallback, "minecraft:textures/item/ggd/" + textureName + ".png", true);
    }

    private static void add(Map<Integer, Definition> definitions, int code, Style style,
                            String translationKey, String fallback, String texture,
                            boolean roleSpecific) {
        Definition previous = definitions.put(code, new Definition(
                code, style, translationKey, fallback, texture, roleSpecific));
        if (previous != null) {
            throw new IllegalStateException("Duplicate player marker code: " + code);
        }
    }

    public enum Style {
        GOOSE(0xCAFFBD, 0x55FF55),
        DUCK(0xFF9C9C, 0xFF5555),
        BIRD(0xFFF9C4, 0xFFFF55),
        GROUP_ONE(0xB3DAFF, 0xB3DAFF),
        GROUP_TWO(0xC9FFAB, 0xC9FFAB),
        GROUP_THREE(0xFFF0AB, 0xFFF0AB),
        KILL(0xFF33AA, 0xFF33AA),
        INFO(0x84CFFF, 0x84CFFF),
        PROTECT(0xEAF7FF, 0xEAF7FF),
        SOLO(0xFFBE65, 0xFFBE65);

        private final int cardRgb;
        private final int glowRgb;

        Style(int cardRgb, int glowRgb) {
            this.cardRgb = cardRgb;
            this.glowRgb = glowRgb;
        }

        public int cardRgb() {
            return cardRgb;
        }

        public int glowRgb() {
            return glowRgb;
        }
    }

    public enum Faction {
        GOOSE("good", Style.GOOSE),
        DUCK("evil", Style.DUCK),
        BIRD("neutral", Style.BIRD);

        private final String translationSegment;
        private final Style style;

        Faction(String translationSegment, Style style) {
            this.translationSegment = translationSegment;
            this.style = style;
        }
    }

    public record Definition(
            int code,
            Style style,
            String translationKey,
            String fallback,
            String texture,
            boolean roleSpecific) {
    }
}
