advancement revoke @s only ggd:task/reading_return
execute unless score map gamesetting matches 11 run return 0
execute unless entity @e[type=marker,tag=reading_book_slot] run function ggd:task/eagleton_simplify/reading/init_slots
data modify storage ggd:reading return_stamp set value {readingReturnedBy:0}
execute store result storage ggd:reading return_stamp.readingReturnedBy int 1 run scoreboard players get @s ggdId
execute in minecraft:overworld as @e[type=marker,tag=reading_book_slot] run function ggd:task/eagleton_simplify/reading/stamp_return with entity @s data
function ggd:task/eagleton_simplify/reading/native_input
