package com.company.onboarding.security;

import com.company.onboarding.entity.Department;
import com.company.onboarding.entity.User;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;

@RowLevelRole(name = "HR managers's departments adn users", code = "hr-managers-rl")
public interface HrManagerRlRole {
    @JpqlRowLevelPolicy(entityClass = Department.class, where = "{E}.hrManager.id = :current_user_id")
    void department();

    @JpqlRowLevelPolicy(entityClass = User.class, where = "{E}.department.hrManager.id = :current_user_id")
    void user();
}