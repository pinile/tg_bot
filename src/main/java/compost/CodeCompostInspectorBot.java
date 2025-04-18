package compost;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.util.concurrent.ScheduledExecutorService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CodeCompostInspectorBot extends TelegramLongPollingBot {

  private boolean usersChanged = false; //
  private static final String STORAGE_FILE = "users.json";

  private static final String TAGS_FILE = "tags.txt";
  private final Set<String> tags = new HashSet<>();

  private final Map<Long, Map<Long, SimpleUser>> groupUsers = new HashMap<>();
  private final ObjectMapper mapper = new ObjectMapper();

  private final String botToken;

  public CodeCompostInspectorBot(String botToken) {
    this.botToken = botToken;
    loadUsersFromFile();
    loadTagsFromFile();

    // шедулер для сохранения users.json раз в минуту
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(this::saveUsersToFile, 60, 60, TimeUnit.SECONDS);
  }

  @Override
  public String getBotUsername() {
    return "codeCompostInspectorBot";
  }

  @Override
  public String getBotToken() {
    return botToken;
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
      Message message = update.getMessage();
      Long chatId = message.getChatId();

      if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
        saveUser(chatId, message.getFrom());
      }

      String fullText = message.getText().trim();
      String command = fullText.split(" ")[0];

      switch (command) {
        case "/help":
          sendText(chatId, getHelpMessage());
          break;
        case "/all":
          mentionAll(chatId);
          break;
        case "/tags":
          sendText(chatId, getTagsList());
          break;
        case "/top":
          sendTop(chatId);
          break;
        case "/addtag":
          handleAddTag(chatId, fullText);
          break;
        case "/deltag":
          handleDeleteTag(chatId, fullText);
          break;
        case "/panic":
          sendText(chatId, enablePanic());
          break;
        default:
          sendText(chatId,
              "Неизвестная команда, падаван 👾! Напиши /help для списка команд.");
      }
    }
  }

  private void saveUser(Long chatId, User user) {
    groupUsers.putIfAbsent(chatId, new HashMap<>());
    Map<Long, SimpleUser> chatMap = groupUsers.get(chatId);

    SimpleUser su = chatMap.get(user.getId());
    if (su == null) {
      su = new SimpleUser(user);
      chatMap.put(user.getId(), su);
    }

    su.messageCount++; // счетчик сообщений
    usersChanged = true; // изменяем файл, если были обновления в течении шедулера (60 секунд)
  }

  private void mentionAll(Long chatId) {
    Map<Long, SimpleUser> users = groupUsers.get(chatId);

    if (users == null || users.isEmpty()) {
      sendText(chatId,
          "Пока никого не видел в чате. Напишите что-нибудь, чтобы я вас запомнил (суки)!");
      return;
    }

    StringBuilder sb = new StringBuilder("🔔 Призыв всех навозников:\n");

    for (SimpleUser user : users.values()) {
      sb.append(user.toMention()).append(" ");
    }

    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(sb.toString());
    message.enableHtml(true);

    try {
      execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private void sendText(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    message.enableHtml(true);
    try {
      execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private String getHelpMessage() {
    return """
        🤖 CompostInspectorBot 🤖
        
        🎯 Философия бота:
        "Если баг нельзя воспроизвести - значит, его нет.
        Если хэштег не добавлен - значит, это не баг, а фича.
        Если все молчат - значит, пора писать /all."
        
        📌 Доступные команды для истинных ценителей:
        /help - Справка... 😑
        /all – Поднимает всех из-под тестовых стендов (включая того, кто спит в углу). 👹💤
        /tags – Список хэштегов, которые вы все равно не используете. #опятьэтоткостыль
        /addtag #тег – Добавить хэштег, чтобы задокументировать бардак. 📌
        /deltag #тег - Удалить хештег ➖
        /top - Показать самых активных ⚔️
        /panic – Автоматически создать 10 задач в Jira
        """;
  }

  private String getTagsList() {
    if (tags.isEmpty()) {
      return "❌ Пока нет добавленных тегов.";
    }

    StringBuilder sb = new StringBuilder("🏷️ Список тегов:\n");
    for (String tag : tags) {
      sb.append(tag).append("\n");
    }
    return sb.toString();
  }

  private String enablePanic() {
    return """
        🚨 PANIC MODE ACTIVATED 🚨
        
        Создание 10 задач в Jira...
        ✅ BUG-123: "Срочно всё переделать"
        ✅ BUG-124: "Ничего не работает, но работает"
        ✅ TASK-777: "Отписаться в чат, что всё плохо"
        ✅ TASK-778: "Переоткрыть баг, который уже закрыли"
        ✅ TASK-999: "Создать ещё 5 задач"
        ✅ BUG-666: "Починить баг, который ты сам и внёс"
        ✅ TASK-001: "Притвориться, что ты в отпуске"
        ✅ TASK-002: "Созвон на 3 часа без повестки"
        ✅ TASK-003: "Удалить продакшен и бежать"
        ✅ TASK-004: "Открыть Notion и просто смотреть на него"
        """;
  }

  private void loadUsersFromFile() {
    try {
      File file = new File(STORAGE_FILE);
      if (file.exists()) {
        Map<Long, Map<Long, SimpleUser>> data = mapper.readValue(file, new TypeReference<>() {
        });
        groupUsers.putAll(data);
      }
    } catch (IOException e) {
      System.out.println("Ошибка при загрузке навозников: " + e.getMessage());
    }
  }

  private void loadTagsFromFile() {
    File file = new File(TAGS_FILE);
    if (!file.exists()) {
      return;
    }

    try (Scanner scanner = new Scanner(file)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine().trim();
        if (!line.isEmpty()) {
          tags.add(line);
        }
      }
    } catch (IOException e) {
      System.out.println("Ошибка при чтении tags.txt: " + e.getMessage());
    }
  }

  private void saveTagsToFile() {
    try (PrintWriter writer = new PrintWriter(new File(TAGS_FILE))) {
      for (String tag : tags) {
        writer.println(tag);
      }
    } catch (IOException e) {
      System.out.println("Ошибка при записи tags.txt: " + e.getMessage());
    }
  }


  private void handleAddTag(Long chatId, String fullText) {
    String[] parts = fullText.split(" ");
    if (parts.length < 2) {
      sendText(chatId, "❗ Укажи тег после команды. Пример: /addtag #важно");
      return;
    }

    String tag = parts[1].trim();
    if (!tag.startsWith("#")) {
      sendText(chatId, "❗ Тег должен начинаться с #. Пример: /addtag #вопрос");
      return;
    }

    if (tags.contains(tag)) {
      sendText(chatId, "⏳ Такой тег уже есть.");
    } else {
      tags.add(tag);
      saveTagsToFile();
      sendText(chatId, "✅ Тег " + tag + " добавлен!");
    }
  }

  private void handleDeleteTag(Long chatId, String fullText) {
    String[] parts = fullText.split(" ");
    if (parts.length < 2) {
      sendText(chatId, "❗ Укажи тег, который нужно удалить. Пример: /deltag #важно");
      return;
    }

    String tag = parts[1].trim();
    if (!tag.startsWith("#")) {
      sendText(chatId, "❗ Тег должен начинаться с #. Пример: /deltag #вопрос");
      return;
    }

    if (tags.remove(tag)) {
      saveTagsToFile();
      sendText(chatId, "🗑️ Тег " + tag + " удалён.");
    } else {
      sendText(chatId, "⚠️ Такого тега нет.");
    }
  }

  private void saveUsersToFile() {
    if (!usersChanged) {
      return; // не сохраняем если ничего не менялось
    }

    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(new File(STORAGE_FILE), groupUsers);
      usersChanged = false; // после успешного сохранения сбрасываем флаг
    } catch (IOException e) {
      System.out.println("Ошибка при сохранении навозников: " + e.getMessage());
    }
  }

  private void sendTop(Long chatId) {
    Map<Long, SimpleUser> users = groupUsers.get(chatId);
    if (users == null || users.isEmpty()) {
      sendText(chatId, "Нет данных об активности.");
      return;
    }

    List<SimpleUser> top = new ArrayList<>(users.values());
    top.sort((a, b) -> Integer.compare(b.messageCount, a.messageCount));

    StringBuilder sb = new StringBuilder("🔥 Топ активных навозников:\n");

    int limit = Math.min(10, top.size());
    for (int i = 0; i < limit; i++) {
      SimpleUser u = top.get(i);
      sb.append(i + 1).append(". ").append(u.toMention())
          .append(" — ").append(u.messageCount).append(" сообщений\n");
    }

    sendText(chatId, sb.toString());
  }


  // Простой сериализуемый пользователь
  public static class SimpleUser {

    public Long id;
    public String username;
    public String firstName;
    public String lastName;
    public int messageCount = 0; //

    public SimpleUser(User user) {
      this.id = user.getId();
      this.username = user.getUserName();
      this.firstName = user.getFirstName();
      this.lastName = user.getLastName();
    }

    public String toMention() {
      if (username != null) {
        return "@" + username;
      } else {
        String name = (firstName != null ? firstName : "??") +
            (lastName != null ? " " + lastName : "");
        return "<a href=\"tg://user?id=" + id + "\">" + name + "</a>";
      }
    }
  }
}