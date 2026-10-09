from pathlib import Path
from PIL import Image,ImageDraw
root=Path('src/main/resources/assets/goosetools/textures/gui/games')
for name,color,light in [('traffic_player','#d94445','#ff8b69'),('traffic_car','#427cb8','#7bc3e7')]:
 im=Image.new('RGBA',(24,40));d=ImageDraw.Draw(im)
 d.rectangle((1,8,4,16),fill='#161b23');d.rectangle((19,8,22,16),fill='#161b23');d.rectangle((1,27,4,35),fill='#161b23');d.rectangle((19,27,22,35),fill='#161b23')
 d.rounded_rectangle((4,1,19,38),radius=3,fill='#19252b');d.rectangle((5,3,18,35),fill=color)
 d.rectangle((6,4,8,33),fill=light);d.rectangle((9,4,10,35),fill='#f5e5cd');d.rectangle((13,4,14,35),fill='#f5e5cd')
 d.rectangle((6,10,17,16),fill='#223d4b');d.rectangle((7,10,16,12),fill='#9dcbd2');d.rectangle((6,25,17,30),fill='#203a49')
 d.rectangle((6,3,8,5),fill='#fff0b2');d.rectangle((15,3,17,5),fill='#fff0b2');d.rectangle((6,34,8,36),fill='#8d2229');d.rectangle((15,34,17,36),fill='#8d2229');im.save(root/(name+'.png'))
im=Image.new('RGBA',(24,24));d=ImageDraw.Draw(im);d.rectangle((2,19,21,23),fill='#3a2620');d.polygon([(11,1),(13,1),(20,20),(4,20)],fill='#e5722b');d.polygon([(9,7),(15,7),(16,11),(8,11)],fill='#fff5d9');d.rectangle((6,16,18,18),fill='#fff5d9');im.save(root/'traffic_cone.png')
im=Image.new('RGBA',(48,24));d=ImageDraw.Draw(im);d.rectangle((4,2,8,23),fill='#513c2d');d.rectangle((39,2,43,23),fill='#513c2d');d.rectangle((1,4,46,16),fill='#302b24');d.rectangle((2,5,45,15),fill='#ffd765')
for x in range(0,46,12):d.polygon([(x,5),(x+6,5),(x+12,15),(x+6,15)],fill='#34363b')
im.save(root/'traffic_barrier.png');print('Generated 4 original traffic sprites')
