import os, random
from PIL import Image, ImageDraw

ROOT = '/home/claude/mod/RedHellbeast'
A = ROOT + '/src/main/resources/assets/redhellbeast/textures'
for d in ['entity', 'item', 'block', 'gui']:
    os.makedirs(f'{A}/{d}', exist_ok=True)
random.seed(1988)

# ---------------------------------------------------------------- entity textures
PAL = {
    1: dict(a=(165, 14, 14), b=(105, 0, 0), c=(58, 0, 0), d=(22, 0, 0), claw=(190, 40, 40)),
    2: dict(a=(120, 8, 8), b=(70, 0, 0), c=(34, 0, 0), d=(10, 0, 0), claw=(150, 20, 20)),
    3: dict(a=(205, 0, 0), b=(60, 0, 0), c=(14, 0, 0), d=(0, 0, 0), claw=(230, 30, 30)),
}
REG = {  # name: (x0,y0,x1,y1)
    'body': (0, 0, 68, 32), 'ridge': (0, 32, 40, 53), 'neck': (70, 0, 98, 19),
    'head': (0, 56, 44, 81), 'tail1': (70, 20, 100, 35), 'tail2': (70, 36, 98, 50),
    'tail3': (70, 52, 96, 65), 'tail4': (70, 66, 94, 78), 'arm': (0, 84, 24, 96),
    'fore': (28, 84, 50, 95), 'claw': (54, 84, 66, 90), 'legU': (0, 100, 22, 104),
    'legL': (0, 106, 48, 110)}

def fill(px, r, fn):
    x0, y0, x1, y1 = r
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = fn(x - x0, y - y0)

def entity_tex(phase, glow=False):
    p = PAL[phase]
    im = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    px = im.load()
    if not glow:
        def ribs(x, y):          # tiger / rib stripes like the sprite
            v = ((y // 2) + (x // 11)) % 3
            if (x * 7 + y * 13) % 17 == 0: return p['d'] + (255,)
            return (p['a'] if v == 0 else p['b'] if v == 1 else p['c']) + (255,)
        def vstripe(x, y):
            return (p['a'] if (x // 2) % 3 == 0 else p['b'] if (x // 2) % 3 == 1 else p['c']) + (255,)
        fill(px, REG['body'], ribs); fill(px, REG['neck'], vstripe)
        fill(px, REG['ridge'], lambda x, y: (p['c'] if y % 4 else p['d']) + (255,))
        fill(px, REG['head'], lambda x, y: (p['b'] if (x + y) % 5 else p['a']) + (255,))
        for t in ['tail1', 'tail2', 'tail3', 'tail4']:
            fill(px, REG[t], lambda x, y: (p['a'] if (x // 2) % 2 == 0 else p['c']) + (255,))
        fill(px, REG['arm'], ribs); fill(px, REG['fore'], ribs)
        fill(px, REG['claw'], lambda x, y: p['claw'] + (255,))
        fill(px, REG['legU'], lambda x, y: (p['c'] if x % 4 else p['d']) + (255,))
        fill(px, REG['legL'], lambda x, y: (p['c'] if x % 6 else p['d']) + (255,))
        if phase >= 2:   # glitch streaks
            for _ in range(60 if phase == 2 else 110):
                x = random.randint(0, 127); y = random.randint(0, 127); l = random.randint(3, 14)
                col = random.choice([(255, 60, 60), (0, 0, 0), (255, 255, 255), (90, 0, 0)])
                for i in range(l):
                    if x + i < 128 and px[x + i, y][3] > 0: px[x + i, y] = col + (255,)
    # ---- face (front of head) at (10,66) size 12x15
    fx, fy = 10, 66
    face = [[0] * 12 for _ in range(15)]
    def put(x, y, v): face[y][x] = v
    for y in range(15):
        for x in range(12): put(x, y, 1)
    for y in range(15):                              # rounded mask outline
        put(0, y, 2); put(11, y, 2)
    for x in range(12): put(x, 0, 2); put(x, 14, 2)
    for x, y in [(0, 0), (11, 0), (0, 14), (11, 14), (1, 0), (10, 0), (0, 1), (11, 1)]: put(x, y, 3)
    for (ex, ey) in [(2, 4), (7, 4)]:                # sunken eye sockets
        for dy in range(3):
            for dx in range(3): put(ex + dx, ey + dy, 4)
        put(ex + (0 if ex < 6 else 2), ey, 1)       # angry / sad brow cut
    put(5, 7, 2); put(6, 7, 2); put(5, 8, 2); put(6, 8, 2)   # nose
    if phase < 3:                                    # frown
        for x in range(3, 9): put(x, 11, 4)
        put(2, 12, 4); put(9, 12, 4)
    else:                                            # screaming open mouth
        for y in range(9, 14):
            for x in range(3, 9): put(x, y, 4)
    for y in range(15):
        for x in range(12):
            v = face[y][x]
            if glow:
                if v == 4 and y in (5,) and phase >= 1:   # eye light
                    px[fx + x, fy + y] = (255, 255, 255, 255)
                elif v == 4 and y == 4 and phase >= 2:
                    px[fx + x, fy + y] = (255, 40, 40, 255)
                elif phase == 3 and y in (8, 14) and 3 <= x < 9:
                    px[fx + x, fy + y] = (255, 30, 30, 255)
            else:
                col = {1: p['a'], 2: p['c'], 3: p['d'], 4: (0, 0, 0)}[v]
                if phase == 2 and v == 4 and y == 5: col = (255, 255, 255)
                px[fx + x, fy + y] = col + (255,)
    return im

for ph in (1, 2, 3):
    entity_tex(ph).save(f'{A}/entity/red_phase{ph}.png')
entity_tex(2, glow=True).save(f'{A}/entity/red_glow.png')
entity_tex(3, glow=True).save(f'{A}/entity/red_glow3.png')

# ---------------------------------------------------------------- hell skull
sk = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(sk)
d.ellipse((2, 1, 13, 11), fill=(225, 205, 195, 255), outline=(90, 0, 0, 255))
d.rectangle((5, 10, 10, 14), fill=(215, 195, 185, 255), outline=(90, 0, 0, 255))
d.rectangle((4, 5, 6, 7), fill=(0, 0, 0, 255)); d.rectangle((9, 5, 11, 7), fill=(0, 0, 0, 255))
d.point((5, 6), fill=(255, 80, 0, 255)); d.point((10, 6), fill=(255, 80, 0, 255))
for x in (6, 8): d.line((x, 11, x, 13), fill=(90, 0, 0, 255))
d.point((7, 8), fill=(60, 0, 0, 255)); d.point((8, 8), fill=(60, 0, 0, 255))
sk.save(f'{A}/entity/hell_skull.png')

# ---------------------------------------------------------------- cartridge item
c = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(c)
d.rectangle((2, 1, 13, 14), fill=(120, 120, 126, 255), outline=(40, 40, 46, 255))
d.rectangle((3, 2, 12, 3), fill=(90, 90, 96, 255))
d.rectangle((4, 4, 11, 10), fill=(170, 20, 20, 255), outline=(255, 190, 150, 255))
for x, y in [(6, 9), (6, 8), (7, 7), (7, 6), (8, 8), (9, 6), (9, 9), (8, 9), (10, 9)]: d.point((x, y), fill=(20, 0, 0, 255))
d.point((9, 5), fill=(255, 255, 255, 255))
d.rectangle((4, 12, 11, 13), fill=(30, 30, 34, 255)); d.line((5, 14, 10, 14), fill=(210, 170, 40, 255))
c.save(f'{A}/item/godzilla_cartridge.png')

# hellbeast heart
h = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(h)
d.ellipse((2, 3, 9, 10), fill=(150, 0, 0, 255), outline=(40, 0, 0, 255))
d.ellipse((7, 3, 14, 10), fill=(150, 0, 0, 255), outline=(40, 0, 0, 255))
d.polygon([(2, 8), (14, 8), (8, 15)], fill=(150, 0, 0, 255), outline=(40, 0, 0, 255))
d.ellipse((4, 4, 6, 6), fill=(255, 70, 70, 255)); d.point((8, 9), fill=(255, 255, 255, 255)); d.point((9, 9), fill=(255, 255, 255, 255))
d.line((8, 3, 8, 12), fill=(30, 0, 0, 255))
h.save(f'{A}/item/hellbeast_heart.png')

# ---------------------------------------------------------------- computer block
def tile(bg):
    t = Image.new('RGBA', (16, 16), bg + (255,)); return t, ImageDraw.Draw(t)
t, d = tile((72, 72, 78)); d.rectangle((0, 0, 15, 15), outline=(40, 40, 46, 255))
for y in (4, 6, 8, 10): d.line((3, y, 12, y), fill=(45, 45, 50, 255))
t.save(f'{A}/block/retro_computer_side.png')
t, d = tile((92, 92, 98)); d.rectangle((0, 0, 15, 15), outline=(50, 50, 56, 255)); d.rectangle((3, 3, 12, 12), outline=(60, 60, 66, 255)); t.save(f'{A}/block/retro_computer_top.png')
t, d = tile((64, 64, 70)); d.rectangle((0, 0, 15, 15), outline=(35, 35, 40, 255))
d.rectangle((2, 2, 13, 9), fill=(6, 0, 0, 255), outline=(25, 25, 28, 255))
for y in range(3, 9, 2): d.line((3, y, 12, y), fill=(120, 0, 0, 255))
d.text((3, 3), "GO", fill=(255, 40, 40, 255))
d.rectangle((4, 12, 11, 13), fill=(15, 15, 18, 255)); d.point((13, 12), fill=(255, 0, 0, 255))
t.save(f'{A}/block/retro_computer_front.png')

# ---------------------------------------------------------------- jumpscare textures + face mask
def square(im, size=256):
    bb = im.getbbox(); im = im.crop(bb)
    s = size / max(im.size)
    im = im.resize((max(1, int(im.width * s)), max(1, int(im.height * s))), Image.NEAREST)
    out = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    out.paste(im, ((size - im.width) // 2, (size - im.height) // 2), im)
    return out

closed = Image.open('/mnt/user-data/uploads/25190.webp').convert('RGBA')
op = Image.open('/mnt/user-data/uploads/25192.png').convert('RGBA')
px = op.load()
for y in range(op.height):
    for x in range(op.width):
        r, g, b, a = px[x, y]
        if r > 240 and g > 240 and b > 240: px[x, y] = (0, 0, 0, 0)
square(closed).save(f'{A}/gui/red_scare_closed.png')
square(op).save(f'{A}/gui/red_scare_open.png')

# silhouette mask of the closed sprite
COLS, ROWS = 34, 35
bb = closed.getchannel('A').getbbox(); cr = closed.crop(bb)
cw, ch = cr.width / COLS, cr.height / ROWS
al = cr.getchannel('A').load()
rows = []
for gy in range(ROWS):
    s = ''
    for gx in range(COLS):
        n = tot = 0
        for yy in range(int(gy * ch), int((gy + 1) * ch)):
            for xx in range(int(gx * cw), int((gx + 1) * cw)):
                tot += 1; n += 1 if al[xx, yy] > 128 else 0
        s += '1' if tot and n / tot > 0.35 else '0'
    rows.append(s)
# eyes: dark ovals in the head (sprite coordinates ~ (400,108) & (455,108) of 546x564)
def cell(sx, sy): return int((sx - bb[0]) / cw), int((sy - bb[1]) / ch)
e1, e2 = cell(398, 112), cell(452, 112)
print('\n'.join(rows)); print('eyes', e1, e2)
jd = ROOT + '/src/main/java/com/redhellbeast/client'
os.makedirs(jd, exist_ok=True)
with open(jd + '/RedFaceMask.java', 'w') as f:
    f.write('package com.redhellbeast.client;\n\n/** Silhouette of Red (generated from the sprite) used to arrange the GAME OVER text. */\n')
    f.write('public final class RedFaceMask {\n    private RedFaceMask() {}\n')
    f.write(f'    public static final int COLS = {COLS};\n    public static final int ROWS = {ROWS};\n')
    f.write(f'    public static final int[] EYE_A = {{{e1[0]}, {e1[1]}}};\n    public static final int[] EYE_B = {{{e2[0]}, {e2[1]}}};\n')
    f.write('    public static final String[] ROWS_DATA = {\n' + ',\n'.join(f'        "{r}"' for r in rows) + '\n    };\n}\n')
print('done')
