import com.auction.server.dao.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestDB {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT auction_id, start_time, end_time, status FROM auctions LIMIT 5;");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("auction_id") + 
                    ", Start: " + rs.getTimestamp("start_time") + 
                    ", End: " + rs.getTimestamp("end_time") + 
                    ", Status: " + rs.getString("status"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
