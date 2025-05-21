package compost.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class CommandLoggingAspect {

  @Around("@annotation(compost.annotation.LoggableCommand)")
  public Object logCommandExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    String methodName = joinPoint.getSignature().getName();
    Object[] args = joinPoint.getArgs();

    log.info("Вызов команды: {} с аргументами: {}", methodName, args);

    try {
      Object result = joinPoint.proceed();
      log.info("Команда {} успешно выполнена", methodName);
      return result;
    } catch (Throwable e) {
      log.error("Ошибка при выполнении команды {}: {}", methodName, e.getMessage(), e);
      throw e;
    }
  }
}

