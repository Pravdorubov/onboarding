package com.company.onboarding.screen.myonboarding;

import com.company.onboarding.entity.User;
import com.company.onboarding.entity.UserStep;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.ui.UiComponents;
import io.jmix.ui.component.*;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.CollectionLoader;
import io.jmix.ui.model.DataContext;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

@UiController("MyOnboardingScreen")
@UiDescriptor("my-onboarding-screen.xml")
public class MyOnboardingScreen extends Screen {

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private CollectionLoader<UserStep> userStepsDl;

    @Autowired
    private DataContext dataContext;

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private Label overdueStepsLabel;

    @Autowired
    private Label totalStepsLabel;

    @Autowired
    private Label completedStepsLabel;

    @Autowired
    private CollectionContainer<UserStep> userStepsDc;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        User user = (User) currentAuthentication.getUser();
        userStepsDl.setParameter("user", user);
        userStepsDl.load();
        updateLabels();
    }

    @Install(to = "userStepsTable.completed", subject = "columnGenerator")
    private Component userStepsTableCompletedColumnGenerator(UserStep userStep) {
        CheckBox checkBox = uiComponents.create(CheckBox.class);
        checkBox.setValue(userStep.getComletedDate() != null);
        checkBox.addValueChangeListener(e -> {
           if (userStep.getComletedDate() == null){
               userStep.setComletedDate(LocalDate.now());
           } else {
               userStep.setComletedDate(null);
           }
        });

        return checkBox;
    }

    @Subscribe(id = "userStepsDc", target = Target.DATA_CONTAINER)
    public void onUserStepsDcItemPropertyChange(InstanceContainer.ItemPropertyChangeEvent<UserStep> event) {
        updateLabels();
    }

    private void updateLabels() {
        totalStepsLabel.setValue("Total steps: " + userStepsDc.getItems().size());

        long completedCount = userStepsDc.getItems().stream().filter(us -> us.getComletedDate() != null).count();
        completedStepsLabel.setValue("Completed steps: " + completedCount);

        long overdueCount = userStepsDc.getItems().stream().filter(us->isOverdue(us)).count();
        overdueStepsLabel.setValue("Overdue steps: " + overdueCount);

    }

    private boolean isOverdue(UserStep us) {
        return us.getComletedDate() == null
                && us.getDueDate() != null
                && us.getDueDate().isBefore(LocalDate.now());
    }

    @Subscribe("saveButton")
    public void onSaveButtonClick(Button.ClickEvent event) {
        dataContext.commit();
        close(StandardOutcome.COMMIT);
    }

    @Subscribe("discardButton")
    public void onDiscardButtonClick(Button.ClickEvent event) {
        close(StandardOutcome.DISCARD);
    }

    @Install(to = "userStepsTable", subject = "styleProvider")
    private String userStepsTableStyleProvider(UserStep entity, String property) {
        if ("dueDate".equals(property) && isOverdue(entity)){
            return "overdue-step";
        }
        return null;
    }
}