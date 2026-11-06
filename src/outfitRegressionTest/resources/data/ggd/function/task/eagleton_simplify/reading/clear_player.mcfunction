execute on vehicle if entity @s[type=minecraft:cushion,x=-1592,y=74,z=-553,dx=14,dy=4,dz=14,nbt={color:"brown"}] on passengers if entity @s[tag=inTaskReading] run ride @s dismount
scoreboard players operation #Owner ggdReading = @s ggdId
execute in minecraft:overworld as @e[tag=reading_record] if score @s ggdId = #Owner ggdReading at @s run function ggd:task/eagleton_simplify/reading/restore
clear @s *[custom_data~{readingLoan:1b}]
clear @s *[custom_data~{readingShelf:1b}]
tag @s remove inTaskReading
tag @s remove inTask
scoreboard players reset @s ggdReading
scoreboard players reset @s ggdReadTime
advancement grant @s only mainsys:test/ifinvchange
