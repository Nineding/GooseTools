execute unless score map gamesetting matches 11 run return run function ggd:task/eagleton_simplify/wipeglass/clear_all
execute if entity @s[tag=spectator,tag=!dlcGhostActive] run return run function ggd:task/eagleton_simplify/gui/clear_player
execute unless entity @s[tag=players,tag=!inTalk,tag=!endGame,tag=!inPelican,tag=!astralProjected,tag=!sniperScoped,tag=!esperPossessing] run return run function ggd:task/eagleton_simplify/gui/clear_player
execute unless dimension minecraft:overworld run return run function ggd:task/eagleton_simplify/wipeglass/hut/fail
execute unless entity @s[tag=task.wipeglass.available,tag=task.wipeglass_hut.available,tag=!task.wipeglass_hut.finished] run return run function ggd:task/eagleton_simplify/gui/clear_player
execute if score @s ggdGuiState matches 2 run return run function ggd:task/eagleton_simplify/wipeglass/hut/finish
execute unless score @s ggdGuiState matches 1 run return run function ggd:task/eagleton_simplify/wipeglass/hut/fail
