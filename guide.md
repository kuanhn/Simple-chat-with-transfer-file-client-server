## Hướng dẫn cơ bản chạy chương trình demo:

### 1, Build
- C server và C client: trong thư mục gốc (ngầm định thư mục gốc là thư mục Simple-chat-with-transfer-file), gõ:

`make` // sinh ra file client, server, client.o, server.o

-> file server được dùng để chạy server

-> file client được dùng để chạy client connect tới server

- Java client: (dùng IDE IntelliJ IDEA mở project và build jar), để tiện thì mình đã build sẵn trong thư mục JavaChat/out/artifacts/JavaChat_jar

-> file JavaChat.jar để chạy client connect tới server

### 2, Run
- Run Server:

`./server -p [PORT]` (chạy ngầm định ./server sẽ chạy trên port 7400)

- Run Client:

C client: `./client -p [PORT] [HOSTNAME]` (chạy ngầm đinh ./client [HOSTNAME] sẽ connect tới port 7400)

Java client: `java -jar JavaChat.jar` (luôn chạy ngầm đinh trên port 7400 vì set cứng)

### 3. Test
C client thì xem trong README.md trên github nhé

Java client
