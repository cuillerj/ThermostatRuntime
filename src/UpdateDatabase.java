
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UpdateDatabase extends Thread{
	public static String pgm="UpdateDatabase";
	String connectionUrl = GetSqlConnection.GetDomotiqueDB();
	String connectionUser = GetSqlConnection.GetUser();
	String connectionPassword = GetSqlConnection.GetPass();
	public static Connection conn = null;
	public UpdateDatabase() {
	String message="";
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			try {
				conn = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				TraceLog log = new TraceLog();
				message="Database connexion problem !";
				log.TraceLog(pgm,message);
				e.printStackTrace();
			}
		}
	 catch (Exception e) {

		e.printStackTrace();
	} 
	    }
	Thread t;

	public void run() 
	{
		TraceLog log = new TraceLog();
		String message="";
		message="UpdateDatabase V1.1";
		log.TraceLog(pgm,message);
		while(true)
		{
			Thread.currentThread();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public  void InsertIndicators (int stationId, int indType, byte[] data) 
	{	
		int IndValue=0;
		String rule1="256*a+b";
		TraceLog log = new TraceLog();
		try {
			Statement stmtRead = conn.createStatement();
			Statement stmtInsert = conn.createStatement();
			ResultSet rs = stmtRead.executeQuery("SELECT * FROM IndDesc WHERE st_id ="+stationId+ " AND Ind_majtype =" +indType);
			int intIdI=0;
			while (rs.next()) {
				String IndIdS= rs.getString("ind_id");
				intIdI=Integer.parseInt(IndIdS);
				String IndPos= rs.getString("ind_pos");
				int i2=Integer.parseInt(IndPos);
				String IndLen = rs.getString("ind_len");
				int i3=Integer.parseInt(IndLen);
				String IndType = rs.getString("ind_type");
				int i4=Integer.parseInt(IndLen);
				String IndComputeRule = rs.getString("ind_compute_rule");

				if (i3==2)
				{
					int lg=IndComputeRule.length();

					if (IndComputeRule.equals (rule1)) //
					{
						int oct0=(byte)(data[i2]&0x7F)-(byte)(data[i2]&0x80); // manip car byte consideré signé
						int oct1=(byte)(data[i2+1]&0x7F)-(byte)(data[i2+1]&0x80);
						IndValue=256*oct0+oct1;
					}
					else 
					{ 

						String message="unknown compute rule: "+IndComputeRule;
						log.TraceLog(pgm,message);
						IndValue=(byte)(data[i2+1]&0x7F)-(byte)(data[i2+1]&0x80); // pas de regle de calcule definie on prende le 2eme octet
					}
				}
				else 
				{
					int oct0=(byte)(data[i2]&0x7F)-(byte)(data[i2]&0x80);
					IndValue=oct0;
				}				
				String sql="INSERT INTO IndValue VALUES ("+stationId+","+IndValue+",now(),"+intIdI+")";
				stmtInsert.executeUpdate(sql);
			}	
			rs.close();
			if (!Dispatcher.upToDateFlag[stationId] && stationId<=Dispatcher.maximumNumberOfStation)
			{
				Dispatcher.upToDateFlag[stationId]=true;
				KeepUpToDateParameters upToDateParamters = new KeepUpToDateParameters(stationId);
				upToDateParamters.start();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}
	}
	public  void InsertIndicator(int stationId, byte indType , int shiftPosition, byte[] data) 
	{	
		TraceLog log = new TraceLog();
		int indId=(byte)(data[4]&0x7F)-(byte)(data[4]&0x80);
		indId=indId+shiftPosition;
		String rule1="256*a+b";
		int IndValue=0;
		try {
			Statement stmtRead = conn.createStatement();
			Statement stmtInsert = conn.createStatement();
			ResultSet rs = stmtRead.executeQuery("SELECT * FROM IndDesc WHERE st_id ="+stationId+ " AND Ind_majtype =" +indType+ " AND Ind_id =" +indId);
			while (rs.next()) {
				String IndIdS= rs.getString("ind_id");
				int iId=Integer.parseInt(IndIdS);
				String IndPos= rs.getString("ind_pos");
				int iPos=Integer.parseInt(IndPos);
				String IndLen = rs.getString("ind_len");
				int iLen=Integer.parseInt(IndLen);
				String IndComputeRule = rs.getString("ind_compute_rule");
				if (iLen==2)
				{
					int lg=IndComputeRule.length();

					if (IndComputeRule.equals (rule1)) //
					{
						int oct0=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // manip car byte consideré signé
						int oct1=(byte)(data[iPos+1]&0x7F)-(byte)(data[iPos+1]&0x80);
						IndValue=256*oct0+oct1;
					}
					else 
					{ 
						String message="unknown compute rule: "+IndComputeRule;
						log.TraceLog(pgm,message);
						IndValue=(byte)(data[iPos+1]&0x7F)-(byte)(data[iPos+1]&0x80); // pas de regle de calcule definie on prende le 2eme octet
					}
				}
				else 
				{
					int oct0=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80);
					IndValue=oct0;
				}
				String sql="INSERT INTO IndValue VALUES ("+stationId+","+IndValue+",now(),"+iId+")";
				stmtInsert.executeUpdate(sql);
				log.TraceLog(pgm,sql);
			}	
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}
	}
	public static  void InsertDoorSystemAction (int stationId, String doorAction, String doorType, String doorUser, String doorUserIP, int doorRetcode) 
	{
		TraceLog log = new TraceLog();
		try {
	//		Statement stmtInsert = conn.createStatement();
			PreparedStatement st = conn.prepareStatement("insert into doorSystem values(now(),?,?,?,?,?,?)"); 
	  
	            // For the first parameter, 
	            // get the data using request object 
	            // sets the data to st pointer 
	            st.setInt(1, stationId); 
	            st.setString(2, doorAction); 
	            st.setString(3, doorType); 
	            st.setString(4, doorUser); 
	            st.setString(5, doorUserIP); 
	            st.setInt(6, doorRetcode); 
	            // Execute the insert command using executeUpdate() 
	            // to make changes in database 
	            st.executeUpdate(); 
	            // Close all the connections 
	            st.close(); 
	//		String sql="INSERT INTO doorSystem VALUES (now(),"+stationId+",+doorAction+,"doorType","doorType","doorType","+doorRetcode+")";
		//	log.TraceLog(pgm,st); 
		//	stmtInsert.executeUpdate(sql);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}