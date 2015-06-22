package org.openehr.docs.magicdraw;


import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.plugins.Plugin;

/**
 * @author Bostjan Lah
 */
public class OpenEHRExporter extends Plugin {
    @Override
    public void init() {
        ActionsConfiguratorsManager manager = ActionsConfiguratorsManager.getInstance();
        manager.addMainMenuConfigurator(new MainMenuConfigurator(getSeparatedActions()));
    }

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    /**
     * Creates group of actions. This group is separated from others using menu separator (when it represented in menu).
     * Separator is added for group of actions in one actions category.
     */
    private NMAction getSeparatedActions() {
        ActionsCategory category = new ActionsCategory(null, null);
        category.addAction(new OpenEHRExportAction(null, "Export to asciidoc"));
        return category;
    }
}

