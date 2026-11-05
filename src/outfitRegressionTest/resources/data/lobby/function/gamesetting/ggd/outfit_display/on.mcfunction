execute unless entity @s[tag=Debugger] run return 0
execute if entity @a[tag=gamingGGD] run return 0
scoreboard players set HideOutfits ggdadv 0
playsound minecraft:entity.experience_orb.pickup player @s ~ ~ ~ 0.45 1.2
execute as @a[tag=inDebugMenu,tag=Debugger] run function lobby:invreplace/menu_debug
function lobby:settings_display/update
