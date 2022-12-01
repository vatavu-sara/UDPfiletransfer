# UDPfiletransfer

Server to n(<=10) clients file transfer using UDP working kind of concurrently. 

Arguments:

- server: `<portNumber> <numberOfClients>`
- client: `<portNumber> <indexOfClient> <fileWanted> <chanceOfPacketLost(/100)>`

Features:
* simulates packet loss and treats the case
* every packet needs to first get(safely) to all clients and then the next one is read from the initial and sent to everyone
* uses `go-back-N` method or `selective repeat` (not yet implemented)
