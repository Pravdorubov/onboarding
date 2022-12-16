package com.company.onboarding.screen.user;

import com.company.onboarding.entity.OnboardingStatus;
import com.company.onboarding.entity.Step;
import com.company.onboarding.entity.User;
import com.company.onboarding.entity.UserStep;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.security.event.SingleUserPasswordChangeEvent;
import io.jmix.ui.Notifications;
import io.jmix.ui.UiComponents;
import io.jmix.ui.component.*;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.CollectionPropertyContainer;
import io.jmix.ui.model.DataContext;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.navigation.Route;
import io.jmix.ui.screen.*;
import liquibase.license.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.TimeZone;

@UiController("User.edit")
@UiDescriptor("user-edit.xml")
@EditedEntityContainer("userDc")
@Route(value = "users/edit", parentPrefix = "users")
public class UserEdit extends StandardEditor<User> {
    @Subscribe(id = "stepsDc", target = Target.DATA_CONTAINER)
    public void onStepsDcItemPropertyChange(InstanceContainer.ItemPropertyChangeEvent<UserStep> event) {
        updateOnboardingStatus();
    }


    @Subscribe(id = "stepsDc", target = Target.DATA_CONTAINER)
    public void onStepsDcCollectionChange(CollectionContainer.CollectionChangeEvent<UserStep> event) {
        updateOnboardingStatus();
    }

    private void updateOnboardingStatus() {
        User user = getEditedEntity();

        long comletedCount = user.getSteps() == null ? 0 : user.getSteps().stream().filter(us -> us.getComletedDate() != null).count();
        if (comletedCount == 0){
            user.setOnboardingStatus(OnboardingStatus.NOT_STARTED);
        }else if (comletedCount == user.getSteps().size()){
            user.setOnboardingStatus(OnboardingStatus.COMPLETED);
        }else {
            user.setOnboardingStatus(OnboardingStatus.IN_PROGRESS);
        }
    }

    @Autowired
    private DataManager dataManager;

    @Autowired
    private DataContext dataContext;

    @Autowired
    private CollectionPropertyContainer<UserStep> stepsDc;

    @Autowired
    private EntityStates entityStates;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordField passwordField;

    @Autowired
    private TextField<String> usernameField;

    @Autowired
    private PasswordField confirmPasswordField;

    @Autowired
    private Notifications notifications;

    @Autowired
    private MessageBundle messageBundle;

    @Autowired
    private ComboBox<String> timeZoneField;

    private boolean isNewEntity;
    @Autowired
    private Form form;
    @Autowired
    private UiComponents uiComponents;

    @Subscribe
    public void onInit(InitEvent event) {
        timeZoneField.setOptionsList(Arrays.asList(TimeZone.getAvailableIDs()));
    }

    @Subscribe
    public void onInitEntity(InitEntityEvent<User> event) {
        usernameField.setEditable(true);
        passwordField.setVisible(true);
        confirmPasswordField.setVisible(true);
        isNewEntity = true;

        User user = event.getEntity();
        user.setOnboardingStatus(OnboardingStatus.NOT_STARTED);
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (entityStates.isNew(getEditedEntity())) {
            usernameField.focus();
        }
    }

    @Subscribe
    protected void onBeforeCommit(BeforeCommitChangesEvent event) {
        if (entityStates.isNew(getEditedEntity())) {
            if (!Objects.equals(passwordField.getValue(), confirmPasswordField.getValue())) {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption(messageBundle.getMessage("passwordsDoNotMatch"))
                        .show();
                event.preventCommit();
            }
            getEditedEntity().setPassword(passwordEncoder.encode(passwordField.getValue()));
        }
    }

    @Subscribe(target = Target.DATA_CONTEXT)
    public void onPostCommit(DataContext.PostCommitEvent event) {
        if (isNewEntity) {
            getApplicationContext().publishEvent(new SingleUserPasswordChangeEvent(getEditedEntity().getUsername(), passwordField.getValue()));
        }
    }

    @Subscribe("generateButton")
    public void onGenerateButtonClick(Button.ClickEvent event) {
        User user = getEditedEntity();

        if (user.getJoiningDate() == null){
            notifications.create().withCaption("Cannot generate steps for user without 'Joining date'").show();
            return;
        }

        List<Step> steps = dataManager.load(Step.class).query("select s from Step s order by s.sortValue asc").list();

        for (Step step : steps){
            if (stepsDc.getItems().stream().noneMatch(userStep -> userStep.getStep().equals(step))){
                UserStep userStep = dataContext.create(UserStep.class);
                userStep.setUser(user);
                userStep.setStep(step);
                userStep.setDueDate(user.getJoiningDate().plusDays(step.getDuration()));
                userStep.setSortValue(step.getSortValue());
                stepsDc.getMutableItems().add(userStep);
            }
        }
    }

    @Install(to = "stepsTable.comleted", subject = "columnGenerator")
    private Component stepsTableComletedColumnGenerator(UserStep userStep) {
        CheckBox checkBox = uiComponents.create(CheckBox.class);
        checkBox.setValue(userStep.getComletedDate() != null);
        checkBox.addValueChangeListener(e -> {
            if (userStep.getComletedDate() == null){
                userStep.setComletedDate(LocalDate.now());
            }else {
                userStep.setComletedDate(null);
            }
        });
        return checkBox;
    }
}