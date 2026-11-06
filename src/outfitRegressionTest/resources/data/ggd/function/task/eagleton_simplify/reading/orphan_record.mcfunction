scoreboard players set #Present ggdReading 0
scoreboard players operation #Owner ggdReading = @s ggdId
execute as @a[tag=inTaskReading] if score @s ggdId = #Owner ggdReading run scoreboard players set #Present ggdReading 1
execute unless score #Present ggdReading matches 1 run function ggd:task/eagleton_simplify/reading/restore
