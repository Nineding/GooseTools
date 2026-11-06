execute unless score ShowMeetingVotes ggdadv matches 1 run return 0
execute if entity @s[tag=inDream] run return 0
execute if entity @s[tag=inTask] run return 0
clear @s *[custom_data~{VoteHistory:1b}]
execute if data storage ggd:vote_history records.r1 run function ggd:talker/vote_history/render_record {round:1,slot:18}
execute if data storage ggd:vote_history records.r2 run function ggd:talker/vote_history/render_record {round:2,slot:19}
execute if data storage ggd:vote_history records.r3 run function ggd:talker/vote_history/render_record {round:3,slot:20}
execute if data storage ggd:vote_history records.r4 run function ggd:talker/vote_history/render_record {round:4,slot:21}
execute if data storage ggd:vote_history records.r5 run function ggd:talker/vote_history/render_record {round:5,slot:22}
execute if data storage ggd:vote_history records.r6 run function ggd:talker/vote_history/render_record {round:6,slot:23}
execute if data storage ggd:vote_history records.r7 run function ggd:talker/vote_history/render_record {round:7,slot:24}
execute if data storage ggd:vote_history records.r8 run function ggd:talker/vote_history/render_record {round:8,slot:25}
execute if data storage ggd:vote_history records.r9 run function ggd:talker/vote_history/render_record {round:9,slot:26}
