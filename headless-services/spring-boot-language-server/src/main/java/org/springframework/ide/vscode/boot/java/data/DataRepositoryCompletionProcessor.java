/*******************************************************************************
 * Copyright (c) 2018 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.lsp4j.CompletionItemKind;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Annotation;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.FullyQualified;
import org.openrewrite.java.tree.JavaType.Parameterized;
import org.openrewrite.java.tree.TypeUtils;
import org.springframework.ide.vscode.boot.java.handlers.CompletionProvider;
import org.springframework.ide.vscode.boot.java.utils.ORAstUtils;
import org.springframework.ide.vscode.commons.languageserver.completion.DocumentEdits;
import org.springframework.ide.vscode.commons.languageserver.completion.ICompletionProposal;
import org.springframework.ide.vscode.commons.util.BadLocationException;
import org.springframework.ide.vscode.commons.util.text.IDocument;
import org.springframework.ide.vscode.commons.util.text.IRegion;
import org.springframework.util.StringUtils;

/**
 * @author Martin Lippert
 */
public class DataRepositoryCompletionProcessor implements CompletionProvider {

	@Override
	public void provideCompletions(J node, Annotation annotation,
			int offset, IDocument doc, Collection<ICompletionProposal> completions) {
	}

	@Override
	public void provideCompletions(J node, int offset, IDocument doc, Collection<ICompletionProposal> completions) {
		ClassDeclaration declaration = ORAstUtils.findNode(node, ClassDeclaration.class);
		DataRepositoryDefinition repo = getDataRepositoryDefinition(declaration, declaration.getType());
		if (repo != null) {
			DomainType domainType = repo.getDomainType();
			if (domainType != null) {

				String prefix = "";
				try {
					IRegion line = doc.getLineInformationOfOffset(offset);
					prefix = doc.get(line.getOffset(), offset - line.getOffset()).trim();
				} catch (BadLocationException e) {
					// ignore if there is a problem computing the prefix, continue without prefix
				}

				DomainProperty[] properties = domainType.getProperties();
				for (DomainProperty property : properties) {
					completions.add(generateCompletionProposal(offset, prefix, repo, property));
				}
			}
		}
	}

	protected ICompletionProposal generateCompletionProposal(int offset, String prefix, DataRepositoryDefinition repoDef, DomainProperty domainProperty) {
		StringBuilder label = new StringBuilder();
		label.append("findBy");
		label.append(StringUtils.capitalize(domainProperty.getName()));
		label.append("(");
		label.append(domainProperty.getType().getSimpleName());
		label.append(" ");
		label.append(StringUtils.uncapitalize(domainProperty.getName()));
		label.append(");");

		DocumentEdits edits = new DocumentEdits(null, false);

		StringBuilder completion = new StringBuilder();
		completion.append("List<");
		completion.append(repoDef.getDomainType().getSimpleName());
		completion.append("> findBy");
		completion.append(StringUtils.capitalize(domainProperty.getName()));
		completion.append("(");
		completion.append(domainProperty.getType().getSimpleName());
		completion.append(" ");
		completion.append(StringUtils.uncapitalize(domainProperty.getName()));
		completion.append(");");

		String filter = label.toString();
		if (prefix != null && label.toString().startsWith(prefix)) {
			edits.replace(offset - prefix.length(), offset, completion.toString());
		}
		else if (prefix != null && completion.toString().startsWith(prefix)) {
			edits.replace(offset - prefix.length(), offset, completion.toString());
			filter = completion.toString();
		}
		else {
			edits.insert(offset, completion.toString());
		}

		DocumentEdits additionalEdits = new DocumentEdits(null, false);
		return new FindByCompletionProposal(label.toString(), CompletionItemKind.Method, edits, null, null, Optional.of(additionalEdits), filter);
	}

	private DataRepositoryDefinition getDataRepositoryDefinition(ClassDeclaration declaration, FullyQualified type) {
		if (type != null && TypeUtils.isAssignableTo(Constants.REPOSITORY_TYPE, type)) {
			JavaType domainType = getFirstParameterType(type);
			return new DataRepositoryDefinition(domainType instanceof FullyQualified ? new DomainType((FullyQualified) domainType) : null);
		}
		return null;
	}
	
	private JavaType getFirstParameterType(FullyQualified type) {
		if (type instanceof Parameterized) {
			List<JavaType> params = ((Parameterized) type).getTypeParameters();
			if (!params.isEmpty()) {
				return params.get(0);
			}
		}
		for (FullyQualified i : type.getInterfaces()) {
			JavaType parameType = getFirstParameterType(i);
			if (parameType != null) {
				return parameType;
			}
		}
		@Nullable
		FullyQualified superclass = type.getSupertype();
		if (superclass != null) {
			return getFirstParameterType(superclass);
		}
		return null;
	}

}
