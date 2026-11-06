# Recover puzzle panes moved to the hotbar/offhand or cursor before redrawing.
clear @s *[custom_data~{clearmice_pane:1b}]
execute if items entity @s player.cursor *[custom_data~{clearmice_pane:1b}] run item replace entity @s player.cursor with air
item replace entity @s inventory.0 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.1 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.2 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.3 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.4 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.5 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.6 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.7 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.8 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.9 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.10 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.11 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.12 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.13 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.14 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.15 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.16 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.17 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.18 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.19 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.20 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.21 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.22 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.23 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.24 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.25 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
item replace entity @s inventory.26 with light_blue_stained_glass_pane[item_name={"text":" ","italic":false},tooltip_display={hide_tooltip:true},custom_data={NeedClear:1b,clearmice_pane:1b}]
execute store result storage ggd:macro mice_slot int 1 run scoreboard players get @s ggdMiceSlot
execute unless score map gamesetting matches 11 run function ggd:task/goosechapel/clearmice/place_mouse with storage ggd:macro
execute if score map gamesetting matches 11 run function ggd:task/eagleton_simplify/clearmice/place_mouse with storage ggd:macro
tag @s add dontGoMainInvClickCheck
