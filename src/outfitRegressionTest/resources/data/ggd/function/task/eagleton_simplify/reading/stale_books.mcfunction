# Migrate old overlays and snapshots whenever their chunks become loaded.
function ggd:task/eagleton_simplify/reading/hide_records
execute in minecraft:overworld run kill @e[type=interaction,tag=reading_shelf]
execute in minecraft:overworld if score map gamesetting matches 11 if entity @e[type=marker,tag=reading] unless entity @e[type=marker,tag=reading_book_slot] run function ggd:task/eagleton_simplify/reading/init_slots
execute as @a[tag=inTaskReading,scores={ggdReading=1..}] run function ggd:task/eagleton_simplify/reading/validate_record
execute in minecraft:overworld as @e[tag=reading_record] run function ggd:task/eagleton_simplify/reading/orphan_record
execute in minecraft:overworld if score map gamesetting matches 11 as @a[tag=!inTaskReading] at @s if items entity @s container.* *[custom_data~{readingShelf:1b}] run function ggd:task/eagleton_simplify/reading/return_stolen_book
execute in minecraft:overworld if score map gamesetting matches 11 as @a[tag=!inTaskReading] at @s if items entity @s player.cursor *[custom_data~{readingShelf:1b}] run function ggd:task/eagleton_simplify/reading/return_stolen_book
execute as @a[tag=!inTaskReading] run clear @s *[custom_data~{readingLoan:1b}]
