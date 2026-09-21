#!/usr/bin/env python3
"""Prints the mean absolute difference (0 to 255) between the middle half of two PNG screenshots of the same size: 0.0 for the same picture. Used by run.sh to see
whether the crop screen's photo moved. Usage: png_diff.py a.png b.png"""
import struct, sys, zlib

def load(path):
    data = open(path, "rb").read(); pos = 8; idat = b""
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8]); body = data[pos + 8:pos + 8 + length]
        if kind == b"IHDR": width, height, depth, ctype = struct.unpack(">IIBB", body[:10]); assert depth == 8
        if kind == b"IDAT": idat += body
        pos += 12 + length
    bpp = {2: 3, 6: 4}[ctype]; raw = zlib.decompress(idat); stride = width * bpp; rows, prev = [], bytearray(stride)
    for y in range(height):
        f = raw[y * (stride + 1)]; line = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for i in range(stride):
            a = line[i - bpp] if i >= bpp else 0; b = prev[i]; c = prev[i - bpp] if i >= bpp else 0
            if f == 1: line[i] = (line[i] + a) & 255
            elif f == 2: line[i] = (line[i] + b) & 255
            elif f == 3: line[i] = (line[i] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c; pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line); prev = line
    return width, height, bpp, rows

w1, h1, bpp1, rows1 = load(sys.argv[1]); w2, h2, bpp2, rows2 = load(sys.argv[2])
assert (w1, h1) == (w2, h2), f"the screenshots differ in size: {w1}x{h1} and {w2}x{h2}"
total = count = 0
for y in range(int(h1 * 0.25), int(h1 * 0.75), 7):
    for x in range(0, w1, 7):
        for k in range(3):
            total += abs(rows1[y][x * bpp1 + k] - rows2[y][x * bpp2 + k]); count += 1
print(round(total / count, 2))
