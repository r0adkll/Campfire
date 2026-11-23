#!/usr/bin/env python3
# Copyright 2026, Drew Heavner and the Campfire project contributors
# SPDX-License-Identifier: GPL-3.0-only
"""Throttling TCP proxy: 127.0.0.1:LISTEN -> 127.0.0.1:TARGET, server-to-client capped at RATE bytes/s per connection.

usage: python3 -I throttle.py LISTEN TARGET RATE      e.g. throttle.py 13390 13389 40000

Sign the app in to http://127.0.0.1:LISTEN to slow its traffic (downloads, streaming) without
touching the server. Stop it with Ctrl-C or by killing the process.
"""
import asyncio
import sys
import time

LISTEN, TARGET, RATE = (int(a) for a in sys.argv[1:4])
CHUNK = 16 * 1024


async def pipe(reader, writer, rate):
    try:
        start, sent = time.monotonic(), 0
        while data := await reader.read(CHUNK):
            writer.write(data)
            await writer.drain()
            if rate:
                sent += len(data)
                ahead = sent / rate - (time.monotonic() - start)
                if ahead > 0:
                    await asyncio.sleep(ahead)
    except (ConnectionError, asyncio.CancelledError):
        pass
    finally:
        writer.close()


async def handle(client_reader, client_writer):
    try:
        server_reader, server_writer = await asyncio.open_connection("127.0.0.1", TARGET)
    except OSError:
        client_writer.close()
        return
    await asyncio.gather(pipe(client_reader, server_writer, 0), pipe(server_reader, client_writer, RATE))


async def main():
    server = await asyncio.start_server(handle, "127.0.0.1", LISTEN)
    print(f"throttling :{LISTEN} -> :{TARGET} at {RATE} B/s", flush=True)
    async with server:
        await server.serve_forever()


asyncio.run(main())
