tag @s add clickProcessed
execute unless entity @s[tag=Debugger] run return 0
execute if entity @a[tag=gamingGGD] run return run function lobby:invreplace/menu_debug
execute if score HideOutfits ggdadv matches 1 run return run function lobby:gamesetting/ggd/outfit_display/on
function lobby:gamesetting/ggd/outfit_display/off
