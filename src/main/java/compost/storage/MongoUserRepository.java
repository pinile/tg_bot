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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.telegram.telegrambots.meta.api.objects.User;

/**
 * Реализация хранилища пользователей в MongoDB. Используется для хранения Telegram-пользователей.
 */
@Repository
public class MongoUserRepository implements UserRepository {

  private final MongoCollection<Document> userCollection;

  public record RankedUser(SimpleUser user, int messageCount, int rank) { }

  public MongoUserRepository(MongoDatabase database) {
    this.userCollection = database.getCollection("users");
  }

  /**
   * Обновляет (сохраняет) информацию о пользователе. Также увеличивает счётчик сообщений.
   *
   * @param chatId                ID чата
   * @param telegramUser          Telegram-пользователь
   * @param incrementMessageCount увеличивать ли messageCount
   */
  @Override
  public void upsertUser(Long chatId, User telegramUser, boolean incrementMessageCount) {
    Bson filter = and(
        eq("chatId", chatId),
        eq("id", telegramUser.getId())
    );

    // Список обновлений
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
    // Если не инкрементим, то гарантируем наличие поля
    else {
      updates.add(setOnInsert("messageCount", 0));
    }

    userCollection.updateOne(
        filter,
        combine(updates),
        new UpdateOptions().upsert(true)
    );
  }

  /**
   * Получает одного пользователя по chatId и userId.
   *
   * @param chatId ID чата
   * @param userId ID пользователя
   * @return Объект SimpleUser или null
   */
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

    // Преобразуем документ MongoDB в SimpleUser
    return new SimpleUser(
        doc.getLong("id"),
        doc.getString("username"),
        doc.getString("firstName"),
        doc.getString("lastName")
    );
  }


  /**
   * Возвращает всех пользователей в чате.
   *
   * @param chatId ID чата
   * @return Коллекция пользователей
   */
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

  /**
   * Возвращает топ пользователей по количеству сообщений.
   *
   * @param chatId ID чата
   * @param limit  Максимальное количество пользователей
   * @return Отсортированная Map: пользователь → количество сообщений
   */
  public List<RankedUser> getTopUsers(Long chatId, int limit) {
    Bson filter = eq("chatId", chatId);

    List<RankedUser> topUsers = new ArrayList<>();

    int rank = 1;
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
      topUsers.add(new RankedUser(user, messageCount, rank));
      rank++;
    }
    return topUsers;
  }
}