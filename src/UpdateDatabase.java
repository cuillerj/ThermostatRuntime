
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
public class UpdateDatabase extends Thread{
	public String connectionUrl = "jdbc:mysql://jserver:3306/domotiquedata";
	public String connectionUser = "jean";
	public String connectionPassword = "manu7890";
	public Connection conn = null;
	public UpdateDatabase() {

		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			try {
				conn = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
				System.out.println("Database connexion Ok");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				System.out.println("Database connexion Ko !");
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
		System.out.println("UpdateDatabase V0.1");
		while(true)
		{
			Thread.currentThread();

		}
	}
	public  void InsertIndicators (int stationId, int indType, byte[] data) 
	{	
		int IndValue=0;
		String rule1="256*a+b";
		System.out.println("1:"+stationId+" "+indType);

		try {
			Statement stmtRead = conn.createStatement();
			Statement stmtInsert = conn.createStatement();
			System.out.println("4");
			ResultSet rs = stmtRead.executeQuery("SELECT * FROM IndDesc WHERE st_id ="+stationId+ " AND Ind_majtype =" +indType);
			System.out.println("5:"+stationId+" "+indType); 
			while (rs.next()) {
				System.out.println("6");
				String IndIdS= rs.getString("ind_id");
				int i1=Integer.parseInt(IndIdS);
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
						System.out.println("unknown compute rule: "+IndComputeRule);
						IndValue=(byte)(data[i2+1]&0x7F)-(byte)(data[i2+1]&0x80); // pas de regle de calcule definie on prende le 2eme octet
					}
				}
				else 
				{
					int oct0=(byte)(data[i2]&0x7F)-(byte)(data[i2]&0x80);
					IndValue=oct0;
				}				
				String sql="INSERT INTO IndValue VALUES ("+stationId+","+IndValue+",now(),"+i1+")";
				System.out.println("ind id "+IndIdS+", pos " + IndPos + ", len: " + IndLen+" value"+IndValue);
				System.out.println(sql);
				stmtInsert.executeUpdate(sql);
			}	
			System.out.println("close");
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}
	}
	public  void InsertIndicator(int stationId, int indType , byte[] data) 
	{	
		int indId=(byte)(data[4]&0x7F)-(byte)(data[4]&0x80);
		String rule1="256*a+b";
		System.out.println("1:"+stationId+" "+indType+" id:"+indId);
		int IndValue=0;
		try {
			Statement stmtRead = conn.createStatement();
			Statement stmtInsert = conn.createStatement();
			System.out.println("44");
			ResultSet rs = stmtRead.executeQuery("SELECT * FROM IndDesc WHERE st_id ="+stationId+ " AND Ind_majtype =" +indType+ " AND Ind_id =" +indId);
			System.out.println("55:"+stationId+" "+indType); 
			while (rs.next()) {
				System.out.println("6");
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
						System.out.println("unknown compute rule: "+IndComputeRule);
						IndValue=(byte)(data[iPos+1]&0x7F)-(byte)(data[iPos+1]&0x80); // pas de regle de calcule definie on prende le 2eme octet
					}
				}
				else 
				{
					int oct0=(byte)(data[iPos]&0x7F)-(byte)(data[iPos]&0x80);
					IndValue=oct0;
				}				
				String sql="INSERT INTO IndValue VALUES ("+stationId+","+IndValue+",now(),"+iId+")";
				System.out.println("ind id "+IndIdS+", pos " + IndPos + ", len: " + IndLen+" value"+IndValue);
				System.out.println(sql);
				stmtInsert.executeUpdate(sql);
			}	
			System.out.println("close");
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}
	}
}
