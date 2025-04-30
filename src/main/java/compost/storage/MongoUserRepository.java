package compost.storage;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.setOnInsert;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import compost.model.SimpleUser;
import compost.util.MongoConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.telegram.telegrambots.meta.api.objects.User;

public class MongoUserRepository implements UserRepository {

  private final MongoCollection<Document> userCollection;

  public MongoUserRepository() {
    MongoDatabase database = MongoConnection.getDatabase();
    userCollection = database.getCollection("users");
  }

  @Override
  public void upsertUser(Long chatId, User telegramUser, boolean incrementMessageCount) {
    Bson filter = and(
        eq("chatId", chatId),
        eq("id", telegramUser.getId())
    );

    // Обновления через set
    List<Bson> updates = new ArrayList<>();
    updates.add(set("chatId", chatId));
    updates.add(set("id", telegramUser.getId()));
    updates.add(set("username", telegramUser.getUserName()));
    updates.add(set("firstName", telegramUser.getFirstName()));
    updates.add(set("lastName", telegramUser.getLastName()));

    // Если нужно увеличить счетчик
    if (incrementMessageCount) {
      updates.add(inc("messageCount", 1));
    }
    // Если не нужно увеличивать, но нужно гарантировать наличие поля при создании
    else {
      updates.add(setOnInsert("messageCount", 0));
    }

    userCollection.updateOne(
        filter,
        combine(updates),
        new UpdateOptions().upsert(true)
    );
  }

  @Override
  public SimpleUser getUser(Long chatId, Long userId) {
    Bson filter = and(
        eq("chatId", chatId),
        eq("id", userId)
    );
    Document doc = userCollection.find(filter).first();
    if (doc == null) {
      return null;
    }

    return new SimpleUser(
        doc.getLong("id"),
        doc.getString("username"),
        doc.getString("firstName"),
        doc.getString("lastName")
    );
  }

  @Override
  public Collection<SimpleUser> getAllUsers(Long chatId) {
    Bson filter = eq("chatId", chatId);
    List<SimpleUser> users = new ArrayList<>();
    for (Document doc : userCollection.find(filter)) {
      users.add(new SimpleUser(
          doc.getLong("id"),
          doc.getString("username"),
          doc.getString("firstName"),
          doc.getString("lastName")
      ));
    }
    return users;
  }

  public Map<SimpleUser, Integer> getTopUsers(Long chatId, int limit) {
    Bson filter = eq("chatId", chatId);

    Map<SimpleUser, Integer> topUsers = new LinkedHashMap<>();
    for (Document doc : userCollection.find(filter)
        .sort(descending("messageCount"))
        .limit(limit)) {

      SimpleUser user = new SimpleUser(
          doc.getLong("id"),
          doc.getString("username"),
          doc.getString("firstName"),
          doc.getString("lastName")
      );

      Integer messageCount = doc.getInteger("messageCount", 0);
      topUsers.put(user, messageCount);
    }
    return topUsers;
  }
}