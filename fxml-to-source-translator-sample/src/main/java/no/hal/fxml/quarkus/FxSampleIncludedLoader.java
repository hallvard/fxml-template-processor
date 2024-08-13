package no.hal.fxml.quarkus;

import no.hal.fxml.runtime.FxLoaderContext;
import javafx.scene.control.Label;
import no.hal.fxml.runtime.AbstractFxLoader;
import java.util.Map;

// // generated from no/hal/fxml/quarkus/FxSampleIncluded.fxml
public class FxSampleIncludedLoader extends AbstractFxLoader<Label, java.lang.Object> {

   public FxSampleIncludedLoader() {
      super();
   }

   public FxSampleIncludedLoader(Map<String, Object> namespace) {
      super(namespace);
   }

   protected Label build(FxLoaderContext fxLoaderContext) {
      // <Label>
      Label label = new Label();
      label.setPrefHeight(21.0);
      label.setPrefWidth(264.0);
      label.setLayoutX(14.0);
      label.setLayoutY(44.0);
      label.setText("Type your name and click!");
      return label;
   }
}
