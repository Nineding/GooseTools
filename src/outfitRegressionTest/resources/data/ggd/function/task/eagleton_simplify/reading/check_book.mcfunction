function ggd:task/eagleton_simplify/reading/validate_record
execute unless entity @s[tag=inTaskReading] run return 0
$execute store result score #Owned ggdReading run clear @s *[custom_data~{readingLoan:1b,readingOwner:$(owner)}] 0
execute store result score #All ggdReading run clear @s *[custom_data~{readingLoan:1b}] 0
execute store result score #Extra ggdReading run clear @s *[custom_data~{readingShelf:1b},!custom_data~{readingLoan:1b}] 0
execute if score #Owned ggdReading matches 2.. run return run function ggd:task/eagleton_simplify/reading/fail {reason:"extra"}
execute if score #All ggdReading matches 2.. run return run function ggd:task/eagleton_simplify/reading/fail {reason:"extra"}
execute if score #Extra ggdReading matches 1.. run return run function ggd:task/eagleton_simplify/reading/fail {reason:"extra"}
execute if score #Owned ggdReading matches 1 if score #All ggdReading matches 1 run return 1
scoreboard players set #Returned ggdReading 0
execute in minecraft:overworld as @e[type=marker,tag=reading_book_slot] run function ggd:task/eagleton_simplify/reading/find_return
execute if score #Returned ggdReading matches 2.. run return run function ggd:task/eagleton_simplify/reading/fail {reason:"extra"}
execute unless score #Returned ggdReading matches 1 run return run function ggd:task/eagleton_simplify/reading/fail {reason:"lost"}
execute unless score @s ggdReading matches 2 run return run function ggd:task/eagleton_simplify/reading/fail {reason:"early"}
function ggd:task/eagleton_simplify/reading/complete_return_slot with storage ggd:reading return_args
function ggd:task/eagleton_simplify/reading/finish
