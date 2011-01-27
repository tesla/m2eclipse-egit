/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.egit.internal;

import java.io.File;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;


/**
 * An adapter factory that enables M2Eclipse to import Maven projects from EGit's repository view.
 */
@SuppressWarnings("restriction")
public class EgitWorkingDirectoryAdapterFactory implements IAdapterFactory {

  private static final Class<?>[] LIST = {WorkingDirectory.class};

  public Class<?>[] getAdapterList() {
    return LIST;
  }

  @SuppressWarnings("rawtypes")
  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if(WorkingDirectory.class.equals(adapterType) && (adaptableObject instanceof RepositoryTreeNode)) {
      // see also org.eclipse.egit.ui.internal.repository.tree.command.ImportProjectsCommand

      if(adaptableObject instanceof RepositoryNode || adaptableObject instanceof WorkingDirNode) {
        RepositoryTreeNode<?> node = (RepositoryTreeNode<?>) adaptableObject;
        File path = node.getRepository().getWorkTree();
        if(path != null) {
          return new WorkingDirectory(path);
        }
      } else if(adaptableObject instanceof FolderNode) {
        File path = ((FolderNode) adaptableObject).getObject();
        if(path != null) {
          return new WorkingDirectory(path);
        }
      }
    }
    return null;
  }

}
