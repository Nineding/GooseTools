$data modify storage ggd:vote_history display.lore set from storage ggd:vote_history records.r$(round).lore
$data modify storage ggd:vote_history display.round set value $(round)
$data modify storage ggd:vote_history display.slot set value $(slot)
function ggd:talker/vote_history/give with storage ggd:vote_history display
