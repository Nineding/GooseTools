package com.goosethings.tools.gomoku;

import java.util.*;

/** In-memory seats and invitations, independent from Minecraft and client screens. */
public final class GomokuRoom {
    public enum Reply { OK, UNAVAILABLE, BUSY, INVALID, EXPIRED }
    public record Invite(long id, UUID from, UUID to, GomokuAccess.Group group, long expires, int kind) {}
    private final Map<UUID, GomokuAccess.Group> access = new HashMap<>();
    private final Map<UUID, GomokuMatch> seats = new HashMap<>();
    private final Map<Long, GomokuMatch> games = new LinkedHashMap<>();
    private final Map<Long, Invite> invites = new LinkedHashMap<>();
    private long nextId = 1;

    public GomokuAccess.Group access(UUID player) { return access.get(player); }
    public GomokuMatch match(UUID player) { return seats.get(player); }
    public Collection<GomokuMatch> games() { return List.copyOf(games.values()); }
    public Collection<Invite> invites() { return List.copyOf(invites.values()); }
    public boolean free(UUID player) { return access.containsKey(player) && !seats.containsKey(player) && pending(player) == null; }
    public Invite pending(UUID player) {
        return invites.values().stream().filter(i -> i.from.equals(player) || i.to.equals(player)).findFirst().orElse(null);
    }
    public void update(UUID player, GomokuAccess.Group group, long now) {
        if (Objects.equals(access.get(player), group)) return;
        cancel(player, now);
        if (group == null) access.remove(player); else access.put(player, group);
    }
    public Reply invite(UUID from, UUID to, long now) {
        return invite(from, to, now, 0);
    }
    public Reply invite(UUID from, UUID to, long now, int kind) {
        if (kind < 0 || kind > 2) return Reply.INVALID;
        tick(now);
        if (from.equals(to)) return Reply.INVALID;
        if (access.get(from) == null || !Objects.equals(access.get(from), access.get(to))) return Reply.UNAVAILABLE;
        if (!free(from) || !free(to)) return Reply.BUSY;
        var invite = new Invite(nextId++, from, to, access.get(from), now + 30_000, kind);
        invites.put(invite.id, invite); return Reply.OK;
    }
    public Reply answer(UUID player, long inviteId, boolean accept, boolean inviterBlack, long now) {
        Invite invite = invites.get(inviteId);
        if (invite == null || now >= invite.expires) { tick(now); return Reply.EXPIRED; }
        if (!invite.to.equals(player)) return Reply.INVALID;
        invites.remove(inviteId);
        if (!accept) return Reply.OK;
        if (!Objects.equals(access.get(player), invite.group) || !Objects.equals(access.get(invite.from), invite.group)) return Reply.UNAVAILABLE;
        if (seats.containsKey(player) || seats.containsKey(invite.from)) return Reply.BUSY;
        add(new GomokuMatch(nextId++, inviterBlack ? invite.from : player, inviterBlack ? player : invite.from, invite.group, 1, now, invite.kind));
        return Reply.OK;
    }
    public void cancelInvite(UUID player) { invites.values().removeIf(i -> i.from.equals(player) || i.to.equals(player)); }
    public Reply startAi(UUID player, int humanColor, int difficulty, long now) {
        return startAi(player, humanColor, difficulty, now, 0);
    }
    public Reply startAi(UUID player, int humanColor, int difficulty, long now, int kind) {
        if (kind < 0 || kind > 2) return Reply.INVALID;
        if (access.get(player) == null) return Reply.UNAVAILABLE;
        if (humanColor < 1 || humanColor > 2 || difficulty < 0 || difficulty > 2) return Reply.INVALID;
        if (seats.containsKey(player)) return Reply.BUSY;
        cancelInvite(player);
        add(new GomokuMatch(nextId++, humanColor == 1 ? player : null, humanColor == 2 ? player : null, access.get(player), difficulty, now, kind));
        return Reply.OK;
    }
    public boolean rematch(UUID player, long id, long sequence, long now) {
        GomokuMatch game = seats.get(player);
        if (game == null || game.id != id || !game.sequence(player, sequence)) return false;
        for (UUID member : new UUID[]{game.black, game.white})
            if (member != null && (seats.get(member) != game || !Objects.equals(access.get(member), game.group))) return false;
        if (!game.requestRematch(player, now)) return false;
        UUID black = game.white, white = game.black;
        remove(game);
        add(new GomokuMatch(nextId++, black, white, game.group, game.difficulty, now, game.kind));
        return true;
    }
    public void leaveFinished(UUID player) {
        GomokuMatch game = seats.get(player);
        if (game != null && game.outcome() != GomokuMatch.RUNNING) remove(game);
    }
    public void cancel(UUID player, long now) {
        cancelInvite(player);
        GomokuMatch game = seats.get(player);
        if (game != null) { game.cancel(now); remove(game); }
    }
    public void disconnect(UUID player, long now) {
        cancelInvite(player); access.remove(player);
        GomokuMatch game = seats.get(player);
        if (game != null) { game.expire(now); game.forfeit(player, GomokuMatch.DISCONNECT, now); seats.remove(player); }
    }
    public void tick(long now) {
        invites.values().removeIf(i -> now >= i.expires);
        for (GomokuMatch game : List.copyOf(games.values())) {
            game.expire(now);
            if (game.outcome() != GomokuMatch.RUNNING && now - game.finishedAt() >= 120_000) remove(game);
        }
    }
    private void add(GomokuMatch game) {
        games.put(game.id, game);
        if (game.black != null) seats.put(game.black, game);
        if (game.white != null) seats.put(game.white, game);
    }
    private void remove(GomokuMatch game) {
        games.remove(game.id);
        if (game.black != null) seats.remove(game.black, game);
        if (game.white != null) seats.remove(game.white, game);
    }
}
