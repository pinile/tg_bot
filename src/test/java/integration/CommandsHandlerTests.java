package compost.bot.handlers;

import static org.mockito.Mockito.*;

import compost.bot.CodeCompostInspectorBot.CommandContext;
import compost.service.TagService;
import compost.util.MessageUtils;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@Log4j2
@SpringBootTest
@DisplayName("Тест для команд бота.")
class CommandsHandlerTests {

  @Autowired TagsCommandHandler handler;

  @MockBean TagService tagService;

  @MockBean MessageUtils messageUtils;

  static Stream<Arguments> provideTagListTestCases() {
    return Stream.of(
        Arguments.of(
            "Тест с несколькими тегами: некоторые с описанием, другие без",
            123L,
            456,
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
            124L,
            456,
            Map.of(
                "#one", "",
                "#two", "",
                "#three", "",
                "#four", ""),
            String.join("\n", "🏷️ Список тегов:", "#four", "#one", "#three", "#two")),
        Arguments.of(
            "Тест с тегами, у которых только описание",
            125L,
            456,
            Map.of(
                "#apple", "фрукт",
                "#banana", "желтый"),
            String.join("\n", "🏷️ Список тегов:", "#apple — фрукт", "#banana — желтый")));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("provideTagListTestCases")
  @DisplayName("Проверка команды /tags")
  void testHandle_tags(
      String testDescription,
      Long chatId,
      Integer threadId,
      Map<String, String> sortedTagMap,
      String expected) {

    when(tagService.getFormattedTagList(chatId)).thenReturn(expected);

    CommandContext context = new CommandContext(chatId, threadId, null, "/tags");

    handler.handle(context);

    log.info("✅ Ожидаемое сообщение:\n{}", expected);

    verify(tagService).getFormattedTagList(chatId);
    verify(messageUtils).sendText(chatId, threadId, expected);

    log.info("✔️ Проверки прошли успешно: вызов tagService и отправка сообщения произведены.");
  }
}
