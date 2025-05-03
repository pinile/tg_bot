package compost.service;

import static compost.util.Constants.TAG_PATTERN;

import compost.storage.TagRepository;
import compost.util.Constants.TagOperationResult;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TagService {

  private final TagRepository tagRepository;

  public TagService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  private static class ParsedTag {

    String tag;
    String description;

    ParsedTag(String tag, String description) {
      this.tag = tag;
      this.description = description;
    }
  }

  public record TagResult(TagOperationResult result, String tag) {

  }


  private Optional<ParsedTag> parseInput(String input) {
    if (input == null || !input.contains(" ")) {
      return Optional.empty();
    }
    String[] parts = input.split(" ", 2);
    String tag = parts[0].trim();
    String desc = parts[1].trim();
    return TAG_PATTERN.matcher(tag).matches()
        ? Optional.of(new ParsedTag(tag, desc))
        : Optional.empty();
  }

  public TagResult tryAddTag(Long chatId, String fullCommandText) {
    Optional<ParsedTag> parsed = parseInput(fullCommandText);
    if (parsed.isEmpty()) {
      return new TagResult(TagOperationResult.INVALID_FORMAT, null);
    }

    Set<String> existingTags = tagRepository.getTags(chatId);
    if (existingTags.contains(parsed.get().tag)) {
      return new TagResult(TagOperationResult.ALREADY_EXISTS, parsed.get().tag);
    }

    tagRepository.addTag(chatId, parsed.get().tag, parsed.get().description);
    return new TagResult(TagOperationResult.SUCCESS, parsed.get().tag);
  }

  public TagResult tryRemoveTag(Long chatId, String fullCommandText) {
    String tag = fullCommandText.contains(" ") ? fullCommandText.split(" ", 2)[1].trim() : null;
    if (tag == null || !TAG_PATTERN.matcher(tag).matches()) {
      return new TagResult(TagOperationResult.INVALID_FORMAT, null);
    }

    Set<String> existingTags = tagRepository.getTags(chatId);
    if (!existingTags.contains(tag)) {
      return new TagResult(TagOperationResult.TAG_NOT_FOUND, tag);
    }

    tagRepository.removeTag(chatId, tag);
    return new TagResult(TagOperationResult.SUCCESS, tag);
  }

  public Map<String, String> getTagMaps(Long chatId) {
    return tagRepository.getTagMap(chatId);
  }
}
