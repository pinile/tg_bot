package compost.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import compost.service.TagService.TagResult;
import compost.storage.TagRepository;
import compost.util.Constants.TagOperationResult;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

@DisplayName("Проверка метода TagService - добавление, удаление, обновление тегов.")
public class TagServiceTests {

  private TagRepository tagRepository;
  private TagService tagService;
  private static final Logger logger = LogManager.getLogger(TagServiceTests.class);

  @BeforeEach
  void setUp() {
    tagRepository = Mockito.mock(TagRepository.class);
    tagService = new TagService(tagRepository);
  }

  static Stream<Arguments> provideAddTagTestCases() {
    return Stream.of(
        // Некорректный формат (нет тега)
        Arguments.of(123L, "/addtag", Set.of(), List.of(
            new TagResult(TagOperationResult.INVALID_FORMAT, null)
        )),

        // Один тег без описания
        Arguments.of(123L, "/addtag #тег1", Set.of(), List.of(
            new TagResult(TagOperationResult.SUCCESS, "#тег1")
        )),

        // Один тег с описанием
        Arguments.of(123L, "/addtag #тег1 описание", Set.of(), List.of(
            new TagResult(TagOperationResult.SUCCESS, "#тег1")
        )),

        // Несколько тегов без описания
        Arguments.of(123L, "/addtag #тег1 #тег2", Set.of(), List.of(
            new TagResult(TagOperationResult.SUCCESS, "#тег1"),
            new TagResult(TagOperationResult.SUCCESS, "#тег2")
        )),

        // Несколько тегов с одинаковым описанием
        Arguments.of(123L, "/addtag #тег4 описание общий #тег3 #тег6", Set.of(), List.of(
            new TagResult(TagOperationResult.SUCCESS, "#тег4"),
            new TagResult(TagOperationResult.SUCCESS, "#тег3"),
            new TagResult(TagOperationResult.SUCCESS, "#тег6")
        )),

        // Несколько тегов с разными описаниями
        Arguments.of(123L, "/addtag #тег4 описание4 #тег3 описание3 #тег6 описание6", Set.of(),
            List.of(
                new TagResult(TagOperationResult.SUCCESS, "#тег4"),
                new TagResult(TagOperationResult.SUCCESS, "#тег3"),
                new TagResult(TagOperationResult.SUCCESS, "#тег6")
            )),

        // Обновление описания для существующего тега
        Arguments.of(123L, "/addtag #тег1 новое описание", Set.of("#тег1"), List.of(
            new TagResult(TagOperationResult.UPDATED_DESCRIPTION, "#тег1")
        )),

        // Очистка описания для существующего тега
        Arguments.of(123L, "/addtag #тег1", Set.of("#тег1"), List.of(
            new TagResult(TagOperationResult.CLEARED_DESCRIPTION, "#тег1")
        )),

        // Один новый тег и один существующий с очисткой описания
        Arguments.of(123L, "/addtag #тег1 описание #тег2", Set.of("#тег2"), List.of(
            new TagResult(TagOperationResult.SUCCESS, "#тег1"),
            new TagResult(TagOperationResult.UPDATED_DESCRIPTION, "#тег2")
        ))
    );
  }

  @ParameterizedTest(name = "[{index}] input=''{1}'' -> ожидаемые результаты=''{3}''")
  @MethodSource("provideAddTagTestCases")
  void testTryAddTag(Long chatId, String input, Set<String> existingTags,
      List<TagResult> expectedResults) {
    logger.debug("Запуск testTryAddTag: input='{}', ожидаемый результат='{}'", input,
        expectedResults);

    when(tagRepository.getTags(chatId)).thenReturn(existingTags);
    List<TagResult> actualResults = tagService.tryAddTag(chatId, input);

    assertEquals(expectedResults.size(), actualResults.size(), "Размер списка результатов");

    // Проверка каждого результата
    for (int i = 0; i < expectedResults.size(); i++) {
      TagResult expected = expectedResults.get(i);
      TagResult actual = actualResults.get(i);
      assertEquals(expected.result(), actual.result(), "Результат для тега " + expected.tag());
      assertEquals(expected.tag(), actual.tag(), "Имя тега");
    }

    logger.debug("Результат testTryAddTag: {} -> {}", input, actualResults);

    // Проверка вызова нужного метода в зависимости от результата
    for (TagResult result : expectedResults) {
      String tag = result.tag();
      TagOperationResult op = result.result();

      switch (op) {
        case SUCCESS -> {
          if (tag != null) {
            verify(tagRepository).addTag(eq(chatId), eq(tag), any());
          }
        }
        case UPDATED_DESCRIPTION ->
            verify(tagRepository).batchUpdateTagDescription(eq(chatId), any());
        case CLEARED_DESCRIPTION ->
            verify(tagRepository).batchClearTagDescription(eq(chatId), eq(List.of(tag)));
        default -> {
          // Ничего не вызывается
        }
      }
    }
  }
}