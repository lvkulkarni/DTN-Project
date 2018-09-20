import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class MySQLConnector {
	static Connection con;
	static Statement stmt;
	
	
	public void startConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/ONE", "root", "Mtech@123");
			// here sonoo is database name, root is username and password
			stmt = con.createStatement();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void stopConnection() {
		try {
			con.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
}


