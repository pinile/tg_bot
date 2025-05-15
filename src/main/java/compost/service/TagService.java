package compost.service;

import static compost.util.Constants.TAG_PATTERN;

import compost.storage.TagRepository;
import compost.util.Constants.BotCommand;
import compost.util.Constants.TagOperationResult;
import compost.util.MessageBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;

/**
 * Сервис для управления тегами в системе. Отвечает за парсинг тегов из текста, их добавление,
 * обновление и удаление, а также за формирование списка тегов для отображения.
 */
@Log4j2
public class TagService {

  private final TagRepository tagRepository;

  public TagService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  /**
   * Обёртка для распарсенного тега: сам тег и описание к нему.
   */
  public record ParsedTag(String tag, String description) {

  }

  /**
   * Результат операции с тегом — успешное добавление, обновление или ошибка, включая сам тег и
   * описание.
   */
  public record TagResult(TagOperationResult result, String tag, String description) {

  }

  /**
   * Удаляет командную часть (например /addtag или /addtag@BotName) из текста команды.
   *
   * @param command         команда без слеша (например, "addtag")
   * @param fullCommandText полный текст команды, включая слеш
   * @return текст без командной части, готовый для парсинга
   */
  private String stripCommand(String command, String fullCommandText) {
    if (fullCommandText == null || command == null) {
      return "";
    }

    String commandName = command.startsWith("/") ? command.substring(1) : command;

    String regex = "^/" + Pattern.quote(commandName) + "(?:@\\w+)?\\s*";

    return fullCommandText.replaceFirst(regex, "").trim();
  }

  /**
   * Парсит входной текст, извлекая из него теги и связанные описания. Поддерживает множественные
   * теги и общие или индивидуальные описания.
   *
   * @param input строка для анализа
   * @return список ParsedTag с валидными тегами и описаниями
   */
  private List<ParsedTag> parseInput(String input) {
    log.debug("parseInput получен: '{}'", input);
    if (input == null || input.isBlank()) {
      log.debug("Input пустой или null.");
      return List.of();
    }

    Matcher matcher = TAG_PATTERN.matcher(input);
    List<String> tags = new ArrayList<>();
    List<Integer> tagStarts = new ArrayList<>();
    List<Integer> tagEnds = new ArrayList<>();

    while (matcher.find()) {
      String tag = matcher.group();
      if (!isValidTag(tag)) {
        log.debug("Невалидный тег '{}', пропускаем", tag);
        continue;
      }

      tags.add(tag);
      tagStarts.add(matcher.start());
      tagEnds.add(matcher.end());
    }

    if (tags.isEmpty()) {
      log.debug("Не найдено ни одного валидного тега");
      return List.of(new ParsedTag(null, null));
    }

    List<ParsedTag> parsed = new ArrayList<>();

    // Собираем все тексты между тегами
    List<String> betweenTexts = new ArrayList<>();
    for (int i = 0; i < tags.size(); i++) {
      int start = tagEnds.get(i);
      int end = (i + 1 < tagStarts.size()) ? tagStarts.get(i + 1) : input.length();
      String between = input.substring(start, end).trim();

      // Если текст содержит невалидный тег - игнорируем это описание
      Matcher tagMatcher = TAG_PATTERN.matcher(between);
      boolean containsInvalidTag = false;
      while (tagMatcher.find()) {
        String found = tagMatcher.group();
        if (!isValidTag(found)) {
          containsInvalidTag = true;
          break;
        }
      }

      betweenTexts.add(containsInvalidTag ? "" : between); // очищаем описание, если оно загрязнено
    }

    long nonEmptyCount = betweenTexts.stream().filter(s -> !s.isEmpty()).count();

    if (nonEmptyCount == 1) {
      // Только один фрагмент текста - считаем это общим описанием
      String commonDescription = betweenTexts.stream().filter(s -> !s.isEmpty()).findFirst()
          .orElse("");
      for (String tag : tags) {
        parsed.add(new ParsedTag(tag, commonDescription));
      }
    } else {
      // Несколько описаний
      for (int i = 0; i < tags.size(); i++) {
        String desc = (i < betweenTexts.size()) ? betweenTexts.get(i) : "";
        parsed.add(new ParsedTag(tags.get(i), desc));
      }
    }

    log.debug("Найдены теги: '{}'", tags);
    log.debug("Описание для каждого тега: '{}'",
        parsed.stream().map(ParsedTag::description).toList());

    return parsed;
  }

  /**
   * Пытается добавить или обновить теги. Возвращает список результатов с указанием, был ли тег
   * добавлен, обновлён или отклонён.
   *
   * @param chatId          ID чата
   * @param fullCommandText полный текст команды
   * @return список результатов для каждого тега
   */
  public List<TagResult> tryAddTag(Long chatId, String fullCommandText) {
    log.debug("tryAddTag вызван с chatId: '{}', fullCommandText: '{}'", chatId,
        fullCommandText);

    String cleanedText = stripCommand(BotCommand.ADDTAG.getCommand(), fullCommandText);

    // Разбор тегов
    List<ParsedTag> parsed = parseInput(cleanedText);
    if (parsed.isEmpty()) {
      log.debug("Не найдено ни одного валидного тега '{}'", cleanedText);
      return List.of(new TagResult(TagOperationResult.INVALID_FORMAT, null, null));
    }
    if (parsed.size() == 1 && parsed.get(0).tag() == null) {
      log.debug("Некорректный формат: ParsedTag с null");
      return List.of(new TagResult(TagOperationResult.INVALID_FORMAT, null, null));
    }

    Set<String> existingTags = tagRepository.getTags(chatId);
    List<TagResult> results = new ArrayList<>();
    List<ParsedTag> toUpdate = new ArrayList<>();

    // Для каждого тега
    for (ParsedTag tag : parsed) {
      boolean alreadyExists = existingTags.contains(tag.tag);

      if (alreadyExists) {
        // Всегда обновляем описание для существующего тега, не важно пустое описание или нет
        toUpdate.add(tag);
        log.debug("Обновлен тег: '{}' с описанием: '{}'", tag.tag, tag.description);
        results.add(
            new TagResult(TagOperationResult.UPDATED_DESCRIPTION, tag.tag, tag.description));
      } else {
        // Если тег новый, добавляем его
        tagRepository.addTag(chatId, tag.tag, tag.description);
        log.debug("Добавлен тег: '{}' с описанием: '{}'", tag.tag, tag.description);
        results.add(new TagResult(TagOperationResult.SUCCESS, tag.tag, tag.description));
      }
    }

    // Обновляем описание для тегов
    if (!toUpdate.isEmpty()) {
      tagRepository.batchUpdateTagDescription(chatId, toUpdate);
      log.debug("Batch-обновлены описания у {} тегов", toUpdate.size());
    }

    return results;
  }

  /**
   * Пытается удалить тег из хранилища.
   *
   * @param chatId          ID чата
   * @param fullCommandText текст команды с тегом
   * @return результат операции (успех, не найден, ошибка формата)
   */
  public TagResult tryRemoveTag(Long chatId, String fullCommandText) {
    log.debug("tryRemoveTag вызван с chatId: '{}', fullCommandText: '{}'", chatId,
        fullCommandText);

    String tag = stripCommand(BotCommand.DELTAG.getCommand(), fullCommandText);
    log.debug("Извлечён тег для удаления: '{}'", tag);
    if (tag.isEmpty() || !TAG_PATTERN.matcher(tag).matches()) {
      log.debug("Невалидный формат тега: '{}'", tag);
      return new TagResult(TagOperationResult.INVALID_FORMAT, null, null);
    }

    Set<String> existingTags = tagRepository.getTags(chatId);
    log.debug("Существующие теги для chatId '{}': {}", chatId, existingTags);

    if (!existingTags.contains(tag)) {
      log.debug("Тег '{}' не найден в хранилище", tag);
      return new TagResult(TagOperationResult.TAG_NOT_FOUND, tag, null);
    }

    tagRepository.removeTag(chatId, tag);
    log.debug("Тег '{}' успешно удалён", tag);
    return new TagResult(TagOperationResult.SUCCESS, tag, null);
  }

  /**
   * Формирует строку со списком тегов: сначала с описаниями, затем без.
   *
   * @param chatId ID чата
   * @return отформатированная строка списка тегов
   */
  public String getFormattedTagList(Long chatId) {
    Map<String, String> tagMap = tagRepository.getTagMap(chatId);

    // Теги с описанием
    List<Map.Entry<String, String>> withDescription = tagMap.entrySet().stream()
        .filter(e -> !e.getValue().isBlank())  // Убираем теги с пустым описанием
        .sorted(Map.Entry.comparingByKey())    // Сортировка по тегу
        .toList();

    // Теги без описания
    List<String> withoutDescription = tagMap.entrySet().stream()
        .filter(e -> e.getValue().isBlank())   // Фильтруем по пустому описанию
        .map(Map.Entry::getKey)                // Берём только ключи
        .sorted()                              // Сортируем по тегу
        .toList();

    return MessageBuilder.tagList(withDescription, withoutDescription);
  }

  /**
   * Проверяет, является ли тег валидным. Должен начинаться с #, содержать 2-30 символов (буквы,
   * цифры, подчёркивания), не состоять только из цифр и не содержать запрещённых символов.
   *
   * @param tag строка тега
   * @return true если тег валиден, иначе false
   */
  private boolean isValidTag(String tag) {
    if (tag == null || tag.isBlank()) {
      return false;
    }

    // Нельзя числа
    if (tag.matches("#\\d+")) {
      return false;
    }

    // Допустимые символы: буквы, цифры, подчёркивания
    // Длина 2-30, первый символ - решётка
    if (!tag.matches("#[\\p{L}\\d_]{2,30}")) {
      return false;
    }

    // Явно запрещаем символы
    String disallowedSymbols = "/.,:'\\-()$*=";
    for (char c : disallowedSymbols.toCharArray()) {
      if (tag.indexOf(c) >= 0) {
        return false;
      }
    }

    return true;
  }
}