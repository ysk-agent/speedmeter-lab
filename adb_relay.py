import os, socket, sys, threading
from urllib.parse import urlparse, unquote
import base64

LOCAL_PORT = int(sys.argv[1]); TARGET_HOST = sys.argv[2]; TARGET_PORT = int(sys.argv[3])

def connect_tunnel():
    raw = os.environ["HTTPS_PROXY"]
    p = urlparse(raw)
    s = socket.create_connection((p.hostname, 3130), timeout=20)
    lines = [f"CONNECT {TARGET_HOST}:{TARGET_PORT} HTTP/1.1", f"Host: {TARGET_HOST}:{TARGET_PORT}"]
    if p.username:
        user = unquote(p.username); pw = unquote(p.password or "")
        tok = base64.b64encode(f"{user}:{pw}".encode()).decode()
        lines.append(f"Proxy-Authorization: Basic {tok}")
    s.sendall(("\r\n".join(lines) + "\r\n\r\n").encode())
    buf = b""
    while b"\r\n\r\n" not in buf:
        chunk = s.recv(4096)
        if not chunk: break
        buf += chunk
    head, _, rest = buf.partition(b"\r\n\r\n")
    if " 200" not in head.split(b"\r\n", 1)[0].decode("latin-1", "replace"):
        s.close(); raise RuntimeError("CONNECT failed: " + head.decode("latin-1", "replace"))
    s.settimeout(None)
    return s, rest

def pump(src, dst):
    try:
        while True:
            data = src.recv(65536)
            if not data: break
            dst.sendall(data)
    except OSError:
        pass
    try: dst.shutdown(socket.SHUT_WR)
    except OSError: pass

def handle(client):
    try:
        tunnel, rest = connect_tunnel()
    except Exception as e:
        print("tunnel failed:", e, flush=True)
        client.close(); return
    if rest:
        client.sendall(rest)
    t = threading.Thread(target=pump, args=(tunnel, client), daemon=True)
    t.start()
    pump(client, tunnel)
    client.close(); tunnel.close()

def main():
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(("127.0.0.1", LOCAL_PORT)); srv.listen(5)
    print(f"relay 127.0.0.1:{LOCAL_PORT} -> {TARGET_HOST}:{TARGET_PORT}", flush=True)
    while True:
        c, _ = srv.accept()
        threading.Thread(target=handle, args=(c,), daemon=True).start()

if __name__ == "__main__":
    main()
