import com.auction.server.dao.AuctionDao;
import com.auction.core.utils.JsonMapper;
import java.util.List;

public class TestDao {
    public static void main(String[] args) {
        AuctionDao dao = new AuctionDao();
        List<?> results = dao.getPublicAuctions(0, 5, List.of("ACTIVE", "PENDING"), false, false);
        System.out.println("Result count: " + results.size());
        if (!results.isEmpty()) {
            System.out.println(JsonMapper.toJson(results));
        }
    }
}
