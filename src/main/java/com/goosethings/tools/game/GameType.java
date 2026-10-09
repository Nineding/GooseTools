package com.goosethings.tools.game;

public enum GameType {
    FLAPPY("flappy", "Flappy Bird"), SNAKE("snake", "Snake"), PONG("pong", "Pong"),
    WHACK("whack", "Whac-A-Mole"), MINES("minesweeper", "Minesweeper"), MERGE("2048", "2048"),
    TRAFFIC("traffic", "Traffic dodge");
    public final String id, fallback;
    GameType(String id, String fallback) { this.id = id; this.fallback = fallback; }
}
