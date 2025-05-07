package compost.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Проверка метода pluralize, склонение слова \"сообщение\"")
@TestInstance(Lifecycle.PER_CLASS)
public class PluralizationHelperTests {

  private static final Logger logger = LogManager.getLogger(PluralizationHelperTests.class);

  static Stream<Arguments> providePluralizeTestCases() {
    String baseWord = "сообщени";

    return Stream.of(
        Arguments.arguments("one (1)", 1, baseWord, "сообщение"),
        Arguments.arguments("one (21)", 21, baseWord, "сообщение"),
        Arguments.arguments("one (11)", 11, baseWord, "сообщений"),
        Arguments.arguments("few (2)", 2, baseWord, "сообщения"),
        Arguments.arguments("few (3)", 3, baseWord, "сообщения"),
        Arguments.arguments("few (12)", 12, baseWord, "сообщений"),
        Arguments.arguments("many (5)", 5, baseWord, "сообщений"),
        Arguments.arguments("many (20)", 20, baseWord, "сообщений"),
        Arguments.arguments("many (328)", 328, baseWord, "сообщений"),
        Arguments.arguments("zero (0)", 0, baseWord, "сообщений")
    );
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("providePluralizeTestCases")
  void testPluralize(
      String description,
      int count,
      String base,
      String expected
  ) {
    String result = PluralizationHelper.pluralize(count, base);
    logger.info("Тест: {} -> pluralize({}, '{}') = {}", description, count, base, result);
    assertEquals(expected, result);
  }
}