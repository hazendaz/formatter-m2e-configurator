/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.revelc.code.formatter.connector.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import net.revelc.code.formatter.connector.FormatterCore;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.osgi.service.prefs.Preferences;

/**
 * The Class FormatterProjectConfigurator.
 */
public class FormatterProjectConfigurator extends AbstractProjectConfigurator {

    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(FormatterProjectConfigurator.class.getName());

    /**
     * The Enum Formatter.
     */
    public enum Formatter {

        /** The java. */
        JAVA("configFile", "src/config/eclipse/formatter/java.xml");

        /** The configuratio name. */
        private final String configuratioName;

        /** The default path. */
        private final String defaultPath;

        /**
         * Instantiates a new formatter.
         *
         * @param newConfiguratioName the new configuratio name
         * @param newDefaultPath the new default path
         */
        private Formatter(String newConfiguratioName, String newDefaultPath) {
            this.configuratioName = newConfiguratioName;
            this.defaultPath = newDefaultPath;
        }

        /**
         * Gets the configuration name.
         *
         * @return the configuration name
         */
        public String getConfigurationName() {
            return this.configuratioName;
        }

        /**
         * Gets the default path.
         *
         * @return the default path
         */
        public String getDefaultPath() {
            return this.defaultPath;
        }
    }

    @Override
    public AbstractBuildParticipant getBuildParticipant(
            IMavenProjectFacade projectFacade, MojoExecution execution, IPluginExecutionMetadata executionMetadata) {
        // nothing to do
        return null;
    }

    @Override
    public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
        IProject eclipseProject = request.mavenProjectFacade().getProject();

        printSettings();
        if (eclipseProject.hasNature(JavaCore.NATURE_ID)) {
            Xpp3Dom[] settings = parseConfigurationFile(request, monitor);

            for (Xpp3Dom setting : settings) {
                Platform.getPreferencesService()
                        .getRootNode()
                        .node("project")
                        .node(eclipseProject.getName())
                        .node(JavaCore.PLUGIN_ID)
                        .put(setting.getAttribute("id"), setting.getAttribute("value"));
            }

            Platform.getPreferencesService()
                    .getRootNode()
                    .node("project")
                    .node(eclipseProject.getName())
                    .node("org.eclipse.jdt.ui")
                    .put("cleanup.format_source_code", "true");
        }
    }

    /**
     * Parses the configuration file.
     *
     * @param request the request
     * @param monitor the monitor
     * @return the xpp 3 dom[]
     * @throws CoreException the core exception
     */
    private Xpp3Dom[] parseConfigurationFile(ProjectConfigurationRequest request, IProgressMonitor monitor)
            throws CoreException {
        Xpp3Dom dom;
        try (InputStream content = readConfigFile(Formatter.JAVA, request, monitor)) {
            dom = Xpp3DomBuilder.build(content, "UTF-8");
        } catch (XmlPullParserException e) {
            throw new CoreException(new Status(IStatus.ERROR, FormatterCore.PLUGIN_ID, "Invalid configuration XML", e));
        } catch (IOException e) {
            throw new CoreException(
                    new Status(IStatus.ERROR, FormatterCore.PLUGIN_ID, "Unable to read configuration XML", e));
        }
        return dom.getChild("profile").getChildren("setting");
    }

    /**
     * Read config file.
     *
     * @param formatter the formatter
     * @param request the request
     * @param monitor the monitor
     * @return the input stream
     * @throws CoreException the core exception
     */
    private InputStream readConfigFile(
            Formatter formatter, ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
        IMavenProjectFacade mavenProject = request.mavenProjectFacade();
        List<MojoExecution> executions = mavenProject.getMojoExecutions(
                "net.revelc.code.formatter", "formatter-maven-plugin", monitor, "validate");

        MojoExecution execution = executions.get(0);
        Xpp3Dom cfg = execution.getConfiguration();
        String javaConfigFile = cfg.getChild(formatter.getConfigurationName()).getValue();
        if (javaConfigFile == null || javaConfigFile.equalsIgnoreCase("${" + formatter.getConfigurationName() + "}")) {
            javaConfigFile = formatter.getDefaultPath();
        }

        Path cfgFile = Path.of(javaConfigFile);
        if (!cfgFile.isAbsolute()) {
            cfgFile = request.mavenProjectFacade()
                    .getProject()
                    .getLocation()
                    .append(javaConfigFile)
                    .toPath();
        }

        if (!cfgFile.toFile().exists()) {
            throw new CoreException(new Status(
                    IStatus.CANCEL, FormatterCore.PLUGIN_ID, "Configuration file '" + javaConfigFile + "' not found!"));
        }

        try {
            return Files.newInputStream(cfgFile);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.CANCEL, FormatterCore.PLUGIN_ID, e.getMessage()));
        }
    }

    /**
     * Prints the settings.
     */
    private void printSettings() {
        StringBuilder sb = new StringBuilder();
        Preferences prefs = Platform.getPreferencesService().getRootNode();
        try {
            eval(prefs, "\t", sb);
        } catch (Exception e1) {
            logger.info("Exception in eval " + e1.getMessage());
        }

        Path f = Path.of("tree.txt");
        try (final OutputStreamWriter osw = new OutputStreamWriter(Files.newOutputStream(f), StandardCharsets.UTF_8)) {
            osw.write(sb.toString().toCharArray());
            f.toAbsolutePath();
        } catch (IOException e1) {
            logger.info("Exception in writing in tree.txt " + e1.getMessage());
        }
    }

    /**
     * Eval.
     *
     * @param prefs the prefs
     * @param spacer the spacer
     * @param sb the sb
     * @throws Exception the exception
     */
    private static void eval(Preferences prefs, String spacer, StringBuilder sb) throws Exception {
        String[] children = prefs.childrenNames();
        for (String child : children) {
            sb.append(spacer).append(child).append("\n");
            eval(prefs.node(child), spacer + "\t", sb);
        }
        String[] keys = prefs.keys();
        for (String key : keys) {
            sb.append(spacer)
                    .append(" * ")
                    .append(key)
                    .append(": ")
                    .append(prefs.get(key, null))
                    .append("\n");
        }
    }

    @Override
    public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
        // Not Implemented
    }
}
