package com.goosethings.tools.nametag;

import java.util.Map;

/** Resolves FullBlood wardrobe scoreboard IDs to the exact colour used by its armour function. */
final class WardrobeColourResolver {
    private static final int CUSTOM_OUTFIT = 25;
    private static final Map<Integer, Integer> PRESET_COLOURS = Map.ofEntries(
            Map.entry(1, 0xff9999),
            Map.entry(2, 0x99d6ff),
            Map.entry(3, 0x99ff99),
            Map.entry(4, 0x000000),
            Map.entry(5, 0xffffff),
            Map.entry(6, 0xfaef56),
            Map.entry(7, 0xeb7e53),
            Map.entry(8, 0xff3b3b),
            Map.entry(9, 0x00ffd5),
            Map.entry(10, 0x0400ff),
            Map.entry(11, 0x8400ff),
            Map.entry(12, 0x818181),
            Map.entry(13, 0x8a5d33),
            Map.entry(14, 0xa06740),
            Map.entry(20, 0xc87847),
            Map.entry(21, 0xd8d8d8),
            Map.entry(22, 0xfcee4b),
            Map.entry(23, 0x4aedd9),
            Map.entry(24, 0x443a3b),
            Map.entry(31, 0x98e2c6),
            Map.entry(32, 0x87ceeb),
            Map.entry(33, 0xf7b7c9),
            Map.entry(34, 0xf9e79f),
            Map.entry(35, 0xffcba4),
            Map.entry(36, 0xc8b6ff),
            Map.entry(37, 0xb8e986),
            Map.entry(38, 0x7fdbda),
            Map.entry(39, 0xff9a8b),
            Map.entry(40, 0xb39ddb),
            Map.entry(41, 0xa9d6f5),
            Map.entry(42, 0xf3d5b5),
            Map.entry(43, 0xa8e6a3),
            Map.entry(44, 0xc9d6d5),
            Map.entry(45, 0xffd166),
            Map.entry(46, 0x9bb7ff));

    private WardrobeColourResolver() {
    }

    static Integer resolve(int outfitId, int customRgb) {
        if (outfitId == CUSTOM_OUTFIT) {
            return customRgb & 0x00ffffff;
        }
        return PRESET_COLOURS.get(outfitId);
    }
}
