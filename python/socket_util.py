import socket
import json

class TribesServer:
    def __init__(self, host="localhost", port=5000):
        self.host = host
        self.port = port
        self.connect()

    def connect(self):
        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server.bind((self.host, self.port))
        self.server.listen(1)
        print(f"[Python] Waiting for Java on {self.host}:{self.port}...")
        self.conn, addr = self.server.accept()
        print(f"[Python] Java connected from {addr}")

    def reconnect (self):
        self.connect()
        self.run()
    def run(self):
        while True:
            try:
                data = self.conn.recv(4096).decode()
            except:
                print("Disconnected: Reconnecting")
                self.reconnect()
            if not data:
                break
            
            # Parse JSON from Java
            payload = json.loads(data)
            available = payload["available_actions"]
            print("Available actions:", available)

            # Example: always choose the first action
            inp = input("option: ")
            chosen = available[int(inp)]

            # Send response
            response = {"action": chosen}
            self.conn.sendall((json.dumps(response) + "\n").encode())

        self.conn.close()

if __name__ == "__main__":
    server = TribesServer()
    server.run()