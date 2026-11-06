# Migrate old snapshots, including those loaded after /reload.
execute in minecraft:overworld as @e[type=armor_stand,tag=reading_record,tag=!reading_record_hidden] at @s run tp @s ~ -60 ~
execute in minecraft:overworld run tag @e[type=armor_stand,tag=reading_record,tag=!reading_record_hidden] add reading_record_hidden
