$item replace entity @s hotbar.3 from entity @s $(source_slot)
$scoreboard players set #SourceSlot ggdReading $(index)
$execute unless score #SourceSlot ggdReading matches 3 run item replace entity @s $(source_slot) with air
data modify storage ggd:reading loan_custom set value {readingShelf:1b,readingLoan:1b,readingOwner:0,readingReturnedBy:0}
execute store result storage ggd:reading loan_custom.readingOwner int 1 run scoreboard players get @s ggdId
item modify entity @s hotbar.3 ggd:reading_loan
summon marker -1583 74 -546 {Tags:["reading_record","reading_record_new"]}
scoreboard players operation @e[type=marker,tag=reading_record_new,limit=1] ggdId = @s ggdId
tag @e[tag=reading_record_new] remove reading_record_new
scoreboard players set @s ggdReading 1
scoreboard players set @s ggdReadTime 0
title @s actionbar {"color":"#c7fbff","italic":false,"translate":"text.task.reading.sit","fallback":"Sit on a brown cushion with your book for 10 seconds."}
