function ggd:talker/vote_history/subject with storage ggd:vote_history queue[0]
data remove storage ggd:vote_history queue[0]
execute if data storage ggd:vote_history queue[0] run function ggd:talker/vote_history/subject_loop
