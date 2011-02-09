/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.egit.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.m2e.scm.internal.wizards.MavenProjectCheckoutJob;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkingSet;


@SuppressWarnings("restriction")
public class AbstractScmHandlerTest extends AbstractMavenProjectTestCase {

  protected void checkout(String... urls) throws Exception {
    final Collection<MavenProjectScmInfo> scmInfos = new ArrayList<MavenProjectScmInfo>();
    for(String url : urls) {
      MavenProjectScmInfo scmInfo = new MavenProjectScmInfo(url, null, null, "HEAD", url, url);
      scmInfos.add(scmInfo);
    }

    ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration();
    List<IWorkingSet> workingSets = Collections.emptyList();
    MavenProjectCheckoutJob job = new MavenProjectCheckoutJob(importConfiguration, true, workingSets) {
      @Override
      protected Collection<MavenProjectScmInfo> getProjects(IProgressMonitor monitor) {
        return scmInfos;
      }
    };
    job.schedule();
    job.join();
    assertTrue(String.valueOf(job.getResult()), job.getResult().isOK());
  }

  protected void assertWorkspaceProject(String projectName) throws Exception {
    IProject project = getWorkspaceProject(projectName);
    assertNotNull("Expected workspace project: " + projectName, project);
    assertTrue("Expected accessible project: " + projectName, project.isAccessible());
  }

  protected void assertWorkspaceProjectShared(String projectName) throws Exception {
    IProject project = getWorkspaceProject(projectName);
    assertNotNull("Expected workspace project: " + projectName, project);
    assertTrue("Expected accessible project: " + projectName, project.isAccessible());
    assertTrue("Expected shared project: " + projectName, RepositoryProvider.isShared(project));
    RepositoryProvider provider = RepositoryProvider.getProvider(project);
    assertNotNull(provider);
  }

  protected IProject getWorkspaceProject(String projectName) {
    // NOTE: IWorkspaceRoot.getProject(String) creates new/missing projects which is not desired here
    IProject[] projects = workspace.getRoot().getProjects();
    for(IProject project : projects) {
      if(project.getName().equals(projectName)) {
        return project;
      }
    }
    return null;
  }

}
