$item replace entity @s inventory.$(slot) with paper[item_name={translate:"item.vote_history.round",with:[{text:"$(round)"}],color:"#FFD86B",italic:false},custom_data={VoteHistory:1b,NeedClear:1b}] $(round)
execute if data storage ggd:vote_history display.lore[0] run function ggd:talker/vote_history/lore_loop
