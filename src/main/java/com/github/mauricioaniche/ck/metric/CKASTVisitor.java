package com.github.mauricioaniche.ck.metric;

import org.eclipse.jdt.core.dom.ASTNode;

public interface CKASTVisitor {

	default <T extends ASTNode> void visit(T node) {
	}

	default <T extends ASTNode> void endVisit(T node) {
	}
}
