/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.egit.internal;

import java.io.File;


/**
 * A description of a working directory to import Maven projects from.
 */
public class WorkingDirectory {

  private final String path;

  public WorkingDirectory(String path) {
    this.path = path;
  }

  public WorkingDirectory(File path) {
    this.path = path.getAbsolutePath();
  }

  public String getPath() {
    return path;
  }

}
