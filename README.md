# ChatProject
Simple chat application implemented with sockets. Package consists of four classes:
- ChatServer - server side,
- ChatClient - client side,
- Handler - used by ChatServer, to make connecection of multiple clients at the same time possible,
- ChatFrame - swing GUI template used by both server and client with some differences.

Every time a client connects to a server, server creates the runnable Handler object, which is responsible for client-server communication (executed in a thread pool).

Some actions are triggered by String commands (/disconnect, /reconnect, /priv...). 

For example, when user choose 'whisper' from options menu, "/whisper" is sent to a server. 

Server then responds with "/whisper" + sysMessage, which triggers further actions (show JOptionPane to select someone to send him a whisper message).

Other users cannot misuse that system, because if they send message that contains String sysMessage, it will not end up anywhere (Handler won't pass it to other clients).
