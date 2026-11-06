execute unless score map gamesetting matches 11 run return 0
execute if entity @s[tag=spectator,tag=!dlcGhostActive] run return 0
execute unless entity @s[tag=players,tag=!inTalk,tag=!endGame,tag=!inPelican,tag=!astralProjected,tag=!sniperScoped,tag=!esperPossessing] run return 0
execute unless entity @e[type=marker,tag=reading,tag=task,distance=..3] run return run title @s actionbar {"color":"#c7fbff","italic":false,"translate":"text.task.reading.wrongplace","fallback":"Go to the library to accept Reading."}
execute if entity @s[tag=inTask] run return 0
execute unless entity @s[tag=task.reading.available,tag=!task.reading.finished] run return 0
execute unless score @s ggdId matches 1.. store result score @s ggdId run random value 1..2147483647
scoreboard players operation #Owner ggdReading = @s ggdId
execute as @e[tag=reading_record] if score @s ggdId = #Owner ggdReading at @s run function ggd:task/eagleton_simplify/reading/restore
tag @s add inTask
tag @s add inTaskReading
tag @s remove navigating_to_reading
clear @s compass[custom_data~{NavCompass:1b}]
clear @s carrot_on_a_stick[custom_data~{reading:1b}]
scoreboard players set @s ggdReading 0
scoreboard players set @s ggdReadTime 0
title @s actionbar {"color":"#c7fbff","italic":false,"translate":"text.task.reading.start","fallback":"Borrow one book from the bookshelves."}
