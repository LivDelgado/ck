package com.github.mauricioaniche.ck;

import org.eclipse.jdt.core.dom.ASTNode;

import com.github.mauricioaniche.ck.metric.CKASTVisitor;
import com.github.mauricioaniche.ck.metric.ClassLevelMetric;

public class ASTDebugger implements CKASTVisitor, ClassLevelMetric {

  @Override
  public <T extends ASTNode> void visit(T node) {
    System.out.println("-- " + node.getClass().getSimpleName());
    System.out.println(node.toString());
  }

  @Override
  public void setResult(CKClassResult result) {}
}
