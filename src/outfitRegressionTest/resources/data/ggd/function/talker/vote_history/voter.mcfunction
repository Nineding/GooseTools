$execute unless entity @a[tag=$(set),tag=vote_for_$(target)] run return 0
$data modify storage ggd:vote_history line.extra append value {nbt:"records.r$(round).names.$(set)",storage:"ggd:vote_history",interpret:true}
data modify storage ggd:vote_history line.extra append value {text:" ",color:"gray"}
