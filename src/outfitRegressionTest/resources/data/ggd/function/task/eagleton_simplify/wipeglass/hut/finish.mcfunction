execute unless entity @s[tag=task.wipeglass.available,tag=task.wipeglass_hut.available,tag=!task.wipeglass_hut.finished,tag=inTaskWipeGlass_hut] run return 0
execute unless score @s ggdGuiState matches 2 run return 0
tag @s remove task.wipeglass_hut.available
tag @s add task.wipeglass_hut.finished
function ggd:task/eagleton_simplify/gui/clear_player
title @s title {"translate":"text.task.wipeglass.node_finished","fallback":"Glass cleaned","color":"#55e6c1","italic":false}
playsound minecraft:entity.player.levelup record @s ~ ~ ~ 1 1
function ggd:task/eagleton_simplify/wipeglass/check_complete
