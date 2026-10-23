#!/usr/bin/env python3
"""Minimal client for real 1.18 / 26.3 server-side integration assertions.

Packet IDs are verified against ConfigurationProtocols/GameProtocols in the
downloaded server. Registry and inventory payloads are intentionally opaque.
"""
import hashlib
import socket
import struct
import sys
import zlib
import pathlib
import threading
import time


def varint(value):
    data = bytearray()
    while True:
        byte = value & 127
        value >>= 7
        data.append(byte | (128 if value else 0))
        if not value:
            return bytes(data)


def read_varint(read):
    value = 0
    for shift in range(0, 35, 7):
        byte = read(1)[0]
        value |= (byte & 127) << shift
        if not byte & 128:
            return value
    raise ValueError("Invalid VarInt")


def string(value):
    data = value.encode()
    return varint(len(data)) + data


def main():
    port = int(sys.argv[1])
    version = sys.argv[2]
    modern = version == "26.3"
    protocol = 777 if modern else 758 if version == "1.18.2" else 757
    sock = socket.create_connection(("127.0.0.1", port), timeout=60)
    sock.settimeout(90)
    compression = False
    send_lock = threading.Lock()
    window_id = None
    window_body = b""

    def read(size):
        data = b""
        while len(data) < size:
            chunk = sock.recv(size - len(data))
            if not chunk:
                raise EOFError("Server closed connection")
            data += chunk
        return data

    def send(ident, data=b""):
        body = varint(ident) + data
        if compression:
            body = b"\x00" + body
        with send_lock:
            sock.sendall(varint(len(body)) + body)

    def attacks():
        marker = pathlib.Path(sys.argv[3]) / "network-ready.txt"
        while not marker.exists() or window_id is None:
            time.sleep(0.05)
        # Fast personal loads can open the final menu before this client has
        # consumed its Open Screen packet. Wait for the intended window title.
        expected_title = marker.read_text(encoding="utf-8").encode()
        while expected_title not in window_body:
            time.sleep(0.05)
        # These packets exercise the real container transaction path, including
        # the cursor, bottom inventory, hotbar swap, offhand, drop and collect.
        combinations = [(49, 0, 0), (49, 1, 0), (0, 0, 1), (0, 1, 1),
                        (0, 0, 2), (0, 40, 2), (0, 0, 4), (0, 1, 4),
                        (49, 0, 6), (60, 0, 1)]
        for slot, button, mode in combinations:
            container = varint(window_id) if modern else bytes([window_id])
            send(18 if modern else 8, container + varint(0) + struct.pack(">hb", slot, button) + varint(mode) + varint(0) + b"\x00")
        print("Sent 10 inventory attack packets", flush=True)

    threading.Thread(target=attacks, daemon=True).start()

    send(0, varint(protocol) + string("127.0.0.1") + struct.pack(">H", port) + varint(2))
    name = "StatsTester"
    ident = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
    ident[6] = (ident[6] & 15) | 48
    ident[8] = (ident[8] & 63) | 128
    send(0, string(name) + (bytes(ident) if modern else b""))
    state = "login"
    while True:
        data = read(read_varint(read))
        import io
        packet = io.BytesIO(data)
        if compression:
            expanded = read_varint(packet.read)
            packet = io.BytesIO(zlib.decompress(packet.read()) if expanded else packet.read())
        packet_id = read_varint(packet.read)
        body = packet.read()
        if state == "login":
            if packet_id == 3:
                compression = True
            elif packet_id == 2:
                if modern:
                    send(3)
                    state = "configuration"
                else:
                    state = "play"
                    print("Client entered play state", flush=True)
            elif packet_id == 0:
                raise RuntimeError("Login refused: " + repr(body))
        elif state == "configuration":
            if packet_id == 15:
                send(7, varint(0))
            elif packet_id == 4:
                send(4, body)
            elif packet_id == 5:
                send(5, body)
            elif packet_id == 3:
                send(3)
                state = "play"
                print("Client entered play state", flush=True)
            elif packet_id == 2:
                raise RuntimeError("Configuration refused: " + repr(body))
        elif not modern:
            if packet_id == 0x2e:
                window_id = read_varint(io.BytesIO(body).read)
                window_body = body
            elif packet_id == 0x21:
                send(0x0f, body)
            elif packet_id == 0x38:
                teleport = read_varint(io.BytesIO(body[33:]).read)
                send(0, varint(teleport))
            elif packet_id == 0x1a:
                raise RuntimeError("Disconnected: " + repr(body))
        else:
            if packet_id == 60:
                window_id = read_varint(io.BytesIO(body).read)
                window_body = body
            elif packet_id == 45:
                send(28, body)
            elif packet_id == 73:
                position = io.BytesIO(body)
                teleport = read_varint(position.read)
                coordinates = position.read(24)
                position.read(24)  # Velocity is not part of the acknowledgement.
                rotation = position.read(8)
                send(0, varint(teleport) + coordinates + rotation)
            elif packet_id == 50:
                send(44)
            elif packet_id == 11:
                send(11, struct.pack(">f", 4.0))
            elif packet_id == 62:
                send(45, body)
            elif packet_id == 32:
                raise RuntimeError("Disconnected: " + repr(body))


if __name__ == "__main__":
    main()
