package org.openehr.docs.magicdraw;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.actions.MDActionsCategory;

/**
 * @author Bostjan Lah
 */
public class MainMenuConfigurator implements AMConfigurator {
    private static final String OPENEHR_CATEGORY = "openEHR";

    /**
     * Action will be added to manager.
     */
    private final NMAction action;

    /**
     * Creates configurator.
     * @param action action to be added to main menu.
     */
    public MainMenuConfigurator(NMAction action) {
        this.action = action;
    }

    @Override
    public void configure(ActionsManager manager) {
        // searching for Examples action category
        ActionsCategory category = (ActionsCategory)manager.getActionFor(OPENEHR_CATEGORY);

        if (category == null) {
            // creating new category
            category = new MDActionsCategory(OPENEHR_CATEGORY, OPENEHR_CATEGORY);
            category.setNested(true);
            manager.addCategory(category);
        }
        category.addAction(action);
    }

    @Override
    public int getPriority() {
        return AMConfigurator.MEDIUM_PRIORITY;
    }
}