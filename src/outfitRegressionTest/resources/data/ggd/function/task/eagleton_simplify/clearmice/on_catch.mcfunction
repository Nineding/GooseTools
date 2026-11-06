execute if entity @s[tag=clearmiceJustCatch] run return 0
execute unless entity @s[tag=inTaskClearMice] run return 0

tag @s add clearmiceJustCatch
clear @s *[custom_data~{clearmice_mouse:1b}]
execute as @s at @s run kill @e[type=item,distance=..6,nbt={Item:{components:{"minecraft:custom_data":{clearmice_mouse:1b}}}}]
scoreboard players add @s ggdMiceCaught 1
execute as @s at @s run playsound minecraft:entity.item.pickup record @s ~ ~ ~ 1 1.4
function ggd:task/eagleton_simplify/clearmice/pick_slot
scoreboard players set @s ggdMiceRefresh 0
execute if score @s ggdMiceCaught matches 3.. run return run function ggd:task/eagleton_simplify/clearmice/finish
execute if entity @s[tag=ggdInvProcessing] run return 0
advancement revoke @s only mainsys:test/ifinvchange
advancement grant @s only mainsys:test/ifinvchange
