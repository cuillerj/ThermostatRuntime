
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
/*
 * function: y=(4/5) X -16
 */

public class MonitorWaterTemperatures extends Thread{
	public static int boilerId=1282;
	public static int thermostatId=1280;
	public static int thermostatIndId=49;
	public static int lowerWaterTemp=40;
	public static int countThreshold1=8;
	public static int countThreshold2=4;
	public static String IndDescTable="IndDesc";
	public int outShift=0;	
	public int inShift=0;	
	public int inThreshold=0;	
	public static int waterOutIndId=5;
	public static int waterInIndId=6;
	public static int waterId=3;
	public static int outTempIndId=2;
	public static int nbOverThresholdIndId=3;
	public int maxOut=0;
	public int nbOver=0;
	public static int minReactivity=30;
	public static int maxReactivity=48;	
	public int reactivity=0;
	public int reactivityInd=0;
	public int InTempAlarmThreshold=0;
	public static float coefA = (float) 4/5;
	public static float coefB=-15;
	public static String pgm="MonitorWaterTemperatures";
	String connectionUrl = GetSqlConnection.GetDomotiqueDB();
	String connectionUser = GetSqlConnection.GetUser();
	String connectionPassword = GetSqlConnection.GetPass();
	public Connection conn = null;
	public MonitorWaterTemperatures() {
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
		Thread.currentThread();
		TraceLog log = new TraceLog();
		String message="";
		message="MonitorWaterTemperatures V1.2";
		log.TraceLog(pgm,message);
		while(true)
		{
			try {
				String mess="";
			    java.util.Date today = new java.util.Date();
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				String connectionUrl = GetSqlConnection.GetDomotiqueDB();
				String connectionUser = GetSqlConnection.GetUser();
				String connectionPassword = GetSqlConnection.GetPass();
				conn = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
				Statement stmt1 = null;
				stmt1 = conn.createStatement();
		//		stmt2 = conn.createStatement();
				ResultSet rs1 = null;
		//		ResultSet rs2 = null;
				rs1 = stmt1.executeQuery("SELECT  st_status_date, TIMESTAMPDIFF(MINUTE,st_status_date,now()) as minutes FROM `domotiquedata`.`stations` where st_id=3"); // 
				int minutes=100;
				while (rs1.next()) {
					minutes= rs1.getInt("minutes");
				}
				rs1.close();
	//			System.out.println (minutes); 
					if (minutes<=30)
					{					
						rs1 = stmt1.executeQuery("SELECT ind_target FROM IndDesc"
								+ " WHERE st_id="+boilerId+" and ind_desc = 'OutShiftTemp'  limit 1"); // 
						while (rs1.next()) {
							outShift= rs1.getInt("ind_target");
						}
						rs1.close();
						rs1 = stmt1.executeQuery("SELECT ind_target FROM IndDesc "
								+ "WHERE  st_id="+boilerId+" and ind_desc = 'InShiftTemp' limit 1"); // 
						while (rs1.next()) {
							inShift= rs1.getInt("ind_target");
						}
						rs1.close();
						rs1 = stmt1.executeQuery("SELECT * FROM IndDesc "
								+ "WHERE  st_id="+thermostatId+" and ind_desc = 'reactivity' limit 1"); // 
						while (rs1.next()) {
							reactivity= rs1.getInt("ind_target");
							reactivityInd=rs1.getInt("ind_id");
							mess=" reactivity:"+reactivity;
						}
						rs1.close();
						
						rs1 = stmt1.executeQuery("SELECT ind_target FROM IndDesc "
								+ "WHERE  st_id="+boilerId+" and ind_desc = 'InTempAlarmThreshold' limit 1"); // 
						while (rs1.next()) {
							InTempAlarmThreshold= rs1.getInt("ind_target");
							mess=mess+" InTempAlarmThreshold:"+InTempAlarmThreshold;
						}
						rs1.close();					
						
						
						rs1 = stmt1.executeQuery("SELECT MAX(mesrument_value) as MaxOut FROM value_mesurment WHERE mesurment_ind_id = "+waterOutIndId+" AND mesurment_st_id = "+waterId+" AND mesurment_time > CURRENT_TIMESTAMP - INTERVAL 12 HOUR");
						int maxOutTemp12=0;
						while (rs1.next()) {
							maxOutTemp12=rs1.getInt("maxOut");
							mess=mess+ " maxOutTemp12:"+maxOutTemp12;
						}
						rs1.close();
						int tempLimit12=maxOutTemp12-1;
						rs1 = stmt1.executeQuery("SELECT  count(mesrument_value) as maxNumber FROM value_mesurment where mesurment_ind_id = "+waterOutIndId+" and mesurment_st_id = "+waterId+" and mesurment_time > CURRENT_TIMESTAMP - INTERVAL 24 HOUR and mesrument_value >="+tempLimit12+"");
						int maxNumber12=0;
						while (rs1.next()) {
							maxNumber12=rs1.getInt("maxNumber");
							mess=mess+ " maxNumber12:"+maxNumber12;
						}
				//		log.TraceLog(pgm,mess);
						rs1.close();
						rs1 = stmt1.executeQuery("SELECT MAX(mesrument_value) as MaxOut FROM value_mesurment WHERE mesurment_ind_id = "+waterOutIndId+" AND mesurment_st_id = "+waterId+" AND mesurment_time > CURRENT_TIMESTAMP - INTERVAL 24 HOUR");
						int maxOutTemp24=0;
						while (rs1.next()) {
							maxOutTemp24=rs1.getInt("maxOut");
							mess=mess+ " maxOutTemp:"+maxOutTemp24;
						}
						rs1.close();
						int tempLimit24=maxOutTemp24-1;
						rs1 = stmt1.executeQuery("SELECT  count(mesrument_value) as maxNumber FROM value_mesurment where mesurment_ind_id = "+waterOutIndId+" and mesurment_st_id = "+waterId+" and mesurment_time > CURRENT_TIMESTAMP - INTERVAL 24 HOUR and mesrument_value >="+tempLimit24+"");
						int maxNumber24=0;
						while (rs1.next()) {
							maxNumber24=rs1.getInt("maxNumber");
							mess=mess+ " maxNumber:"+maxNumber24;
						}
				//		log.TraceLog(pgm,mess);
						rs1.close();
						int maxOutTemp=0;
						if (maxOutTemp12 >=lowerWaterTemp)
						{
							if (maxNumber12>=countThreshold1)
							{
								maxOutTemp=maxOutTemp12;
							}
							else 
							{
								if (maxNumber12>=countThreshold2)
								{
									maxOutTemp=maxOutTemp12-1;
								}
							}
						}
						if (maxOutTemp==0)
						{
							if (maxOutTemp24 >=lowerWaterTemp)
							{
								if (maxNumber24>=countThreshold1)
								{
									maxOutTemp=maxOutTemp24;
								}
								else 
								{
									if (maxNumber24>=countThreshold2)
									{
										maxOutTemp=maxOutTemp24-1;
									}
								}	
							}
						}
						if (maxOutTemp!=0)
						{
								maxOutTemp=maxOutTemp+outShift;
								if (Math.abs(maxOutTemp-maxOut)>=2)
								{
									maxOut=maxOutTemp;
									Statement stmtInsert1 = null;
									stmtInsert1 = conn.createStatement();
									mess="update maxout:"+maxOut;
									log.TraceLog(pgm,mess);
									String sql="INSERT INTO IndValue VALUES ("+thermostatId+","+maxOut+",now(),"+thermostatIndId+")";
									stmtInsert1.executeUpdate(sql);
									log.TraceLog(pgm,sql);
									sql="INSERT INTO IndValue VALUES ("+boilerId+","+maxOut+",now(),"+outTempIndId+")";
									stmtInsert1.executeUpdate(sql);
									log.TraceLog(pgm,sql);
									float tempReactivity=coefA*maxOut+coefB;
									mess="computed tempReactivity:"+tempReactivity;
									if (tempReactivity <=maxReactivity && tempReactivity >=minReactivity && Math.abs(tempReactivity-reactivity) >=2)
									{
										Statement stmtUpdate = null;
										stmtUpdate = conn.createStatement();
										mess="update reactivity:"+reactivity+ " new value:"+tempReactivity;
										reactivity= (int) Math.round(tempReactivity);
										sql = "UPDATE IndDesc " +
								                   "SET ind_target = "+reactivity+"  WHERE st_id = "+thermostatId+" AND ind_id = "+reactivityInd+"";
										stmtUpdate.executeUpdate(sql);
										log.TraceLog(pgm,sql);
									}
								}
						}	
						
						rs1 = stmt1.executeQuery("SELECT  count(mesrument_value) as number FROM value_mesurment "
								+ "where mesurment_ind_id ="+waterInIndId+" and mesurment_st_id="+waterId+" and mesurment_time > CURRENT_TIMESTAMP - INTERVAL 24 HOUR and mesrument_value >"+InTempAlarmThreshold+"");
						int number=0;
						while (rs1.next()) {
							number=rs1.getInt("number");
							mess=mess+ " number of time over threshold:"+number;	
						}
						log.TraceLog(pgm,mess);	
						rs1.close();
						if (Math.abs(nbOver-number)>=5)
						{
							Statement stmtInsert1 = null;
							stmtInsert1 = conn.createStatement();
							nbOver=number;
							String sql="INSERT INTO IndValue VALUES ("+boilerId+","+number+",now(),"+nbOverThresholdIndId+")";
							stmtInsert1.executeUpdate(sql);
							log.TraceLog(pgm,sql);
						}
			}
					else{
						mess="monitor Water not updated since:"+minutes+ "mn";
						log.TraceLog(pgm,mess);				
					}
				
				conn.close();
		}	
			catch (Exception e) {
				e.printStackTrace();
			} finally {

			}
			try {
				
				Thread.sleep(300000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
