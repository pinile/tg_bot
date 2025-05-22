package compost.service;

import compost.model.SimpleUser;
import compost.storage.MongoUserRepository.RankedUser;
import compost.storage.UserRepository;
import compost.util.MessageBuilder;
import java.util.Collection;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

/**
 * Сервис для управления пользователями.
 */
@Service
@Log4j2
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Метод для обработки пользователя: сохраняет его в репозитории и, при необходимости, увеличивает
   * счетчик сообщений.
   *
   * @param chatId                Идентификатор чата.
   * @param telegramUser          Пользователь Telegram.
   * @param incrementMessageCount Флаг, указывающий, нужно ли увеличивать счетчик сообщений.
   */
  public void handleUser(Long chatId, User telegramUser, boolean incrementMessageCount) {
    try {
      userRepository.upsertUser(chatId, telegramUser, incrementMessageCount);
    } catch (Exception e) {
      log.error("Ошибка в UserService.handleUser: ", e);
    }

  }

  /**
   * Метод возвращает всех пользователей из указанного чата.
   *
   * @param chatId Идентификатор чата.
   * @return Коллекция пользователей, связанных с указанным чатом.
   */
  public Collection<SimpleUser> getAllUsers(Long chatId) {
    return userRepository.getAllUsers(chatId);
  }

  /**
   * Метод возвращает пользователя по указанному идентификаторам чата и пользователя.
   *
   * @param chatId Идентификатор чата.
   * @param userId Идентификатор пользователя.
   * @return Объект SimpleUser, представляющий пользователя, или null, если пользователь не найден.
   */
  public SimpleUser getUser(Long chatId, Long userId) {
    return userRepository.getUser(chatId, userId);
  }

  /**
   * Метод возвращает 10 пользователей с самым большим количеством сообщений в группе по убыванию
   *
   * @param chatId Идентификатор чата.
   * @param limit  Лимит пользователей (10).
   * @return Список объектов {@link SimpleUser}, отсортированный по убыванию количества сообщений.
   */
  public List<RankedUser> getTopUsers(Long chatId, int limit) {
    return userRepository.getTopUsers(chatId, limit);
  }

  /**
   * Строит сообщение с упоминанием всех пользователей чата.
   * <p>
   * Извлекает всех пользователей для указанного чата и формирует строку с упоминаниями. Если
   * пользователи не найдены, возвращает сообщение о пустом списке.
   *
   * @param chatId идентификатор чата, из которого нужно получить список пользователей
   * @return строка с упоминаниями всех пользователей или сообщение об отсутствии пользователей
   */
  public String buildMentionAllMessage(Long chatId) {
    Collection<SimpleUser> users = getAllUsers(chatId);
    if (users == null || users.isEmpty()) {
      return MessageBuilder.noUsersInChat();
    }
    return MessageBuilder.mentionAll(users);
  }
}
