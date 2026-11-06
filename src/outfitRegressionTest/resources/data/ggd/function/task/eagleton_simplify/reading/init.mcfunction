execute unless score map gamesetting matches 11 run return 0
execute unless loaded -1581 74 -550 run return run schedule function ggd:task/eagleton_simplify/reading/init 1t replace
execute unless loaded -1581 76 -542 run return run schedule function ggd:task/eagleton_simplify/reading/init 1t replace
function ggd:task/eagleton_simplify/reading/load
execute as @a[tag=inTaskReading] at @s run function ggd:task/eagleton_simplify/reading/clear_player
execute as @e[tag=reading_record] at @s run function ggd:task/eagleton_simplify/reading/restore
kill @e[type=interaction,tag=reading_shelf]
kill @e[type=marker,tag=reading]
summon marker -1583 74 -546 {Tags:["task","taskgui","reading","eagleton_marker","ggdGameTransient"]}
function ggd:task/eagleton_simplify/reading/init_slots
function ggd:task/eagleton_simplify/reading/refill
