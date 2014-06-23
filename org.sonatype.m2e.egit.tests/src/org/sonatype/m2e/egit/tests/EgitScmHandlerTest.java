/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.egit.tests;

import java.io.File;

import org.eclipse.ui.PlatformUI;
import org.sonatype.m2e.egit.internal.EgitScmHandler;


/**
 * Tests the proper integration of the EGit SCM handler.
 */
@SuppressWarnings("restriction")
public class EgitScmHandlerTest extends AbstractScmHandlerTest {

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // NOTE: The EGit resource decorator opens pack files which interferes with the workspace cleanup
    PlatformUI.getWorkbench().getDecoratorManager()
        .setEnabled("org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator", false);
  }

  public void testCheckout() throws Exception {
    checkout(EgitScmHandler.GIT_SCM_ID + "file://" + new File("resources/git/simple").toURI().getPath());
    waitForJobsToComplete();

    assertWorkspaceProject("git-test");
  }

  public void testCheckoutNoMaster() throws Exception {
    checkout(EgitScmHandler.GIT_SCM_ID + "file://" + new File("resources/git/nomaster").toURI().getPath());
    waitForJobsToComplete();

    assertWorkspaceProject("git-test");
  }
}
