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

  public record TagResult(TagOperationResult result, String tag, String description) {

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
    if (input == null || input.isBlank()) {
      logger.debug("Input пустой или null.");
      return List.of();
    }

    Matcher matcher = TAG_PATTERN.matcher(input);
    List<String> tags = new ArrayList<>();
    List<Integer> tagStarts = new ArrayList<>();
    List<Integer> tagEnds = new ArrayList<>();

    while (matcher.find()) {
      tags.add(matcher.group());
      tagStarts.add(matcher.start());
      tagEnds.add(matcher.end());
    }

    if (tags.isEmpty()) {
      logger.debug("Не найдено ни одного тега");
      return List.of(new ParsedTag(null, null)); // обработается как INVALID_FORMAT
    }

    List<ParsedTag> parsed = new ArrayList<>();

    // Собираем все тексты между тегами
    List<String> betweenTexts = new ArrayList<>();
    for (int i = 0; i < tagEnds.size(); i++) {
      int start = tagEnds.get(i);
      int end = (i + 1 < tagStarts.size()) ? tagStarts.get(i + 1) : input.length();
      String between = input.substring(start, end).trim();
      betweenTexts.add(between);
    }

    long nonEmptyCount = betweenTexts.stream().filter(s -> !s.isEmpty()).count();

    if (nonEmptyCount == 1) {
      // Только один фрагмент текста — считаем это общим описанием
      String commonDescription = betweenTexts.stream().filter(s -> !s.isEmpty()).findFirst()
          .orElse("");
      for (String tag : tags) {
        parsed.add(new ParsedTag(tag, commonDescription));
      }
    } else {
      // Несколько описаний — считаем что они индивидуальные (может быть и пустые)
      for (int i = 0; i < tags.size(); i++) {
        parsed.add(new ParsedTag(tags.get(i), betweenTexts.get(i)));
      }
    }

    logger.debug("Найдены теги: '{}'", tags);
    logger.debug("Описание для каждого тега: '{}'",
        parsed.stream().map(ParsedTag::description).toList());

    return parsed;
  }

  public List<TagResult> tryAddTag(Long chatId, String fullCommandText) {
    logger.debug("tryAddTag вызван с chatId: '{}', fullCommandText: '{}'", chatId,
        fullCommandText);

    String cleanedText = stripCommand("addtag",
        fullCommandText); //TODO сделать enum с командами боту?

    List<ParsedTag> parsed = parseInput(cleanedText);
    if (parsed.isEmpty()) {
      logger.debug("Не найдено ни одного валидного тега '{}'", cleanedText);
      return List.of(new TagResult(TagOperationResult.INVALID_FORMAT, null, null));
    }

    Set<String> existingTags = tagRepository.getTags(chatId);
    List<TagResult> results = new ArrayList<>();
    List<ParsedTag> toUpdate = new ArrayList<>();
    List<String> toClear = new ArrayList<>();

    // Для каждого тега
    for (ParsedTag tag : parsed) {
      boolean alreadyExists = existingTags.contains(tag.tag);

      if (alreadyExists) {
        // Всегда обновляем описание для существующего тега, не важно пустое описание или нет
        toUpdate.add(tag);
        logger.debug("Обновлен тег: '{}' с описанием: '{}'", tag.tag, tag.description);
        results.add(
            new TagResult(TagOperationResult.UPDATED_DESCRIPTION, tag.tag, tag.description));
      } else {
        // Если тег новый, добавляем его
        tagRepository.addTag(chatId, tag.tag, tag.description);
        logger.debug("Добавлен тег: '{}' с описанием: '{}'", tag.tag, tag.description);
        results.add(new TagResult(TagOperationResult.SUCCESS, tag.tag, tag.description));
      }
    }

    // Обновляем описание для тегов
    if (!toUpdate.isEmpty()) {
      tagRepository.batchUpdateTagDescription(chatId, toUpdate);
      logger.debug("Batch-обновлены описания у {} тегов", toUpdate.size());
    }

    // Очищаем описание для тегов
    if (!toClear.isEmpty()) {
      tagRepository.batchClearTagDescription(chatId, toClear);
      logger.debug("Batch-очищены описания у {} тегов", toClear.size());
    }

    return results;
  }

  //TODO добавить тесты для tryRemoveTag
  public TagResult tryRemoveTag(Long chatId, String fullCommandText) {
    logger.debug("tryRemoveTag вызван с chatId: '{}', fullCommandText: '{}'", chatId,
        fullCommandText);

    String tag = stripCommand("deltag", fullCommandText); //TODO сделать enum с командами боту?
    logger.debug("Извлечён тег для удаления: '{}'", tag);
    if (tag.isEmpty() || !TAG_PATTERN.matcher(tag).matches()) {
      logger.debug("Невалидный формат тега: '{}'", tag);
      return new TagResult(TagOperationResult.INVALID_FORMAT, null, null);
    }

    Set<String> existingTags = tagRepository.getTags(chatId);
    logger.debug("Существующие теги для chatId '{}': {}", chatId, existingTags);

    if (!existingTags.contains(tag)) {
      logger.debug("Тег '{}' не найден в хранилище", tag);
      return new TagResult(TagOperationResult.TAG_NOT_FOUND, tag, null);
    }

    tagRepository.removeTag(chatId, tag);
    logger.debug("Тег '{}' успешно удалён", tag);
    return new TagResult(TagOperationResult.SUCCESS, tag, null);
  }

  public Map<String, String> getTagMaps(Long chatId) {
    return tagRepository.getTagMap(chatId);
  }
}
