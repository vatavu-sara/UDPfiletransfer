# UDPfiletransfer

### Networks 1 project
### Bachelor in Applied Information Technology 2nd year, University of Luxembourg

Server to n clients file transfer using UDP and transmitting using the Go-Back-N Algorithm.


Arguments:

- server: `<portNumber> <numberOfClients> <fileWanted> <windowSize>`
- client: `<portNumber> <numberOfClients> <indexOfClient> <chanceOfPacketLost(/100)> <windowSize>`

Features:
* intuitive launcher to launch a number of clients and set the arguments to each
* statistics file provided at the end 
* simulates packet loss and treats the case
* every packet needs to first get(safely) to all clients and then the next one is read from the initial and sent to everyone
* uses `go-back-N` method: starts sending N packets to each client and treats them as following:
   - if one packet is lost for client i, all next packets in the window will be ignored
   - after all clients finished receiving, the window is moved by the number packets received by everyone, and retransmission may start for only the packets missed by a certain client
   
 Bugs:
 
 * may crash if window size too big
 * may crash if number of packets too big(files of tens of GB tho)
 * Working kind of concurrently(when it comes to listening for confirmations), but not fully since parelizing all threads made problems. 
  
