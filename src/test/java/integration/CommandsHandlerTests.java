package compost.bot.handlers;

import compost.bot.CodeCompostInspectorBot.CommandContext;
import compost.service.TagService;
import compost.util.MessageUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.*;

@SpringBootTest
class CommandsHandlerTests {

  @Autowired
  TagsCommandHandler handler;

  @MockBean
  TagService tagService;

  @MockBean
  MessageUtils messageUtils;

  @Test
  void testHandle_tags() {
    long chatId = 123L;
    int threadId = 456;
    String expectedMessage = "🏷️ Список тегов:\n#tag1 — описание";

    when(tagService.getFormattedTagList(chatId)).thenReturn(expectedMessage);

    CommandContext context = new CommandContext(chatId, threadId, null, "/tags");

    handler.handle(context);

    verify(tagService).getFormattedTagList(chatId);
    verify(messageUtils).sendText(chatId, threadId, expectedMessage);
  }
}
