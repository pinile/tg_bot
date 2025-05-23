package compost.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import compost.model.SimpleUser;
import compost.storage.MongoUserRepository.RankedUser;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Log4j2
@DisplayName("Проверка класса MessageBuilder.")
public class MessageBuilderTests {

  private SimpleUser user(Long id, String name) {
    return new SimpleUser(id, name, "Имя", "Фамилия");
  }

  @Test
  @DisplayName("Проверка отображения топ пользователей, метод topUsers")
  public void testTopUsersWithMedals() {
    List<RankedUser> users =
        List.of(
            new RankedUser(user(1L, "user1"), 44, 1),
            new RankedUser(user(2L, "user2"), 33, 2),
            new RankedUser(user(3L, "user3"), 22, 3),
            new RankedUser(user(4L, "user4"), 11, 4),
            new RankedUser(user(4L, "user5"), 11, 5));
    log.info("──────────────────────────────────────────");
    log.info("input: '{}'", users);

    String result = MessageBuilder.topUsers(users);

    log.info("ФР: {}\n", result);

    assertTrue(result.contains("🥇 @user1 - 44 сообщения"));
    assertTrue(result.contains("🥈 @user2 - 33 сообщения"));
    assertTrue(result.contains("🥉 @user3 - 22 сообщения"));
    assertTrue(result.contains("4. @user4 - 11 сообщений"));
    assertTrue(result.contains("5. @user5 - 11 сообщений"));
  }

  @Test
  @DisplayName("Проверка отображения топ пользователей (пустой), метод topUsers")
  public void testTopUsersEmpty() {
    String result = MessageBuilder.topUsers(List.of());
    log.info("──────────────────────────────────────────");
    log.info("ФР: {}\n", result);

    assertTrue(result.contains(MessageBuilder.noActiveUser()));
  }
}
