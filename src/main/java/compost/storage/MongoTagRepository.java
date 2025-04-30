package compost.storage;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import compost.util.MongoConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MongoTagRepository implements TagRepository {

  private final MongoCollection<Document> tagCollection;

  public MongoTagRepository() {
    MongoDatabase database = MongoConnection.getDatabase();
    tagCollection = database.getCollection("tags");
  }

  @Override
  public Set<String> getTags(Long chatId) {
    Document doc = tagCollection.find(Filters.eq("chatId", chatId)).first();
    if (doc == null || !doc.containsKey("tags")) {
      return new HashSet<>();
    }
    List<String> tagList = doc.getList("tags", String.class);
    return new HashSet<>(tagList);
  }

  @Override
  public boolean addTag(Long chatId, String tag) {
    Bson filter = Filters.eq("chatId", chatId);
    Bson update = Updates.addToSet("tags", tag);
    tagCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
    return true;
  }

  @Override
  public boolean removeTag(Long chatId, String tag) {
    Bson filter = Filters.eq("chatId", chatId);
    Bson update = Updates.pull("tags", tag);
    tagCollection.updateOne(filter, update);
    return true;
  }
}
