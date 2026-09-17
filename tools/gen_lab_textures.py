#!/usr/bin/env python3
"""Tiny 16×16 Host Protocol item/block textures (no Pillow)."""
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hostprotocol/textures"


def write_png(path: Path, pixels: list[list[tuple[int, int, int, int]]]) -> None:
	h = len(pixels)
	w = len(pixels[0])
	raw = bytearray()
	for row in pixels:
		raw.append(0)
		for r, g, b, a in row:
			raw.extend((r, g, b, a))

	def chunk(tag: bytes, data: bytes) -> bytes:
		return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

	ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
	png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
	path.parent.mkdir(parents=True, exist_ok=True)
	path.write_bytes(png)
	print(f"wrote {path} ({w}x{h})")


def fill(w: int, h: int, color: tuple[int, int, int, int]) -> list[list[tuple[int, int, int, int]]]:
	return [[color for _ in range(w)] for _ in range(h)]


def put(px, x, y, color):
	if 0 <= y < len(px) and 0 <= x < len(px[0]):
		px[y][x] = color


def rect(px, x0, y0, x1, y1, color):
	for y in range(y0, y1):
		for x in range(x0, x1):
			put(px, x, y, color)


def pda_mk2():
	# Recolor of the existing PDA: cyan screen, gold bezel.
	bg = (0, 0, 0, 0)
	body = (18, 16, 22, 255)
	body2 = (28, 24, 34, 255)
	gold = (212, 168, 64, 255)
	gold_d = (156, 112, 32, 255)
	screen = (48, 230, 210, 255)
	screen_d = (20, 120, 140, 255)
	px = fill(16, 16, bg)
	rect(px, 4, 1, 12, 15, body)
	rect(px, 5, 2, 11, 14, body2)
	rect(px, 5, 1, 11, 2, gold)
	rect(px, 6, 3, 10, 8, screen_d)
	rect(px, 7, 4, 9, 7, screen)
	put(px, 6, 4, screen)
	put(px, 9, 6, (180, 255, 240, 255))
	rect(px, 6, 9, 10, 10, gold_d)
	rect(px, 6, 11, 8, 12, (80, 80, 88, 255))
	rect(px, 8, 11, 10, 12, (48, 48, 56, 255))
	rect(px, 6, 12, 10, 13, (36, 36, 42, 255))
	return px


def lab_top():
	px = fill(16, 16, (42, 36, 28, 255))
	rect(px, 0, 0, 16, 16, (168, 132, 42, 255))
	rect(px, 1, 1, 15, 15, (212, 170, 58, 255))
	rect(px, 3, 3, 13, 13, (36, 24, 48, 255))
	rect(px, 5, 5, 11, 11, (92, 48, 150, 255))
	rect(px, 7, 7, 9, 9, (180, 120, 255, 255))
	for i in range(16):
		put(px, i, 0, (90, 70, 20, 255))
		put(px, i, 15, (90, 70, 20, 255))
		put(px, 0, i, (90, 70, 20, 255))
		put(px, 15, i, (90, 70, 20, 255))
	return px


def lab_side():
	px = fill(16, 16, (88, 88, 96, 255))
	rect(px, 0, 0, 16, 3, (196, 154, 48, 255))
	rect(px, 0, 3, 16, 16, (72, 72, 80, 255))
	for y in range(5, 15, 3):
		for x in range(2, 15, 4):
			put(px, x, y, (123, 44, 191, 255))
			put(px, x + 1, y, (60, 24, 90, 255))
	rect(px, 6, 8, 10, 14, (28, 24, 36, 255))
	rect(px, 7, 9, 9, 13, (48, 220, 200, 255))
	return px


def lab_bottom():
	px = fill(16, 16, (58, 58, 64, 255))
	rect(px, 1, 1, 15, 15, (48, 48, 54, 255))
	return px


def main():
	item = ROOT / "item"
	block = ROOT / "block"
	write_png(item / "pda_mk2.png", pda_mk2())
	write_png(block / "lab_table_top.png", lab_top())
	write_png(block / "lab_table_side.png", lab_side())
	write_png(block / "lab_table_bottom.png", lab_bottom())


if __name__ == "__main__":
	main()
