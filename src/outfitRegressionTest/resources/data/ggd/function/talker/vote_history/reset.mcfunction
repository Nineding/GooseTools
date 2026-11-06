scoreboard objectives add ggdVoteHistory dummy
scoreboard players set #Round ggdVoteHistory 0
scoreboard players set #Captured ggdVoteHistory 0
data modify storage ggd:vote_history records set value {}
clear @a *[custom_data~{VoteHistory:1b}]
