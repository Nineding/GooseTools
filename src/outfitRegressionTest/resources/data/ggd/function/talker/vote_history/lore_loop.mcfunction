data modify storage ggd:vote_history display.line set from storage ggd:vote_history display.lore[0]
function ggd:talker/vote_history/append_lore with storage ggd:vote_history display
data remove storage ggd:vote_history display.lore[0]
execute if data storage ggd:vote_history display.lore[0] run function ggd:talker/vote_history/lore_loop
