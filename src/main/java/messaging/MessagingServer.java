package messaging;

import messaging.helpers.Message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class MessagingServer {

    private DatagramSocket udpSocket;
    private int port;

    public MessagingServer(int port) throws SocketException, IOException {
        this.port = port;
        this.udpSocket = new DatagramSocket(this.port);
    }



    public void listen() throws Exception {
        byte incomingData[] = new byte[1024];

        while (true){

            DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
            udpSocket.receive(incomingPacket);
            byte[] data = incomingPacket.getData();
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            try {
                Message message = (Message) is.readObject();


                System.out.println(message.getMessageType());

                switch (message.getMessageType()){
                    case 1:
                        System.out.println("Received a prepare message");
                        //TODO:Go to acceptor
                        break;
                    case 2:
                        System.out.println("Received an accept message");
                        //TODO: Go to acceptor
                        break;
                    case 3:
                        System.out.println("Received a promise message");
                        //TODO: Go to proposer
                        break;
                    case 4:
                        System.out.println("Received an nack for proposal");
                        //TODO: Go to proposer
                        break;
                    case 5:
                        System.out.println("Received a nack for log position");
                        //TODO: Go to proposer
                        break;
                    case 6:
                        System.out.println("Recieved an ack for proposal");
                        //TODO: Proposal was accepted!!. Let the proposer know
                        break;
                    case 7:
                        System.out.println("Recieved nack for accept message");
                        //TODO: Send to proposer
                        break;
                    case 8:
                        System.out.println("A new value was learned, commit it");
                        //TODO: Send to learner and maybe proposer
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
