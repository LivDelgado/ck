package com.github.mauricioaniche.ck.metric;

import com.github.mauricioaniche.ck.CKClassResult;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class LCOM implements CKASTVisitor, ClassLevelMetric {

	ArrayList<TreeSet<String>> methods = new ArrayList<>();
	Set<String> declaredFields;
	
	public LCOM() { this.declaredFields = new HashSet<>(); }
	
	@Override
	public <T extends ASTNode> void visit(T node) {
		if (node instanceof FieldDeclaration nodeT) internalVisit(nodeT);
		else if (node instanceof SimpleName nodeT) internalVisit(nodeT);
		else if (node instanceof MethodDeclaration nodeT) internalVisit(nodeT);
	}
  
	void internalVisit(FieldDeclaration node) {
		for(Object o : node.fragments()) {
			VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
			declaredFields.add(vdf.getName().toString());
		}
	}
	
	void internalVisit(SimpleName node) {
		String name = node.getFullyQualifiedName();
		if(declaredFields.contains(name)) acessed(name);	
	}

	private void acessed(String name) {
		if(!methods.isEmpty()){
			methods.get(methods.size() - 1).add(name);
		}
	}
	
	void internalVisit(MethodDeclaration node) { methods.add(new TreeSet<>()); }
	
	@Override
	public void setResult(CKClassResult result) {
		
		/*
		 * LCOM = |P| - |Q| if |P| - |Q| > 0
		 * where
		 * P = set of all empty set intersections
		 * Q = set of all nonempty set intersections
		 */
		
		// extracted from https://github.com/dspinellis/ckjm
		int lcom = 0;
		for (int i = 0; i < methods.size(); i++)
		    for (int j = i + 1; j < methods.size(); j++) {
		    	
				TreeSet<?> intersection = (TreeSet<?>)methods.get(i).clone();
				intersection.retainAll(methods.get(j));
				if (intersection.isEmpty()) lcom++;
				else lcom--;
		    }
		result.setLcom(lcom > 0 ? lcom : 0);
	}

}
