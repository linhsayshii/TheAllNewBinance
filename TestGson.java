import com.auction.core.dto.auction.PublicAuctionDto;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestGson {
    public static void main(String[] args) {
        DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(src.format(FORMATTER));
                }
            })
            .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                        throws JsonParseException {
                    return LocalDateTime.parse(json.getAsString(), FORMATTER);
                }
            })
            .create();
            
        PublicAuctionDto dto = new PublicAuctionDto();
        dto.setAuctionId(1);
        dto.setItemId(2);
        dto.setItemName("Test");
        dto.setCurrentPrice(10.0);
        dto.setStartTime(LocalDateTime.now());
        dto.setEndTime(LocalDateTime.now().plusDays(1));
        dto.setStatus("PENDING");
        dto.setSellerDisplayName("Seller");
        
        System.out.println(GSON.toJson(dto));
    }
}
