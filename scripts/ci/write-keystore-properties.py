#!/usr/bin/env python3
"""Writes the release keystore's properties file (add-ci-workflows, design.md decision 3).

androidApp/build.gradle.kts reads it with java.util.Properties.load, which treats backslashes as escapes, strips
leading whitespace and reads ISO-8859-1. So every value is escaped the way Properties.store escapes it, and any
password round-trips exactly (e.g. `test\\secret` would otherwise be read back as `testsecret`).

Usage: KEYSTORE_PASSWORD=... write-keystore-properties.py <properties file> <keystore file> <key alias>
"""
import os
import sys


def escape(value: str) -> str:
    out = []
    for i, ch in enumerate(value):
        code = ord(ch)
        if ch == "\\":
            out.append("\\\\")
        elif ch == " " and i == 0:
            out.append("\\ ")
        elif ch in "\t\n\r\f":
            out.append({"\t": "\\t", "\n": "\\n", "\r": "\\r", "\f": "\\f"}[ch])
        elif ch in "=:#!":
            out.append("\\" + ch)
        elif code < 0x20 or code > 0x7E:
            # Properties.load reads ISO-8859-1; \\uXXXX keeps anything else exact (UTF-16 units, as Java does).
            for unit in ch.encode("utf-16-be").hex(" ", 2).split():
                out.append("\\u" + unit.upper())
        else:
            out.append(ch)
    return "".join(out)


def main() -> None:
    properties_file, keystore_file, key_alias = sys.argv[1:4]
    password = os.environ["KEYSTORE_PASSWORD"]
    lines = {
        "storeFile": keystore_file,
        "storePassword": password,
        "keyAlias": key_alias,
        # PKCS12 keystores have one password, so the key password is the store's (docs/distribution.md).
        "keyPassword": password,
    }
    with open(properties_file, "w", encoding="ascii") as f:
        for key, value in lines.items():
            f.write(f"{key}={escape(value)}\n")


if __name__ == "__main__":
    main()
