execute unless entity @s[tag=Debugger] run return 0
execute if entity @a[tag=gamingGGD] run return 0
scoreboard players set ShowMeetingVotes ggdadv 0
clear @a *[custom_data~{VoteHistory:1b}]
execute as @a[tag=inDebugMenu,tag=Debugger] run function lobby:invreplace/menu_debug
