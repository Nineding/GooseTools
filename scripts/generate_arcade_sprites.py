"""Original tiny pixel artwork for the native arcade. Requires Pillow; no external assets."""
from pathlib import Path
from PIL import Image, ImageDraw

target = Path(__file__).resolve().parents[1] / "src/main/resources/assets/goosetools/textures/gui/games"
target.mkdir(parents=True, exist_ok=True)

def save(image, name):
    image.save(target / (name + ".png"))

for frame in range(3):
    image = Image.new("RGBA", (24, 17))
    d = ImageDraw.Draw(image)
    d.ellipse((2, 1, 20, 15), fill="#503322")
    d.ellipse((3, 2, 19, 14), fill="#f7cf32")
    d.ellipse((4, 3, 15, 10), fill="#ffe567")
    d.rectangle((6, 11, 16, 13), fill="#ee9d23")
    d.ellipse((13, 1, 21, 10), fill="#503322")
    d.ellipse((14, 2, 20, 9), fill="#ffffff")
    d.rectangle((18, 4, 19, 7), fill="#25221e")
    d.rectangle((15, 9, 23, 12), fill="#503322")
    d.rectangle((16, 10, 22, 11), fill="#e75a2b")
    wing_y = [7, 9, 5][frame]
    d.ellipse((1, wing_y, 10, min(16, wing_y + 6)), fill="#503322")
    d.ellipse((2, wing_y + 1, 9, min(15, wing_y + 5)), fill="#fff0ab")
    save(image, "bird" + str(frame))

for hit in (False, True):
    image = Image.new("RGBA", (28, 30))
    d = ImageDraw.Draw(image)
    d.ellipse((3, 7, 24, 29), fill="#4b3024")
    d.ellipse((4, 8, 23, 28), fill="#9d7150")
    d.ellipse((2, 5, 9, 13), fill="#4b3024")
    d.ellipse((18, 5, 25, 13), fill="#4b3024")
    d.ellipse((4, 6, 7, 11), fill="#bd8f65")
    d.ellipse((20, 6, 23, 11), fill="#bd8f65")
    d.ellipse((4, 3, 23, 24), fill="#4b3024")
    d.ellipse((5, 4, 22, 23), fill="#ae805b")
    d.ellipse((7, 5, 20, 11), fill="#bd9166")
    d.ellipse((7, 15, 20, 24), fill="#edd0a1")
    if hit:
        for x in (9, 17):
            d.line((x-1, 10, x+1, 12), fill="#30221d")
            d.line((x+1, 10, x-1, 12), fill="#30221d")
        d.polygon([(13, 0), (14, 2), (17, 2), (15, 4), (16, 6), (13, 5), (11, 6), (11, 3), (9, 2), (12, 2)], fill="#ffdc36")
    else:
        for x in (9, 17):
            d.rectangle((x, 10, x+1, 13), fill="#25221e")
            d.point((x, 10), fill="#ffffff")
    d.ellipse((11, 14, 16, 17), fill="#663f32")
    d.line((13, 18, 13, 21), fill="#563b2e")
    d.line((10, 20, 13, 22, 17, 20), fill="#563b2e")
    d.ellipse((3, 24, 10, 29), fill="#d8b487")
    d.ellipse((17, 24, 24, 29), fill="#d8b487")
    save(image, "mole_hit" if hit else "mole")

image = Image.new("RGBA", (18, 24)); d = ImageDraw.Draw(image)
d.rectangle((8, 7, 12, 23), fill="#50321f"); d.rectangle((9, 7, 11, 22), fill="#c18b4d")
d.rectangle((2, 1, 17, 10), fill="#3b302b"); d.rectangle((3, 2, 16, 9), fill="#aa7248")
d.rectangle((4, 2, 15, 3), fill="#d9a46c"); d.rectangle((2, 2, 4, 9), fill="#64646a")
d.rectangle((15, 2, 17, 9), fill="#64646a"); d.rectangle((15, 2, 16, 4), fill="#b7b9bd")
save(image, "hammer")

image = Image.new("RGBA", (14, 16)); d = ImageDraw.Draw(image)
d.rectangle((6, 0, 7, 5), fill="#6b3b27"); d.polygon([(7, 3),(9, 0),(12, 1),(10, 4)], fill="#418b37")
d.ellipse((1, 4, 12, 14), fill="#742421"); d.ellipse((2, 5, 11, 13), fill="#da3d32")
d.rectangle((3, 6, 5, 8), fill="#ff9382"); d.rectangle((8, 10, 10, 12), fill="#b82927")
save(image, "apple")

image = Image.new("RGBA", (14, 14)); d = ImageDraw.Draw(image)
d.rectangle((7, 2, 8, 11), fill="#151515"); d.polygon([(7,1),(2,3),(2,5),(7,6)], fill="#ed2323")
d.rectangle((5, 10, 10, 11), fill="#151515"); d.rectangle((3, 12, 12, 13), fill="#151515")
save(image, "flag")

image = Image.new("RGBA", (14, 14)); d = ImageDraw.Draw(image)
d.rectangle((6,0,7,13), fill="#131313"); d.rectangle((0,6,13,7), fill="#131313")
d.line((2,2,11,11), fill="#131313", width=2); d.line((11,2,2,11), fill="#131313", width=2)
d.ellipse((3,3,10,10), fill="#131313"); d.rectangle((4,4,5,5), fill="#ffffff")
save(image, "mine")
print("Generated 9 original pixel sprites in", target)
