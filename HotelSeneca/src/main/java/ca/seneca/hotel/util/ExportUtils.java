package ca.seneca.hotel.util;

import javafx.scene.Node;
import javafx.stage.FileChooser;

import java.io.File;

public final class ExportUtils {

    private ExportUtils() {}

    public static File chooseSaveFile(Node ownerNode, String suggestedName, String description, String extensionFilter) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(suggestedName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensionFilter));
        return chooser.showSaveDialog(ownerNode.getScene().getWindow());
    }
}
