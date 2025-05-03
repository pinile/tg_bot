package compost.storage;

import java.util.Map;
import java.util.Set;

public interface TagRepository {

  Set<String> getTags(Long chatId); // получить список тегов

  boolean addTag(Long chatId, String tag, String description); // добавить тег

  boolean removeTag(Long chatId, String tag);

  Map<String, String> getTagMap(Long chatId);
}