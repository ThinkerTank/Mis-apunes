package co.edu.icesi.sgmms.controller;

/** Contract of the controllers of the three screens. */
public interface ScreenController {

    /** Receives the shared application context once, right after the view is loaded. */
    void init(AppContext context);

    /** Called every time the screen becomes visible. */
    void onShow();

    /** Redraws the screen from the current model state (always on the JavaFX thread). */
    void refresh();
}
