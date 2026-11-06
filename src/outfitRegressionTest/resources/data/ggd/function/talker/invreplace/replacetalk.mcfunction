#角色信息模块
item replace entity @s inventory.0 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.1 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.2 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.9 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
execute as @s[tag=!clearAll] at @s run function ggd:invrepevent/currentrole {num: 10}
execute as @s[tag=!clearAll,tag=classicPrefer] at @s run function ggd:invrepevent/currentrole_new_good_classic {num: 10}
item replace entity @s inventory.11 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.18 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.19 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.20 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1

#会议共用布局
item replace entity @s inventory.3 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.4 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.5 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.12 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.13 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.14 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.21 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.22 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.23 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
execute unless score FullBloodDLC ggdadv matches 1 as @s[tag=Assassin] run item replace entity @s inventory.4 with light_blue_stained_glass_pane[custom_model_data={strings:["assassin_menu"]},item_name={"color":"#FF4DA6","italic":false,"translate":"item.assassin.menu"},lore=[{"color":"#FF8FCB","italic":false,"translate":"item.assassin.menu.lore"}],custom_data={inAssassinGuesting:1b}] 1
execute unless score FullBloodDLC ggdadv matches 1 as @s[tag=Spook,tag=!SpookHasGuessed,scores={ggdSpookGuess=1..}] run item replace entity @s inventory.4 with light_blue_stained_glass_pane[custom_model_data={strings:["assassin_menu"]},item_name={"color":"#A8FFBE","italic":false,"translate":"item.spook.menu","fallback":"Spook Ability"},lore=[{"color":"#D7FFE1","italic":false,"translate":"item.spook.menu.lore","fallback":"Guess a Duck or Bird identity"}],custom_data={inSpookGuessing:1b}] 1


#未来
item replace entity @s inventory.6 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.7 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.8 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.15 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.16 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.17 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.18 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.19 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.20 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.24 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.25 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1
item replace entity @s inventory.26 with light_blue_stained_glass_pane[tooltip_display={hide_tooltip:true}] 1

#会议信息菜单
item replace entity @s inventory.13 with light_blue_stained_glass_pane[custom_model_data={strings:["false"]},item_name={"color":"#FF7777","italic":false,"translate":"item.meeting.dead_players"},lore=[{"color":"#FFAAAA","italic":false,"translate":"item.meeting.dead_players.lore"}],custom_data={MeetingDeadPlayersMenu:1b}] 1
execute if score HowRole ggdadv matches 1 run item replace entity @s inventory.16 with blue_stained_glass_pane[item_name={"color":"#a6cfff","italic":false,"translate":"item.ingame_role_info"},lore=[{"color":"#83bdff","italic":false,"translate":"item.ingame_role_info.lore"}],custom_model_data={strings:["ingame_role_info"]},custom_data={GameRoleInfo:1b}] 1
data modify storage ggd:game_info slots set value [0,1,2,6,7,8,15,17,24,25,26,9,11,18,19,20]
function ggd:talker/vote_history/render
function ggd:invrepevent/game_info/place_from_list
