package messaging;

import messaging.helpers.AcceptMessage;
import messaging.helpers.LearnMessage;
import messaging.helpers.Message;
import messaging.helpers.PrepareMessage;
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


//                System.out.println(message.getMessageType());

                switch (message.getMessageType()){
                    case 1:
//                        System.out.println("Received a prepare message");
                        instance.processPrepareRequest((PrepareMessage) message);
                        //TODO:Go to acceptor
                        break;
                    case 2:
//                        System.out.println("Received an accept message");
                        instance.processAcceptRequest((AcceptMessage)message);
                        //TODO: Go to acceptor
                        break;
                    case 3:
//                        System.out.println("Received a promise message");
                        Proposer.getInstance(null,null,null).processProposalAcks(message,true);
                        break;
                    case 4:
//                        System.out.println("Received an nack for proposal");
                        Proposer.getInstance(null,null,null).processProposalAcks(message,false);
                        //TODO: Go to proposer
                        break;
                    case 5:
//                        System.out.println("Received a nack for log position");
                        //TODO: Go to proposer
                        break;
                    case 7:
//                        System.out.println("Recieved nack for accept message");
                        //TODO: Send to proposer
                        break;
                    case 8:
                        learner.learner((LearnMessage) message);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
