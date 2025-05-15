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

/**
 * Реализация интерфейса TagRepository на базе MongoDB.
 * Хранит теги и их описания в коллекции "tags".
 */
public class MongoTagRepository implements TagRepository {

  private final MongoCollection<Document> tagCollection;

  public MongoTagRepository() {
    MongoDatabase database = MongoConnection.getDatabase();
    tagCollection = database.getCollection("tags");
  }

  /**
   * Получает все теги для заданного чата.
   * @param chatId Идентификатор чата
   * @return Множество строковых тегов
   */
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

  /**
   * Добавляет тег с описанием для заданного чата.
   * Если документа с таким chatId нет — создаёт.
   * @param chatId Идентификатор чата
   * @param tag Тег
   * @param description Описание тега
   * @return true всегда
   */
  @Override
  public boolean addTag(Long chatId, String tag, String description) {
    Bson filter = Filters.eq("chatId", chatId);
    Document tagDoc = new Document("tag", tag).append("description", description);
    Bson update = Updates.addToSet("tags", tagDoc);
    tagCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
    return true;
  }

  /**
   * Удаляет тег из документа по chatId.
   * @param chatId Идентификатор чата
   * @param tag Тег, который нужно удалить
   * @return true всегда
   */
  @Override
  public boolean removeTag(Long chatId, String tag) {
    Bson filter = Filters.eq("chatId", chatId);
    Bson update = Updates.pull("tags", new Document("tag", tag));
    tagCollection.updateOne(filter, update);
    return true;
  }

  /**
   * Возвращает отображение тег → описание, отсортированное по тегам.
   * @param chatId Идентификатор чата
   * @return Map с тегами и их описаниями
   */
  @Override
  public Map<String, String> getTagMap(Long chatId) {
    Document doc = tagCollection.find(Filters.eq("chatId", chatId)).first();
    if (doc == null || !doc.containsKey("tags")) {
      return Map.of();
    }

    List<Document> tagList = doc.getList("tags", Document.class);

    // Преобразуем список документов в отсортированную Map: тег -> описание
    Map<String, String> withDesc = tagList.stream()
        .map(d -> Map.entry(d.getString("tag"), d.getString("description")))
        .sorted(Map.Entry.comparingByKey())
        .collect(LinkedHashMap::new, // сохраняем порядок сортировки
            (m, e) -> m.put(e.getKey(), e.getValue()),
            LinkedHashMap::putAll);

    return withDesc;
  }

  /**
   * Массовое обновление описаний для списка тегов.
   * Использует оператор positional $ для обновления нужного элемента массива.
   * @param chatId Идентификатор чата
   * @param tagsToUpdate Список тегов с новыми описаниями
   */
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

  /**
   * Массовое удаление описаний (обнуляет строку) для указанных тегов.
   * @param chatId Идентификатор чата
   * @param tagsToClear Список тегов, для которых нужно очистить описание
   */
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
