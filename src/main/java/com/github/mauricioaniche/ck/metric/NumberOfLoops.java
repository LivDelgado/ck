package com.github.mauricioaniche.ck.metric;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;

public class NumberOfLoops implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {

  private int qty = 0;

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof EnhancedForStatement
        || node instanceof DoStatement
        || node instanceof WhileStatement
        || node instanceof ForStatement) qty++;
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setLoopQty(qty);
  }

  @Override
  public void setResult(CKClassResult result) {
    result.setLoopQty(qty);
  }
}
