package compost.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import compost.service.TagService.TagResult;
import compost.storage.TagRepository;
import compost.util.Constants.BotCommand;
import compost.util.Constants.CaseType;
import compost.util.Constants.TagOperationResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Log4j2
@DisplayName("Проверка метода TagService - добавление, удаление, обновление тегов.")
public class TagServiceTests {

  @Mock
  private TagRepository tagRepository;

  private TagService tagService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    tagService = new TagService(tagRepository);
  }

  static Stream<Arguments> provideInvalidFormatTestCases() {
    return Stream.of(
        Arguments.arguments("Некорректный формат (нет тега)", 123L,
            BotCommand.ADDTAG.getCommand(), Set.of(), Map.of(),
            List.of(
                new TagResult(TagOperationResult.INVALID_FORMAT, null, null)
            )),

        Arguments.arguments("Некорректный формат #####", 123L,
            BotCommand.ADDTAG.getCommandWithArg("#####"), Set.of(), Map.of(),
            List.of(
                new TagResult(TagOperationResult.INVALID_FORMAT, null, null)
            ))
    );
  }

  static Stream<Arguments> provideAddTagTestCases() {
    return Stream.of(
        Arguments.arguments("Один тег без описания", 123L,
            BotCommand.ADDTAG.getCommandWithArg("#тег1"), Set.of(), Map.of(),
            List.of(
                new TagResult(TagOperationResult.SUCCESS, "#тег1", "")
            )),

        Arguments.arguments("Один тег с описанием", 123L,
            BotCommand.ADDTAG.getCommandWithArg("#тег1 описание"), Set.of(),
            Map.of(), List.of(
                new TagResult(TagOperationResult.SUCCESS, "#тег1", "описание")
            )),

        Arguments.arguments("Несколько тегов без описания", 123L,
            BotCommand.ADDTAG.getCommandWithArg("#тег1 #тег2"), Set.of(),
            Map.of(), List.of(
                new TagResult(TagOperationResult.SUCCESS, "#тег1", ""),
                new TagResult(TagOperationResult.SUCCESS, "#тег2", "")
            )),

        Arguments.arguments("Несколько тегов с одинаковым описанием", 123L,
            BotCommand.ADDTAG.getCommandWithArg("#тег4 описание общий #тег3 #тег6"), Set.of(),
            Map.of(), List.of(
                new TagResult(TagOperationResult.SUCCESS, "#тег4", "описание общий"),
                new TagResult(TagOperationResult.SUCCESS, "#тег3", "описание общий"),
                new TagResult(TagOperationResult.SUCCESS, "#тег6", "описание общий")
            )),

        Arguments.arguments("Несколько тегов с одинаковым описанием (другой порядок описания)",
            123L, BotCommand.ADDTAG.getCommandWithArg("#тег4 #тег3 описание общий #тег6"), Set.of(),
            Map.of(), List.of(
                new TagResult(TagOperationResult.SUCCESS, "#тег4", "описание общий"),
                new TagResult(TagOperationResult.SUCCESS, "#тег3", "описание общий"),
                new TagResult(TagOperationResult.SUCCESS, "#тег6", "описание общий")
            )),

        Arguments.arguments("Несколько тегов с разными описаниями", 123L,
            BotCommand.ADDTAG.getCommandWithArg(
                "#тег4 описание4 #тег3 описание3 в несколько предложений с символами!> %^&*() 3 #тег6 описание_6"),
            Set.of(),
            Map.of(),
            List.of(
                new TagResult(TagOperationResult.SUCCESS, "#тег4", "описание4"),
                new TagResult(TagOperationResult.SUCCESS, "#тег3",
                    "описание3 в несколько предложений с символами!> %^&*() 3"),
                new TagResult(TagOperationResult.SUCCESS, "#тег6", "описание_6")
            )),

        Arguments.arguments("Обновление описания для существующего тега с описанием", 123L,
            BotCommand.ADDTAG.getCommandWithArg("#тег1 новое описание"), Set.of("#тег1"),
            Map.of("#тег1", "старое описание"), List.of(
                new TagResult(TagOperationResult.UPDATED_DESCRIPTION, "#тег1", "новое описание")
            )),

        Arguments.arguments("Очистка описания для существующего тега с описанием", 123L,
            BotCommand.ADDTAG.getCommandWithArg("#тег1"), Set.of("#тег1"),
            Map.of("#тег1", "старое описание"),
            List.of(
                new TagResult(TagOperationResult.UPDATED_DESCRIPTION, "#тег1", "")
            )),

        Arguments.arguments("Один новый тег и один существующий с очисткой описания", 123L,
            BotCommand.ADDTAG.getCommandWithArg("#тег1 новое описание #тег2"), Set.of("#тег2"),
            Map.of("#тег2", "старое описание"), List.of(
                new TagResult(TagOperationResult.SUCCESS, "#тег1", "новое описание"),
                new TagResult(TagOperationResult.UPDATED_DESCRIPTION, "#тег2", "новое описание")
            )),

        Arguments.arguments("Обновление существующего тега без описания", 123L,
            BotCommand.ADDTAG.getCommandWithArg("#тег1 тег и его описание"), Set.of("#тег1"),
            Map.of("#тег1", ""), List.of(
                new TagResult(TagOperationResult.UPDATED_DESCRIPTION, "#тег1", "тег и его описание")
            ))
    );
  }

  static Stream<Arguments> provideAllAddTagTestCases() {
    return Stream.concat(
        provideInvalidFormatTestCases().map(args ->
            Arguments.of(CaseType.INVALID_FORMAT, args.get()[0], args.get()[1], args.get()[2],
                args.get()[3], args.get()[4], args.get()[5])
        ),
        provideAddTagTestCases().map(args ->
            Arguments.of(CaseType.SUCCESS, args.get()[0], args.get()[1], args.get()[2],
                args.get()[3], args.get()[4], args.get()[5])
        )
    );
  }


  static Stream<Arguments> provideRemoveTagTestCases() {
    return Stream.of(
        Arguments.arguments("Удаление существующего тега", 123L,
            BotCommand.DELTAG.getCommandWithArg("#тег1"),
            Set.of("#тег1"), new TagResult(TagOperationResult.SUCCESS, "#тег1", null)),

        Arguments.arguments("Удаление несуществующего тега", 123L,
            BotCommand.DELTAG.getCommandWithArg("#тег2"),
            Set.of("#тег1"), new TagResult(TagOperationResult.TAG_NOT_FOUND, "#тег2", null)),

        Arguments.arguments("Пустой ввод", 123L,
            BotCommand.DELTAG.getCommand(),
            Set.of("#тег1"), new TagResult(TagOperationResult.INVALID_FORMAT, null, null)),

        Arguments.arguments("Удаление невалидного тега", 123L,
            BotCommand.DELTAG.getCommandWithArg("#######"),
            Set.of("#тег1"), new TagResult(TagOperationResult.INVALID_FORMAT, null, null))
    );
  }

  @ParameterizedTest(name = "[{index}] {0}: {2}")
  @MethodSource("provideRemoveTagTestCases")
  @DisplayName("Проверка команды /deltag (удаление тега)")
  void testTryRemoveTag(
      String testDescription,
      Long chatId,
      String input,
      Set<String> existingTags,
      TagResult expectedResults
  ) {
    log.info("──────────────────────────────────────────");
    log.info("Тест: '{}'. (input: '{}')", testDescription, input);
    log.info("ОР: '{}'", expectedResults);

    when(tagRepository.getTags(chatId)).thenReturn(existingTags);

    TagResult actualResult = tagService.tryRemoveTag(chatId, input);

    assertEquals(expectedResults.result(), actualResult.result(),
        "Результат для тега " + expectedResults.tag());
    assertEquals(expectedResults.tag(), actualResult.tag(), "Имя тега");
    assertEquals(expectedResults.description(), actualResult.description(),
        "Описание тега " + expectedResults.tag());

    log.info("ФР: '{}'", actualResult);

    if (expectedResults.result() == TagOperationResult.SUCCESS) {
      verify(tagRepository).removeTag(chatId, expectedResults.tag());
    }
  }


  @ParameterizedTest(name = "[{index}] [Тип: {0}]. {1}")
  @MethodSource("provideAllAddTagTestCases")
  @DisplayName("Проверка команды /addtag.")
  void testTryAddTagSuccess(
      CaseType caseType,
      String testDescription,
      Long chatId,
      String input,
      Set<String> existingTags,
      Map<String, String> existingTagDescriptions,
      List<TagResult> expectedResults
  ) {
    log.info("──────────────────────────────────────────");
    log.info("Тест '{}'. (input: '{}')", testDescription, input);
    log.info("ОР: '{}'", expectedResults);

    when(tagRepository.getTags(chatId)).thenReturn(existingTags);
    when(tagRepository.getTagMap(chatId)).thenReturn(existingTagDescriptions);

    List<TagResult> actualResults = tagService.tryAddTag(chatId, input);

    assertEquals(expectedResults.size(), actualResults.size(), "Размер списка результатов");

    for (int i = 0; i < expectedResults.size(); i++) {
      TagResult expected = expectedResults.get(i);
      TagResult actual = actualResults.get(i);
      assertEquals(expected.result(), actual.result(), "Результат для тега " + expected.tag());
      assertEquals(expected.tag(), actual.tag(), "Имя тега");
      assertEquals(expected.description(), actual.description(), "Описание тега " + expected.tag());
    }

    log.info("ФР: '{}'", actualResults);

    if (caseType == CaseType.SUCCESS) {
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
          }
        }
      }
    }
  }
}