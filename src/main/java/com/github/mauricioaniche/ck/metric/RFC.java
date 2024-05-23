package com.github.mauricioaniche.ck.metric;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.mauricioaniche.ck.util.JDTUtils;
import java.util.HashSet;
import org.eclipse.jdt.core.dom.*;

public class RFC implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {
  private final HashSet<String> methodInvocations = new HashSet<>();

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof MethodInvocation nodeT) internalVisit(nodeT);
    if (node instanceof SuperMethodInvocation nodeT) internalVisit(nodeT);
  }

  private void internalVisit(MethodInvocation node) {
    String methodName = JDTUtils.getQualifiedMethodFullName(node);
    methodInvocations.add(methodName);
  }

  private void internalVisit(SuperMethodInvocation node) {
    String methodName = JDTUtils.getQualifiedMethodFullName(node);
    methodInvocations.add(methodName);
  }

  @Override
  public void setResult(CKClassResult result) {
    result.setRfc(methodInvocations.size());
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setRfc(methodInvocations.size());
    result.setMethodInvocations(methodInvocations);
  }
}
