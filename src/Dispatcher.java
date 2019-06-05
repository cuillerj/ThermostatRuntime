import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DateFormat;
import java.util.Locale;


final class Dispatcher {
// boolean InitMeteo=false;
	public static byte ackByte=0x3f;
	public static byte padByte=0x3b;
	public static boolean request=true;
	public static boolean response=false;
	public static boolean toAck=true;
	public static boolean noAck=false;
	public static String pgm="Dispatcher";
	/*
	 * output
	 */
	public static final byte respTime=0x01;
	public static final byte respExtTemp=0x02;

	public static final byte statusRequest=0x01;
	public static final byte setModeRequest = 0x02;
	/*
	 * input
	 */
	public static final byte timeUpdateRequest=respTime;
	public static final byte statusResponse =statusRequest|0x10;
	public static final byte ring= 0x13;
	public static final byte open =0x14;

	public static int[] meteoValue=new int[255]; 
	public static final int maximumNumberOfGroup=255;
	public static boolean[] meteoFlag=new boolean[maximumNumberOfGroup];
	public static final int maximumNumberOfStation=9999;
	public static boolean[] upToDateFlag=new boolean[9999]; 
	public static int nonDefinedExtTemp = -9999;
	public static int firstScheduleIndicatorPosition=50;
	static int currentSentFrameNumber=0;
	static int inHeaderLen=6;
	static int outHeaderLen=8;
	public static int commandListenIPPort = 0;
	public static void main(String args[]) throws Exception
	{  
		String message="";
		TraceLog log = new TraceLog();
		message="DoorSysteme V1.1";
		log.TraceLog(pgm,message);
		boolean running=true;
		int listenIPPort = 0;

		/*
		 * frame structure
		 */
		if (args.length > 0) {
		    try {
		    	listenIPPort = Integer.parseInt(args[0]);
		    	commandListenIPPort = Integer.parseInt(args[1]);
				message="Listen port:"+listenIPPort;
				log.TraceLog(pgm,message);
		    } catch (NumberFormatException e) {
		        System.err.println("listenIPPort" + args[0] + " must be an integer.");
		        System.err.println("commandListenIPPort" + args[1] + " must be an integer.");
		        System.exit(1);
		    }
		}
		int i=0;

		DatagramSocket serverSocket = new DatagramSocket(listenIPPort); 
		UpdateDatabase database = new UpdateDatabase();
		database.start();
		CommandServer commandServer = new CommandServer();
	//	MonitorWaterTemperatures monitorWaterTemp = new MonitorWaterTemperatures();
		commandServer.start();
//		monitorWaterTemp.start();
		while(running)
		{
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			InetAddress IPSource = receivePacket.getAddress();
			int IPport = receivePacket.getPort();

			Dispatcher.FrameIn newFrame = new FrameIn(receiveData);
			message="receive from:"+IPSource+":"+IPport+" toAck:"+newFrame.toAcknoledge();
			log.TraceLog(pgm,message);
			if (newFrame.isGoodFrame){	
				if  (newFrame.toAcknoledge()){
					SendFrame.AcknoledgeFrame(IPSource,IPport,newFrame.frameNumber(),newFrame.command());
				}
				byte command=newFrame.command();
				if(newFrame.request()){						
					switch (command)
					{
						case timeUpdateRequest:  // request time
						{
							SendFrame send = new SendFrame();
							send.SendTime(IPSource,IPport);
							break;
						}
					}
				}
				else {
					switch (command)
					{
	
						case statusResponse:  // 
						{
							database.InsertIndicators(newFrame.stationId(),newFrame.command(),newFrame.data());
							break;
						}
						case ring:  // 
						{
							database.InsertIndicators(newFrame.stationId(),newFrame.command(),newFrame.data());
							break;
						}
						case open:  // 
						{
							database.InsertIndicators(newFrame.stationId(),newFrame.command(),newFrame.data());
							break;
						}
					}
				}
			}
			else{
				message=" CRC error ";
				log.TraceLog(pgm,message);

			}
		}
		serverSocket.close();
	}
	
	static class FrameIn{

		byte frameFlag;
		byte requestResponse;
		byte frameLen;
		byte dataLen;
		byte unitGroup;
		byte unitId;
		byte command;
		byte CRC8;
		boolean isGoodFrame;
		byte[] data =new byte[1014];

		public FrameIn(byte[] receivedData){
			this.frameFlag=receivedData[0];
			this.requestResponse=receivedData[2];
			this.frameLen=receivedData[4];
			this.dataLen=(byte) (frameLen-2);
			this.unitGroup=receivedData[6];
			this.unitId=receivedData[7];
			this.command=receivedData[9];
			this.isGoodFrame=true;
			this.CRC8=(byte) (receivedData[receivedData[4]+inHeaderLen-1]);		
			System.arraycopy(receivedData,inHeaderLen, this.data, 0, frameLen);
			if (CRC8!=Crc8(data,dataLen))
			{
				this.isGoodFrame=false;
			}
		}
		public byte frameNumber() {return frameFlag;}
		public byte requestResponse() {return requestResponse;}
		public byte frameLen() {return frameLen;}
		public byte dataLen() {return dataLen;}
		public byte unitGroup() {return unitGroup;}
		public byte unitId() {return unitId;}
		public byte command() {return command;}
		public byte[] data() {return data;}
		public boolean toAcknoledge() {return (requestResponse&0x40)==0x40;}
		public boolean request() {return (requestResponse&0x80)==0x80;}
		public boolean response() {return (requestResponse&0x80)==0x00;}
		public int stationId() {return (unitGroup*256+unitId);}
	}
	static class FrameOutAck{
		byte frameFlag;
		byte requestResponse;
		byte frameLen;
		byte command;
		byte[] data =new byte[15];

		public byte[] BuildFrameOutAck(byte frameNumber, byte command){
				data[0]=frameNumber;
				data[1]=padByte;
				data[2]=ackByte;
				data[3]=padByte;
				data[4]=0x0f; // frame len
				data[5]=padByte;
				data[9]=(byte) (command);
				return this.data;
		}
	}
	static class FrameOut{

		int frameLen;
		byte[] outFrame =new byte[1024];
		byte[] outData =new byte[1024];
		public FrameOut(){
	}
		public byte[] BuildFrameOut(boolean requestResponse,boolean toBeAck, byte command, byte inputdata[], int dataLen){
				currentSentFrameNumber++;
				System.arraycopy(inputdata, 0, outFrame, outHeaderLen, dataLen);
				outFrame[0]=(byte) ((currentSentFrameNumber)%256);
				outFrame[1]=padByte;
				if (requestResponse && toBeAck)
				{
					outFrame[2]=(byte) 0xc0;
				}
				else if (requestResponse)
				{
					outFrame[2]=(byte) 0x80;				
				}
				else if (toBeAck)
				{
					outFrame[2]=(byte) 0x40;				
				}
				else
				{
					outFrame[2]=(byte) 0x00;				
				}
				outFrame[3]=padByte;
				outFrame[4]=(byte)(dataLen+10); // frame len
				outFrame[5]=padByte;
				outFrame[7]=(byte) (command);
				this.frameLen=dataLen+10;  // reserved for 0x00 +crc
				System.arraycopy(outFrame, outHeaderLen-1, this.outData, 0, dataLen+1);
				outFrame[frameLen-1]=Crc8(outData,dataLen+1);
				return this.outFrame;
		}
		public int FrameOutLen()
			{
				return this.frameLen;
			}
	}
	public static String byteToHex(byte b) {
		    StringBuilder sb = new StringBuilder();
		    sb.append(Integer.toHexString(b));
		    if (sb.length() < 2) {
		        sb.insert(0, '0'); // pad with leading zero if needed
		    }
		    String hex = sb.toString();
		    return hex;
		  }

	public class SendTime extends Thread{
		Thread t;

	}
	protected static byte Crc8(byte[] stringData,int len) {
	    int i = 0;
	    byte crc = 0x00;
	    while (len-- > 0) {
	        byte extract = (byte) stringData[i++];	        
	        for (byte tempI = 8; tempI != 0; tempI--) {
	            byte sum = (byte) ((crc & 0xFF) ^ (extract & 0xFF));
	            sum = (byte) ((sum & 0xFF) & 0x01); // I had Problems writing this as one line with previous
	            crc = (byte) ((crc & 0xFF) >>> 1);
	            if (sum != 0) {
	                crc = (byte)((crc & 0xFF) ^ 0x8C);
	            }
	            extract = (byte) ((extract & 0xFF) >>> 1);
	        }
	    }
	    return (byte) (crc & 0xFF);
	}

}