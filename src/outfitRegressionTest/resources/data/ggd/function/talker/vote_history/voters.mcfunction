data modify storage ggd:vote_history line.extra append value {text:" ← ",color:"#8CAEA0"}
data modify storage ggd:vote_history voter_queue set from storage ggd:role data
execute if data storage ggd:vote_history voter_queue[0] run function ggd:talker/vote_history/voter_loop
