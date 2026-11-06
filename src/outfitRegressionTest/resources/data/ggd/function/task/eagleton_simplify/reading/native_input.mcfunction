# The native transaction is already complete; reconcile this player synchronously.
execute if entity @s[tag=spectator,tag=!dlcGhostActive] run return run function ggd:task/eagleton_simplify/reading/return_stolen_book
execute unless entity @s[tag=inTaskReading,tag=players,tag=!inTalk,tag=!endGame,tag=!inPelican,tag=!astralProjected] run return run function ggd:task/eagleton_simplify/reading/return_stolen_book
execute unless entity @s[x=-1592,y=74,z=-553,dx=14,dy=4,dz=14] run return run function ggd:task/eagleton_simplify/reading/fail {reason:"leave"}
execute if score #MeetingPhase ggdSession matches 1.. run return run function ggd:task/eagleton_simplify/reading/clear_player
execute if score @s ggdReading matches 0 run return run function ggd:task/eagleton_simplify/reading/book_taken
execute store result storage ggd:reading args.owner int 1 run scoreboard players get @s ggdId
function ggd:task/eagleton_simplify/reading/check_book with storage ggd:reading args
