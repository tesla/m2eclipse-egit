/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.egit.tests;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
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

  public void testNormalizeURI() throws Exception {
    EgitScmHandlerExt handler = new EgitScmHandlerExt();

    String uri = EgitScmHandler.GIT_SCM_ID + "git@github.com:errai/errai.git/errai-common";

    assertEquals("ssh://git@github.com/errai/errai.git", handler.normalizeUri(uri, false));
    assertEquals("git://github.com/errai/errai.git", handler.normalizeUri(uri, true));
  }

  /**
   * Authentication success is mocked.
   * Expected:
   * 	- no exception,
   * 	- EgitScmHandler.runCloneOperation() is called,
   * 	- EgitScmHandler.onAuthFailed() is not called.
   */
  public void testAuthFailed1() throws Exception {
    EgitScmHandlerExt handler = new EgitScmHandlerExt();
    String url = EgitScmHandlerExt.GIT_SCM_ID + "git@github.com:errai/errai.git/errai-common";
    MavenProjectScmInfo scmInfo = new MavenProjectScmInfo(url, null, null, "HEAD", url, url);
    CoreException exc = null;
    try {
      handler.checkoutProject(scmInfo, null, new NullProgressMonitor());
    } catch (CoreException e) {
    	exc = e;
    }
    assertNull(exc);
    assertEquals(1, handler.callsOfrunCloneOperation);
    assertEquals(0, handler.callsOfonAuthFailed);
  }

  /**
   * Authentication failure is mocked, user selects cancel to connect anonymously.
   * Expected:
   * 	- no exception,
   * 	- EgitScmHandler.runCloneOperation() is called,
   * 	- EgitScmHandler.onAuthFailed() is called.
   */
  public void testAuthFailed2() throws Exception {
    EgitScmHandlerExt handler = new EgitScmHandlerExt(true, false);
    String url = EgitScmHandlerExt.GIT_SCM_ID + "git@github.com:errai/errai.git/errai-common";
    MavenProjectScmInfo scmInfo = new MavenProjectScmInfo(url, null, null, "HEAD", url, url);
    CoreException exc = null;
    try {
      handler.checkoutProject(scmInfo, null, new NullProgressMonitor());
    } catch (CoreException e) {
      exc = e;
    }
    assertNull(exc);
    assertEquals(1, handler.callsOfrunCloneOperation);
    assertEquals(1, handler.callsOfonAuthFailed);
  }

  /**
   * Authentication failure is mocked, user selects ok to connect anonymously.
   * Expected:
   * 	- no exception,
   * 	- EgitScmHandler.runCloneOperation() is called twice,
   * 	- EgitScmHandler.onAuthFailed() is called.
   */
  public void testAuthFailed3() throws Exception {
    EgitScmHandlerExt handler = new EgitScmHandlerExt(true, true);
    String url = EgitScmHandlerExt.GIT_SCM_ID + "git@github.com:errai/errai.git/errai-common";
    MavenProjectScmInfo scmInfo = new MavenProjectScmInfo(url, null, null, "HEAD", url, url);
    CoreException exc = null;
    try {
      handler.checkoutProject(scmInfo, null, new NullProgressMonitor());
    } catch (CoreException e) {
      exc = e;
	}
	assertNull(exc);
	assertEquals(2, handler.callsOfrunCloneOperation);
	assertEquals(1, handler.callsOfonAuthFailed);
  }

  class EgitScmHandlerExt extends EgitScmHandler {
    boolean throwAuthFailed = false;
    boolean accessAnonymously = false;
    
    int callsOfonAuthFailed = 0;
    int callsOfrunCloneOperation = 0;

    EgitScmHandlerExt() {}

    EgitScmHandlerExt(boolean throwAuthFailed, boolean accessAnonymously) {
      this.throwAuthFailed = throwAuthFailed;
      this.accessAnonymously = accessAnonymously;
    }

    //Make normalizeUri(String, boolean) accessible.
    @Override
    public String normalizeUri(String uri, boolean avoidSSH) throws URISyntaxException {
      return super.normalizeUri(uri, avoidSSH);
    }

    //Mock super
    @Override
    protected void runCloneOperation(URIish uri, File location, String refName, SubMonitor pm)
              throws InvocationTargetException, IOException, InterruptedException {
      callsOfrunCloneOperation++;
      if(throwAuthFailed) {
        throwAuthFailed = false; //next time run smoothly.
  	    throw new InvocationTargetException(new TransportException("Auth failed"));
      }
	}

    //Mock super
    @Override
    protected boolean onAuthFailed() {
      callsOfonAuthFailed++;
      return accessAnonymously;
    }
  }
}
