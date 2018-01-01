
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class KeepUpToDateParameters extends Thread{
	public static String pgm="KeepUpToDateParameters";
	String connectionUrl = GetSqlConnection.GetDomotiqueDB();
	String connectionUser = GetSqlConnection.GetUser();
	String connectionPassword = GetSqlConnection.GetPass();
	public Connection conn = null;
	public int STID;
	public KeepUpToDateParameters(int stid) {
		STID = stid;
	}

		Thread t;
	
	public void run() 
	{
		TraceLog log = new TraceLog();
		String message="";
		Thread.currentThread();

		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			try {
				conn = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
				message="start KeepUpToDateParameters v1.0 for station :"+STID;
				log.TraceLog(pgm,message);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				message="Database connexion problem !";
				log.TraceLog(pgm,message);
				e.printStackTrace();
			}
		}
	 catch (Exception e) {

		e.printStackTrace();
	} 		
		while(true)
		{

		try {
		    java.util.Date today = new java.util.Date();
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			String connectionUrl = GetSqlConnection.GetDomotiqueDB();
			String connectionUser = GetSqlConnection.GetUser();
			String connectionPassword = GetSqlConnection.GetPass();
			conn = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
			Statement stmt1 = null;
			Statement stmt2 = null;
			stmt1 = conn.createStatement();
			stmt2 = conn.createStatement();
			ResultSet rs1 = null;
			ResultSet rs2 = null;
			rs1 = stmt1.executeQuery("SELECT * FROM IndDesc WHERE ind_compute_rule = 'checkvalue' and st_id = "+STID+ " order by ind_id"); // 
			while (rs1.next()) {
				String IndIdS= rs1.getString("ind_id");
				int IndIdSNum= rs1.getInt("ind_id");
				String IndTargetS= rs1.getString("ind_target");
				int IndTarget= rs1.getInt("ind_target");
				int IndTargetRef= rs1.getInt("ind_target_ref");
				int IndRefType=IndTargetRef/1000;
				int IndRef=IndTargetRef-1000*IndRefType;
				rs2 = stmt2.executeQuery("SELECT * FROM IndValue WHERE st_id = "+STID+ " and ind_id = "+IndIdS+" order by ind_time desc limit 1"); // 
				while (rs2.next()) {
					int IndValue= rs2.getInt("ind_value");

					if (IndTargetS != null) // 
					{
	
						if (IndTarget != IndValue)
						{
	
						    message=today+" maj a faire station:"+STID+" indicateur "+IndIdS+ " valeur:"+IndValue +" target:"+IndTarget+" IndRef"+IndRef+" IndRefType"+IndRefType;	
							log.TraceLog(pgm,message);
					   	    InetAddress IPAddress = InetAddress.getByName(CommandServer.GetStationIPAddress(STID));
							SendFrame sendCmd = new SendFrame();
							byte[] data = new byte[6];
							if(CommandServer.paramNumber[IndRefType]==2)
							{
								data[0]=(byte)(IndRef);
								data[1]=(byte)(IndTarget);
							}
							if(CommandServer.paramNumber[IndRefType]==1)
							{
								data[0]=(byte)(IndTarget);
							}
							sendCmd.SendExtCommand(IPAddress, CommandServer.GetStationIPPort(STID),CommandServer.commandB[IndRefType],data);						
							Thread.sleep(10000);
						}
					}
				}
				 rs2.close();
		}
			rs1.close();
			conn.close();
			Thread.sleep(60000);
	}	
		catch (Exception e) {
			e.printStackTrace();
		} finally {


		}
		
	    }
	
	}
}
