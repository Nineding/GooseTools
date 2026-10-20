package com.goosethings.tools.client.ai;

/** Pure queue policy: protected presentation never consumes a pending question. */
final class AiNameVoteOpenQueue<T> {
    private long id;
    private T pending;

    void offer(long id, T value) { this.id = id; pending = value; }
    T consume(boolean blocked) {
        if (blocked) return null;
        T value = pending;
        pending = null;
        return value;
    }
    void cancel(long id) { if (this.id == id) clear(); }
    void clear() { pending = null; }
    void removeIf(java.util.function.Predicate<T> test) {if(pending!=null && test.test(pending)) clear();}
}
