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
package org.springframework.ide.vscode.boot.app;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ide.vscode.boot.java.reconcilers.AutowiredConstructorReconciler;
import org.springframework.ide.vscode.boot.java.reconcilers.BeanMethodNotPublicReconciler;
import org.springframework.ide.vscode.boot.java.rewrite.ORCompilationUnitCache;
import org.springframework.ide.vscode.boot.java.rewrite.RewriteRecipeRepository;
import org.springframework.ide.vscode.boot.java.rewrite.RewriteRefactorings;
import org.springframework.ide.vscode.boot.java.rewrite.codeaction.BeanMethodsNoPublicCodeAction;
import org.springframework.ide.vscode.boot.java.rewrite.codeaction.ConvertAutowiredField;
import org.springframework.ide.vscode.boot.java.rewrite.codeaction.NoRequestMapping;
import org.springframework.ide.vscode.boot.java.rewrite.codeaction.NoRequestMappings;
import org.springframework.ide.vscode.boot.java.rewrite.codeaction.UnnecessarySpringExtensionCodeAction;
import org.springframework.ide.vscode.boot.java.rewrite.quickfix.AutowiredConstructorQuickFixHandler;
import org.springframework.ide.vscode.boot.java.rewrite.quickfix.BeanMethodNoPublicQuickFixHandler;
import org.springframework.ide.vscode.commons.languageserver.java.JavaProjectFinder;
import org.springframework.ide.vscode.commons.languageserver.quickfix.QuickfixRegistry;
import org.springframework.ide.vscode.commons.languageserver.util.SimpleLanguageServer;

@Configuration
public class RewriteConfig implements InitializingBean {

	@Autowired
	private SimpleLanguageServer server;

	@Autowired
	private JavaProjectFinder projectFinder;
	
	@Autowired
	private ORCompilationUnitCache orCuCache;
	
	@Bean
	ConvertAutowiredField convertAutowiredField(SimpleLanguageServer server, JavaProjectFinder projectFinder,
			RewriteRefactorings rewriteRefactorings, RewriteRecipeRepository recipesRepo,
			ORCompilationUnitCache orCuCache) {
		return new ConvertAutowiredField(server, projectFinder, rewriteRefactorings, orCuCache);
	}
	
	@ConditionalOnClass({org.openrewrite.java.spring.NoRequestMappingAnnotation.class})
	@Bean
	NoRequestMapping noRequestMapping(SimpleLanguageServer server, JavaProjectFinder projectFinder,
			RewriteRefactorings rewriteRefactorings, RewriteRecipeRepository recipesRepo,
			ORCompilationUnitCache orCuCache) {
		return new NoRequestMapping(server, projectFinder, rewriteRefactorings, orCuCache);
	}

	@ConditionalOnClass({org.openrewrite.java.spring.NoRequestMappingAnnotation.class})
	@Bean
	NoRequestMappings noRequestMappings(SimpleLanguageServer server, JavaProjectFinder projectFinder,
			RewriteRefactorings rewriteRefactorings, RewriteRecipeRepository recipesRepo,
			ORCompilationUnitCache orCuCache) {
		return new NoRequestMappings(server, projectFinder, rewriteRefactorings, orCuCache);
	}

	@ConditionalOnClass({org.openrewrite.java.spring.BeanMethodsNotPublic.class})
	@Bean
	BeanMethodsNoPublicCodeAction beanMethodsNoPublic(SimpleLanguageServer server, JavaProjectFinder projectFinder,
			RewriteRefactorings rewriteRefactorings, RewriteRecipeRepository recipesRepo,
			ORCompilationUnitCache orCuCache) {
		return new BeanMethodsNoPublicCodeAction(server, projectFinder, rewriteRefactorings, orCuCache);
	}
	
	@ConditionalOnClass({org.openrewrite.java.spring.boot2.UnnecessarySpringExtension.class})
	@Bean
	UnnecessarySpringExtensionCodeAction unnecessarySpringExtension(SimpleLanguageServer server, JavaProjectFinder projectFinder,
			RewriteRefactorings rewriteRefactorings, RewriteRecipeRepository recipesRepo,
			ORCompilationUnitCache orCuCache) {
		return new UnnecessarySpringExtensionCodeAction(server, projectFinder, rewriteRefactorings, orCuCache);
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		QuickfixRegistry registry = server.getQuickfixRegistry();
		
		registry.register(AutowiredConstructorReconciler.REMOVE_UNNECESSARY_AUTOWIRED_FROM_CONSTRUCTOR, new AutowiredConstructorQuickFixHandler(server, projectFinder, orCuCache));
		registry.register(BeanMethodNotPublicReconciler.REMOVE_PUBLIC_FROM_BEAN_METHOD, new BeanMethodNoPublicQuickFixHandler(server, projectFinder, orCuCache));
		
	}

}
