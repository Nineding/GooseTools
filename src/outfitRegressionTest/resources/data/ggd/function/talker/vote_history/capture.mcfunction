# Called after the public tally, before any vote tags or scores are cleared.
execute if score #Captured ggdVoteHistory matches 1 run return 0
scoreboard players set #Captured ggdVoteHistory 1
execute unless score #Round ggdVoteHistory matches 1..9 run return 0
data modify storage ggd:vote_history build set value {names:{},lore:[]}
execute store result storage ggd:vote_history row.round int 1 run scoreboard players get #Round ggdVoteHistory
data modify storage ggd:vote_history queue set from storage ggd:role data
execute if data storage ggd:vote_history queue[0] run function ggd:talker/vote_history/subject_loop
data modify storage ggd:vote_history row.set set value "skip"
data modify storage ggd:vote_history row.votes set value 0
execute store result storage ggd:vote_history row.votes int 1 run scoreboard players get #skip ggdGetVote
function ggd:talker/vote_history/skip_line with storage ggd:vote_history row
execute if score DoNikeInVote ggdadv matches 0 run function ggd:talker/vote_history/voters
data modify storage ggd:vote_history build.lore append from storage ggd:vote_history line
data modify storage ggd:vote_history build.lore append value {translate:"item.vote_history.anonymous",color:"gray",italic:false}
execute if score DoNikeInVote ggdadv matches 0 run data modify storage ggd:vote_history build.lore[-1] set value {translate:"item.vote_history.named",color:"gray",italic:false}
function ggd:talker/vote_history/store with storage ggd:vote_history row
data remove storage ggd:vote_history build
data remove storage ggd:vote_history queue
data remove storage ggd:vote_history voter_queue
execute as @a[tag=inShowSummaryStep] run advancement grant @s only mainsys:test/ifinvchange
