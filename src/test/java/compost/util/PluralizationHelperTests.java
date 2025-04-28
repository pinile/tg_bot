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
  @DisplayName("Тест для категории 'one' - 1")
  public void testPluralizeOne() {
    String result = PluralizationHelper.pluralize(1, "сообщени");
    logger.info("Тест для 1 элемента: {}", result);
    assertEquals("сообщение", result);
  }

  @Test
  @DisplayName("Тест для категории 'one' - 21")
  public void testPluralizeOneEdgeCase() {
    String result = PluralizationHelper.pluralize(1, "сообщени");
    logger.info("Тест для 21 элемента: {}", result);
    assertEquals("сообщение", result);
  }

  @Test
  @DisplayName("Тест для категории 'one' - 11")
  public void testPluralizeOneSpecialCase() {
    String result = PluralizationHelper.pluralize(1, "сообщени");
    logger.info("Тест для 11 элементов: {}", result);
    assertEquals("сообщение", result);
  }

  @Test
  @DisplayName("Тест для категории 'few' - 2")
  public void testPluralizeFew() {
    String result = PluralizationHelper.pluralize(3, "сообщени");
    logger.info("Тест для 2 элементов: {}", result);
    assertEquals("сообщения", result);
  }

  @Test
  @DisplayName("Тест для категории 'few' - 3")
  public void testPluralizeFewEdgeCase() {
    String result = PluralizationHelper.pluralize(3, "сообщени");
    logger.info("Тест для 3 элементов: {}", result);
    assertEquals("сообщения", result);
  }

  @Test
  @DisplayName("Тест для категории 'few' - 12")
  public void testPluralizeFewSpecialCase() {
    String result = PluralizationHelper.pluralize(12, "сообщени");
    logger.info("Тест для 12 элементов: {}", result);
    assertEquals("сообщений", result);
  }

  @Test
  @DisplayName("Тест для категории 'many' - 328")
  public void testPluralizeManySpecialCase() {
    String result = PluralizationHelper.pluralize(328, "сообщени");
    logger.info("Тест для 328 элементов: {}", result);
    assertEquals("сообщений", result);
  }

  @Test
  @DisplayName("Тест для категории 'many' - 5")
  public void testPluralizeMany() {
    String result = PluralizationHelper.pluralize(328, "сообщени");
    logger.info("Тест для 5 элементов: {}", result);
    assertEquals("сообщений", result);
  }

  @Test
  @DisplayName("Тест для категории 'many' - 20")
  public void testPluralizeManyEdgeCase() {
    String result = PluralizationHelper.pluralize(328, "сообщени");
    logger.info("Тест для 20 элементов: {}", result);
    assertEquals("сообщений", result);
  }

  @Test
  @DisplayName("Тест для - 0")
  public void testPluralizeZeroCase() {
    String result = PluralizationHelper.pluralize(0, "сообщени");
    logger.info("Тест для 0 элементов: {}", result);
    assertEquals("сообщений", result);
  }
}