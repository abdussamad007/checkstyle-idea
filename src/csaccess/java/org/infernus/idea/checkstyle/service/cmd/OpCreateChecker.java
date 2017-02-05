package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.Configurations;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Command which creates new {@link CheckStyleChecker}s.
 */
public class OpCreateChecker
        implements CheckstyleCommand<CheckStyleChecker> {

    private final Module module;
    private final ConfigurationLocation location;
    private final Map<String, String> variables;
    private final TabWidthAndBaseDirProvider configurations;
    private final ClassLoader loaderOfCheckedCode;

    public OpCreateChecker(@Nullable final Module module,
                           @NotNull final ConfigurationLocation location,
                           final Map<String, String> variables,
                           @Nullable final TabWidthAndBaseDirProvider configurations,
                           @NotNull final ClassLoader loaderOfCheckedCode) {
        this.module = module;
        this.location = location;
        this.variables = variables;
        this.configurations = configurations;
        this.loaderOfCheckedCode = loaderOfCheckedCode;
    }

    @Override
    @NotNull
    @SuppressWarnings("deprecation")  // setClassloader() must be used for backwards compatibility
    public CheckStyleChecker execute(@NotNull final Project project) throws CheckstyleException {

        final Configuration csConfig = loadConfig(project);

        final Checker checker = new Checker();
        checker.setModuleClassLoader(getClass().getClassLoader());   // for Checkstyle to load modules (checks)
        checker.setClassloader(loaderOfCheckedCode);  // for checks to load the classes and resources to be analyzed
        checker.configure(csConfig);

        CheckerWithConfig cwc = new CheckerWithConfig(checker, csConfig);
        final TabWidthAndBaseDirProvider configs = configurations != null
                ? configurations
                : new Configurations(module, csConfig);
        return new CheckStyleChecker(cwc, configs.tabWidth(), configs.baseDir(), loaderOfCheckedCode,
                CheckstyleProjectService.getInstance(project).getCheckstyleInstance());
    }

    private Configuration loadConfig(@NotNull final Project project) throws CheckstyleException {
        return new OpLoadConfiguration(location, variables, project, module).execute(project).getConfiguration();
    }
}
