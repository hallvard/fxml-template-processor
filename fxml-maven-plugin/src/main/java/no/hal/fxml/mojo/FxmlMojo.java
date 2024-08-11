package no.hal.fxml.mojo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import no.hal.fxml.model.JavaCode;
import no.hal.fxml.model.QName;
import no.hal.fxml.translator.FxLoaderProviderGenerator;
import no.hal.fxml.translator.FxmlTranslator;
import no.hal.fxml.translator.FxmlTranslator.FxmlTranslation;

@Mojo(name = "fxml", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class FxmlMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
    File basedir;

    @Parameter(defaultValue = "src/main/resource", required = true, readonly = true)
    File resourceDirectory;

    @Parameter(defaultValue = "src/main/java", required = true, readonly = true)
    File sourceDirectory;

    @Parameter(required = true, readonly = true)
    String fxLoaderProviderClass;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Path source = basedir.toPath().resolve(resourceDirectory.toPath());
            Path target = basedir.toPath().resolve(sourceDirectory.toPath());
            if (! Files.isDirectory(source)) {
                throw new IllegalArgumentException("sourceDirectory %s isn't a directory".formatted(sourceDirectory));
            }
            var translations = Files.find(source, Integer.MAX_VALUE, (path, attributes) -> path.getFileName().toString().endsWith(".fxml"))
                .map(path -> translateFxml(source, path, target))
                .filter(Objects::nonNull)
                .toList();
            var providerClass = new FxLoaderProviderGenerator().generateFxLoaderProvider(QName.valueOf(fxLoaderProviderClass), translations);
            var javaSource = JavaCode.toJavaSource(providerClass, "// generated");
            var javaPath = target.resolve(providerClass.className().toString().replace(".", "/") + ".java");
            Files.write(javaPath, javaSource.getBytes());
        } catch (Exception ex) {
            throw new MojoFailureException(ex);
        }
    }

    private FxmlTranslation translateFxml(Path basePath, Path fxmlPath, Path outputFolder) {
        try {
            return FxmlTranslator.translateFile(basePath, fxmlPath, outputFolder);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
