package com.github.mauricioaniche.ck.metric;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.github.mauricioaniche.ck.CKMethodResult;

public class MethodLevelFieldUsageCount
    implements CKASTVisitor, MethodLevelMetric, VariableOrFieldMetric {
  private final Set<String> declaredFields;
  private final Map<String, Integer> occurrences;

  private final Set<String> variables;
  private boolean isFieldAccess;
  private boolean isQualifiedName;

  public MethodLevelFieldUsageCount() {
    declaredFields = new HashSet<>();
    this.occurrences = new HashMap<>();
    this.variables = new HashSet<>();
  }

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof MethodDeclaration nodeT) internalVisit(nodeT);
    else if (node instanceof VariableDeclarationFragment nodeT) internalVisit(nodeT);
    else if (node instanceof SimpleName nodeT) internalVisit(nodeT);
    else if (node instanceof FieldAccess) isFieldAccess = true;
    else if (node instanceof QualifiedName) isQualifiedName = true;
  }

  @Override
  public <T extends ASTNode> void endVisit(T node) {
    if (node instanceof FieldAccess) isFieldAccess = false;
    else if (node instanceof QualifiedName) isQualifiedName = false;
  }

  void internalVisit(MethodDeclaration node) {
    IMethodBinding binding = node.resolveBinding();
    if (binding == null) return;

    IVariableBinding[] fields = binding.getDeclaringClass().getDeclaredFields();

    for (IVariableBinding field : fields) {
      declaredFields.add(field.getName());
    }
  }

  void internalVisit(VariableDeclarationFragment node) {
    variables.add(node.getName().toString());
  }

  private void plusOne(String var) {
    if (!occurrences.containsKey(var)) occurrences.put(var, 0);
    occurrences.put(var, occurrences.get(var) + 1);
  }

  void internalVisit(SimpleName node) {
    String variableName = node.getIdentifier();

    boolean accessFieldUsingThis = isFieldAccess && declaredFields.contains(variableName);
    boolean accessFieldUsingOnlyVariableName =
        !isFieldAccess
            && declaredFields.contains(variableName)
            && !variables.contains(variableName);
    if ((accessFieldUsingThis || accessFieldUsingOnlyVariableName) && !isQualifiedName) {
      plusOne(variableName);
    }
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setFieldUsage(occurrences);
  }
}
