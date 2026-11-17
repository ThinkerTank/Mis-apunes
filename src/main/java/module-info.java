/**
 * SGMMS module. The model has no dependency on JavaFX; only the controllers and the view do.
 */
module co.edu.icesi.sgmms {
    requires javafx.controls;
    requires javafx.fxml;

    exports co.edu.icesi.sgmms;
    exports co.edu.icesi.sgmms.model;
    exports co.edu.icesi.sgmms.model.exceptions;
    exports co.edu.icesi.sgmms.model.simulation;
    exports co.edu.icesi.sgmms.model.structures;
    exports co.edu.icesi.sgmms.persistence;
    exports co.edu.icesi.sgmms.view;
    exports co.edu.icesi.sgmms.controller;

    opens co.edu.icesi.sgmms.controller to javafx.fxml;
    opens co.edu.icesi.sgmms.view to javafx.fxml, javafx.graphics;
}
