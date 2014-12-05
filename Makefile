all: client server
client: client.o
	cc client.o -o client
server: server.o
	cc server.o -o server
client.o: client.c
	cc -c client.c
server.o: server.c
	cc -c server.c
clean:
	rm -f *.o
clean-all:
	rm -f *.o temp_* client server
