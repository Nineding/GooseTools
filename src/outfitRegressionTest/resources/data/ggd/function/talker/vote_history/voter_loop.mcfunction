data modify storage ggd:vote_history voter set from storage ggd:vote_history voter_queue[0]
data modify storage ggd:vote_history voter.round set from storage ggd:vote_history row.round
data modify storage ggd:vote_history voter.target set from storage ggd:vote_history row.target
execute if data storage ggd:vote_history row{set:"skip"} run data modify storage ggd:vote_history voter.target set value "skip"
function ggd:talker/vote_history/voter with storage ggd:vote_history voter
data remove storage ggd:vote_history voter_queue[0]
execute if data storage ggd:vote_history voter_queue[0] run function ggd:talker/vote_history/voter_loop
