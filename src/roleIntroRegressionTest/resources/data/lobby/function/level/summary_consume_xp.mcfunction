# Presentation only: the authoritative match reward has already been paid.
# Consume the old XP animation only after the server accepts this summary recipient.
function lobby:achievement/show_match_chat
function lobby:level/tellraw_detail
execute if score @s matchGooseCoin matches 1.. run tellraw @s [{"translate":"match.goose_coin.gain","fallback":"Goose coins %s: +%s","color":"#FFD76B","italic":false,"with":[{"text":"\uE082","color":"white","italic":false},{"score":{"name":"@s","objective":"matchGooseCoin"},"color":"#7CFF9A","italic":false}]}]
tag @s remove xp_anim
tag @s remove pending_xp_show
tag @s remove pending_xp_after_trust
tag @s remove match_xp_death
tag @s remove match_xp_mvp
tag @s remove match_xp_mvp15
tag @s remove match_xp_smvp
scoreboard players reset @s xpAnimLeft
scoreboard players reset @s xpAnimStep
scoreboard players reset @s xpAnimChunk
scoreboard players operation @s levelDisp = @s playerLevel
function lobby:level/refresh_bar_exp
scoreboard players operation @s expDisp = @s playerExp
function lobby:level/sync_xp_bar
