package com.github.mauricioaniche.ck.metric;

import com.github.mauricioaniche.ck.CKMethodResult;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class VariableOrParameterUsageCount
    implements CKASTVisitor, MethodLevelMetric, VariableOrFieldMetric {
  private final Set<String> declaredVariables;
  private final Map<String, Integer> occurrences;

  public VariableOrParameterUsageCount() {
    declaredVariables = new HashSet<>();
    this.occurrences = new HashMap<>();
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setVariablesUsage(occurrences);
  }

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof SimpleName nodeT) internalVisit(nodeT);
    else if (node instanceof VariableDeclaration nodeT)
      declaredVariables.add(nodeT.getName().toString());
  }

  private void internalVisit(SimpleName node) {
    if (declaredVariables.contains(node.toString())) {
      String var = node.getIdentifier();
      if (!occurrences.containsKey(var)) occurrences.put(var, -1);

      occurrences.put(var, occurrences.get(var) + 1);
    }
  }
}
