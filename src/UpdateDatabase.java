
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
public class UpdateDatabase extends Thread{
	public static String pgm="UpdateDatabase";
	public String connectionUrl = "jdbc:mysql://jserver:3306/domotiquedata";
	public String connectionUser = "jean";
	public String connectionPassword = "manu7890";
	public Connection conn = null;
	public UpdateDatabase() {
	String message="";
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			try {
				conn = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
	//			System.out.println("Database connexion Ok");
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
		message="UpdateDatabase V1.0";
		log.TraceLog(pgm,message);
		while(true)
		{
			Thread.currentThread();
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
//				System.out.println("ind id "+IndIdS+", pos " + IndPos + ", len: " + IndLen+" value"+IndValue);
//				System.out.println(sql);
				stmtInsert.executeUpdate(sql);
			}	
			rs.close();
			if (!ThermostatDispatcher.upToDateFlag[stationId] && stationId<=ThermostatDispatcher.maximumNumberOfStation)
			{
				ThermostatDispatcher.upToDateFlag[stationId]=true;
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
	//	System.out.println("1:"+stationId+" "+indType+" id:"+indId);
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
//					System.out.println("compute rule: "+IndComputeRule+ " len:"+IndComputeRule.length());
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

	//			System.out.println("ind id "+IndIdS+", pos " + IndPos + ", len: " + IndLen+" value"+IndValue);
	//			System.out.println(sql);
				stmtInsert.executeUpdate(sql);
				log.TraceLog(pgm,sql);
			}	
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}
	}

public  void InsertPID(int stationId, byte[] data) 
{	

//	System.out.println("1:"+stationId);
	int iPos=4;
	int relayStatus=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // 
	iPos=6;
	int tempInstruction=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // 
	iPos=7;
	int AverageTemp=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // 
	iPos=9;
	int oct0=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // manip car byte consideré signé
	int oct1=(byte)(data[iPos+1]&0x7F)-(byte)(data[iPos+1]&0x80);
	int windowSize=256*oct0+oct1;
	if(data[iPos-1]==0x2d)
	{
		windowSize=-windowSize;
	}
	iPos=12;
	int PIDCycle=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // 
	iPos=14;
	int data1=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // 
	iPos=16;
	int data2=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // 
	iPos=18;
	int data3=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // 
	iPos=20;
	int data4=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // 
	iPos=22;
	int data5=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // 
	iPos=24;
	int data6=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80); // 

	try {
		Statement stmtRead = conn.createStatement();
		Statement stmtInsert = conn.createStatement();
		String sql="INSERT INTO PIDData VALUES (now(),"+stationId+","+relayStatus+","+tempInstruction+","+AverageTemp+","+windowSize+","+PIDCycle+","+data1+","+data2+","+data3+","+data4+","+data5+","+data6+")";
		stmtInsert.executeUpdate(sql);
	
	} catch (SQLException e) {
		// TODO Auto-generated catch block

		e.printStackTrace();
	}
	}
}