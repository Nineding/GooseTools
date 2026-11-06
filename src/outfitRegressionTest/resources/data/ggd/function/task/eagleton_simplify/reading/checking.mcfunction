execute if entity @s[tag=spectator,tag=!dlcGhostActive] run return run function ggd:task/eagleton_simplify/reading/clear_player
execute unless entity @s[tag=players,tag=!inTalk,tag=!endGame,tag=!inPelican,tag=!astralProjected,tag=!sniperScoped,tag=!esperPossessing] run return run function ggd:task/eagleton_simplify/reading/clear_player
execute unless dimension minecraft:overworld run return run function ggd:task/eagleton_simplify/reading/fail {reason:"leave"}
execute unless entity @s[x=-1592,y=74,z=-553,dx=14,dy=4,dz=14] run return run function ggd:task/eagleton_simplify/reading/fail {reason:"leave"}
execute if score @s ggdReading matches 0 run function ggd:task/eagleton_simplify/reading/book_taken
execute unless entity @s[tag=inTaskReading] run return 0
execute if score @s ggdReading matches 0 run return run title @s actionbar {"color":"#c7fbff","italic":false,"translate":"text.task.reading.start","fallback":"Borrow one book from the bookshelves."}
execute store result storage ggd:reading args.owner int 1 run scoreboard players get @s ggdId
function ggd:task/eagleton_simplify/reading/check_book with storage ggd:reading args
execute unless entity @s[tag=inTaskReading] run return 0
execute if score @s ggdReading matches 2 run return run title @s actionbar {"color":"#c7fbff","italic":false,"translate":"text.task.reading.return","fallback":"Reading complete! Hold your book and right-click a bookshelf to return it."}
scoreboard players set #Sitting ggdReading 0
execute on vehicle if entity @s[type=minecraft:cushion,x=-1592,y=74,z=-553,dx=14,dy=4,dz=14,nbt={color:"brown"}] run scoreboard players set #Sitting ggdReading 1
execute if score #Sitting ggdReading matches 1 run scoreboard players add @s ggdReadTime 1
execute unless score #Sitting ggdReading matches 1 run scoreboard players set @s ggdReadTime 0
execute if score @s ggdReadTime matches 200.. run scoreboard players set @s ggdReading 2
execute if score @s ggdReading matches 2 run return run title @s actionbar {"color":"#c7fbff","italic":false,"translate":"text.task.reading.return","fallback":"Reading complete! Hold your book and right-click a bookshelf to return it."}
execute unless score #Sitting ggdReading matches 1 run return run title @s actionbar {"color":"#c7fbff","italic":false,"translate":"text.task.reading.sit","fallback":"Sit on a brown cushion with your book for 10 seconds."}
scoreboard players operation #Seconds ggdReadTime = @s ggdReadTime
scoreboard players set #20 ggdReadTime 20
scoreboard players operation #Seconds ggdReadTime /= #20 ggdReadTime
title @s actionbar {"color":"#8af1ff","translate":"text.task.reading.progress","fallback":"Reading: %s / 10 seconds","with":[{"score":{"name":"#Seconds","objective":"ggdReadTime"}}]}
