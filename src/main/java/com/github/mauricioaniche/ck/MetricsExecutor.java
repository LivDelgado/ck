package com.github.mauricioaniche.ck;

import com.github.mauricioaniche.ck.metric.ClassLevelMetric;
import com.github.mauricioaniche.ck.metric.MethodLevelMetric;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

public class MetricsExecutor extends FileASTRequestor {

  private final Callable<List<ClassLevelMetric>> classLevelMetrics;
  private final Callable<List<MethodLevelMetric>> methodLevelMetrics;
  private final CKNotifier notifier;

  private static final Logger log = Logger.getLogger(MetricsExecutor.class);

  public MetricsExecutor(
      Callable<List<ClassLevelMetric>> classLevelMetrics,
      Callable<List<MethodLevelMetric>> methodLevelMetrics,
      CKNotifier notifier) {
    this.classLevelMetrics = classLevelMetrics;
    this.methodLevelMetrics = methodLevelMetrics;
    this.notifier = notifier;
  }

  @Override
  public void acceptAST(String sourceFilePath, CompilationUnit cu) {

    try {
      log.info("Processing: " + sourceFilePath);
      CKVisitor visitor = new CKVisitor(sourceFilePath, cu, classLevelMetrics, methodLevelMetrics);

      cu.accept(visitor);
      Set<CKClassResult> collectedClasses = visitor.getCollectedClasses();

      for (CKClassResult collectedClass : collectedClasses) {
        log.info(collectedClass);
        notifier.notify(collectedClass);
      }
    } catch (Exception e) {
      log.error("error in " + sourceFilePath, e);
      notifier.notifyError(sourceFilePath, e);
    }
  }
}
