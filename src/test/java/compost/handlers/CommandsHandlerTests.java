package compost.handlers;

import static org.mockito.Mockito.*;

import compost.bot.CodeCompostInspectorBot.CommandContext;
import compost.bot.handlers.AddTagCommandHandler;
import compost.bot.handlers.DeleteTagCommandHandler;
import compost.bot.handlers.HelpCommandHandler;
import compost.bot.handlers.MentionAllCommandHandler;
import compost.bot.handlers.TagsCommandHandler;
import compost.bot.handlers.TopCommandHandler;
import compost.model.SimpleUser;
import compost.service.TagService;
import compost.service.TagService.TagResult;
import compost.service.UserService;
import compost.storage.MongoUserRepository.RankedUser;
import compost.util.Constants.BotCommand;
import compost.util.Constants.TagOperationResult;
import compost.util.MessageBuilder;
import compost.util.MessageUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Message;

@Log4j2
@SpringBootTest
@DisplayName("Тест для команд бота.")
class CommandsHandlerTests {

  @Autowired TagsCommandHandler tagsHandler;
  @Autowired AddTagCommandHandler addTagHandler;
  @Autowired DeleteTagCommandHandler deleteTagHandler;
  @Autowired HelpCommandHandler helpHandler;
  @Autowired MentionAllCommandHandler allHandler;
  @Autowired TopCommandHandler topHandler;

  @MockBean TagService tagService;
  @MockBean MessageUtils messageUtils;
  @MockBean UserService userService;

  static Stream<Arguments> provideTagListTestCases() {
    return Stream.of(
        Arguments.of(
            "Тест с несколькими тегами: некоторые с описанием, другие без",
            Map.of(
                "#beta", "",
                "#apple", "фрукт",
                "#zebra", "",
                "#delta", "буква",
                "#gamma", "",
                "#banana", "желтый",
                "#alpha", ""),
            String.join(
                "\n",
                "🏷️ Список тегов:",
                "#apple — фрукт",
                "#banana — желтый",
                "#delta — буква",
                "#alpha",
                "#beta",
                "#gamma",
                "#zebra")),
        Arguments.of(
            "Тест с тегами, у которых нет описания",
            Map.of(
                "#one", "",
                "#two", "",
                "#three", "",
                "#four", ""),
            String.join("\n", "🏷️ Список тегов:", "#four", "#one", "#three", "#two")),
        Arguments.of(
            "Тест с тегами, у которых только описание",
            Map.of(
                "#apple", "фрукт",
                "#banana", "желтый"),
            String.join("\n", "🏷️ Список тегов:", "#apple — фрукт", "#banana — желтый")));
  }

  static Stream<Arguments> provideAddTagTestCases() {
    return Stream.of(
        Arguments.arguments(
            "Один тег без описания",
            BotCommand.ADDTAG.getCommandWithArg("#тег1"),
            MessageBuilder.addTagResults(List.of(TagResult.success("#тег1", null)))),
        Arguments.arguments(
            "Один тег с описанием",
            BotCommand.ADDTAG.getCommandWithArg("#тег1 описание"),
            MessageBuilder.addTagResults(List.of(TagResult.success("#тег1", "описание")))),
        Arguments.arguments(
            "Несколько тегов без описания",
            BotCommand.ADDTAG.getCommandWithArg("#тег1 #тег2"),
            MessageBuilder.addTagResults(
                List.of(TagResult.success("#тег1", null), TagResult.success("#тег2", null)))),
        Arguments.arguments(
            "Несколько тегов с одинаковым описанием",
            BotCommand.ADDTAG.getCommandWithArg("#тег4 описание общий #тег3 #тег6"),
            MessageBuilder.addTagResults(
                List.of(
                    TagResult.success("#тег4", "описание общий"),
                    TagResult.success("#тег3", "описание общий"),
                    TagResult.success("#тег6", "описание общий")))),
        Arguments.arguments(
            "Несколько тегов с одинаковым описанием (другой порядок описания)",
            BotCommand.ADDTAG.getCommandWithArg("#тег4 #тег3 описание общий #тег6"),
            MessageBuilder.addTagResults(
                List.of(
                    TagResult.success("#тег4", "описание общий"),
                    TagResult.success("#тег3", "описание общий"),
                    TagResult.success("#тег6", "описание общий")))),
        Arguments.arguments(
            "Несколько тегов с разными описаниями",
            BotCommand.ADDTAG.getCommandWithArg(
                "#тег4 описание4 #тег3 описание3 в несколько предложений с символами!> %^&*() 3 #тег6 описание_6"),
            MessageBuilder.addTagResults(
                List.of(
                    TagResult.success("#тег4", "описание4"),
                    TagResult.success(
                        "#тег3", "описание3 в несколько предложений с символами!> %^&*() 3"),
                    TagResult.success("#тег6", "описание_6")))),
        Arguments.arguments(
            "Обновление описания для существующего тега с описанием",
            BotCommand.ADDTAG.getCommandWithArg("#тег1 новое описание"),
            MessageBuilder.addTagResults(List.of(TagResult.updated("#тег1", "новое описание")))),
        Arguments.arguments(
            "Очистка описания для существующего тега с описанием",
            BotCommand.ADDTAG.getCommandWithArg("#тег1"),
            MessageBuilder.addTagResults(List.of(TagResult.updated("#тег1", null)))),
        Arguments.arguments(
            "Один новый тег и один существующий с очисткой описания",
            BotCommand.ADDTAG.getCommandWithArg("#тег1 новое описание #тег2"),
            MessageBuilder.addTagResults(
                List.of(
                    TagResult.success("#тег1", "новое описание"),
                    TagResult.updated("#тег2", null)))),
        Arguments.arguments(
            "Обновление существующего тега без описания",
            BotCommand.ADDTAG.getCommandWithArg("#тег1 тег и его описание"),
            MessageBuilder.addTagResults(
                List.of(TagResult.updated("#тег1", "тег и его описание")))),
        Arguments.arguments(
            "Добавление тегов с символами",
            BotCommand.ADDTAG.getCommandWithArg("#тег1_тег1 #тег2-тег2 #тег3//тег3"),
            MessageBuilder.addTagResults(List.of(TagResult.success("#тег1_тег1", null)))),
        Arguments.arguments(
            "Добавление тегов, один невалидный с '/'",
            BotCommand.ADDTAG.getCommandWithArg("#тег/1 описание1 #тег2 описание2"),
            MessageBuilder.addTagResults(
                List.of(TagResult.invalidFormat(), TagResult.success("#тег2", "описание2")))),
        Arguments.arguments(
            "Добавление тегов, тег == 30 символов",
            BotCommand.ADDTAG.getCommandWithArg("#йцукефывапячсмийцукенекуцйфыва"),
            MessageBuilder.addTagResults(
                List.of(TagResult.success("#йцукефывапячсмийцукенекуцйфыва", null)))));
  }

  static Stream<Arguments> provideRemoveTagTestCases() {
    return Stream.of(
        Arguments.arguments(
            TagOperationResult.SUCCESS,
            "Удаление существующего тега",
            TagResult.success("#тег1", null),
            MessageBuilder.tagDeleted("#тег1")),
        Arguments.arguments(
            TagOperationResult.TAG_NOT_FOUND,
            "Удаление несуществующего тега",
            TagResult.tagNotFound("#тег2"),
            MessageBuilder.tagNotFound("#тег2")),
        Arguments.arguments(
            TagOperationResult.INVALID_FORMAT,
            "Удаление с неверным форматом",
            TagResult.invalidFormat(),
            MessageBuilder.invalidTagFormat()));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("provideTagListTestCases")
  @DisplayName("Проверка bot/handlers/TagsCommandHandler.java. Команда /tags")
  void testHandle_tags(String testDescription, Map<String, String> sortedTagMap, String expected) {
    long chatId = 123L;
    int threadId = 1;
    when(tagService.getFormattedTagList(chatId)).thenReturn(expected);

    CommandContext context = new CommandContext(chatId, threadId, null, "/tags");

    tagsHandler.handle(context);

    log.info("Ожидаемое сообщение:\n{}", expected);

    verify(tagService).getFormattedTagList(chatId);
    verify(messageUtils).sendText(chatId, threadId, expected);

    log.info("✅ Проверка для TagsCommandHandler прошла успешно .");
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("provideAddTagTestCases")
  @DisplayName("Проверка bot/handlers/AddTagCommandHandler.java. Команда /addtag")
  void testHandle_addTag(String testDescription, String input, String expectedMessage) {

    long chatId = 123L;
    int threadId = 1;
    Message message = mock(Message.class);
    when(message.getText()).thenReturn(input);

    when(tagService.buildAddTagResponse(chatId, input)).thenReturn(expectedMessage);

    CommandContext context = new CommandContext(chatId, threadId, message, input);

    addTagHandler.handle(context);

    log.info("Ожидаемое сообщение:\n{}", expectedMessage);

    verify(tagService).buildAddTagResponse(chatId, input);
    verify(messageUtils).sendText(chatId, threadId, expectedMessage);
    verifyNoMoreInteractions(tagService, messageUtils);

    log.info("✅ Проверка для AddTagCommandHandler прошла успешно.");
  }

  @ParameterizedTest(name = "[{index}] [{0}] → {1}")
  @MethodSource("provideRemoveTagTestCases")
  @DisplayName("Проверка bot/handlers/DeleteTagCommandHandler.java. Команда /deltag")
  void testHandle_deleteTag(
      TagOperationResult caseType,
      String testDescription,
      TagResult tagResult,
      String expectedMessage) {

    long chatId = 123L;
    int threadId = 1;
    String text = BotCommand.DELTAG.getCommandWithArg("#test");
    Message message = mock(Message.class);
    when(message.getText()).thenReturn(text);

    when(tagService.tryRemoveTag(chatId, text)).thenReturn(tagResult);

    CommandContext context = new CommandContext(chatId, threadId, message, text);

    deleteTagHandler.handle(context);

    log.info("Ожидаемое сообщение:\n{}", expectedMessage);

    verify(tagService).tryRemoveTag(chatId, text);
    verify(messageUtils).sendText(chatId, threadId, expectedMessage);
    verifyNoMoreInteractions(tagService, messageUtils);

    log.info("✅ Проверка для DeleteTagCommandHandler прошла успешно.");
  }

  @DisplayName("Проверка bot/handlers/HelpCommandHandler.java. Команда /deltag")
  @Test
  void testHandle_help() {
    long chatId = 1L;
    int threadId = 5;
    CommandContext context = new CommandContext(chatId, threadId, null, BotCommand.HELP.getCommand());

    helpHandler.handle(context);
    String expected = MessageBuilder.getHelp();

    log.info("Ожидаемое сообщение:\n{}", expected);
    verify(messageUtils).sendText(chatId, threadId, expected);
    verifyNoMoreInteractions(messageUtils);

    log.info("✅ Проверка для HelpCommandHandler прошла успешно.");
  }

  @DisplayName("Проверка bot/handlers/MentionAllCommandHandler.java. Команда /all")
  @Test
  void testHandle_mentionAll() {
    long chatId = 5L;
    int threadId = 2;
    List<SimpleUser> users = List.of(
        new SimpleUser(1L, "u1", "First", "One"),
        new SimpleUser(2L, null, "Second", null)
    );

    String built = MessageBuilder.mentionAll(users);
    when(userService.buildMentionAllMessage(chatId)).thenReturn(built);

    String command = BotCommand.ALL.getCommand();
    CommandContext context = new CommandContext(chatId, threadId, null, command);

    allHandler.handle(context);

    log.info("Ожидаемое сообщение:\n{}", built);

    verify(userService).buildMentionAllMessage(chatId);
    verify(messageUtils).sendText(chatId, threadId, built);
    verifyNoMoreInteractions(userService, messageUtils);

    log.info("✅ Проверка для HelpCommandHandler прошла успешно.");
  }

  @DisplayName("Проверка bot/handlers/TopCommandHandler.java. Команда /all")
  @Test
  void testHandle_topUsers() {
    long chatId = 9L;
    int threadId = 3;
    List<RankedUser> top = List.of(
        new RankedUser(new SimpleUser(1L, "u1", "First", "User"), 10, 1),
        new RankedUser(new SimpleUser(2L, "u2", "Second", "User"), 5, 2)
    );
    String expectedText = MessageBuilder.topUsers(top);

    when(userService.getTopUsers(chatId, 10)).thenReturn(top);

    String command = BotCommand.TOP.getCommand();
    CommandContext ctx = new CommandContext(chatId, threadId, null, command);

    topHandler.handle(ctx);

    log.info("Ожидаемое сообщение:\n{}", expectedText);

    verify(userService).getTopUsers(chatId, 10);
    verify(messageUtils).sendText(chatId, threadId, expectedText);
    verifyNoMoreInteractions(userService, messageUtils);

    log.info("✅ Проверка для TopCommandHandler прошла успешно.");
  }
}
