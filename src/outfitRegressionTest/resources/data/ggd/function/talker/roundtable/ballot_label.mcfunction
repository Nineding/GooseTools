# Show the label only after the half-second rise, in the correct dimension.
execute unless score #MeetingPhase ggdSession matches 2 run return 0
execute in minecraft:overworld as @e[type=minecraft:text_display,tag=ballotbox,tag=talk,tag=ggd.ballotRevealPending] run data merge entity @s {view_range:1f}
execute in minecraft:the_end as @e[type=minecraft:text_display,tag=ballotbox,tag=talk,tag=ggd.ballotRevealPending] run data merge entity @s {view_range:1f}
execute in minecraft:overworld at @e[type=minecraft:text_display,tag=ballotbox,tag=talk,tag=ggd.ballotRevealPending] as @e[type=minecraft:block_display,tag=meetingBallotModel,tag=talk,distance=..2,sort=nearest,limit=1] run data merge entity @s {Glowing:1b,glow_color_override:16766720}
execute in minecraft:the_end at @e[type=minecraft:text_display,tag=ballotbox,tag=talk,tag=ggd.ballotRevealPending] as @e[type=minecraft:block_display,tag=meetingBallotModel,tag=talk,distance=..2,sort=nearest,limit=1] run data merge entity @s {Glowing:1b,glow_color_override:16766720}
execute in minecraft:overworld run tag @e[tag=ggd.ballotRevealPending] remove ggd.ballotRevealPending
execute in minecraft:the_end run tag @e[tag=ggd.ballotRevealPending] remove ggd.ballotRevealPending
