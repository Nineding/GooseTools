execute if score #Books ggdReading matches 40.. run return 0
execute unless entity @e[type=marker,tag=reading_empty] run return 0
execute as @e[type=marker,tag=reading_empty,sort=random,limit=1] run function ggd:task/eagleton_simplify/reading/fill_selected
function ggd:task/eagleton_simplify/reading/fill_loop
