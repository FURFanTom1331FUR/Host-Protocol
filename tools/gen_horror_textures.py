#!/usr/bin/env python3
"""High-contrast infection overlay (no Pillow). Septic HUD faces are generated images, not this script."""
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


def hash2(x: int, y: int, salt: int = 0) -> int:
	n = (x * 374761393 + y * 668265263 + salt * 1274126177) & 0xFFFFFFFF
	n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
	return n & 0xFFFF


def infection_overlay() -> list[list[tuple[int, int, int, int]]]:
	"""Opaque dark membrane + bright protocol-purple veins that read outdoors."""
	w = h = 16
	px = fill(w, h, (0, 0, 0, 0))
	void = (0, 0, 0, 0)
	rot = (8, 0, 10, 255)
	flesh = (28, 4, 36, 255)
	flesh2 = (46, 8, 58, 255)
	vein = (123, 44, 191, 255)
	vein_hi = (210, 92, 255, 255)
	edge = (255, 180, 255, 255)
	for y in range(h):
		for x in range(w):
			n = hash2(x, y, 17) / 65535.0
			n2 = hash2(x + 3, y * 2, 91) / 65535.0
			# Crackle: several diagonal + warped bands.
			v1 = abs(((x * 3 + y * 5) % 7) - 3) + n * 1.4
			v2 = abs(((x * 5 - y * 2) % 9) - 4) + n2
			v3 = abs((x + y * 2) % 6 - 2.5)
			vein_d = min(v1, v2, v3)
			border = x in (0, 15) or y in (0, 15)
			if vein_d < 0.85:
				put(px, x, y, edge if n > 0.72 else vein_hi)
			elif vein_d < 1.55:
				put(px, x, y, vein)
			elif border or n < 0.78:
				put(px, x, y, flesh2 if n2 > 0.45 else flesh)
			elif n < 0.90:
				put(px, x, y, rot)
			else:
				put(px, x, y, void)
	# Extra bright junctions so the overlay pops against sunlit stone.
	for x, y in ((2, 3), (7, 2), (12, 4), (4, 9), (9, 11), (13, 12), (5, 14), (10, 7)):
		put(px, x, y, edge)
		put(px, x + 1, y, vein_hi)
	return px


def main():
	block = ROOT / "block"
	write_png(block / "infection_overlay.png", infection_overlay())


if __name__ == "__main__":
	main()
