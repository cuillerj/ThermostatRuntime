import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*
 * this program convert text formated command to binary command 
 * listen for input UDP
 * input format: stationId command parameter(s)
 * stationId: text formated number 
 * command: text that must match with one of the below defined commandS - command is converted with commndB array
 * parameter(s):1 or 2 parameters expected according to below defined paramNumber 
 * 	if 2 parameters equalChar is used to separate first and second one (parameter values are text formated number
 *  parameters value are binary converted 1 or 2 bytes according to paramLen (in case of 2 parameters the first one is converted 1 byte (must not exceeded 255) and second to paramLen-1 bytes
 */
public class CommandServer extends Thread{
	public static String pgm="CommandServer";
	final byte byte0=0x01;
	final byte byte1=ThermostatDispatcher.padByte;
	final byte byte2=0x00;
	final byte byte3=ThermostatDispatcher.padByte;	
	final byte separator=0x2f;
	final byte equalChar=0x3a;
	final byte commandNumber=13;
	final String[] commandS = new String[commandNumber];
	final static byte[] commandB={ThermostatDispatcher.setModeRequest,ThermostatDispatcher.setInstructionRequest,ThermostatDispatcher.setSecurityRequest,
			ThermostatDispatcher.updateTemperatureRequest,ThermostatDispatcher.updateRegisterRequest,ThermostatDispatcher.updateSchedulRequest,
			ThermostatDispatcher.writeEepromRequest,ThermostatDispatcher.setTemporarilyHoldRequest,ThermostatDispatcher.uploadScheduleRequest,
			ThermostatDispatcher.uploadTemperatures,ThermostatDispatcher.uploadRegisters,ThermostatDispatcher.tracePIDRequest,ThermostatDispatcher.setInstruction};
	final static int[] paramNumber={1,1,1,2,2,2,1,1,0,0,0,1,1};  
	final int[] paramLen={1,1,1,2,2,2,1,1,0,0,0,1,1};

public void run(){
	commandS[0]="setMode";
	commandS[1]="setTemp";
	commandS[2]="setSecurity";
	commandS[3]="updateTemperature";
	commandS[4]="updateRegister";
	commandS[5]="updateSchedul";
	commandS[6]="writeEeprom";
	commandS[7]="temporarilyHold";
	commandS[8]="uploadSchedule";
	commandS[9]="uploadTemperatures";
	commandS[10]="uploadRegisters";
	commandS[11]="tracePID";
	commandS[12]="setInstruction";
	String message="";
	message="CommandServer V1.0";
	TraceLog log = new TraceLog();
	log.TraceLog(pgm,message);
//	System.out.println("CommandServer V1.0");
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(ThermostatDispatcher.commandListenIPPort);
			message="Listen port:"+ThermostatDispatcher.commandListenIPPort;
			log.TraceLog(pgm,message);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	while(true)
	{
	try{
		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		try {
			serverSocket.receive(receivePacket);
			int i=0;
			/*
			for (i=0;i<receiveData.length;i++)
			{
				System.out.print(byteToHex(receiveData[i])+" ");
			}
			System.out.println();
			System.out.println("check:"+CheckInputFrame(receiveData));
			*/
			byte[] inData = new byte[512];
			int startIdx=6;
			i=0;
			if(CheckInputFrame(receiveData))
			{
				boolean stationFound=false;
				// extract station id
				while(receiveData[i+startIdx]!=separator && i< receiveData.length)
				{
					inData[i]=(byte) (receiveData[i+startIdx]-0x30);
					i++;
				}
				int stLen=i;
				int st_id=0;
				if(stLen!=0)
				{
					stationFound=true;
					int j=0;
					for (j=0;j<stLen;j++)
					{
						st_id=st_id+(int) (inData[j]*Math.pow(10, stLen-j-1));
					}				
				}
				if (stationFound)
				{
					startIdx=6+stLen+1;
					i=0;
					// extract command
					while(receiveData[i+startIdx]!=separator && i< receiveData.length)
					{
						inData[i]=(byte) (receiveData[i+startIdx]);
	//					System.out.print(" "+i+">"+byteToHex(inData[i]));
						i++;
					}
	//				System.out.println(i);
					int cmdLen=i;
					byte[] cmd = new byte[cmdLen];
					System.arraycopy(inData, 0, cmd, 0, cmdLen);
					String command = new String(cmd, "UTF-8"); // for UTF-8 encoding
					// extract data
					startIdx=6+stLen+cmdLen+2;
					i=0;
					while(receiveData[i+startIdx]!=separator && i< receiveData.length)
					{
						inData[i]=(byte) (receiveData[i+startIdx]);
//						System.out.print(" "+i+">"+byteToHex(inData[i]));
						i++;
					}
//					System.out.println(i);
					int dataLen=i;
					byte[] data = new byte[dataLen];
					System.arraycopy(inData, 0, data, 0, dataLen);
					String datas = new String(data, "UTF-8"); // for UTF-8 encoding
					message="dest station:"+st_id+" "+GetStationIPAddress(st_id)+"/"+GetStationIPPort(st_id);
					log.TraceLog(pgm,message);
					message="cmd:"+command+" len:"+cmd.length+ " data:"+datas+" datalen"+data.length;
					log.TraceLog(pgm,message);
					int j=0;

					// decode command
					byte cmdB=(byte) 0xff;
					int cmdIdx=-1;
					for (i=0;i<commandNumber;i++)
					{
						if(command.compareTo(commandS[i])==0)
							{
							cmdB=commandB[i];
							cmdIdx=i;
							}
					}
					if (cmdIdx!=-1)    // if command is defined
					{
						if(paramNumber[cmdIdx]==0) // if only one parameter
						{	
								byte[] dataB = new byte[6];					
						   	    InetAddress IPAddress = InetAddress.getByName(GetStationIPAddress(st_id));
								SendFrame sendCmd = new SendFrame();
								sendCmd.SendExtCommand(IPAddress, GetStationIPPort(st_id),cmdB,dataB);								
						}
						if(paramNumber[cmdIdx]==1) // if only one parameter
						{	
							int value=0;
							for (j=0;j<dataLen;j++)
							{
								value=value+(int) ((data[j]-0x30)*Math.pow(10, dataLen-j-1));
							}

								byte[] dataB = new byte[paramLen[cmdIdx]];					
								if(paramLen[cmdIdx]==1)    // if value to code with one byte
								{
									dataB[0]=(byte) (value);
								}
								if(paramLen[cmdIdx]==2) // if value to code with two byte
								{
									dataB[0]=(byte) ((byte) (value)/256);
									dataB[1]=(byte) (value);
								}
		
						   	    InetAddress IPAddress = InetAddress.getByName(GetStationIPAddress(st_id));
								SendFrame sendCmd = new SendFrame();
								sendCmd.SendExtCommand(IPAddress, GetStationIPPort(st_id),cmdB,dataB);								
						}
						if(paramNumber[cmdIdx]==2) // if two parameters
						{	
							byte[] dataB = new byte[paramLen[cmdIdx]];			
							byte[] param1B= new byte[data.length];
							byte[] param2B= new byte[data.length];
							i=0;
							while(data[i]!=equalChar && i< data.length-1)
							{
								param1B[i]=(byte) (data[i]-0x30);
								System.out.print(" p1 i:"+i+" 0x"+byteToHex(param1B[i]));
								i++;
							}
							System.out.println();
							int param1Len=i;
							System.out.println("param1Len:"+param1Len);
							i=param1Len+1;
							while(i< data.length)
							{
								param2B[i-(param1Len+1)]=(byte) (data[i]-0x30);
								System.out.print(" p2 i:"+i+" 0x"+byteToHex(param2B[i-(param1Len+1)]));
								i++;
							}
							System.out.println();
							int param2Len=i-param1Len-1;
							System.out.println("param2Len:"+param2Len);
							int value1=0;
							int value2=0;
							for (j=0;j<param1Len;j++)
							{
								value1=value1+(int) ((param1B[j])*Math.pow(10, param1Len-j-1));
							}
							System.out.println("value1:"+value1);
							for (j=0;j<param2Len;j++)
							{
								value2=value2+(int) ((param2B[j])*Math.pow(10, param2Len-j-1));
							}
							System.out.println("value2:"+value2);
							if(paramLen[cmdIdx]==2)    // if value to code with one byte
							{
								dataB[0]=(byte) (value1);
								dataB[1]=(byte) (value2);
							}
							if(paramLen[cmdIdx]==3) // if value to code with two byte
							{
								dataB[0]=(byte) (value1);
								dataB[1]=(byte) ((byte) (value2)/256);
								dataB[2]=(byte) (value2);	
							}
					   	    InetAddress IPAddress = InetAddress.getByName(GetStationIPAddress(st_id));
							SendFrame sendCmd = new SendFrame();
							sendCmd.SendExtCommand(IPAddress, GetStationIPPort(st_id),cmdB,dataB);	
						}
					}
					else{
						message=" unknown command";
						log.TraceLog(pgm,message);

					}
				
				}				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	finally{}}
}

public boolean CheckInputFrame(byte[]data)
{	
	
	if (data[0]!=byte0)
	{
		return false;
	}
	if (data[1]!=byte1)
	{
		return false;
	}
	if (data[2]!=byte2)
	{
		return false;
	}
	if (data[3]!=byte3)
	{
		return false;
	}
	if (data[4]!=byte2)
	{
		return false;
	}
	return true;
	
}
public static String GetStationIPAddress(int st_ID)
{
	Statement stmtS = null;
	ResultSet rs = null;
	Connection conn =null;
	String st_ip="";
try {
	Class.forName("com.mysql.jdbc.Driver").newInstance();
	String connectionUrl = "jdbc:mysql://jserver:3306/domotiquedata";
	String connectionUser = "jean";
	String connectionPassword = "manu7890";

	conn = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
	stmtS = conn.createStatement();
	rs = stmtS.executeQuery("SELECT * FROM stations WHERE st_id = "+st_ID+" limit 1"); // 
	while (rs.next()) {
		st_ip= rs.getString("st_ip");
	//	System.out.print("IP: " + st_ip);
	}
	rs.close();

	} catch (Exception e) {
		e.printStackTrace();
	} finally {
		try { if (stmtS != null) stmtS.close(); } catch (SQLException e) { e.printStackTrace(); }
		try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
	}
return st_ip;
}

public static int GetStationIPPort(int st_ID)
{
	Statement stmtS = null;
	ResultSet rs = null;
	Connection conn =null;
	int udpPort=0;
try {
	Class.forName("com.mysql.jdbc.Driver").newInstance();
	String connectionUrl = "jdbc:mysql://jserver:3306/domotiquedata";
	String connectionUser = "jean";
	String connectionPassword = "manu7890";


	int crc=0;
	conn = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
	stmtS = conn.createStatement();
	rs = stmtS.executeQuery("SELECT * FROM stations WHERE st_id = "+st_ID+" limit 1"); // 
	while (rs.next()) {
		udpPort = rs.getInt("st_listen_udp");
//		System.out.print("IP port:"+udpPort);
	}
	rs.close();

	} catch (Exception e) {
		e.printStackTrace();
	} finally {
		try { if (stmtS != null) stmtS.close(); } catch (SQLException e) { e.printStackTrace(); }
		try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
	}
return udpPort;
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
}
