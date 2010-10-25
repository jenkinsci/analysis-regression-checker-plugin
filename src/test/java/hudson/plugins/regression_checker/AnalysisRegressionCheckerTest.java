package hudson.plugins.regression_checker;

import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.pmd.PmdPublisher;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class AnalysisRegressionCheckerTest extends HudsonTestCase {
    public void test1() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        assertBuildStatusSuccess(p.scheduleBuild2(0).get()); // baseline
        FilePath pmdXml = p.getLastBuild().getWorkspace().child("pmd.xml");

        p.getPublishersList().add(new PmdPublisher(null,null,"LOW",null,false,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,false,"*.xml"));

        pmdXml.copyFrom(getClass().getResource("2.xml"));
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());

        p.getPublishersList().add(new AnalysisRegressionChecker(true,false));
        
        // going down is OK
        pmdXml.copyFrom(getClass().getResource("1.xml"));
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());

        // but going up is not
        pmdXml.copyFrom(getClass().getResource("3.xml"));
        assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());

        // one less but we still need 1 more fix
        pmdXml.copyFrom(getClass().getResource("2.xml"));
        assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());

        // back to 1, and thus should be OK
        pmdXml.copyFrom(getClass().getResource("1.xml"));
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
    }
}
