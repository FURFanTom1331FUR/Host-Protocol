#!/usr/bin/env python3
"""Host Protocol 0.1.2 art + non-speech stingers.

Processes generated analog-horror stills into Minecraft textures and synthesizes
short OGG hits (impact / static / drip / heartbeat). Run from repo root:
  python3 tools/gen_horror_012.py
"""
from __future__ import annotations

import math
import os
import struct
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parents[1]
TEX = ROOT / "src/main/resources/assets/hostprotocol/textures"
SND = ROOT / "src/main/resources/assets/hostprotocol/sounds/horror"
ART = Path("/opt/cursor/artifacts/assets")


def find_source(name: str) -> Path | None:
	for base in (ART, Path("/tmp"), ROOT / "tools" / "_gen"):
		p = base / name
		if p.is_file():
			return p
	return None


def save_png(img: Image.Image, path: Path) -> None:
	path.parent.mkdir(parents=True, exist_ok=True)
	img.save(path, "PNG")
	print(f"wrote {path} ({img.size[0]}x{img.size[1]})")


def grain(img: Image.Image, amount: float = 18.0) -> Image.Image:
	img = img.convert("RGB")
	w, h = img.size
	noise = Image.effect_noise((w, h), amount).convert("L")
	noise = ImageOps.autocontrast(noise)
	return Image.blend(img, ImageOps.colorize(noise, (0, 0, 0), (40, 8, 48)), 0.18)


def fit_square(src: Image.Image, size: int, zoom: float = 1.08) -> Image.Image:
	src = src.convert("RGB")
	w, h = src.size
	side = int(min(w, h) / zoom)
	left = (w - side) // 2
	top = max(0, (h - side) // 2 - side // 18)
	crop = src.crop((left, top, left + side, top + side))
	return crop.resize((size, size), Image.Resampling.LANCZOS)


def add_drips(face: Image.Image) -> Image.Image:
	img = face.convert("RGBA")
	overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
	d = ImageDraw.Draw(overlay)
	w, h = img.size
	streaks = [
		(int(w * 0.34), int(h * 0.42), int(w * 0.37), int(h * 0.92)),
		(int(w * 0.38), int(h * 0.44), int(w * 0.40), int(h * 0.80)),
		(int(w * 0.62), int(h * 0.42), int(w * 0.65), int(h * 0.90)),
		(int(w * 0.58), int(h * 0.46), int(w * 0.60), int(h * 0.78)),
		(int(w * 0.49), int(h * 0.62), int(w * 0.52), int(h * 0.98)),
	]
	for x0, y0, x1, y1 in streaks:
		d.rounded_rectangle((x0, y0, x1, y1), radius=max(2, (x1 - x0) // 2), fill=(28, 0, 18, 210))
		d.ellipse((x1 - 6, y1 - 4, x1 + 4, y1 + 10), fill=(90, 12, 40, 230))
	out = Image.alpha_composite(img, overlay)
	return out.filter(ImageFilter.GaussianBlur(radius=0.6))


def drip_layer(size: int = 256) -> Image.Image:
	img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
	d = ImageDraw.Draw(img)
	for i, x in enumerate((70, 88, 120, 148, 176, 198)):
		length = 90 + (i * 17) % 80
		width = 3 + i % 3
		d.rectangle((x, 40, x + width, 40 + length), fill=(12, 0, 8, 200))
		d.ellipse((x - 2, 40 + length, x + width + 4, 40 + length + 10), fill=(80, 8, 30, 220))
	return img.filter(ImageFilter.GaussianBlur(radius=0.8))


def vignette(size: int = 256) -> Image.Image:
	img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
	px = img.load()
	cx = cy = (size - 1) / 2.0
	for y in range(size):
		for x in range(size):
			dx = (x - cx) / (size * 0.50)
			dy = (y - cy) / (size * 0.50)
			r = math.sqrt(dx * dx + dy * dy)
			t = max(0.0, min(1.0, (r - 0.08) / 0.95))
			t = t * t * (3.0 - 2.0 * t)
			px[x, y] = (6, 0, 10, int(255 * t))
	return img


def infection_overlay() -> Image.Image:
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	px = img.load()

	def h2(x, y, salt=0):
		n = (x * 374761393 + y * 668265263 + salt * 1274126177) & 0xFFFFFFFF
		n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
		return (n & 0xFFFF) / 65535.0

	for y in range(16):
		for x in range(16):
			n = h2(x, y, 17)
			n2 = h2(x + 3, y * 2, 91)
			v1 = abs(((x * 3 + y * 5) % 7) - 3) + n * 1.4
			v2 = abs(((x * 5 - y * 2) % 9) - 4) + n2
			v3 = abs((x + y * 2) % 6 - 2.5)
			vein_d = min(v1, v2, v3)
			border = x in (0, 15) or y in (0, 15)
			if vein_d < 0.7:
				px[x, y] = (255, 196, 255, 255) if n > 0.7 else (230, 80, 255, 255)
			elif vein_d < 1.4:
				px[x, y] = (140, 36, 200, 255)
			elif border or n < 0.82:
				px[x, y] = (52, 6, 48, 255) if n2 > 0.45 else (22, 2, 28, 255)
			elif n < 0.93:
				px[x, y] = (10, 0, 12, 255)
	for x, y in ((2, 3), (7, 2), (12, 4), (4, 9), (9, 11), (13, 12), (5, 14), (10, 7)):
		px[x, y] = (255, 200, 255, 255)
	return img


def put_box(px, x0, y0, x1, y1, color):
	for y in range(y0, y1):
		for x in range(x0, x1):
			if 0 <= x < 64 and 0 <= y < 64:
				px[x, y] = color


def septic_skin(face: Image.Image | None, body: Image.Image | None) -> Image.Image:
	"""64×64 Steve UV: pale eyeless head, charcoal wet body, purple veins."""
	img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
	px = img.load()
	void = (0, 0, 0, 0)
	pale = (186, 178, 172, 255)
	pale_d = (132, 122, 118, 255)
	socket = (8, 4, 10, 255)
	drip = (72, 8, 28, 255)
	hair = (10, 8, 12, 255)
	body_c = (16, 10, 14, 255)
	body_v = (92, 22, 110, 255)
	body_h = (36, 18, 40, 255)
	if body is not None:
		sample = body.convert("RGB").resize((8, 8), Image.Resampling.BOX)
		r, g, b = sample.getpixel((4, 6))
		body_c = (max(8, r // 6), max(4, g // 8), max(8, b // 6), 255)

	# head (Steve)
	put_box(px, 8, 0, 16, 8, hair)       # top
	put_box(px, 16, 0, 24, 8, pale_d)    # bottom
	put_box(px, 0, 8, 8, 16, pale_d)     # right
	put_box(px, 8, 8, 16, 16, pale)      # front
	put_box(px, 16, 8, 24, 16, pale_d)   # left
	put_box(px, 24, 8, 32, 16, hair)     # back
	# eyeless sockets + purple drip on front
	put_box(px, 10, 10, 12, 12, socket)
	put_box(px, 13, 10, 15, 12, socket)
	put_box(px, 10, 12, 12, 16, drip)
	put_box(px, 13, 12, 15, 16, drip)
	put_box(px, 11, 14, 14, 16, (40, 8, 18, 255))
	# hat overlay — wet membrane
	put_box(px, 40, 0, 48, 8, (40, 8, 48, 180))
	put_box(px, 32, 8, 40, 16, (20, 4, 24, 120))
	put_box(px, 40, 8, 48, 16, (20, 4, 24, 90))
	put_box(px, 48, 8, 56, 16, (20, 4, 24, 90))
	put_box(px, 56, 8, 64, 16, (12, 0, 16, 140))

	if face is not None:
		front = face.convert("RGBA").resize((8, 8), Image.Resampling.LANCZOS)
		for y in range(8):
			for x in range(8):
				px[8 + x, 8 + y] = front.getpixel((x, y))
		# force sockets dark even after sample
		put_box(px, 10, 10, 12, 12, socket)
		put_box(px, 13, 10, 15, 12, socket)

	# body
	put_box(px, 20, 16, 28, 20, body_h)  # top
	put_box(px, 28, 16, 36, 20, (8, 4, 8, 255))
	put_box(px, 16, 20, 20, 32, body_c)
	put_box(px, 20, 20, 28, 32, body_c)
	put_box(px, 28, 20, 32, 32, body_c)
	put_box(px, 32, 20, 40, 32, (10, 6, 12, 255))
	for y in range(20, 32):
		px[22, y] = body_v
		px[25, y] = body_v
	# right arm
	put_box(px, 44, 16, 48, 20, pale_d)
	put_box(px, 40, 20, 44, 32, body_c)
	put_box(px, 44, 20, 48, 32, body_c)
	put_box(px, 48, 20, 52, 32, body_c)
	put_box(px, 52, 20, 56, 32, body_c)
	# right leg
	put_box(px, 4, 16, 8, 20, body_c)
	put_box(px, 0, 20, 4, 32, body_c)
	put_box(px, 4, 20, 8, 32, body_c)
	put_box(px, 8, 20, 12, 32, body_c)
	put_box(px, 12, 20, 16, 32, body_c)
	# left arm (64x64)
	put_box(px, 36, 48, 40, 52, pale_d)
	put_box(px, 32, 52, 36, 64, body_c)
	put_box(px, 36, 52, 40, 64, body_c)
	put_box(px, 40, 52, 44, 64, body_c)
	put_box(px, 44, 52, 48, 64, body_c)
	# left leg
	put_box(px, 20, 48, 24, 52, body_c)
	put_box(px, 16, 52, 20, 64, body_c)
	put_box(px, 20, 52, 24, 64, body_c)
	put_box(px, 24, 52, 28, 64, body_c)
	put_box(px, 28, 52, 32, 64, body_c)
	return img


def write_wav(path: Path, samples: list[float], rate: int = 22050) -> None:
	path.parent.mkdir(parents=True, exist_ok=True)
	with wave.open(str(path), "w") as w:
		w.setnchannels(1)
		w.setsampwidth(2)
		w.setframerate(rate)
		frames = b"".join(struct.pack("<h", int(max(-1.0, min(1.0, s)) * 32000)) for s in samples)
		w.writeframes(frames)


def noise(i: int, salt: int = 0) -> float:
	n = (i * 1103515245 + 12345 + salt * 997) & 0x7FFFFFFF
	return (n / 0x7FFFFFFF) * 2.0 - 1.0


def synth_impact(rate: int = 22050) -> list[float]:
	n = int(0.55 * rate)
	out = []
	for i in range(n):
		t = i / rate
		env = math.exp(-t * 7.5)
		boom = math.sin(2 * math.pi * 42 * t) * 0.72 + math.sin(2 * math.pi * 27 * t) * 0.48
		click = math.sin(2 * math.pi * 190 * t) * math.exp(-t * 38) * 0.45
		hiss = noise(i, 3) * math.exp(-t * 12) * 0.38
		out.append((boom + click + hiss) * env)
	return out


def synth_static(rate: int = 22050) -> list[float]:
	n = int(0.38 * rate)
	out = []
	for i in range(n):
		t = i / rate
		env = min(1.0, t * 40) * math.exp(-t * 9)
		burst = noise(i, 9) * 0.7 + noise(i // 3, 4) * 0.3
		tone = math.sin(2 * math.pi * (1400 + 400 * noise(i, 1)) * t) * 0.08
		out.append((burst + tone) * env)
	return out


def synth_drip(rate: int = 22050) -> list[float]:
	n = int(0.32 * rate)
	out = []
	for i in range(n):
		t = i / rate
		ping = math.sin(2 * math.pi * 880 * t) * math.exp(-t * 22) * 0.35
		splat = math.sin(2 * math.pi * 90 * t) * math.exp(-t * 14) * 0.55
		wet = noise(i, 11) * math.exp(-t * 18) * 0.2
		out.append(ping + splat + wet)
	return out


def synth_heartbeat(rate: int = 22050) -> list[float]:
	n = int(0.85 * rate)
	out = [0.0] * n

	def thump(start: float, pitch: float, amp: float) -> None:
		for i in range(n):
			t = i / rate - start
			if t < 0:
				continue
			env = math.exp(-t * 18) * amp
			out[i] += math.sin(2 * math.pi * pitch * t) * env
			out[i] += noise(i, 21) * env * 0.12

	thump(0.02, 48, 0.95)
	thump(0.22, 40, 0.72)
	return out


def synth_whisper_bed(rate: int = 22050) -> list[float]:
	n = int(2.4 * rate)
	out = []
	lp = 0.0
	for i in range(n):
		t = i / rate
		env = min(t * 2.0, 1.0, (2.4 - t) * 2.0)
		lp = lp * 0.92 + noise(i, 33) * 0.08
		hiss = lp * 0.55 + math.sin(2 * math.pi * 73 * t) * 0.04
		out.append(hiss * env * 0.45)
	return out


def to_ogg(wav: Path, ogg: Path) -> None:
	ogg.parent.mkdir(parents=True, exist_ok=True)
	cmd = ["ffmpeg", "-y", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "4", str(ogg)]
	r = subprocess.run(cmd, capture_output=True, text=True)
	if r.returncode != 0:
		print(r.stderr, file=sys.stderr)
		raise SystemExit(f"ffmpeg failed for {ogg}")
	print(f"wrote {ogg}")


def main() -> None:
	face_src = find_source("septic_face_source.png")
	scream_src = find_source("screamer_flash_source.png")
	body_src = find_source("septic_body_ref.png")
	pda_src = find_source("pda_corrupted_source.png")

	face_img = Image.open(face_src) if face_src else None
	scream_img = Image.open(scream_src) if scream_src else None
	body_img = Image.open(body_src) if body_src else None
	pda_img = Image.open(pda_src) if pda_src else None

	if face_img is not None:
		face = grain(fit_square(face_img, 256, zoom=1.12))
		save_png(face.convert("RGBA"), TEX / "gui" / "septic_face.png")
		save_png(add_drips(face), TEX / "gui" / "septic_face_drip.png")
	else:
		print("WARN: septic_face_source.png missing — keeping existing face")

	if scream_img is not None:
		scream = grain(fit_square(scream_img, 256, zoom=1.05), amount=22)
		save_png(scream.convert("RGBA"), TEX / "gui" / "screamer_flash.png")
	elif face_img is not None:
		save_png(add_drips(grain(fit_square(face_img, 256, 1.0))), TEX / "gui" / "screamer_flash.png")

	save_png(drip_layer(256), TEX / "gui" / "septic_drip.png")
	save_png(vignette(256), TEX / "gui" / "septic_vignette.png")

	if pda_img is not None:
		pda = grain(pda_img.convert("RGB").resize((256, 192), Image.Resampling.LANCZOS), amount=14)
		save_png(pda.convert("RGBA"), TEX / "gui" / "pda_corrupted.png")

	save_png(infection_overlay(), TEX / "block" / "infection_overlay.png")
	save_png(septic_skin(face_img, body_img), TEX / "entity" / "septic.png")

	with tempfile.TemporaryDirectory() as td:
		tdp = Path(td)
		clips = {
			"impact": synth_impact(),
			"static_burst": synth_static(),
			"drip": synth_drip(),
			"heartbeat": synth_heartbeat(),
			"whisper_bed": synth_whisper_bed(),
		}
		for name, samples in clips.items():
			wav = tdp / f"{name}.wav"
			write_wav(wav, samples)
			to_ogg(wav, SND / f"{name}.ogg")


if __name__ == "__main__":
	main()
