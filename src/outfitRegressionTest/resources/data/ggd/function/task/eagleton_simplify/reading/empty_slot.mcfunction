$summon marker -1580.5 $(y) $(z) {Tags:["reading_empty","reading_empty_new"]}
$scoreboard players set @e[tag=reading_empty_new,limit=1] ggdReadY $(y)
$scoreboard players set @e[tag=reading_empty_new,limit=1] ggdReadZ $(z)
$scoreboard players set @e[tag=reading_empty_new,limit=1] ggdReadSlot $(slot)
tag @e[tag=reading_empty_new] remove reading_empty_new
