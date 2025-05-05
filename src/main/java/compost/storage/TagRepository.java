package compost.storage;

import compost.service.TagService.ParsedTag;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TagRepository {

  Set<String> getTags(Long chatId); // получить список тегов

  boolean addTag(Long chatId, String tag, String description); // добавить тег

  boolean removeTag(Long chatId, String tag);

  Map<String, String> getTagMap(Long chatId);

  void batchUpdateTagDescription(Long chatId, List<ParsedTag> tagsToUpdate);

  void batchClearTagDescription(Long chatId, List<String> tagsToClear);
}