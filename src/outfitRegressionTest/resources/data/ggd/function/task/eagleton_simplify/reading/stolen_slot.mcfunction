function ggd:task/eagleton_simplify/reading/scan_shelves
$data modify storage ggd:reading args.source_slot set value "$(source_slot)"
scoreboard players set #Stashed ggdReading 0
execute as @e[type=marker,tag=reading_empty,sort=random,limit=1] run function ggd:task/eagleton_simplify/reading/stash_stolen
$execute if score #Stashed ggdReading matches 1 run item replace entity @s $(source_slot) with air
