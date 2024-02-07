package no.hal.fxml.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import javafx.fxml.FXMLLoader;
import no.hal.fxml.templateprocessor.FxmlTemplateProcessor;

@ApplicationScoped
public class FxmlTemplateProcessorProvider {

    @Inject
    Instance<FXMLLoader> fxmlLoaderProvider;

    @Produces
    public FxmlTemplateProcessor getFxmlTemplateProcessor() {
        return get();
    }

    public FxmlTemplateProcessor get() {
        return FxmlTemplateProcessor.FXML(fxmlLoaderProvider.get());
    }

    public FxmlTemplateProcessor get(Object controller) {
        var fxmlLoader = fxmlLoaderProvider.get();
        fxmlLoader.setController(controller);
        return FxmlTemplateProcessor.FXML(fxmlLoader);
    }
}
