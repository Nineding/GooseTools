execute if entity @s[tag=spectator,tag=!dlcGhostActive] run return 0
execute unless entity @s[tag=!inTask,tag=task.reading.available,tag=!task.reading.finished,tag=!inTalk] run return 0
execute unless entity @e[tag=task,tag=reading,distance=..3] run return 0
execute store result score #eagletonTrigger ggd_temp run function ggd:task/eagleton_simplify/has_dream_trigger {state:"reading_accept"}
execute unless score #eagletonTrigger ggd_temp matches 1 run item replace entity @s hotbar.1 with carrot_on_a_stick[item_name={"color":"#b3f793","italic":false,"translate":"item.task.reading.available","fallback":"Reading"},lore=[{"color":"#d9f5cc","italic":false,"translate":"item.task.reading.available.lore","fallback":"Borrow one book, sit on a brown cushion for 10 seconds, then return it."}],custom_model_data={strings:["33"]},custom_data={ggdManagedHotbar:1b,ggdHotbarSlot:1,ggdHotbarState:"reading_accept",task:1b,reading:1b,ghost_interaction:1b}] 1
