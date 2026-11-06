$execute if entity @s[tag=!inDream] if items entity @s hotbar.1 *[custom_data~{ggdHotbarState:"$(state)"}] run return 1
$execute if entity @s[tag=inDream] if items entity @s hotbar.4 *[custom_data~{ggdHotbarState:"$(state)"}] run return 1
return 0
