/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.revelc.code.formatter.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.ClasspathHelpers;
import org.junit.jupiter.api.Test;

@SuppressWarnings("restriction")
public class BuildhelperTest extends AbstractMavenProjectTestCase {

    @Test
    public void test_p001_simple() throws Exception {
        IProject project = importBuildHelperProject("buildhelper-001");
        IJavaProject javaProject = JavaCore.create(project);
        IClasspathEntry[] classpath = javaProject.getRawClasspath();

        ClasspathHelpers.assertClasspath(
                new String[] {
                    "/buildhelper-001/src/main/java", //
                    "/buildhelper-001/src/custom/java", //
                    "/buildhelper-001/src/test/java", //
                    "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
                    "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", //
                },
                classpath);
    }

    @Test
    public void test_p002_resources() throws Exception {
        IProject project = importBuildHelperProject("buildhelper-002");
        IJavaProject javaProject = JavaCore.create(project);
        IClasspathEntry[] classpath = javaProject.getRawClasspath();

        ClasspathHelpers.assertClasspath(
                new String[] {
                    "/buildhelper-002/src/main/java", //
                    "/buildhelper-002/src/custom/main/java", //
                    "/buildhelper-002/src/main/resources", //
                    "/buildhelper-002/src/custom/main/resources", //
                    "/buildhelper-002/src/test/java", //
                    "/buildhelper-002/src/custom/test/java", //
                    "/buildhelper-002/src/custom/test/resources", //
                    "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
                    "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", //
                },
                classpath);

        File target = project.findMember("target").getRawLocation().toFile();
        assertTrue(target.exists(), target + " does not exist");
        assertTrue(new File(target, "classes/buildhelper002/custom/CustomTreeClass.class").exists(), "Class");
        assertTrue(new File(target, "classes/buildhelper002/custom/customTree.txt").exists(), "Resource");
        assertTrue(
                new File(target, "test-classes/buildhelper002/custom/CustomTreeClassTest.class").exists(),
                "Test Class");
        assertTrue(new File(target, "test-classes/buildhelper002/custom/customTreeTest.txt").exists(), "Test Resource");
    }

    private IProject importBuildHelperProject(String name) throws Exception {
        ResolverConfiguration configuration = new ResolverConfiguration();
        IProject project = importProject("projects/buildhelper/" + name + "/pom.xml", configuration);
        waitForJobsToComplete();

        project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        waitForJobsToComplete();

        assertNoErrors(project);
        return project;
    }
}
