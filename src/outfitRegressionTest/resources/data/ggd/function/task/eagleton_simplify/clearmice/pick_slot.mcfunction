scoreboard players operation #mice_old ggdMiceSlot = @s ggdMiceSlot
execute store result score @s ggdMiceSlot run random value 0..26
execute if score @s ggdMiceSlot = #mice_old ggdMiceSlot store result score @s ggdMiceSlot run random value 0..26
