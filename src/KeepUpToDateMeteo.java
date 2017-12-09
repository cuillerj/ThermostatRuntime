/*
 * a ajouter un test fraicheur des data via recid
 */
 
	import java.io.*; 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.net.*;
import java.sql.*;

public class KeepUpToDateMeteo extends Thread{
		public static String pgm="KeepUpToDateMeteo";
		public int meteoId;
		public KeepUpToDateMeteo(int meteoID) {
	       meteoId = meteoID;
	    }
		Thread t;
	
	public void run() 
	{
		TraceLog log = new TraceLog();
		String message="KeepUpToDateMeteo V1.0";
		log.TraceLog(pgm,message);
		
		while(true)
		{

	Thread.currentThread();


			Connection conn1 = null;
			Statement stmt1 = null;
			ResultSet rs1 = null;

		      //InetAddress IPAddress = InetAddress.getByName("localhost");
			try {
//				System.out.print("try2: ");
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				String connectionUrl = "jdbc:mysql://jserver:3306/meteo";
				String connectionUser = "jean";
				String connectionPassword = "manu7890";
				conn1 = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
				stmt1 = conn1.createStatement();
				boolean recFound=false;
			rs1 = stmt1.executeQuery("SELECT * FROM hist_lha WHERE st_id ="+meteoId+" ORDER by rec_id DESC limit 1"); // modif le 10/09/14 pour integration seb
			while (rs1.next()) {
				ThermostatDispatcher.meteoValue[meteoId]=rs1.getInt("temp");
				String tempSign=rs1.getString("tempSign");
				recFound=true;
				message="Meteo Station: "+ meteoId+":"+ThermostatDispatcher.meteoValue[meteoId];
				log.TraceLog(pgm,message);
				ThermostatDispatcher.meteoFlag[meteoId]=true;
					}
			if (!recFound)
			{
				ThermostatDispatcher.meteoValue[meteoId]=ThermostatDispatcher.nonDefinedExtTemp;
			}
			Thread.sleep(300*1000);
		
	}
			catch (Exception e) {
				e.printStackTrace();
			} finally {
				try { if (rs1 != null) rs1.close(); } catch (SQLException e) { e.printStackTrace(); }
				try { if (stmt1 != null) stmt1.close(); } catch (SQLException e) { e.printStackTrace(); }
				try { if (conn1 != null) conn1.close(); } catch (SQLException e) { e.printStackTrace(); }
			}
		
	}

	}
	}


