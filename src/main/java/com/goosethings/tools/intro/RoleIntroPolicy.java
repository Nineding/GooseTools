package com.goosethings.tools.intro;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Per-viewer opening roster. Other players' roles never enter the wire snapshot. */
public final class RoleIntroPolicy {
    public static boolean hasOpeningLock(java.util.Set<String> tags, boolean participantsLocked) {
        return tags.contains("ggdRoleShowLocked")
                || tags.contains("Parasite") && tags.contains("parasiteInside") && participantsLocked;
    }
    public static final int GOOSE = 0, DUCK = 1, BIRD = 2, LOVER = 3;
    public static final int MAX_PLAYERS = 21;

    private RoleIntroPolicy() {}

    public static boolean eligible(boolean dlc, Set<String> tags, int ready) {
        return dlc && ready >= 1 && ready <= 2 && tags.contains("players")
                && !tags.contains("spectator") && !tags.contains("inTutorial");
    }

    public static int theme(Set<String> tags) {
        if (tags.contains("Lover")) return LOVER;
        if (tags.contains("evil")) return DUCK;
        return tags.contains("good") ? GOOSE : BIRD;
    }

    public static List<Member> roster(Player viewer, List<Player> participants) {
        List<Member> result = new ArrayList<>();
        // Put self in the middle, rather than sorting the public Goose roster by faction.
        List<Player> main = new ArrayList<>();
        int theme = theme(viewer.tags());
        for (Player target : participants) {
            if (target.id().equals(viewer.id())) continue;
            boolean show = switch (theme) {
                case GOOSE -> true;
                case DUCK -> target.tags().contains("evil") || target.tags().contains("Mimic");
                case LOVER -> partners(viewer, target, participants);
                default -> false;
            };
            if (show) main.add(target);
        }
        main.add(main.size() / 2, viewer);
        for (Player player : main) result.add(new Member(player.id(), false));
        if (theme == LOVER && viewer.tags().contains("evil")) {
            for (Player target : participants) {
                // Match show_duck_teammates: non-Lover ducks plus Mimic, without duplicates.
                boolean teammate = target.tags().contains("evil") && !target.tags().contains("Lover")
                        || target.tags().contains("Mimic");
                if (teammate && result.stream().noneMatch(member -> member.id().equals(target.id()))) {
                    result.add(new Member(target.id(), true));
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean partners(Player viewer, Player target, List<Player> participants) {
        if (!target.tags().contains("Lover")) return false;
        if (viewer.seat() > 0 && target.seat() > 0
                && (viewer.tags().contains("lover_with_" + target.seat())
                || target.tags().contains("lover_with_" + viewer.seat()))) return true;
        if (hasEdges(viewer.tags()) || hasEdges(target.tags())) return false;
        return participants.stream().filter(player -> player.tags().contains("Lover")).count() == 2;
    }

    private static boolean hasEdges(Set<String> tags) {
        return tags.stream().anyMatch(tag -> tag.startsWith("lover_with_"));
    }

    public static int seat(Set<String> tags) {
        for (int i = 1; i <= MAX_PLAYERS; i++) if (tags.contains("p" + i)) return i;
        return 0;
    }

    public record Player(UUID id, int seat, Set<String> tags) {
        public Player { tags = Set.copyOf(tags); }
    }
    public record Member(UUID id, boolean small) {}
}
