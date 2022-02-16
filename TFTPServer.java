import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;

import java.net.*;
import java.util.*;
import java.io.*;

/**
 * TFTPServer - Connects to TFTPClient
 * @author  I. Babani & Z. Asad
 * @version 2205
 */

public class TFTPServer extends Application implements EventHandler<ActionEvent>, TFTPConstants{
   // Window attributes
   private Stage stage;
   private Scene scene;
   private VBox root;
   
   // GUI Components
   TextField tfServerIP = new TextField();
   Button btnStart = new Button("Start");
   Button btnChooseFolder = new Button("Choose Folder");
   TextField tfDirectory = new TextField();
   TextArea taLog = new TextArea();
   
   // Socket stuff
   private DatagramSocket mainSocket = null;
   
   // Other stuff
   public Server server;
   private byte[] holder;
   File currentDir;
   
    
   /**
    * main program
    */
   public static void main(String[] args) {
      launch(args);
   }
   
   /**
    * Launch, draw and set up GUI
    * Do server stuff
    */
   public void start(Stage _stage) {
      // Window setup
      stage = _stage;
      stage.setTitle("TFTPServer - Babani & Asad");
      stage.setOnCloseRequest(
         new EventHandler<WindowEvent>() {
            public void handle(WindowEvent evt) { doShutDown(); }
         });
      stage.setResizable(false);
      root = new VBox(8);
       
      //Add start/stop button
      FlowPane fpTop = new FlowPane(8, 8);
      fpTop.setAlignment(Pos.CENTER_RIGHT);
      
      fpTop.getChildren().addAll(btnStart, btnChooseFolder);
      root.getChildren().add(fpTop);
      
      //ACTIVATE BUTTONS
      btnStart.setOnAction(this);
      btnChooseFolder.setOnAction(this);
      
      //Log components
      
      FlowPane fpBot = new FlowPane(8,8);
      fpBot.setAlignment(Pos.CENTER_LEFT);
      
      FlowPane fpBotBot = new FlowPane(8,8);
      fpBotBot.setAlignment(Pos.CENTER_LEFT);
      
      FlowPane fpLog = new FlowPane(8, 8);
      fpLog.getChildren().add(taLog);
      taLog.setPrefHeight(193);
      taLog.setPrefWidth(300);
      
      fpBot.getChildren().addAll(new Label("Server IP: "), tfServerIP);
      fpBot.getChildren().addAll(new Label("Current Directory: "), tfDirectory);
      tfServerIP.setEditable(false);
      taLog.setEditable(false);
      tfDirectory.setEditable(false);
      root.getChildren().add(fpBot);
      root.getChildren().add(fpBotBot);
      root.getChildren().add(fpLog);
      
      // Show window
      scene = new Scene(root, 300, 300);
      stage.setScene(scene);
      stage.show();
      
      doStartProcedure();
   }
  
   public void handle(ActionEvent evt) {
      Button btn = (Button)evt.getSource();
      switch(btn.getText()) {
         case "Start":
            doStart();
            break;
         case "Stop":
            doStop();
            break;
         case "Choose Folder":
            doChooseFolder();
            break;
      }
   }
   
   /** 
    * doStart - starts server
    */
   public void doStart() {
      server = new Server();
      server.start();
      btnStart.setText("Stop");
      log("Server started.");
   }  
   
   /** 
    * doStop - stops server
    */
   public void doStop() {
      server.stopServer();
      btnStart.setText("Start");
   }
   
   /** 
    * doChooseFolder - change directory
    */
   public void doChooseFolder() 
   {
   
   }   
   
   /** 
    * doStartProcedure - do on startup
    */
   public void doStartProcedure() {
      //fill server ip text field
      try{
         URL whatismyip = new URL("http://checkip.amazonaws.com");
         BufferedReader in = new BufferedReader(new InputStreamReader(
                      whatismyip.openStream()));
      
         tfServerIP.setText(in.readLine());
      
      }
      catch(Exception e){doShutDown();}
      
      //set current directory
      currentDir = new File(".");
      tfDirectory.setText(currentDir.getAbsolutePath());
   }
   
   /** 
    * doShutDown - launched on window close
    */
   public void doShutDown() {
      try {
         //close all sockets
         doStop();
         System.exit(0);      
      }
      catch(Exception e) {
         log("Exception: " + e + "\n");
      }
   }
   
   /** 
   * Server - makes an instance of a server to connect to a client
   */
   class Server extends Thread
   {
      public void run()
      {
      // Make server socket
         try
         {
            mainSocket = new DatagramSocket(TFTP_PORT);
         }
         catch(IOException ioe) {
            log("IO Exception: " + ioe + "\n");
            return;
         }
      
      // Wait for client here
         while(true)
         {   
            try{
            
               holder = new byte[MAX_PACKET];
               DatagramPacket pkt = new DatagramPacket(holder, MAX_PACKET);
               mainSocket.receive(pkt);
               
               Client client = new Client(pkt);
               client.start();
            
            }
            catch(Exception e){
               return;}
            
         
         }
      }
         
      /*
      * stopServer() - stops the server
      */
      public void stopServer()
      {
         try
         {
            mainSocket.close();
            log("Server stopped.");
         }
         catch(Exception e){
            log("Exception: " + e + "\n");
         }
      }
      
   }
  
   /** 
    * Client - Instance of server, only for client.
    */
   class Client extends Thread {
      // ATTRIBUTES
      private DatagramPacket firstPkt = null;
      private DatagramSocket socket = null;
         
      //CONSTRUCTOR
      public Client(DatagramPacket _pkt)
      {
         firstPkt = _pkt;
         try{socket = new DatagramSocket();}
         catch(Exception e){};
      }
      
      //METHODS   
      public void run()
      {
      
         log("Client packet received!\n");
        
         
         //dissect packet here, and send new one
      
         try
         {
            //now that packet has been received, do something
            //packet name is firstPkt
            
            DatagramPacket checkOp = firstPkt;
            //First, find opcode in packet
            ByteArrayInputStream bais =
               new ByteArrayInputStream(checkOp.getData(), checkOp.getOffset(), checkOp.getLength());
            DataInputStream dis = new DataInputStream(bais);
            short opcode = dis.readShort();
            
            switch (opcode)
            {
               case (1) :
                  Packet rrq = new Packet();
                  rrq.dissect(checkOp);
                  doRRQ(rrq,socket);
                  break;
               case (2) :
                  Packet wrq = new Packet();
                  wrq.dissect(checkOp);
                  doWRQ(wrq,socket);
                  break;
               default :
                  Packet prrq = new Packet();
                  prrq.dissect(checkOp);
                  doError(prrq,socket);
                  break;
                  
            }
            
            
         }
         catch(Exception e)
         {
            log(" Exception opening streams: " + e + "\n" );
            //because we're using TFTP, send an error packet
            return;
         }      
      
      }      
     
     /**
     * doError() - Sends undef error.
     */
     
      public void doError(Packet _err, DatagramSocket _socket)
      {
      
      }
     
     /**
     * doRRQ() - Deals with an RRQ Packet.
     */
     
      public void doRRQ(Packet _rrq, DatagramSocket _socket)
      {
         Packet rrq = _rrq;
         DatagramSocket socket = _socket;
         InetAddress address = null;
         //now that Packet has everything DP had, we can use it.
         int ackBlockNum = 0;
         int size = 512;
         DataInputStream dis = null;
         FileInputStream fis = null;
         
         File chosenFile = new File(rrq.getFilename());      
         try 
         {
            log("RRQ - Opening file " + rrq.getFilename());
            fis = new FileInputStream(chosenFile);
            dis = new DataInputStream(fis);
         }
         catch (Exception e) 
         {
         //filename does not exist, send error and return
            log("Exception caught.");
            return;
         }
      
      //if we get here, request is legit and all values are set.
      //get while loop to send data  
         
         //get bytes of file
         int fileSize = (int)chosenFile.length();
         
         //infinite loop that checks for reply
         while(true)
         {
            if (fileSize > 512)
            {
               //send file more than 512 bytes
               ++ackBlockNum;
                  
               byte[] blockDat = new byte[512];
               int dataSize = 0;        
            
               try 
               {  
                  dataSize = dis.read(blockDat, 0, blockDat.length);
               }
               catch (Exception e) {
                  log("" + e);
               }
                  
               log("Sending DATA packet \n");
                  
               //build the data packet
               Packet dataPB = new Packet(rrq.getAddress(), rrq.getPort(), ackBlockNum, blockDat, dataSize, 3);
               
               DatagramPacket dataPkt = dataPB.build();
                  //send the packet 
               try{socket.send(dataPkt);}
               catch(Exception e){}
                  
               log("Client sending ... Opcode3 (DATA) Blk#(" + ackBlockNum + ") \n" );
                  
            
                  //check for incoming ACK packet
               DatagramPacket inAckPacket = new DatagramPacket(new byte[1500],1500);
                  
                  //recieve incoming packet
               try{
                  log("Waiting to receive ACK confirmation \n");
                  socket.receive(inAckPacket);
                  Packet inPB = new Packet();
                  inPB.dissect(inAckPacket);
                  log("Client received ACK packet \n");
               }catch(Exception e){}
               
               fileSize = fileSize - 512;
            
            }
            
            
            else
            {
               //send file under 512 bytes or finish off sending
               //send file more than 512 bytes
               ++ackBlockNum;
                  
               byte[] blockDat = new byte[512];
               int dataSize = 0;        
            
               try 
               {  
                  dataSize = dis.read(blockDat, 0, blockDat.length);
               }
               catch (Exception e) {
                  log("" + e);
               }
                  
               log("Sending DATA packet \n");
                  
               //build the data packet
               Packet dataPB = new Packet(rrq.getAddress(), rrq.getPort(), ackBlockNum, blockDat, dataSize, 3);
               
               DatagramPacket dataPkt = dataPB.build();
                  //send the packet 
               try{socket.send(dataPkt);}
               catch(Exception e){}
                  
               log("Client sending ... Opcode3 (DATA) Blk#(" + ackBlockNum + ") \n" );
                  
            
                  //check for incoming ACK packet
               DatagramPacket inAckPacket = new DatagramPacket(new byte[1500],1500);
                  
                  //recieve incoming packet
               try{
                  log("Waiting to receive ACK confirmation \n");
                  socket.receive(inAckPacket);
                  Packet inPB = new Packet();
                  inPB.dissect(inAckPacket);
                  log("Client received ACK packet \n");
               }catch(Exception e){}
                              
               log("SUCCESS: File " + chosenFile.getName() + " uploaded.");
               break;
            }
         }
       
      }
   
   
      public void doWRQ(Packet _wrq, DatagramSocket _socket)
      {  
      //dissect inbound WRQ packet
         //Packet wrq = new Packet();
         //wrq.dissect(dpWRQ);
         Packet wrq = _wrq;
         DatagramSocket socket = _socket;
         InetAddress address = null;
      
      
      //now that Packet has everything DP had, we can use it.
      
         int ackBlockNum = 0;
         int size = 512;
         DataOutputStream dos = null;
         FileOutputStream fos = null;
      
      
         File chosenFile = new File(wrq.getFilename());      
         try 
         {
            chosenFile.createNewFile();
            log("WRQ - Opening file " + wrq.getFilename());
            fos = new FileOutputStream(chosenFile);
            dos = new DataOutputStream(fos);
         }
         catch (Exception e) 
         {
         //filename does not exist, send error and return
            log("Exception caught.");
            return;
         }
      
      //if we get here, request is legit and all values are set.
      //send ack, and get while loop ready to receive data.
         Packet ack1 = new Packet(wrq.getAddress(), wrq.getPort(), ackBlockNum,4);
         DatagramPacket ack = ack1.build();
         log("Client sending ... Opcode " + 4 + " (ACK) Blk " + ack1.getBlockNum() + " \n");
         try{socket.send(ack);
            log("Client Sent ... Opcode " + 4 + " (ACK) Blk " + ack1.getBlockNum() + " \n");
         }
         catch(Exception e){log("Error occured sending: " + e);}
      
         //get ready to receive data
         
         while(true) {
            DatagramPacket inDownloadDP = new DatagramPacket(new byte[1500],1500);
            //recieve incoming packet
            try {
               socket.receive(inDownloadDP);
               log("Client received packet \n");
               
               Packet inData = new Packet();
               inData.dissect(inDownloadDP);
               
               if (inData.getOpcode() == 3) {
                  //data extracted
                  try {
                     byte[] dataToWrite = inData.getData();
                     if (dataToWrite.length == 512) {
                        //not last, write, ack, and continue
                        fos.write(dataToWrite);
                        log("Packet No. " + inData.getBlockNum() + " received. Sending ACK. \n");
                        //Build and send ACK packet 
                        Packet ackP = new Packet(inDownloadDP.getAddress(), inDownloadDP.getPort(), inData.getBlockNum(), ACK);
                        DatagramPacket dpACK = ackP.build();
                        log("Client sending ... Opcode " + 4 + " (ACK) Blk " + inData.getBlockNum() + " \n");
                        try{socket.send(ack);}
                        catch(Exception e){}
                     }
                     else {
                        //last packet, write, ack, and break
                        fos.write(dataToWrite);
                        log("Packet No. " + inData.getBlockNum() + " received. Sending ACK. \n");
                        //Build and send ACK packet 
                        Packet ackP = new Packet(inDownloadDP.getAddress(), inDownloadDP.getPort(), inData.getBlockNum(), ACK);
                        DatagramPacket dpACK = ackP.build();
                        log("Client sending ... Opcode " + 4 + " (ACK) Blk " + inData.getBlockNum() + " \n");
                        try{socket.send(ack);}
                        catch(Exception e){}
                        
                        log("SUCCESS: File " + chosenFile.getName() + " downloaded.");
                        break;
                     }
                  }
                  catch(Exception e){}
               }
               else {
                  //indicates error
                  log("ERROR packet detected, expected DATA. Halting process.");
                  return;
               }
            }
            catch(Exception e){}
         }
         try{fos.close();}
         catch(Exception e){}
      
         
      }
     
   }
        

   /**
     * log() - thread-safe updating of taLog
     */
   private void log(String message){
      Platform.runLater(
            new Runnable() {
               public void run(){
                  taLog.appendText(message + "\n");
                  System.out.println(message);
               }
            });
   }

}