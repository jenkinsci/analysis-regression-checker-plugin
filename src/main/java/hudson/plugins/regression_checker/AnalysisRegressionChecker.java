package hudson.plugins.regression_checker;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.analysis.core.AbstractResultAction;
import hudson.plugins.analysis.core.BuildResult;
import hudson.plugins.findbugs.FindBugsResultAction;
import hudson.plugins.pmd.PmdResultAction;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class AnalysisRegressionChecker extends Recorder {
    public final boolean checkPMD;
    public final boolean checkFindbugs;

    @DataBoundConstructor
    public AnalysisRegressionChecker(Boolean checkPMD, Boolean checkFindbugs) {
        // Boolean and not boolean because younger version of Hudson fails to convert null to false
        // if the plugin is not installed.
        this.checkPMD = checkPMD!=null && checkPMD;
        this.checkFindbugs = checkFindbugs!=null && checkFindbugs;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        if (checkPMD)
            check(build, listener, PmdResultAction.class);
        if (checkFindbugs)
            check(build, listener, FindBugsResultAction.class);
        return true;
    }

    private <T extends AbstractResultAction<R>,R extends BuildResult> void check(AbstractBuild<?, ?> build, BuildListener listener, Class<T> resultType) {
        T a = build.getAction(resultType);
        if (a!=null) {
            R r = a.getResult();
//            if (r.getDelta()>0) {
//                listener.getLogger().println(Messages.PmdRegressionChecker_RegressionsDetected(a.getDisplayName()));
//                build.setResult(Result.FAILURE);
//                return;
//            }

            // find the previous successful build
            AbstractBuild<?,?> ref = build.getPreviousBuild();
            while (ref!=null && ref.getResult()!= Result.SUCCESS)
                ref = ref.getPreviousBuild();

            if (ref!=null) {
                T base = ref.getAction(resultType);
                if (base!=null) {
                    R baser = base.getResult();
                    int diff = r.getNumberOfWarnings() - baser.getNumberOfWarnings();
                    if (diff>0) {
                        listener.getLogger().println(Messages.PmdRegressionChecker_RegressionsDetected2(a.getDisplayName(),ref.getNumber(),diff));
                        build.setResult(Result.FAILURE);
                        return;
                    }
                }
            }
        }
    }

    @Extension(ordinal=-100)
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public boolean hasFindbugs() {
            try {
                FindBugsResultAction.class.getName();
                return true;
            } catch (LinkageError e) {
                return false;
            }
        }

        public boolean hasPMD() {
            try {
                PmdResultAction.class.getName();
                return true;
            } catch (LinkageError e) {
                return false;
            }
        }

        @Override
        public String getDisplayName() {
            return "Fail the build if the code analysis worsens";
        }
    }
}
