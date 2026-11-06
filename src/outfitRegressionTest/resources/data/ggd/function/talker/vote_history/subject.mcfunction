$data modify storage ggd:vote_history build.names.$(set) set value {text:"$(real_name)"}
$data modify storage ggd:vote_history build.names.$(set) set from storage ggd:role $(set).display_name
$execute unless entity @a[tag=$(set),tag=inTalk] run return 0
$data modify storage ggd:vote_history row.set set value "$(set)"
$data modify storage ggd:vote_history row.target set value "$(real_name)"
data modify storage ggd:vote_history row.votes set value 0
$execute store result storage ggd:vote_history row.votes int 1 run scoreboard players get @a[tag=$(set),limit=1] ggdGetVote
function ggd:talker/vote_history/total_line with storage ggd:vote_history row
execute if score DoNikeInVote ggdadv matches 0 run function ggd:talker/vote_history/voters
data modify storage ggd:vote_history build.lore append from storage ggd:vote_history line
