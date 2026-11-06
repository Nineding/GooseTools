scoreboard players operation #Owner ggdReading = @s ggdId
execute in minecraft:overworld as @e[tag=reading_record] if score @s ggdId = #Owner ggdReading run kill @s
tag @s remove task.reading.available
tag @s add task.reading.finished
function ggd:task/eagleton_simplify/reading/clear_player
title @s title {"color":"#55ff55","translate":"text.task.finish","fallback":"Task complete"}
title @s subtitle {"color":"#aaffaa","italic":false,"translate":"text.task.reading.success","fallback":"Book returned. Reading task complete!"}
playsound minecraft:entity.player.levelup record @s ~ ~ ~ 1 1
