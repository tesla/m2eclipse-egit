/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.ide.eclipse.egit.internal;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.m2e.core.scm.ScmUrl;
import org.eclipse.m2e.core.wizards.MavenCheckoutWizard;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;


/**
 * A menu/toolbar command to clone a Git repository and imports its Maven projects.
 */
public class CloneAsMavenProjects extends AbstractHandler {

  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = HandlerUtil.getActiveShell(event);
    String url = getUrlFromClipboard(shell.getDisplay());
    if(url == null || url.length() <= 0 || !url.contains("://")) {
      url = "https://github.com/<your-project>";
    }
    if(!url.startsWith(EgitScmHandler.GIT_SCM_ID)) {
      url = EgitScmHandler.GIT_SCM_ID + url;
    }
    ScmUrl scmUrl = new ScmUrl(url);
    MavenCheckoutWizard wizard = new MavenCheckoutWizard(new ScmUrl[] {scmUrl});
    WizardDialog dialog = new WizardDialog(shell, wizard);
    dialog.open();
    return null;
  }

  private String getUrlFromClipboard(Display display) {
    Clipboard clipboard = new Clipboard(display);
    try {
      return (String) clipboard.getContents(TextTransfer.getInstance());
    } finally {
      clipboard.dispose();
    }
  }

}
