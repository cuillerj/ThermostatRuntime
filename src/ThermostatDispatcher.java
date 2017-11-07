import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DateFormat;
import java.util.Locale;


final class ThermostatDispatcher {
	static boolean InitMeteo=false;
	public static byte ackByte=0x3f;
	public static byte padByte=0x3b;
	public static boolean request=true;
	public static boolean response=false;
	public static boolean toAck=true;
	public static boolean noAck=false;
	public static byte respTime=0x01;
	public static byte respExtTemp=0x02;
	public static int[] meteoValue=new int[255]; 
	public static boolean[] meteoFlag=new boolean[255]; 
	public static int nonDefinedExtTemp = -9999;
	static int currentSentFrameNumber=0;
	static int inHeaderLen=6;
	static int outHeaderLen=8;
	public static void main(String args[]) throws Exception
	{  
		System.out.println("ThermostatDispatcher 1.0");
		boolean running=true;
		int listenIPPort = 0;

		/*
		 * frame structure
		 */
		if (args.length > 0) {
		    try {
		    	listenIPPort = Integer.parseInt(args[0]);
		    } catch (NumberFormatException e) {
		        System.err.println("listenIPPort" + args[0] + " must be an integer.");
		        System.exit(1);
		    }
		}
		int i=0;
		for (i=0;i<255;i++)
		{
			meteoFlag[i]=false;
		}
		DatagramSocket serverSocket = new DatagramSocket(listenIPPort); 
//		KeepUpToDateMeteo meteo = new KeepUpToDateMeteo(5);
//		meteo.start();
		while(running)
		{
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			InetAddress IPSource = receivePacket.getAddress();
			int IPport = receivePacket.getPort();
			System.out.println("receive from:"+IPSource+":"+IPport);
			ThermostatDispatcher.FrameIn newFrame = new FrameIn(receiveData);
			System.out.println(newFrame.toAcknoledge());
			if (newFrame.isGoodFrame){	
				if  (newFrame.toAcknoledge()){
					SendFrame.AcknoledgeFrame(IPSource,IPport,newFrame.frameNumber(),newFrame.command());
				}				
				for (i=0;i<newFrame.frameLen;i++)
					{
					System.out.print("0x"+byteToHex(newFrame.data[i])+"-");
					}
				System.out.println();
				System.out.println(" group " + newFrame.unitGroup()+" Id "+newFrame.unitId() +" data0:"+newFrame.data[0]);
				if(!meteoFlag[newFrame.unitGroup()])
				{
					meteoFlag[newFrame.unitGroup()]=true;
					KeepUpToDateMeteo meteo = new KeepUpToDateMeteo(newFrame.unitGroup());
					meteo.start();
				}
				byte command=newFrame.command();
				System.out.println(" cmd:" + command);			
				switch (command)
				{
					case 0x01:  // request time
					{
						SendFrame send = new SendFrame();
						send.SendTime(IPSource,IPport);
						break;
					}
					case 0x02:  // request ext temp
					{
						if(meteoFlag[newFrame.unitGroup()] && meteoValue[newFrame.unitGroup()]!=nonDefinedExtTemp)
						{
							SendFrame send = new SendFrame();
							send.SendExtTemp(IPSource,IPport,newFrame.unitGroup());							
						}

						break;
					}
				}
			}
			else{
				System.out.println(" CRC error ");
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
		public byte command() {return (byte) (command&0xcf);}
		public byte[] data() {return data;}
		public boolean toAcknoledge() {return (requestResponse&0x40)==0x40;}
		public boolean request() {return (requestResponse&0x80)==0x80;}
		public boolean response() {return (requestResponse&0x80)==0x00;}
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
				System.out.println("Crc out:0x" +byteToHex(Crc8(outData,dataLen+1))+" datalen:"+dataLen);
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
	        System.out.print("0x"+byteToHex(extract)+"-");
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
	    System.out.println();
	    return (byte) (crc & 0xFF);
	}

}