tag @s remove task.clearmice.available
tag @s add task.clearmice.finished
title @s title {"color":"#55ff55","translate":"text.task.finish","fallback":"Task Complete"}
title @s subtitle {"color":"#aaffaa","translate":"text.task.clearmice.success","fallback":"The mice are gone"}
execute as @s at @s run playsound minecraft:entity.player.levelup record @s ~ ~ ~ 1 1
function ggd:task/eagleton_simplify/clearmice/clear_player
