SERVER  := TCPServer.class
CLIENT  := TCPClient.class
CC      := javac
CFLAGS  := -d .
RM      := rm -f
SRCSRV  := Server/src/TCPServer.java
SRCCLI  := Client/src/TCPClient.java
RUN     := java

.PHONY: all clean re runserver runclient

all: $(SERVER) $(CLIENT)

$(SERVER):
	$(CC) $(CFLAGS) $(SRCSRV)

$(CLIENT):
	$(CC) $(CFLAGS) $(SRCCLI)

clean:
	$(RM) $(SERVER) $(CLIENT)

re: clean all

runserver: $(SERVER)
	@$(RUN) TCPServer || true

runclient: $(CLIENT)
	@$(RUN) TCPClient || true