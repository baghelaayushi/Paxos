package messaging;

import messaging.helpers.*;
import roles.Acceptor;
import roles.Learner;
import roles.Proposer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class MessagingServer {

    private DatagramSocket udpSocket;
    private int port;
    private Acceptor acceptor;

    public MessagingServer(int port) throws SocketException, IOException {
        this.port = port;
        this.udpSocket = new DatagramSocket(this.port);
        //need to change this(probably create an object in server class)
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
                Acceptor instance = Acceptor.getInstance();
                Learner learner = Learner.getInstance();

                switch (message.getMessageType()){
                    case 1:
                        instance.processPrepareRequest((PrepareMessage) message);
                        //TODO:Go to acceptor
                        break;
                    case 2:
                        instance.processAcceptRequest((AcceptMessage)message);
                        //TODO: Go to acceptor
                        break;
                    case 3:
                        PrepareAck received =  (PrepareAck) message;
                        Proposer.getInstance(null,null,null).processProposalAcks(received,received.isAck());
                        break;
                    case 5:
                        PrepareAck recvd =  (PrepareAck) message;
                        Proposer.getInstance(null,null,null).processProposalAcks(recvd,recvd.isAck());
                        //TODO: Go to proposer
                        break;
                    case 6:
                        Proposer.getInstance(null,null,null).learnAcceptanceAcks(message);
                    case 7:
//                        System.out.println("Recieved nack for accept message");
                        //TODO: Send to proposer
                        break;
                    case 8:
                        learner.learner((LearnMessage) message);
                        break;
                    case 9:
                        learner.lastLogPointer(message);
                        break;
                    case 10:
                        System.err.println("Recvd setPtr");
                        learner.setPointer((LogPositionMessage) message);
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
