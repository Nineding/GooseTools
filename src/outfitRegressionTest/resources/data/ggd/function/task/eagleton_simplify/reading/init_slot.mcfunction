$summon marker -1580.5 $(y) $(z) {Tags:["reading_book_slot","reading_slot_new","eagleton_marker","ggdGameTransient"],data:{y:$(y),z:$(z),slot:$(slot)}}
$scoreboard players set @e[type=marker,tag=reading_slot_new,limit=1] ggdReadY $(y)
$scoreboard players set @e[type=marker,tag=reading_slot_new,limit=1] ggdReadZ $(z)
$scoreboard players set @e[type=marker,tag=reading_slot_new,limit=1] ggdReadSlot $(slot)
tag @e[tag=reading_slot_new] remove reading_slot_new
$execute if items block -1581 $(y) $(z) container.$(slot) * run item modify block -1581 $(y) $(z) container.$(slot) ggd:reading_shelf
