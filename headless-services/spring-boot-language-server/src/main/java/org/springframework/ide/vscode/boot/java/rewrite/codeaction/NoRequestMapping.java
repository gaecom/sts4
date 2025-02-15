/*******************************************************************************
 * Copyright (c) 2022 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.rewrite.codeaction;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.spring.NoRequestMappingAnnotation;
import org.springframework.ide.vscode.boot.java.rewrite.ORCompilationUnitCache;
import org.springframework.ide.vscode.boot.java.rewrite.RewriteRefactorings;
import org.springframework.ide.vscode.commons.java.IJavaProject;
import org.springframework.ide.vscode.commons.languageserver.java.JavaProjectFinder;
import org.springframework.ide.vscode.commons.languageserver.util.SimpleLanguageServer;
import org.springframework.ide.vscode.commons.rewrite.java.ORAstUtils;
import org.springframework.ide.vscode.commons.util.text.IRegion;
import org.springframework.ide.vscode.commons.util.text.TextDocument;

public class NoRequestMapping extends NoRequestMappings {
	
	private static final String CODE_ACTION_ID = "RemoveRequestMappings";

	public NoRequestMapping(SimpleLanguageServer server, JavaProjectFinder projectFinder,
			RewriteRefactorings rewriteRefactorings, ORCompilationUnitCache orCuCache) {
		super(server, projectFinder, rewriteRefactorings, orCuCache, CODE_ACTION_ID);
	}

	@Override
	public WorkspaceEdit perform(List<?> args) {
		String docUri = (String) args.get(0);
		String matchStr = (String) args.get(1);
		
		Optional<IJavaProject> project = projectFinder.find(new TextDocumentIdentifier(docUri));
		
		if (project.isPresent()) {
			return orCuCache.withCompilationUnit(project.get(), URI.create(docUri), cu -> {
				if (cu == null) {
					throw new IllegalStateException("Cannot parse Java file: " + docUri);
				}
				MethodMatcher macther = new MethodMatcher(matchStr);
				
				return applyRecipe(ORAstUtils.nodeRecipe(new NoRequestMappingAnnotation(), t -> {
					if (t instanceof org.openrewrite.java.tree.J.MethodDeclaration) {
						org.openrewrite.java.tree.J.MethodDeclaration m = (org.openrewrite.java.tree.J.MethodDeclaration) t;
						return macther.matches(m.getMethodType());
					}
					return false;
				}), project.get(), List.of(cu));
			});
		}
		return null;
	}

	@Override
	protected List<Either<Command, CodeAction>> provideCodeActions(CodeActionContext context, TextDocument doc, IRegion region, IJavaProject project,
			CompilationUnit cu, ASTNode node) {
		return findAppropriateMethodDeclaration(node).map(method -> {
			String methodMatcher = "* " + method.getName().getIdentifier() + "(*)";
			IMethodBinding methodBinding = method.resolveBinding();
			if (methodBinding != null) {
				StringBuilder sb = new StringBuilder(methodBinding.getDeclaringClass().getQualifiedName());
				sb.append(' ');
				sb.append(methodBinding.getName());
				sb.append('(');
				sb.append(Arrays.stream(methodBinding.getParameterTypes()).map(b -> {
					if (b.isParameterizedType() ) {
						return b.getErasure().getQualifiedName();
					}
					return b.getQualifiedName();
				}).collect(Collectors.joining(","))); 
				sb.append(')');
				methodMatcher = sb.toString();
			}
			return createCodeAction(CodeActionKind.Refactor, "Replace single @RequestMapping with @GetMapping etc.", List.of(
					doc.getId().getUri(),
					methodMatcher
			));
		}).map(ca -> List.of(Either.<Command, CodeAction>forRight(ca))).orElse(Collections.emptyList());
	}

	

}
