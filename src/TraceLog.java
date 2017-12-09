

import java.sql.Timestamp;
import java.util.Date;

public class TraceLog {
    public  void TraceLog (String source, String message)
    {
	 java.util.Date date= new java.util.Date();
	 System.out.println(new Timestamp(date.getTime())+ " "+source+": "+message);
    }
}
