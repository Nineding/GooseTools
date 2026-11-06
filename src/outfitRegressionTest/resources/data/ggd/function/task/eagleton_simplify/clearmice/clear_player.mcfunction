tag @s remove inTask
tag @s remove inTaskClearMice
tag @s remove clearmiceJustCatch
title @s actionbar ""
scoreboard players reset @s ggdMiceTime
scoreboard players reset @s ggdMiceCaught
scoreboard players reset @s ggdMiceSlot
scoreboard players reset @s ggdMiceRefresh
clear @s carrot_on_a_stick[custom_data~{clearmice:1b}]
clear @s *[custom_data~{clearmice_mouse:1b}]
clear @s *[custom_data~{clearmice_pane:1b}]
execute unless entity @a[tag=inTaskClearMice,limit=1] run scoreboard objectives remove ggdMiceTime
execute unless entity @a[tag=inTaskClearMice,limit=1] run scoreboard objectives remove ggdMiceCaught
execute unless entity @a[tag=inTaskClearMice,limit=1] run scoreboard objectives remove ggdMiceSlot
execute unless entity @a[tag=inTaskClearMice,limit=1] run scoreboard objectives remove ggdMiceRefresh
advancement grant @s only mainsys:test/ifinvchange
