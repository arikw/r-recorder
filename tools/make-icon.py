"""Draws the R-Recorder launcher icons: a red circle with a thin white ring and the word REC.

Run in a container from the project root:
  podman run --rm -v "$PWD:/app:Z" docker.io/library/python:3.12-alpine sh -c \
    'apk add -q font-dejavu && pip install -q pillow && python -I /app/tools/make-icon.py'
"""
from PIL import Image, ImageDraw, ImageFont

RED = (241, 69, 61, 255)
WHITE = (255, 255, 255, 255)
LIGHT = (243, 245, 247, 255)  # @color/icon_background
FONT = "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf"
RES = "/app/app/src/releaseConfig/res"
SS = 4  # draw larger, then scale down for smooth edges


def red_badge(draw, cx, cy, r):
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=RED)
    ring = r * 0.86
    w = max(1, round(r * 0.045))
    draw.ellipse((cx - ring, cy - ring, cx + ring, cy + ring), outline=WHITE, width=w)
    font = ImageFont.truetype(FONT, round(r * 0.52))
    draw.text((cx, cy), "REC", font=font, fill=WHITE, anchor="mm")


def adaptive_foreground(px):
    # 108dp canvas; the badge stays well inside the 66dp area every launcher mask keeps.
    size = px * SS
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    red_badge(ImageDraw.Draw(im), size / 2, size / 2, size * 28 / 108)
    return im.resize((px, px), Image.LANCZOS)


def legacy(px):
    # Pre-Android 8 icon: the badge on a light round plate.
    size = px * SS
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    m = size * 0.04
    d.ellipse((m, m, size - m, size - m), fill=LIGHT)
    red_badge(d, size / 2, size / 2, size * 0.30)
    return im.resize((px, px), Image.LANCZOS)


for density, scale in {"hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}.items():
    adaptive_foreground(round(108 * scale)).save(f"{RES}/mipmap-{density}-v26/audio_recorder_adaptive_logo_min.png")
    legacy(round(48 * scale)).save(f"{RES}/mipmap-{density}/audio_recorder_logo.png")
print("done")
