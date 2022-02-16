/**
 * Packet - Builds/Dissects any of the 5 packets. This includes RRQ, WRQ, DATA, ACK, and ERROR
 * @author  I. Babani
 * @version 2205
 */
import java.net.*;
import java.io.*;

public class Packet implements TFTPConstants
{
   //ATTRIBUTES
   private InetAddress address;
   private int port;
   private String filename;
   private String mode;
   private int opcode;
   private int blockNum;
   private byte data[];
   private int errorCode;
   private String errorMessage;
   private int dataLen;
   
   
   //CONSTRUCTOR
   public Packet(InetAddress _toAddress, int _port, String _fileName, int _opcode) 
   //used for RRQ/WRQ
   {
      address = _toAddress;
      port = _port;
      filename = _fileName;
      opcode = _opcode;
   }
   
   public Packet(InetAddress _toAddress, int _port, int _blockNum, byte[] _data, int _dataLen, int _opcode) 
   //used for DATA
   {
      address = _toAddress;
      port = _port;
      blockNum = _blockNum;
      data = _data;
      dataLen = _dataLen;
      opcode = _opcode;
   }
   
   public Packet(InetAddress _toAddress, int _port, int _blockNum, int _opcode) 
   //used for ACK
   {
      address = _toAddress;
      port = _port;
      blockNum = _blockNum;
      opcode = _opcode;
   }
   
   public Packet(InetAddress _toAddress, int _port, int _errorCode, String _errorMessage, int _opcode) 
   //used for ERROR
   {
      address = _toAddress;
      port = _port;
      opcode = _opcode;
      errorMessage = _errorMessage;
      errorCode = _errorCode;
   }
   
   public Packet()
   //used for dissect()
   {
   
   }
   //ACCESSORS
   public InetAddress getAddress()
   {
      return address;
   }
   
   public String getFilename()
   {
      return filename;
   }
   
   public int getPort()
   {
      return port;
   }
   
   public String getMode()
   {
      return mode;
   }
   
   public int getOpcode()
   {
      return opcode;
   }
   
   public int getBlockNum()
   {
      return blockNum;
   }
   
   public byte[] getData()
   {
      return data;
   }
   
   public int getErrorCode()
   {
      return errorCode;
   }
   
   public String getErrorMessage()
   {
      return errorMessage;
   }
   
   public int getDataLen()
   {
      return dataLen;
   }
   
   
   //METHODS
   public DatagramPacket build()
   {
      DatagramPacket pkt = null;
      switch (opcode)
      {
         case RRQ:
            try{
               ByteArrayOutputStream baos = new ByteArrayOutputStream(
                  2 + filename.length() + 1 + "octet".length() + 1 );  
               DataOutputStream dos = new DataOutputStream(baos);    
            
               dos.writeShort(opcode);      
               dos.writeBytes(filename);   
               dos.writeByte(0);  
               dos.writeBytes("octet");      
               dos.writeByte(0);     
            
               dos.close(); 
            
            
               byte[] holder = baos.toByteArray();  
               pkt = new DatagramPacket(holder, holder.length, address, port);
            }
            catch (Exception e){}
            break;
         case WRQ:
            try
            {
               ByteArrayOutputStream baos = new ByteArrayOutputStream(2 + filename.length() + 1 + "octet".length() + 1);
               DataOutputStream dos = new DataOutputStream(baos);
               dos.writeShort(opcode);
               dos.writeBytes(filename);
               dos.writeByte(0);
               dos.writeBytes("octet");
               dos.writeByte(0);
               
               dos.close();
               byte[] holder = baos.toByteArray();
               pkt = new DatagramPacket(holder, holder.length, address, port);
            }
            catch(Exception e)
            {
               e.getStackTrace();
            }
         
            break;
         case DATA:
            try
            {
               ByteArrayOutputStream baos = new ByteArrayOutputStream(2 + 2 + data.length);  
               DataOutputStream dos = new DataOutputStream(baos);    
            
               dos.writeShort(opcode); 
               dos.writeShort(blockNum);  
               dos.write(data, 0 , data.length);  
               
               dos.close();
               byte[] holder = baos.toByteArray();
               pkt = new DatagramPacket(holder, holder.length, address, port); 
            }
            catch(Exception e)
            {
               e.getStackTrace();
            }  
         
            break;
         case ACK:
            try
            {
               ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
               DataOutputStream dos = new DataOutputStream(baos);
               dos.writeShort(opcode);
               dos.writeShort(blockNum);
               
               dos.close();
               byte[] holder = baos.toByteArray();
               pkt = new DatagramPacket(holder, holder.length, address, port); 
            }
            catch(Exception e)
            {
               e.getStackTrace();
            }
            
            break;
         case ERROR: //ERROR
            try
            {
               ByteArrayOutputStream baos = new ByteArrayOutputStream(2 + 2 + errorMessage.length() + 1);
               DataOutputStream dos = new DataOutputStream(baos);
               dos.writeShort(opcode);
               dos.writeShort(errorCode);
               dos.writeUTF(errorMessage);
               dos.writeByte(0);
               
               dos.close();
               byte[] holder = baos.toByteArray();
               pkt = new DatagramPacket(holder, holder.length, address, port); 
            }
            catch(Exception e)
            {
               e.getStackTrace();
            }
         
            break;
      }   
      
      return pkt;
   }
   
   public void dissect(DatagramPacket pkt)
   {
      address = pkt.getAddress();
      port = pkt.getPort();
      
      ByteArrayInputStream bais = new ByteArrayInputStream(pkt.getData(), pkt.getOffset(), pkt.getLength());
      DataInputStream dis = new DataInputStream(bais);
      try{
         opcode = dis.readShort();
      
         switch(opcode){
            case RRQ:
               filename = readToZ(dis);
               mode = readToZ(dis);
            
               try{
                  dis.close();
               }catch(Exception e){}
            
               break;
            case WRQ:
               filename = readToZ(dis);
               mode = readToZ(dis);
            
               try{
                  dis.close();
               }catch(Exception e){}
            
               break;
            case DATA:
               blockNum = dis.readShort();
               ByteArrayOutputStream baos = new ByteArrayOutputStream();
               int dataLen = 0;
               while (true) 
               {
                  byte holder = 0;
                  try{holder = dis.readByte();}
                  catch(Exception e){}
                  if (holder == 0)
                  {
                     break;
                  }
                  else
                  {
                     baos.write(holder);
                     dataLen++;
                  } 
               }
               
               data = new byte[dataLen];
               data = baos.toByteArray();
               
               try{
                  dis.close();
               }catch(Exception e){}
            
               break;
            case ACK:
               blockNum = dis.readShort();  
            
               try{
                  dis.close();
               }catch(Exception e){}
            
               break;
            case ERROR:
               errorCode = dis.readShort();  
               errorMessage = dis.readLine();
            
               try{
                  dis.close();
               }catch(Exception e){}
            
               break;
            default:
               filename = "";
               mode = "";
               blockNum = -1;
               data = null;
               errorCode = -1;
               errorMessage = "";
            
               try{
                  dis.close();
               }catch(Exception e){}
            
               return;
         }//end of switch
           
      }catch(Exception e){}
   }
   
      // Utility method
   public String readToZ(DataInputStream dis) 
   {
      String value = "";
      byte b = 0;
      while (true) 
      {
         try{b = dis.readByte();}
         catch(Exception e){}
         if (b == 0) {
            return value;}
         value += (char) b;
      }
   
   }
}
