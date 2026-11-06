execute unless score map gamesetting matches 11 run return 0
execute if entity @s[tag=spectator,tag=!dlcGhostActive] run return 0
execute unless entity @s[tag=players,tag=!inTalk,tag=!endGame,tag=!inPelican,tag=!astralProjected,tag=!sniperScoped,tag=!esperPossessing] run return 0
execute unless dimension minecraft:overworld run return 0
execute unless entity @s[tag=task.wipeglass.available,tag=task.wipeglass_hut.available,tag=!task.wipeglass_hut.finished,tag=!inTask] run return 0
execute unless entity @e[type=marker,tag=task,tag=wipeglass_hut,distance=..3] run return 0
tag @s add inTask
tag @s add inTaskGooseGui
tag @s add inTaskWipeGlass_hut
tag @s remove navigating_to_wipeglass_hut
clear @s compass[custom_data~{NavCompass:1b}]
clear @s carrot_on_a_stick[custom_data~{wipeglass_hut:1b}]
execute store result score @s ggdGuiState run goosetools gui bind @s cleaning
execute unless score @s ggdGuiState matches 1 run return run function ggd:task/eagleton_simplify/wipeglass/hut/fail
title @s title {"translate":"text.task.start","fallback":"Task Start","color":"#8af1ff"}
title @s subtitle {"translate":"text.task.wipeglass_hut.start","fallback":"Wipe all glass stains. Complete every location to finish this series.","color":"#c7fbff"}
playsound minecraft:entity.player.levelup record @s ~ ~ ~ 1 0.5
advancement grant @s only mainsys:test/ifinvchange
