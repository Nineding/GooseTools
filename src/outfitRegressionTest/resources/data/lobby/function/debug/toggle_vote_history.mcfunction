tag @s add clickProcessed
execute unless entity @s[tag=Debugger] run return 0
execute if entity @a[tag=gamingGGD] run return 0
execute if score ShowMeetingVotes ggdadv matches 1 run return run function lobby:debug/vote_history_off
scoreboard players set ShowMeetingVotes ggdadv 1
execute as @a[tag=inDebugMenu,tag=Debugger] run function lobby:invreplace/menu_debug
