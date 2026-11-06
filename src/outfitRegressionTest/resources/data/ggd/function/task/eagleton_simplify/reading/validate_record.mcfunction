# A reconnect must not reuse a book whose offline loan has already been restored.
scoreboard players operation #Owner ggdReading = @s ggdId
scoreboard players set #HasRecord ggdReading 0
execute in minecraft:overworld as @e[tag=reading_record] if score @s ggdId = #Owner ggdReading run scoreboard players add #HasRecord ggdReading 1
execute unless score #HasRecord ggdReading matches 1 run function ggd:task/eagleton_simplify/reading/fail {reason:"lost"}
