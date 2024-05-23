package com.github.mauricioaniche.ck.metric;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;

import com.github.mauricioaniche.ck.CKClassResult;

public class NOSI implements CKASTVisitor, ClassLevelMetric {

  private int count = 0;

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof MethodInvocation nodeT) {
      IMethodBinding binding = nodeT.resolveMethodBinding();
      if (binding != null && Modifier.isStatic(binding.getModifiers())) count++;
    }
  }

  @Override
  public void setResult(CKClassResult result) {
    result.setNosi(count);
  }
}
