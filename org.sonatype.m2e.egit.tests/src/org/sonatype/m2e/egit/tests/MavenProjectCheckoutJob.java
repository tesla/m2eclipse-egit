package org.sonatype.m2e.egit.tests;


import static org.junit.Assert.fail;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.wizards.ImportMavenProjectsJob;
import org.eclipse.m2e.scm.MavenCheckoutOperation;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.m2e.scm.internal.Messages;


/**
 * Maven project checkout Job
 *
 * @author Eugene Kuleshov
 */
public abstract class MavenProjectCheckoutJob extends WorkspaceJob {

  final ProjectImportConfiguration configuration;

  boolean checkoutAllProjects;

  Collection<MavenProjectInfo> projects;

  File location;

  List<String> collectedLocations = new ArrayList<>();

  final List<IWorkingSet> workingSets;

  MavenProjectCheckoutJob(ProjectImportConfiguration importConfiguration, boolean checkoutAllProjects,
      List<IWorkingSet> workingSets) {
    super(Messages.MavenProjectCheckoutJob_title);
    this.configuration = importConfiguration;
    this.checkoutAllProjects = checkoutAllProjects;
    this.workingSets = workingSets;

    addJobChangeListener(new CheckoutJobChangeListener());
  }

  public void setLocation(File location) {
    this.location = location;
  }

  protected abstract Collection<MavenProjectScmInfo> getProjects(IProgressMonitor monitor) throws InterruptedException;

  // WorkspaceJob

  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    try {
      MavenCheckoutOperation operation = new MavenCheckoutOperation(location, getProjects(monitor));
      operation.run(monitor);
      collectedLocations.addAll(operation.getLocations());

      IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();

      MavenModelManager modelManager = MavenPlugin.getMavenModelManager();

      LocalProjectScanner scanner = new LocalProjectScanner(operation.getLocations(), true, modelManager);
      scanner.run(monitor);

      this.projects = MavenPlugin.getProjectConfigurationManager().collectProjects(scanner.getProjects());

      if(checkoutAllProjects) {
        // check if there any project name conflicts
        for(MavenProjectInfo projectInfo : projects) {
          Model model = projectInfo.getModel();
          if(model == null) {
            model = modelManager.readMavenModel(projectInfo.getPomFile());
            projectInfo.setModel(model);
          }

          String projectName = ProjectConfigurationManager.getProjectName(configuration, model);
          IProject project = workspace.getProject(projectName);
          if(project.exists()) {
            checkoutAllProjects = false;
            break;
          }
        }
      }

      return Status.OK_STATUS;

    } catch(InterruptedException ex) {
      return Status.CANCEL_STATUS;
    }
  }

  /**
   * Checkout job listener
   */
  final class CheckoutJobChangeListener extends JobChangeAdapter {

    public void done(IJobChangeEvent event) {
      IStatus result = event.getResult();
      if(result.getSeverity() == IStatus.CANCEL) {
        return;
      } else if(!result.isOK()) {
        // XXX report errors
        return;
      }

      if(projects.isEmpty()) {
        fail("No Maven projects to import");
      }

      if(checkoutAllProjects) {
        WorkspaceJob job = new ImportMavenProjectsJob(projects, workingSets, configuration);

        ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRuleFactory()
            .modifyRule(ResourcesPlugin.getWorkspace().getRoot());
        job.setRule(rule);
        job.schedule();

      } else {
    	fail("import was disabled");
      }
    }
  }
}
