package com.github.mauricioaniche.ck.metric;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.github.mauricioaniche.ck.CKClassResult;

public class NOC implements CKASTVisitor, ClassLevelMetric {

  private String name;
  private final NOCExtras extras;

  public NOC() {
    this.extras = NOCExtras.getInstance();
  }

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof TypeDeclaration nodeT) {
      ITypeBinding binding = nodeT.resolveBinding();

      if (binding != null) {
        this.name = binding.getQualifiedName();
        ITypeBinding father = binding.getSuperclass();
        if (father != null) this.extras.plusOne(father.getQualifiedName());
        return;
      }

      this.name = nodeT.getName().getFullyQualifiedName();

      SimpleType castedFatherType = null;

      if (nodeT.getSuperclassType() instanceof SimpleType simpleType)
        castedFatherType = simpleType;

      if (castedFatherType != null) {
        this.extras.plusOne(castedFatherType.getName().getFullyQualifiedName());
      }
      
	  List<Type> list = nodeT.superInterfaceTypes();
      list = list.stream().filter(x -> (x instanceof SimpleType)).collect(Collectors.toList());
      list.stream()
          .map(x -> (SimpleType) x)
          .forEach(x -> this.extras.plusOne(x.getName().getFullyQualifiedName()));
    }
  }

  @Override
  public void setResult(CKClassResult result) {}
}
