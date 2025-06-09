package com.polatholding.procurementsystem.config.security;

import com.polatholding.procurementsystem.model.User;
import com.polatholding.procurementsystem.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("securityHelper")
public class SecurityHelper {

    private final UserRepository userRepository;

    public SecurityHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isProcurementStaff(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }
        boolean isProcurementManager = user.getRoles().stream()
                .anyMatch(r -> "ProcurementManager".equals(r.getRoleName()));
        boolean isProcurementEmployee = user.getRoles().stream()
                .anyMatch(r -> "Employee".equals(r.getRoleName())) &&
                user.getDepartment() != null &&
                "Procurement".equals(user.getDepartment().getDepartmentName());
        return isProcurementManager || isProcurementEmployee;
    }
}
