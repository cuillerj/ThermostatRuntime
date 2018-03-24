import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DateFormat;
import java.util.Locale;

public class SendFrame extends Thread{
	public static String pgm="SendFrame";
	Thread t;
	public  void SendTime (InetAddress IPAddress, int IPport) 
	{

		TraceLog log = new TraceLog();
		String message=" send time to" + IPAddress;
		log.TraceLog(pgm,message);
		Locale locale=Locale.FRENCH;
		java.util.Date today = new java.util.Date();
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
		String DateChar=dateFormat.format(today);
		String DatC1="";
		String DatC2="";
		DatC1=DateChar.substring(0, 1);
		int DatI1= Integer.parseInt(DatC1);
		DatC2=DateChar.substring(1, 2);
		int DatI2= Integer.parseInt(DatC2);
		int DatJ=DatI1*10+DatI2;
		DatC1=DateChar.substring(3, 4);
		DatI1= Integer.parseInt(DatC1);
		DatC2=DateChar.substring(4, 5);
		DatI2= Integer.parseInt(DatC2);
		int DatM=DatI1*10+DatI2;
		DatC1=DateChar.substring(6, 7);
		DatI1= Integer.parseInt(DatC1);
		DatC2=DateChar.substring(7, 8);
		DatI2= Integer.parseInt(DatC2);
		int DatA=DatI1*10+DatI2;
		DateFormat fullDf = DateFormat.getTimeInstance(DateFormat.LONG,locale);
		String TimeChar=fullDf.format(today);
		String TimC1="";
		String TimC2="";
		TimC1=TimeChar.substring(0, 1);
		int TimI1= Integer.parseInt(TimC1);
		TimC2=TimeChar.substring(1, 2);
		int TimI2= Integer.parseInt(TimC2);
		int TimH=TimI1*10+TimI2;
		TimC1=TimeChar.substring(3, 4);
		TimI1= Integer.parseInt(TimC1);
		TimC2=TimeChar.substring(4, 5);
		TimI2= Integer.parseInt(TimC2);
		int TimM=TimI1*10+TimI2;
		TimC1=TimeChar.substring(6, 7);
		TimI1= Integer.parseInt(TimC1);
		TimC2=TimeChar.substring(7, 8);
		TimI2= Integer.parseInt(TimC2);
		int TimS=TimI1*10+TimI2;
		DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();
		} catch (SocketException e1) {
		// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int timeLen=8;
		byte[] TimeToSend = new byte[timeLen];
		TimeToSend[0]=(byte)DatJ;
		TimeToSend[1]=(byte)DatM;
		TimeToSend[2]=0x00;				
		TimeToSend[3]=(byte)DatA;
		TimeToSend[4]=(byte)TimH;
		TimeToSend[5]=0x00;	
		TimeToSend[6]=(byte)TimM;
		TimeToSend[7]=(byte)TimS;
		ThermostatDispatcher.FrameOut newFrame = new ThermostatDispatcher.FrameOut();
		byte[] dataToSend =  newFrame.BuildFrameOut(ThermostatDispatcher.response,ThermostatDispatcher.noAck, ThermostatDispatcher.respTime,TimeToSend, timeLen) ;
		DatagramPacket sendPacket2 = new DatagramPacket(dataToSend, newFrame.FrameOutLen(), IPAddress, IPport);
		try {
			clientSocket.send(sendPacket2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clientSocket.close();
	}
	public  void SendExtTemp (InetAddress IPAddress, int IPport,int meteoId) 
	{
		TraceLog log = new TraceLog();

		DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();
		} catch (SocketException e1) {
		// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int tempLen=6;
		byte[] tempToSend = new byte[tempLen];
		if(ThermostatDispatcher.meteoValue[meteoId]>=0){
			tempToSend[0]=0x2b;		
		}
		else{
			tempToSend[0]=0x2d;					
		}
		tempToSend[1]=(byte)Math.abs(ThermostatDispatcher.meteoValue[meteoId]);
		String message=" send ext temp to" + IPAddress+ " "+tempToSend[0]+ " "+ThermostatDispatcher.meteoValue[meteoId];
		log.TraceLog(pgm,message);
		ThermostatDispatcher.FrameOut newFrame = new ThermostatDispatcher.FrameOut();
		byte[] dataToSend =  newFrame.BuildFrameOut(ThermostatDispatcher.response,ThermostatDispatcher.noAck, ThermostatDispatcher.respExtTemp,tempToSend, tempLen) ;
		DatagramPacket sendPacket2 = new DatagramPacket(dataToSend, newFrame.FrameOutLen(), IPAddress, IPport);
		try {
			clientSocket.send(sendPacket2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clientSocket.close();
	}
	public  void SendExtCommand (InetAddress IPAddress, int IPport,byte command,byte data[]) 
	{

		TraceLog log = new TraceLog();
		String message=" send command to" + IPAddress+ " len:"+data.length;
		log.TraceLog(pgm,message);
		DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();
		} catch (SocketException e1) {
		// TODO Auto-generated catch block

			message=" socket exception";
			log.TraceLog(pgm,message);
			e1.printStackTrace();
		}
		int dLen=data.length;

		ThermostatDispatcher.FrameOut newFrame = new ThermostatDispatcher.FrameOut();
		byte[] dataToSend =  newFrame.BuildFrameOut(ThermostatDispatcher.request,ThermostatDispatcher.toAck, command,data, dLen) ;
		DatagramPacket sendPacket2 = new DatagramPacket(dataToSend, newFrame.FrameOutLen(), IPAddress, IPport);
		try {
			clientSocket.send(sendPacket2);
		} catch (IOException e) {
			e.printStackTrace();
			message=" packet exception";
			log.TraceLog(pgm,message);
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clientSocket.close();
	}
	public static void AcknoledgeFrame(InetAddress IPAddress,int IPport, byte frameNumber, byte command)
	{
		ThermostatDispatcher.FrameOutAck newFrame = new ThermostatDispatcher.FrameOutAck();
		  byte[] dataToSend =  newFrame.BuildFrameOutAck(frameNumber, command) ;
	      DatagramSocket clientSocket = null;
			try {
				clientSocket = new DatagramSocket();
			} catch (SocketException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		      DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, IPAddress, IPport);
		      try {
				clientSocket.send(sendPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
				e.printStackTrace();
				}
		      clientSocket.close();
	}

}
