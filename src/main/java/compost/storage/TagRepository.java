package compost.storage;

import java.util.Set;

public interface TagRepository {

  Set<String> getTags(Long chatId); // получить список тегов

  boolean addTag(Long chatId, String tag); // добавить тег

  boolean removeTag(Long chatId, String tag);
}