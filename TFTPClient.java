// Upload and Download buttons still not completly coded
import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;
import javafx.collections.*;
import java.io.FileReader;
import javafx.stage.FileChooser.*;
import java.io.File;

import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;

/**
 * TFTPClient - Connects to TFTPServer
 * @author  I. Babani & Z. Asad
 * @version 2205
 */

public class TFTPClient extends Application implements EventHandler<ActionEvent>, TFTPConstants{
   // Window attributes
   private Stage stage;
   private Scene scene;
   private VBox root;
   
   // GUI Attributes
   // server ip, tfIP, connect
   FlowPane fpIP = new FlowPane(8,8);
   Label lblIP = new Label("Server IP: ");
   TextField tfIP = new TextField("localhost");
   Button btnConnect = new Button("Connect");
      
   // upload, download, choose folder
   FlowPane fpButtons = new FlowPane(8,8);
   Button btnUpload = new Button("Upload");
   Button btnDownload = new Button("Download");
   Button btnChooseFolder = new Button("Choose Folder");

   // current dir, tfcurrentdir
   FlowPane fpDir = new FlowPane(8,8);
   Label lblDir = new Label("Current Directory");
   TextField tfDir = new TextField();
   
   // log
   FlowPane fpLog = new FlowPane(8,8);
   TextArea taLog = new TextArea();
   
   // Other attributes
   public static final int SERVER_PORT = 69;
   private DatagramSocket socket = null;
   private InetAddress address;
   private DataInputStream dis;
   private DataOutputStream dos;
   private File fileObj = null;
   private FileInputStream fis = null;
   private String curDir = null;
   private ObjectOutputStream oos = null;
   private ObjectInputStream ois = null;
   private Scanner scn = null;
   private PrintWriter pw = null;
   private File chosenFile = null;
   private FileOutputStream fos;
    
   /**
    * main program 
    */
   public static void main(String[] args) {
      launch(args);
   }
   
   /**
    * launch - draw and set up GUI
    */
   public void start(Stage _stage) {
      stage = _stage;
      stage.setTitle("TFTPClient - Babani & Asad");
      stage.setOnCloseRequest(
         new EventHandler<WindowEvent>() {
            public void handle(WindowEvent evt) { System.exit(0); }
         });
      stage.setResizable(false);
      root = new VBox(8);
      
      //Set up FlowPanes
      fpIP.getChildren().addAll(lblIP, tfIP);
      fpButtons.getChildren().addAll(btnUpload, btnDownload, btnChooseFolder);
      fpDir.getChildren().addAll(lblDir, tfDir);
      fpLog.getChildren().add(taLog);
      taLog.setPrefWidth(500);
      taLog.setPrefHeight(150);
      
      //Center all
      fpIP.setAlignment(Pos.CENTER);
      fpButtons.setAlignment(Pos.CENTER);
      fpDir.setAlignment(Pos.CENTER);
      fpLog.setAlignment(Pos.CENTER);
      
      taLog.setPrefHeight(124);
      taLog.setEditable(false);
      
      tfDir.setEditable(false);
      
      File original = new File(".");
      tfDir.setText(original.getAbsolutePath());
      tfDir.setPrefWidth(420);
      
      //Activate buttons
      btnConnect.setOnAction(this);
      btnUpload.setOnAction(this);
      btnDownload.setOnAction(this);
      btnChooseFolder.setOnAction(this);
   
      
      root.getChildren().addAll(fpIP, fpButtons, fpDir, fpLog);
      
               
      // Show window
      scene = new Scene(root, 500, 250);
      stage.setScene(scene);
      stage.show();      
   }
   
   /**
    * Button dispatcher
    */
   public void handle(ActionEvent evt) {
      Button btn = (Button)evt.getSource();
      switch(btn.getText()) {
         case "Upload":
            doUpload();
            break;
         case "Download":
            doDownload();
            break;
         case "Choose Folder":
            doChooseFolder();
            break;
      }
   }
   
   /**
    * doConnect - Connect to the server
    */
   
   public void doConnect() {
      try{
         address = InetAddress.getByName(tfIP.getText());
         socket = new DatagramSocket();
         log("Connected! \n");
      }
      catch(UnknownHostException uhe){
         Alert alert = new Alert(AlertType.ERROR, "no such host: " + uhe);
         alert.setHeaderText("Unknown Host");
         alert.showAndWait();
         System.exit(1);
      }
      catch(SocketException se){}
   } 


   /**
    * doUpload - upload
    */
   private void doUpload() {
      FileChooser chooseFile = new FileChooser();
      chooseFile.setTitle("Select a File to Upload");
      chosenFile = chooseFile.showOpenDialog(stage);
      if (!(chosenFile == null))
      {
         System.out.println(chosenFile.getAbsolutePath());
         TextInputDialog tig = new TextInputDialog(chosenFile.getName());
         tig.setTitle("Name to save file as: ");
         tig.showAndWait();
         String remName = tig.getEditor().getText();
         System.out.println(remName);
         doConnect();
         Uploader upThread = new Uploader(remName, chosenFile);
         upThread.start();
      }  
   }   
   
   /**
    * doDownload - download
    */
   private void doDownload() {
      TextInputDialog tig = new TextInputDialog("message.txt");
      tig.setTitle("Remote Name");
      tig.setHeaderText("Remote File to Download: ");
      tig.showAndWait();
      String remName = tig.getEditor().getText();
      FileChooser fileChoose = new FileChooser();
      //fileChoose.setInitialDirectory(new File(tfDir.getText()));
      fileChoose.setTitle("File for Saving for Download");
      File localFile = fileChoose.showSaveDialog((Window)stage);
      doConnect();
      Downloader downThread = new Downloader(remName, localFile);
      downThread.start();
   }
   
   /**
    * doChooseFolder - change folder
    */
   private void doChooseFolder(){
      DirectoryChooser dirChooser = new DirectoryChooser();
      dirChooser.setInitialDirectory(new File(tfDir.getText()));
      dirChooser.setTitle("Select Folder for Uploads and Downloads");
      File file = dirChooser.showDialog((Window)stage);
      tfDir.setText(file.getAbsolutePath());
      tfDir.setPrefColumnCount(tfDir.getText().length());
   }
    
   /**
    * Upload Thread
    */
   class Uploader extends Thread {
      // Attributes
      private DatagramSocket socket;
      private String fileName;
      private File file;
      InetAddress netAddress = null;
      int port = 69;
      private int blockNum = 0;
      private int size = 512;
      
      // Constructor
      public Uploader(String _fileName, File _file){
         fileName = _fileName;
         file = _file;
         try {
            socket = new DatagramSocket();
            InetAddress.getByName(tfIP.getText());
         }
         catch (Exception e) {
            log("Failed to start upload: " + e);
            System.exit(1);
         }
      }
      public void run() 
      {
         
         log("Starting upload " + fileName + " --> " + file.getName() +"\n");
            
         
         //Build and send WRQ packet 
         Packet wrqP = new Packet(address, port, fileName, WRQ);
         DatagramPacket wrq = wrqP.build();
         log("Client sending ... Opcode " + 2 + " (WRQ) " + " Filename " + "<" + fileName + "> Mode <octet> \n");
         try{socket.send(wrq);}
         catch(Exception e){}
            
         //used for DATA packet
         DataInputStream dis = null;
         DatagramPacket inPacket = new DatagramPacket(new byte[1500],1500);
                  
         //recieve incoming packet
         try{
            socket.receive(inPacket);
            log("Client received packet \n");
                     
         }catch(Exception e){}
               
         
         //get bytes of file
         int fileSize = (int)file.length();
         
         //infinite loop that checks for reply
         while(true)
         {
            if (fileSize > 512)
            {
               //send file more than 512 bytes
               Packet inPB = new Packet();
               inPB.dissect(inPacket);
            
               ++blockNum;
                  
               byte[] blockDat = new byte[512];
               int dataSize = 0;        
            
               try 
               {
                  dis = new DataInputStream(new FileInputStream(file));    
                  dataSize = dis.read(blockDat, 0, blockDat.length);
               }
               catch (Exception e) {
                  log("" + e);
               }
                  
               log("Sending DATA packet \n");
                  
               //build the data packet
               Packet dataPB = new Packet(address, inPB.getPort(), blockNum, blockDat, dataSize, 3);
               
               DatagramPacket dataPkt = dataPB.build();
                  //send the packet 
               try{socket.send(dataPkt);}
               catch(Exception e){}
                  
               log("Client sending ... Opcode3 (DATA) Blk#(" + blockNum + ") \n" );
                  
            
                  //check for incoming ACK packet
               DatagramPacket inAckPacket = new DatagramPacket(new byte[1500],1500);
                  
                  //recieve incoming packet
               try{
                  log("Waiting to receive ACK confirmation \n");
                  socket.receive(inAckPacket);
                  inPB.dissect(inAckPacket);
                  log("Client received ACK packet \n");
               }catch(Exception e){}
               
               fileSize = fileSize - 512;
            
            }
            
            
            else
            {
               //send file under 512 bytes or finish off sending
               Packet inPB = new Packet();
               inPB.dissect(inPacket);
            
               ++blockNum;
                  
               byte[] blockDat = new byte[fileSize];
               int dataSize = 0;        
            
               try 
               {
                  dis = new DataInputStream(new FileInputStream(file));    
                  dataSize = dis.read(blockDat, 0, blockDat.length);
               }
               catch (Exception e) {
                  log("" + e);
               }
                  
               log("Sending DATA packet \n");
                  
               //build the data packet
               Packet dataPB = new Packet(address, inPB.getPort(), blockNum, blockDat, dataSize, 3);
               
               DatagramPacket dataPkt = dataPB.build();
                  //send the packet 
               try{socket.send(dataPkt);}
               catch(Exception e){}
                  
               log("Client sending ... Opcode3 (DATA) Blk#(" + blockNum + ") \n" );
                  
            
                  //check for incoming ACK packet
               DatagramPacket inAckPacket = new DatagramPacket(new byte[1500],1500);
                  
                  //recieve incoming packet
               int avail;
               try{
                  log("Waiting to receive ACK confirmation \n");
                  socket.receive(inAckPacket);
                  inPB.dissect(inAckPacket);
                  log("Client received ACK packet \n");
                  avail = dis.available();
                  System.out.println("dis.available() = " + avail);
               }catch(Exception e){}
               
               log("SUCCESS: File " + fileName + " uploaded.");
               break;
            }
         }
      
      }
   } //end of UploadThread
   
   class Downloader extends Thread {
      // Attributes
      private DatagramSocket socket;
      private String fileName;
      private File file;
      InetAddress netAddress = null;
      int port = 69;
      private int blockNum = 0;
      private int size = 512;
      
      // Constructor
      public Downloader(String _fileName, File _file){
         fileName = _fileName;
         file = _file;
         try {
            socket = new DatagramSocket();
            InetAddress.getByName(tfIP.getText());
         }
         catch (Exception e) {
            log("Failed to start upload: " + e);
            System.exit(1);
         }
      }
      public void run() {
         log("Starting download " + fileName + " --> " + file.getName() +"\n");
      
         //Build and send RRQ packet 
         Packet rrqP = new Packet(address, port, fileName, RRQ);
         DatagramPacket rrq = rrqP.build();
         log("Client sending ... Opcode " + 1 + " (RRQ) " + " Filename " + "<" + fileName + "> Mode <octet> \n");
         try{socket.send(rrq);}
         catch(Exception e){}
         
         //launch fos to write to file
         try{fos = new FileOutputStream(file);}
         catch(Exception e){}
         
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
                        DatagramPacket ack = ackP.build();
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
                        DatagramPacket ack = ackP.build();
                        log("Client sending ... Opcode " + 4 + " (ACK) Blk " + inData.getBlockNum() + " \n");
                        try{socket.send(ack);}
                        catch(Exception e){}
                        
                        log("SUCCESS: File " + fileName + " downloaded.");
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
                  taLog.appendText(message);
                  System.out.print(message);
               }
            });
      
   }
   
   private byte[] getByteArray(File f)
   {
      byte[] bytes = null;
      try {bytes = Files.readAllBytes(f.toPath());}
      catch(Exception e){}
      
      return bytes;
   }
}