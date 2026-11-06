execute unless score map gamesetting matches 11 run return 0
execute unless entity @e[tag=task,tag=clearmice,distance=..3] at @s run playsound block.anvil.land record @s ~ ~ ~ 1 1
execute unless entity @e[tag=task,tag=clearmice,distance=..3] at @s run title @s title {"color":"#ff3d81","translate":"text.task.wrongplace","fallback":"Wrong Place"}
execute unless entity @e[tag=task,tag=clearmice,distance=..3] at @s run return run title @s subtitle {"color":"#f86f9f","translate":"text.task.wrongplace.clearmice","fallback":"Accept Clear the Mice at the task point"}

execute if entity @s[tag=inTask] run return 0
execute if entity @s[tag=!task.clearmice.available] run return 0
execute if entity @s[tag=task.clearmice.finished] run return 0

scoreboard objectives add ggdMiceTime dummy
scoreboard objectives add ggdMiceCaught dummy
scoreboard objectives add ggdMiceSlot dummy
scoreboard objectives add ggdMiceRefresh dummy

tag @s add inTask
tag @s add inTaskClearMice
tag @s remove navigating_to_clearmice
tag @s remove inTaskMenuGUI
tag @s remove inTaskMenuGUI_Drawn
tag @s remove inHelpInfo
tag @s remove inGameRoleInfo
tag @s add dontGoMainInvClickCheck
clear @s compass[custom_data~{NavCompass:1b}]
clear @s carrot_on_a_stick[custom_data~{clearmice:1b}]
scoreboard players set @s ggdMiceTime 0
scoreboard players set @s ggdMiceCaught 0
scoreboard players set @s ggdMiceRefresh 0
function ggd:task/eagleton_simplify/clearmice/pick_slot
# Release the dream remote before the task takes inventory ownership.
execute if entity @s[tag=inDream] run function ggd:skill/dream/render_remote
function ggd:invrepevent/clearmice

title @s title {"color":"#8af1ff","translate":"text.task.start","fallback":"Task Started"}
title @s subtitle {"color":"#c7fbff","translate":"text.task.clearmice.start","fallback":"Open your inventory and catch 3 mice in 10 seconds"}
execute as @s at @s run playsound minecraft:entity.player.levelup record @s ~ ~ ~ 1 0.5
advancement grant @s only mainsys:test/ifinvchange
