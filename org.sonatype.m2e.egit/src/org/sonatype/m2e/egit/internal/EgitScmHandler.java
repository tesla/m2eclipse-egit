/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.egit.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.m2e.scm.spi.ScmHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SCM handler to enable M2Eclipse to checkout using EGit.
 */
@SuppressWarnings("restriction")
public class EgitScmHandler extends ScmHandler {

  private final Logger log = LoggerFactory.getLogger(EgitScmHandler.class);

  public static final String GIT_SCM_ID = "scm:git:";

  @Override
  public void checkoutProject(MavenProjectScmInfo info, File location, IProgressMonitor monitor) throws CoreException,
      InterruptedException {
    log.debug("Checking out project from {} to {}", info, location);

    SubMonitor pm = SubMonitor.convert(monitor, 100);
    try {
      URIish uri = getUri(info);

      String refName = getRefName(info);

      ListRemoteOperation ls = new ListRemoteOperation(new FileRepository(location), uri, getTimeout());
      ls.run(pm.newChild(1));

      Ref ref = ls.getRemoteRef(refName);

      CloneOperation clone = new CloneOperation(uri, true /* allSelected */, new ArrayList<Ref>(), location, ref,
          "origin", getTimeout());
      clone.run(pm.newChild(99));

      fixAutoCRLF(clone.getGitDir());
    } catch(InvocationTargetException e) {
      Throwable cause = e.getTargetException();
      throw new CoreException(new Status(IStatus.ERROR, getClass().getName(), cause.getMessage(), cause));
    } catch(IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, getClass().getName(), e.getMessage(), e));
    } catch(URISyntaxException e) {
      throw new CoreException(new Status(IStatus.ERROR, getClass().getName(), e.getMessage(), e));
    } catch(InterruptedException e) {
      // The monitor was canceled
    } finally {
      pm.done();
    }
  }

  protected int getTimeout() {
    return 30;
  }

  protected URIish getUri(MavenProjectScmInfo info) throws URISyntaxException {
    String url = info.getRepositoryUrl();
    url = normalizeUri(url);

    URIish uri = new URIish(url);

    if(isProtocolAuthAware(uri.getScheme()) && info.getUsername() != null && info.getUsername().length() > 0) {
      if(uri.getUser() == null || info.getUsername().equals(uri.getUser())) {
        uri = uri.setUser(info.getUsername());
      }
    }

    return uri;
  }

  protected String normalizeUri(String uri) throws URISyntaxException {
    if(!uri.startsWith(GIT_SCM_ID)) {
      return uri;
    }

    uri = uri.substring(GIT_SCM_ID.length());
    if(uri.startsWith("file:") && !uri.startsWith("file:///")) {
      throw new URISyntaxException(uri, "Invalid git URI");
    }

    URIish gitUri = new URIish(uri);
    if(gitUri.getScheme() == null) {
      if(gitUri.getHost() == null || "file".equals(gitUri.getHost())) {
        gitUri = gitUri.setHost(null);
        gitUri = gitUri.setScheme("file");
      } else {
        // This must be an scp-like syntax git URL
        // See http://www.kernel.org/pub/software/scm/git/docs/git-clone.html#_git_urls_a_id_urls_a
        gitUri = gitUri.setScheme("ssh");
      }
    }
    return gitUri.toString();
  }

  protected boolean isProtocolAuthAware(String protocol) {
    return !"file".equalsIgnoreCase(protocol);
  }

  protected String getRefName(MavenProjectScmInfo info) {
    String branch = info.getBranch();

    if(branch == null || branch.trim().length() == 0) {
      branch = Constants.MASTER;
    }

    if(!branch.startsWith(Constants.R_REFS)) {
      branch = Constants.R_HEADS + branch;
    }

    return branch;
  }

  protected void fixAutoCRLF(File gitDirectory) throws IOException {
    // jgit does not have support for core.autocrlf but it sets the core.autocrlf=false in the local git
    // repository config (https://bugs.eclipse.org/bugs/show_bug.cgi?id=301775).
    // We need to unset it.
    FileRepository localRepository = new FileRepository(gitDirectory);
    try {
      FileBasedConfig localConfig = localRepository.getConfig();
      localConfig.unset(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_AUTOCRLF);
      localConfig.save();
    } finally {
      localRepository.close();
    }
  }

}
