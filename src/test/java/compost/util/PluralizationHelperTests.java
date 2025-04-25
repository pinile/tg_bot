package compost.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Проверка метода pluralize, склонение слова \"сообщение\"")
public class PluralizationHelperTests {

  private static final Logger logger = LogManager.getLogger(PluralizationHelperTests.class);

  @Test
  @DisplayName("Тест для 1 элемента")
  public void testPluralizeOneItem() {
    String result = PluralizationHelper.pluralize(1, "сообщени");
    logger.info("Тест для 1 элемента: {}", result);
    assertEquals("сообщение", result);
  }

  @Test
  @DisplayName("Тест для 3 элементов")
  public void testPluralizeFewItem() {
    String result = PluralizationHelper.pluralize(3, "сообщени");
    logger.info("Тест для 3 элементов: {}", result);
    assertEquals("сообщения", result);
  }

  @Test
  @DisplayName("Тест для 'many' элементов")
  public void testPluralizeManyItem() {
    String result = PluralizationHelper.pluralize(328, "сообщени");
    logger.info("Тест для 'many' элементов: {}", result);
    assertEquals("сообщений", result);
  }
}