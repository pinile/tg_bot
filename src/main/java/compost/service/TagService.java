package compost.service;

import static compost.util.Constants.TAG_PATTERN;

import compost.storage.TagRepository;
import compost.util.Constants.TagOperationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TagService {

  private final TagRepository tagRepository;
  private static final Logger logger = LogManager.getLogger(TagService.class);

  public TagService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  public record ParsedTag(String tag, String description) {

  }

  public record TagResult(TagOperationResult result, String tag) {

  }

  private String stripCommand(String commandName, String fullCommandText) {
    if (fullCommandText == null) {
      return "";
    }

    return fullCommandText
        .replaceFirst("^/" + commandName + "(@\\w+)?\\s*", "")
        .trim();
  }

  private List<ParsedTag> parseInput(String input) {
    logger.debug("parseInput получен: '{}'", input);
    if (input == null) {
      logger.debug("Input = null.");
      return List.of();
    }

    Matcher matcher = TAG_PATTERN.matcher(input);
    List<String> tags = new ArrayList<>();
    List<String> descriptions = new ArrayList<>();

    while (matcher.find()) {
      tags.add(matcher.group());
    }

    if (tags.isEmpty()) {
      logger.debug("List тегов пуст: '{}'", tags);
      return List.of();
    }
    // кейс 1: Несколько тегов с общим описанием (например, #tag1 description1 #tag2 #tag3)
    if (tags.size() > 1) {
      String remainingText = input.replaceAll(TAG_PATTERN.pattern(), "").trim();
      if (!remainingText.isEmpty()) {
        // Если есть описание после тегов
        descriptions = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
          descriptions.add(remainingText); // Одно описание для всех тегов
        }
      } else {
        // Если описание отсутствует после тегов, оставляем пустое описание
        descriptions = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
          descriptions.add(""); // Пустое описание для всех тегов
        }
      }
    }

    // кейс 2: Каждый тег имеет свое описание (например, #tag1 description1 #tag2 description2)
    else if (tags.size() == 1) {
      // Обрабатываем только один тег
      String remainingText = input.replace(tags.get(0), "").trim();
      if (!remainingText.isEmpty()) {
        descriptions.add(remainingText); // Описание для этого тега
      } else {
        descriptions.add(""); // Пустое описание
      }
    }

    // кейс 3: Если на входе только один тег без описания
    else if (tags.size() == 1 && input.trim().equals(tags.get(0))) {
      // Если строка состоит только из одного тега (без описания)
      descriptions.add(""); // Пустое описание для этого тега
    }

    // Если строка не соответствует ни одному из случаев
    if (tags.size() != descriptions.size()) {
      throw new IllegalStateException("Количество тегов и описаний не совпадает.");
    }

    logger.debug("Найдены теги: '{}'", tags);
    logger.debug("Описание для каждого тега: '{}'", descriptions);

    List<ParsedTag> parsedTags = new ArrayList<>();
    for (int i = 0; i < tags.size(); i++) {
      parsedTags.add(new ParsedTag(tags.get(i), descriptions.get(i)));
    }

    return parsedTags;
  }

  public List<TagResult> tryAddTag(Long chatId, String fullCommandText) {
    logger.debug("tryAddTag вызван с chatId: '{}', fullCommandText: '{}'", chatId, fullCommandText);

    String cleanedText = stripCommand("addtag",
        fullCommandText); //TODO сделать enum с командами боту?1
    logger.debug("Очищенный текст команды: '{}'", cleanedText);

    List<ParsedTag> parsed = parseInput(cleanedText);
    if (parsed.isEmpty()) {
      logger.debug("Не найдено ни одного валидного тега '{}'", cleanedText);
      return List.of(new TagResult(TagOperationResult.INVALID_FORMAT, null));
    }

    Set<String> existingTags = tagRepository.getTags(chatId);
    List<TagResult> results = new ArrayList<>();

    List<ParsedTag> toUpdate = new ArrayList<>();
    List<String> toClear = new ArrayList<>();

    for (ParsedTag tag : parsed) {
      boolean alreadyExists = existingTags.contains(tag.tag);

      if (alreadyExists) {
        if (!tag.description.isBlank()) {
          toUpdate.add(tag);
          results.add(new TagResult(TagOperationResult.UPDATED_DESCRIPTION, tag.tag));
        } else {
          // Удаляем описание, если оно есть
          toClear.add(tag.tag);
          results.add(new TagResult(TagOperationResult.CLEARED_DESCRIPTION, tag.tag));
        }
      } else {
        // Добавляем новый тег
        tagRepository.addTag(chatId, tag.tag, tag.description);
        logger.debug("Добавлен тег: '{}' с описанием: '{}'", tag.tag, tag.description);
        results.add(new TagResult(TagOperationResult.SUCCESS, tag.tag));
      }
    }

    if (!toUpdate.isEmpty()) {
      tagRepository.batchUpdateTagDescription(chatId, toUpdate);
      logger.debug("Batch-обновлены описания у {} тегов", toUpdate.size());
    }

    if (!toClear.isEmpty()) {
      tagRepository.batchClearTagDescription(chatId, toClear);
      logger.debug("Batch-очищены описания у {} тегов", toClear.size());

    }

    return results;
  }

  public TagResult tryRemoveTag(Long chatId, String fullCommandText) {
    logger.debug("tryRemoveTag вызван с chatId: '{}', fullCommandText: '{}'", chatId,
        fullCommandText);

    String tag = stripCommand("deltag", fullCommandText); //TODO сделать enum с командами боту?
    logger.debug("Извлечён тег для удаления: '{}'", tag);
    if (tag.isEmpty() || !TAG_PATTERN.matcher(tag).matches()) {
      logger.debug("Невалидный формат тега: '{}'", tag);
      return new TagResult(TagOperationResult.INVALID_FORMAT, null);
    }

    Set<String> existingTags = tagRepository.getTags(chatId);
    logger.debug("Существующие теги для chatId '{}': {}", chatId, existingTags);

    if (!existingTags.contains(tag)) {
      logger.debug("Тег '{}' не найден в хранилище", tag);
      return new TagResult(TagOperationResult.TAG_NOT_FOUND, tag);
    }

    tagRepository.removeTag(chatId, tag);
    logger.debug("Тег '{}' успешно удалён", tag);
    return new TagResult(TagOperationResult.SUCCESS, tag);
  }

  public Map<String, String> getTagMaps(Long chatId) {
    return tagRepository.getTagMap(chatId);
  }
}
