scoreboard players add @s ggdMiceTime 1
execute if score @s ggdMiceTime matches 200.. run return run function ggd:task/eagleton_simplify/clearmice/fail_timeout

scoreboard players add @s ggdMiceRefresh 1
execute if score @s ggdMiceRefresh matches 20.. run function ggd:task/eagleton_simplify/clearmice/hop

execute as @s at @s if entity @e[type=item,distance=..4,nbt={Item:{components:{"minecraft:custom_data":{clearmice_mouse:1b}}}}] run function ggd:task/eagleton_simplify/clearmice/on_catch

scoreboard players set #mice_left ggdMiceTime 200
scoreboard players operation #mice_left ggdMiceTime -= @s ggdMiceTime
scoreboard players set #mice_div ggdMiceTime 20
scoreboard players operation #mice_left ggdMiceTime /= #mice_div ggdMiceTime
title @s actionbar [{"color":"#8af1ff","translate":"text.task.clearmice.progress","fallback":"Caught %s/%s  %s s left","with":[{"score":{"name":"@s","objective":"ggdMiceCaught"}},{"text":"3"},{"score":{"name":"#mice_left","objective":"ggdMiceTime"}}]}]

tag @s remove clearmiceJustCatch
