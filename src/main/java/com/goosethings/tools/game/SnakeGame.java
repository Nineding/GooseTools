package com.goosethings.tools.game;

import java.util.ArrayDeque;

final class SnakeGame extends ArcadeGame {
    final ArrayDeque<Integer> snake = new ArrayDeque<>(), directions = new ArrayDeque<>();
    int direction = 1, food;
    private int accumulator;
    SnakeGame(long seed, int mode, int difficulty) {
        super(seed, mode, difficulty); cols = 20; rows = 15;
        snake.add(7 * cols + 8); snake.add(7 * cols + 7); snake.add(7 * cols + 6); spawn();
    }
    private void spawn() {
        int count = cols * rows - snake.size();
        if (count == 0) { food = -1; win(); return; }
        int skip = random.nextInt(count);
        for (int c = 0; c < cols * rows; c++) if (!snake.contains(c) && skip-- == 0) { food = c; return; }
    }
    @Override boolean input(int action, int value, double x, double y) {
        if (action != GameSession.DIRECTION || value < 0 || value > 3 || directions.size() >= 2) return false;
        int last = directions.isEmpty() ? direction : directions.getLast();
        if (value == last || value == (last + 2) % 4) return false;
        directions.add(value); return true;
    }
    @Override void tick(int millis) {
        super.tick(millis); accumulator += millis;
        int interval = new int[]{200, 140, 90}[difficulty];
        if (accumulator < interval) return; accumulator -= interval;
        if (!directions.isEmpty()) direction = directions.removeFirst();
        int head = snake.getFirst(), x = head % cols, y = head / cols;
        x += direction == 1 ? 1 : direction == 3 ? -1 : 0;
        y += direction == 2 ? 1 : direction == 0 ? -1 : 0;
        if (x < 0 || x >= cols || y < 0 || y >= rows) { lose(); return; }
        int next = y * cols + x; boolean eat = next == food;
        // Moving into the departing tail is legal when not growing.
        if (snake.contains(next) && (eat || next != snake.getLast())) { lose(); return; }
        snake.addFirst(next);
        if (eat) { score = add(score, 1); cue(2, next); spawn(); }
        else snake.removeLast();
    }
    @Override int[] board() {
        int[] board = new int[cols * rows]; for (int c : snake) board[c] = 1;
        board[snake.getFirst()] = 2; if (food >= 0) board[food] = 3; return board;
    }
    @Override double[] actors() { return new double[]{direction, snake.size()}; }
}
