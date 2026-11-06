schedule clear ggd:task/eagleton_simplify/reading/init_slots
schedule clear ggd:task/eagleton_simplify/reading/refill
schedule clear ggd:task/eagleton_simplify/reading/init
execute in minecraft:overworld as @a[tag=inTaskReading] at @s run function ggd:task/eagleton_simplify/reading/clear_player
execute in minecraft:overworld as @e[tag=reading_record] run function ggd:task/eagleton_simplify/reading/restore
execute in minecraft:overworld run kill @e[type=interaction,tag=reading_shelf]
execute in minecraft:overworld run kill @e[type=marker,tag=reading_empty]
execute in minecraft:overworld run kill @e[type=marker,tag=reading_book_slot]
execute in minecraft:overworld run kill @e[type=marker,tag=reading]
schedule clear ggd:task/eagleton_simplify/reading/refill
schedule clear ggd:task/eagleton_simplify/reading/init_slots
