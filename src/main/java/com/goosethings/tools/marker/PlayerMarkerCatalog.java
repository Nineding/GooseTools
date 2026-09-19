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
        add(definitions, 1, Faction.GOOSE, "text.faction.good", "Goose",
                "minecraft:textures/item/ggd/goose.png", false);
        add(definitions, 2, Faction.DUCK, "text.faction.evil", "Duck",
                "minecraft:textures/item/kill.png", false);
        add(definitions, 3, Faction.BIRD, "text.faction.neutral", "Bird",
                "minecraft:textures/item/ggd/dodo.png", false);

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
        add(definitions, code, faction, "role." + faction.translationSegment + "." + role,
                fallback, "minecraft:textures/item/ggd/" + textureName + ".png", true);
    }

    private static void add(Map<Integer, Definition> definitions, int code, Faction faction,
                            String translationKey, String fallback, String texture,
                            boolean roleSpecific) {
        Definition previous = definitions.put(code, new Definition(
                code, faction, translationKey, fallback, texture, roleSpecific));
        if (previous != null) {
            throw new IllegalStateException("Duplicate player marker code: " + code);
        }
    }

    public enum Faction {
        GOOSE("good", 0xCAFFBD),
        DUCK("evil", 0xFF9C9C),
        BIRD("neutral", 0xFFF9C4);

        private final String translationSegment;
        private final int rgb;

        Faction(String translationSegment, int rgb) {
            this.translationSegment = translationSegment;
            this.rgb = rgb;
        }

        public int rgb() {
            return rgb;
        }
    }

    public record Definition(
            int code,
            Faction faction,
            String translationKey,
            String fallback,
            String texture,
            boolean roleSpecific) {
    }
}
