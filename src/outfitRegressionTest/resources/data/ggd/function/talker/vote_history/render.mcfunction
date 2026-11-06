execute unless score ShowMeetingVotes ggdadv matches 1 run return 0
execute if entity @s[tag=inDream] run return 0
execute if entity @s[tag=inTask] run return 0
clear @s *[custom_data~{VoteHistory:1b}]
item replace entity @s inventory.18 with paper[item_name={translate:"item.vote_history.round",with:[{text:"1"}],color:"gray",italic:false},lore=[{translate:"item.vote_history.empty",color:"gray",italic:false}],custom_data={VoteHistory:1b,NeedClear:1b}] 1
execute if data storage ggd:vote_history records.r1 run function ggd:talker/vote_history/render_record {round:1,slot:18}
item replace entity @s inventory.19 with paper[item_name={translate:"item.vote_history.round",with:[{text:"2"}],color:"gray",italic:false},lore=[{translate:"item.vote_history.empty",color:"gray",italic:false}],custom_data={VoteHistory:1b,NeedClear:1b}] 2
execute if data storage ggd:vote_history records.r2 run function ggd:talker/vote_history/render_record {round:2,slot:19}
item replace entity @s inventory.20 with paper[item_name={translate:"item.vote_history.round",with:[{text:"3"}],color:"gray",italic:false},lore=[{translate:"item.vote_history.empty",color:"gray",italic:false}],custom_data={VoteHistory:1b,NeedClear:1b}] 3
execute if data storage ggd:vote_history records.r3 run function ggd:talker/vote_history/render_record {round:3,slot:20}
item replace entity @s inventory.21 with paper[item_name={translate:"item.vote_history.round",with:[{text:"4"}],color:"gray",italic:false},lore=[{translate:"item.vote_history.empty",color:"gray",italic:false}],custom_data={VoteHistory:1b,NeedClear:1b}] 4
execute if data storage ggd:vote_history records.r4 run function ggd:talker/vote_history/render_record {round:4,slot:21}
item replace entity @s inventory.22 with paper[item_name={translate:"item.vote_history.round",with:[{text:"5"}],color:"gray",italic:false},lore=[{translate:"item.vote_history.empty",color:"gray",italic:false}],custom_data={VoteHistory:1b,NeedClear:1b}] 5
execute if data storage ggd:vote_history records.r5 run function ggd:talker/vote_history/render_record {round:5,slot:22}
item replace entity @s inventory.23 with paper[item_name={translate:"item.vote_history.round",with:[{text:"6"}],color:"gray",italic:false},lore=[{translate:"item.vote_history.empty",color:"gray",italic:false}],custom_data={VoteHistory:1b,NeedClear:1b}] 6
execute if data storage ggd:vote_history records.r6 run function ggd:talker/vote_history/render_record {round:6,slot:23}
item replace entity @s inventory.24 with paper[item_name={translate:"item.vote_history.round",with:[{text:"7"}],color:"gray",italic:false},lore=[{translate:"item.vote_history.empty",color:"gray",italic:false}],custom_data={VoteHistory:1b,NeedClear:1b}] 7
execute if data storage ggd:vote_history records.r7 run function ggd:talker/vote_history/render_record {round:7,slot:24}
item replace entity @s inventory.25 with paper[item_name={translate:"item.vote_history.round",with:[{text:"8"}],color:"gray",italic:false},lore=[{translate:"item.vote_history.empty",color:"gray",italic:false}],custom_data={VoteHistory:1b,NeedClear:1b}] 8
execute if data storage ggd:vote_history records.r8 run function ggd:talker/vote_history/render_record {round:8,slot:25}
item replace entity @s inventory.26 with paper[item_name={translate:"item.vote_history.round",with:[{text:"9"}],color:"gray",italic:false},lore=[{translate:"item.vote_history.empty",color:"gray",italic:false}],custom_data={VoteHistory:1b,NeedClear:1b}] 9
execute if data storage ggd:vote_history records.r9 run function ggd:talker/vote_history/render_record {round:9,slot:26}
