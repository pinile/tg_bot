package compost.util;

import compost.model.SimpleUser;
import compost.service.TagService.TagResult;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MessageBuilder {

  public static String getHelp() {
    return """
        🤖 CompostInspectorBot 🤖
        📌 Доступные команды:
        /help - Справка... 
        /all - Поднимает всех из-под тестовых стендов 👹
        
        /tags - Список хэштегов, которые вы все равно не используете.
        /addtag #тег - Добавить хэштег 📌
        /deltag #тег - Удалить хештег
        
        /top - Показать самых активных ⚔️
        /panic - Создать видимость работы
        """;
  }

  public static String enablePanic() {
    return """
        Создание задач в Jira...
        ✅ BUG-124: "Ничего не работает, но работает"
        ✅ TASK-923: "Выделить личного водителя"
        ✅ TASK-777: "Изучение турецкого плагина AIO Tests"
        ✅ EPIC-932: "ПМ спалил, что ты вкатун"
        """;
  }

  public static String wrongThreadId(SimpleUser user) {
    return "Бот может отвечать только в теме \"спам для бота\", " + mention(user);
  }

  public static String unknownCommand() {
    return "Неизвестная команда, падаван \uD83D\uDC7E! Напиши /help для списка команд.";
  }

  public static String noActiveUser() {
    return "Нет данных об активности.";
  }

  public static String noTags() {
    return "❌ Пока нет добавленных тегов.";
  }

  public static String noUsersInChat() {
    return "Пока никого не видел в чате. Напишите что-нибудь, чтобы я вас запомнил (суки)!";
  }

  public static String missingTagArg() {
    return "❗ Неверный формат. Пример: /addtag #важно описание тега.";
  }

  public static String missingTagToDelete() {
    return "❗ Укажи тег, который нужно удалить. Пример: /deltag #важно.";
  }

  public static String invalidTagFormat() {
    return "❗ Неверный формат. Тег должен начинаться с #. Пример: /deltag #вопрос";
  }

  public static String tagExists(String tag) {
    return "⏳ Такой тег уже есть: " + tag;
  }

  public static String tagAdded(String tag) {
    return "✅ Тег " + tag + " добавлен!";
  }

  public static String tagDeleted(String tag) {
    return "🗑️ Тег " + tag + " удалён.";
  }

  public static String tagNotFound(String tag) {
    return "⚠️ Такого тега нет: " + tag;
  }

  public static String addTagResults(Collection<TagResult> results) {
    StringBuilder sb = new StringBuilder("📋 Результат добавления тегов:\n");

    for (TagResult result : results) {
      switch (result.result()) {
        case SUCCESS -> sb.append(tagAdded(result.tag())).append("\n");
        case ALREADY_EXISTS -> sb.append(tagExists(result.tag())).append("\n");
        case INVALID_FORMAT -> sb.append(invalidTagFormat()).append("\n");
        default -> sb.append(tagException()).append("\n");
      }
    }
    return sb.toString().trim();
  }

  public static String tagException() {
    return "⚠️Не удалось добавить тег.";
  }

  public static String mention(SimpleUser user) {
    if (user.getUsername() != null) {
      return "@" + user.getUsername();
    } else {
      String name = (user.getFirstName() != null ? user.getFirstName() : "??") +
          (user.getLastName() != null ? " " + user.getLastName() : "");
      return "<a href=\"tg://user?id=" + user.getId() + "\">" + name + "</a>";
    }
  }

  public static String topUsers(Map<SimpleUser, Integer> users) {
    if (users == null || users.isEmpty()) {
      return MessageBuilder.noActiveUser();
    }

    StringBuilder sb = new StringBuilder("🔥 Топ активных навозников:\n");
    int rank = 1;

    for (Map.Entry<SimpleUser, Integer> entry : users.entrySet()) {
      SimpleUser user = entry.getKey();
      int messageCount = entry.getValue();

      sb.append(String.format(
          "%d. %s - %d %s\n",
          rank++,
          mention(user),
          messageCount,
          PluralizationHelper.pluralize(messageCount, "сообщени")
      ));
    }
    return sb.toString();
  }

  public static String mentionAll(Collection<SimpleUser> users) {
    StringBuilder sb = new StringBuilder("🔔 Призыв всех навозников:\n");
    for (SimpleUser user : users) {
      sb.append(mention(user)).append(" ");
    }
    return sb.toString();
  }

  public static String tagList(List<Entry<String, String>> withDesc, List<String> withoutDesc) {
    if ((withDesc == null || withDesc.isEmpty()) && (withoutDesc == null
        || withoutDesc.isEmpty())) {
      return noTags();
    }

    StringBuilder sb = new StringBuilder("🏷️ Список тегов:\n");

    if (withDesc != null) {
      for (Map.Entry<String, String> entry : withDesc) {
        sb.append(entry.getKey()).append(" — ").append(entry.getValue()).append("\n");
      }
    }

    if (withoutDesc != null) {
      for (String tag : withoutDesc) {
        sb.append(tag).append("\n");
      }
    }

    return sb.toString().trim();
  }
}
