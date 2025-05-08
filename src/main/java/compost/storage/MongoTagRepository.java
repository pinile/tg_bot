package compost.storage;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import compost.service.TagService.ParsedTag;
import compost.util.MongoConnection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    List<Document> tagList = doc.getList("tags", Document.class);
    Set<String> tags = new HashSet<>();
    for (Document tagDoc : tagList) {
      tags.add(tagDoc.getString("tag"));
    }
    return tags;
  }

  @Override
  public boolean addTag(Long chatId, String tag, String description) {
    Bson filter = Filters.eq("chatId", chatId);
    Document tagDoc = new Document("tag", tag).append("description", description);
    Bson update = Updates.addToSet("tags", tagDoc);
    tagCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
    return true;
  }

  @Override
  public boolean removeTag(Long chatId, String tag) {
    Bson filter = Filters.eq("chatId", chatId);
    Bson update = Updates.pull("tags", new Document("tag", tag));
    tagCollection.updateOne(filter, update);
    return true;
  }

  @Override
  public Map<String, String> getTagMap(Long chatId) {
    Document doc = tagCollection.find(Filters.eq("chatId", chatId)).first();
    Map<String, String> tagMap = new LinkedHashMap<>();

    if (doc != null && doc.containsKey("tags")) {
      List<Document> tagList = doc.getList("tags", Document.class);
      for (Document tagDoc : tagList) {
        tagMap.put(tagDoc.getString("tag"), tagDoc.getString("description"));
      }
    }

    return tagMap;
  }

  @Override
  public void batchUpdateTagDescription(Long chatId, List<ParsedTag> tagsToUpdate) {
    for (ParsedTag tag : tagsToUpdate) {
      Bson filter = Filters.and(
          Filters.eq("chatId", chatId),
          Filters.eq("tags.tag", tag.tag())
      );
      Bson update = Updates.set("tags.$.description", tag.description());
      tagCollection.updateOne(filter, update);
    }
  }

  @Override
  public void batchClearTagDescription(Long chatId, List<String> tagsToClear) {
    for (String tag : tagsToClear) {
      Bson filter = Filters.and(
          Filters.eq("chatId", chatId),
          Filters.eq("tags.tag", tag)
      );
      Bson update = Updates.set("tags.$.description", "");
      tagCollection.updateOne(filter, update);
    }
  }
}
