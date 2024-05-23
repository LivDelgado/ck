package com.github.mauricioaniche.ck.metric;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;

import com.github.mauricioaniche.ck.CKClassResult;
import static com.github.mauricioaniche.ck.util.JDTUtils.getVariableName;

public class NumberOfFields implements CKASTVisitor, ClassLevelMetric {

	private final Set<String> fieldNames = new HashSet<>();
	private int fields;
	private int staticFields;
	private int publicFields;
	private int privateFields;
	private int protectedFields;
	private int defaultFields;
	private int finalFields;
	private int synchronizedFields;

	@Override
	public <T extends ASTNode> void visit(T node) {
		if (node instanceof FieldDeclaration nodeT) {
			fields++;
			fieldNames.addAll(getVariableName(nodeT.fragments()));
	
			boolean isPublic = Modifier.isPublic(nodeT.getModifiers());
			boolean isPrivate = Modifier.isPrivate(nodeT.getModifiers());
			boolean isProtected = Modifier.isProtected(nodeT.getModifiers());
	
			if(isPublic)
				publicFields++;
			else if(isPrivate)
				privateFields++;
			else if(isProtected)
				protectedFields++;
			else
				defaultFields++;
	
			// other characteristics rather than visibility
			boolean isStatic = Modifier.isStatic(nodeT.getModifiers());
			boolean isFinal = Modifier.isFinal(nodeT.getModifiers());
			boolean isSynchronized = Modifier.isSynchronized(nodeT.getModifiers());
			
			if(isStatic)
				staticFields++;
	
			if(isFinal)
				finalFields++;
	
			if(isSynchronized)
				synchronizedFields++;
		}
	}
  
	@Override
	public void setResult(CKClassResult result) {
		result.setNumberOfFields(fields);
		result.setFieldNames(fieldNames);
		result.setNumberOfStaticFields(staticFields);
		result.setNumberOfPublicFields(publicFields);
		result.setNumberOfPrivateFields(privateFields);
		result.setNumberOfProtectedFields(protectedFields);
		result.setNumberOfDefaultFields(defaultFields);
		result.setNumberOfFinalFields(finalFields);
		result.setNumberOfSynchronizedFields(synchronizedFields);
	}
}