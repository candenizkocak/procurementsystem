package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.AdminUserFormDto;
import com.polatholding.procurementsystem.dto.UserDto;
import com.polatholding.procurementsystem.model.Department;
import com.polatholding.procurementsystem.model.Role;
import com.polatholding.procurementsystem.model.User;
import com.polatholding.procurementsystem.repository.DepartmentRepository;
import com.polatholding.procurementsystem.repository.RoleRepository;
import com.polatholding.procurementsystem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // For hasText

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Role Names - Ensure these EXACTLY match RoleName in your Roles TABLE
    public static final String AUDITOR_ROLE_NAME = "Auditor";
    public static final String DIRECTOR_ROLE_NAME = "Director";
    public static final String ADMIN_ROLE_NAME = "Admin";
    public static final String FINANCE_ROLE_NAME = "Finance Officer";
    public static final String MANAGER_ROLE_NAME = "Manager"; // Base Manager role (selected on form)
    public static final String PROCUREMENT_MANAGER_ROLE_NAME = "ProcurementManager"; // Actual derived role
    public static final String EMPLOYEE_ROLE_NAME = "Employee"; // Assuming a general employee role exists

    // Department Names
    public static final String PROCUREMENT_DEPARTMENT_NAME = "Procurement";
    public static final String FINANCE_DEPARTMENT_NAME = "Finance";
    public static final String ADMINISTRATION_DEPARTMENT_NAME = "Administration";
    public static final String AUDIT_DEPARTMENT_NAME = "Audit";


    public AdminServiceImpl(UserRepository userRepository,
                            DepartmentRepository departmentRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        if (user.getDepartment() != null) {
            dto.setDepartmentName(user.getDepartment().getDepartmentName());
        } else {
            dto.setDepartmentName("N/A (Global)");
        }
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            dto.setRoles(user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()));
        } else {
            dto.setRoles(Collections.emptySet());
        }
        dto.setCreatedAt(user.getCreatedAt());
        dto.setFormerEmployee(user.isFormerEmployee());
        return dto;
    }


    @Override
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll().stream()
                .filter(role -> !PROCUREMENT_MANAGER_ROLE_NAME.equals(role.getRoleName())) // ProcurementManager is derived
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createUser(AdminUserFormDto userFormDto) {
        log.debug("createUser: Starting for email: {}", userFormDto.getEmail());

        if (userRepository.findByEmail(userFormDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + userFormDto.getEmail());
        }
        if (!StringUtils.hasText(userFormDto.getPassword())) {
            throw new IllegalArgumentException("Password is required for new user creation.");
        }
        if (userFormDto.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }
        if (userFormDto.getRoleId() == null) {
            throw new IllegalArgumentException("A role must be selected.");
        }

        User newUser = new User();
        newUser.setFirstName(userFormDto.getFirstName());
        newUser.setLastName(userFormDto.getLastName());
        newUser.setEmail(userFormDto.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(userFormDto.getPassword()));
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setFormerEmployee(userFormDto.isFormerEmployee()); // Default to false for new user

        Role selectedRoleOnForm = roleRepository.findById(userFormDto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Selected role not found with ID: " + userFormDto.getRoleId()));
        String selectedRoleName = selectedRoleOnForm.getRoleName();

        Department finalDepartmentToAssign = null;
        Role finalRoleToAssignToUser = selectedRoleOnForm;

        if (DIRECTOR_ROLE_NAME.equals(selectedRoleName)) {
            log.debug("createUser: Director role selected. Department will be null.");
            finalDepartmentToAssign = null;
        } else if (FINANCE_ROLE_NAME.equals(selectedRoleName)) {
            finalDepartmentToAssign = departmentRepository.findAll().stream()
                    .filter(d -> FINANCE_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: '" + FINANCE_DEPARTMENT_NAME + "' department not found."));
            log.debug("createUser: Finance role selected. Department set to '{}'.", FINANCE_DEPARTMENT_NAME);
        } else if (ADMIN_ROLE_NAME.equals(selectedRoleName)) {
            finalDepartmentToAssign = departmentRepository.findAll().stream()
                    .filter(d -> ADMINISTRATION_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: '" + ADMINISTRATION_DEPARTMENT_NAME + "' department not found."));
            log.debug("createUser: Admin role selected. Department set to '{}'.", ADMINISTRATION_DEPARTMENT_NAME);
        } else if (AUDITOR_ROLE_NAME.equals(selectedRoleName)) {
            finalDepartmentToAssign = departmentRepository.findAll().stream()
                    .filter(d -> AUDIT_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: '" + AUDIT_DEPARTMENT_NAME + "' department not found."));
            log.debug("createUser: Auditor role selected. Department set to '{}'.", AUDIT_DEPARTMENT_NAME);
        } else if (MANAGER_ROLE_NAME.equals(selectedRoleName)) {
            if (userFormDto.getDepartmentId() == null) {
                throw new IllegalArgumentException("Department is required for the Manager role.");
            }
            finalDepartmentToAssign = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found for Manager role with ID: " + userFormDto.getDepartmentId()));
            if (PROCUREMENT_DEPARTMENT_NAME.equals(finalDepartmentToAssign.getDepartmentName())) {
                finalRoleToAssignToUser = roleRepository.findAll().stream()
                        .filter(r -> PROCUREMENT_MANAGER_ROLE_NAME.equals(r.getRoleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Critical: '" + PROCUREMENT_MANAGER_ROLE_NAME + "' role not found."));
                log.debug("createUser: Manager role in Procurement dept. Actual role set to '{}'.", PROCUREMENT_MANAGER_ROLE_NAME);
            } else {
                log.debug("createUser: Manager role in dept '{}'. Actual role remains '{}'.", finalDepartmentToAssign.getDepartmentName(), MANAGER_ROLE_NAME);
            }
        } else { // Employee or other non-special roles
            if (userFormDto.getDepartmentId() == null) {
                // Allow Employee role to have null department if needed, or make it required
                // Forcing Employee to have a department:
                throw new IllegalArgumentException("Department is required for the role: " + selectedRoleName);
            }
            finalDepartmentToAssign = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found with ID: " + userFormDto.getDepartmentId()));
            log.debug("createUser: Standard role '{}' assigned to department '{}'.", selectedRoleName, finalDepartmentToAssign.getDepartmentName());
        }

        newUser.setDepartment(finalDepartmentToAssign);
        newUser.setRoles(Collections.singleton(finalRoleToAssignToUser));

        User savedUser = userRepository.save(newUser);
        log.info("createUser: User {} (ID: {}) created successfully with role {} and department {}.",
                savedUser.getEmail(), savedUser.getUserId(), finalRoleToAssignToUser.getRoleName(),
                finalDepartmentToAssign != null ? finalDepartmentToAssign.getDepartmentName() : "N/A");

        // If the created user is a manager of the assigned department, update the department record
        if (finalDepartmentToAssign != null &&
                (MANAGER_ROLE_NAME.equals(finalRoleToAssignToUser.getRoleName()) || PROCUREMENT_MANAGER_ROLE_NAME.equals(finalRoleToAssignToUser.getRoleName()))) {
            if (finalDepartmentToAssign.getManagerUser() != null && !finalDepartmentToAssign.getManagerUser().getUserId().equals(savedUser.getUserId())) {
                log.warn("Department {} already had manager {}. Overwriting with new user {}.",
                        finalDepartmentToAssign.getDepartmentName(),
                        finalDepartmentToAssign.getManagerUser().getEmail(),
                        savedUser.getEmail());
            }
            finalDepartmentToAssign.setManagerUser(savedUser);
            departmentRepository.save(finalDepartmentToAssign);
            log.debug("createUser: User {} set as manager for department {}.", savedUser.getEmail(), finalDepartmentToAssign.getDepartmentName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserFormDto getUserFormById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        AdminUserFormDto formDto = new AdminUserFormDto();
        formDto.setUserId(user.getUserId());
        formDto.setFirstName(user.getFirstName());
        formDto.setLastName(user.getLastName());
        formDto.setEmail(user.getEmail());
        formDto.setPassword(null); // Password is not pre-filled for editing

        if (user.getDepartment() != null) {
            formDto.setDepartmentId(user.getDepartment().getDepartmentId());
        }

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            Role currentActualRole = user.getRoles().iterator().next(); // Assuming single role from DB
            if (PROCUREMENT_MANAGER_ROLE_NAME.equals(currentActualRole.getRoleName())) {
                Role managerFormRole = roleRepository.findAll().stream()
                        .filter(r -> MANAGER_ROLE_NAME.equals(r.getRoleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Base 'Manager' role not found for form display."));
                formDto.setRoleId(managerFormRole.getRoleId());
            } else {
                formDto.setRoleId(currentActualRole.getRoleId());
            }
        }
        formDto.setFormerEmployee(user.isFormerEmployee());
        return formDto;
    }

    @Override
    @Transactional
    public void updateUser(AdminUserFormDto userFormDto) {
        log.debug("updateUser: Starting update for UserID: {}", userFormDto.getUserId());
        User userToUpdate = userRepository.findById(userFormDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userFormDto.getUserId() + " for update."));

        Department oldDepartmentOfUser = userToUpdate.getDepartment();
        boolean wasManagerOfOldDepartment = oldDepartmentOfUser != null &&
                oldDepartmentOfUser.getManagerUser() != null &&
                oldDepartmentOfUser.getManagerUser().getUserId().equals(userToUpdate.getUserId());

        userToUpdate.setFirstName(userFormDto.getFirstName());
        userToUpdate.setLastName(userFormDto.getLastName());

        if (!userToUpdate.getEmail().equalsIgnoreCase(userFormDto.getEmail())) {
            userRepository.findByEmail(userFormDto.getEmail()).ifPresent(existingUserWithNewEmail -> {
                if (!existingUserWithNewEmail.getUserId().equals(userToUpdate.getUserId())) {
                    throw new IllegalArgumentException("Email already exists: " + userFormDto.getEmail());
                }
            });
            userToUpdate.setEmail(userFormDto.getEmail());
            log.debug("updateUser: Email changed to: {}", userFormDto.getEmail());
        }

        if (StringUtils.hasText(userFormDto.getPassword())) {
            if (userFormDto.getPassword().length() < 8) {
                throw new IllegalArgumentException("New password must be at least 8 characters long.");
            }
            userToUpdate.setPasswordHash(passwordEncoder.encode(userFormDto.getPassword()));
            log.debug("updateUser: Password updated for UserID: {}", userFormDto.getUserId());
        }

        Role selectedRoleOnForm = roleRepository.findById(userFormDto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Selected role not found with ID: " + userFormDto.getRoleId()));
        String selectedRoleName = selectedRoleOnForm.getRoleName();

        Department finalDepartmentToAssign = null;
        Role finalRoleToAssignToUser = selectedRoleOnForm;

        if (DIRECTOR_ROLE_NAME.equals(selectedRoleName)) {
            log.debug("updateUser: Director role selected. Department will be null.");
            finalDepartmentToAssign = null;
        } else if (FINANCE_ROLE_NAME.equals(selectedRoleName)) {
            finalDepartmentToAssign = departmentRepository.findAll().stream()
                    .filter(d -> FINANCE_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: '" + FINANCE_DEPARTMENT_NAME + "' department not found."));
            log.debug("updateUser: Finance role selected. Department set to '{}'.", FINANCE_DEPARTMENT_NAME);
        } else if (ADMIN_ROLE_NAME.equals(selectedRoleName)) {
            finalDepartmentToAssign = departmentRepository.findAll().stream()
                    .filter(d -> ADMINISTRATION_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: '" + ADMINISTRATION_DEPARTMENT_NAME + "' department not found."));
            log.debug("updateUser: Admin role selected. Department set to '{}'.", ADMINISTRATION_DEPARTMENT_NAME);
        } else if (AUDITOR_ROLE_NAME.equals(selectedRoleName)) {
            finalDepartmentToAssign = departmentRepository.findAll().stream()
                    .filter(d -> AUDIT_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: '" + AUDIT_DEPARTMENT_NAME + "' department not found."));
            log.debug("updateUser: Auditor role selected. Department set to '{}'.", AUDIT_DEPARTMENT_NAME);
        } else if (MANAGER_ROLE_NAME.equals(selectedRoleName)) {
            if (userFormDto.getDepartmentId() == null) {
                throw new IllegalArgumentException("Department is required for the Manager role.");
            }
            finalDepartmentToAssign = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department for Manager role not found with ID: " + userFormDto.getDepartmentId()));
            if (PROCUREMENT_DEPARTMENT_NAME.equals(finalDepartmentToAssign.getDepartmentName())) {
                finalRoleToAssignToUser = roleRepository.findAll().stream()
                        .filter(r -> PROCUREMENT_MANAGER_ROLE_NAME.equals(r.getRoleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Critical: '" + PROCUREMENT_MANAGER_ROLE_NAME + "' role not found."));
                log.debug("updateUser: Manager role in Procurement dept. Actual role set to '{}'.", PROCUREMENT_MANAGER_ROLE_NAME);
            } else {
                log.debug("updateUser: Manager role in dept '{}'. Actual role remains '{}'.", finalDepartmentToAssign.getDepartmentName(), MANAGER_ROLE_NAME);
            }
        } else { // Employee or other non-special roles
            if (userFormDto.getDepartmentId() == null) {
                throw new IllegalArgumentException("Department is required for the role: " + selectedRoleName);
            }
            finalDepartmentToAssign = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found with ID: " + userFormDto.getDepartmentId()));
            log.debug("updateUser: Standard role '{}' assigned to department '{}'.", selectedRoleName, finalDepartmentToAssign.getDepartmentName());
        }

        userToUpdate.setDepartment(finalDepartmentToAssign);
        userToUpdate.getRoles().clear();
        userToUpdate.getRoles().add(finalRoleToAssignToUser);

        // Handle department manager changes
        // 1. If user was manager of `oldDepartmentOfUser` but no longer is manager of *that specific department*
        if (wasManagerOfOldDepartment) {
            boolean isStillManagerOfOldDept = finalDepartmentToAssign != null &&
                    finalDepartmentToAssign.getDepartmentId().equals(oldDepartmentOfUser.getDepartmentId()) &&
                    (MANAGER_ROLE_NAME.equals(finalRoleToAssignToUser.getRoleName()) || PROCUREMENT_MANAGER_ROLE_NAME.equals(finalRoleToAssignToUser.getRoleName()));
            if (!isStillManagerOfOldDept) {
                log.debug("updateUser: User {} is no longer manager of old department {}. Clearing manager link.", userToUpdate.getEmail(), oldDepartmentOfUser.getDepartmentName());
                oldDepartmentOfUser.setManagerUser(null);
                departmentRepository.save(oldDepartmentOfUser);
            }
        }

        // 2. If user is now manager of `finalDepartmentToAssign`
        boolean isNowManagerOfNewAssignedDept = finalDepartmentToAssign != null &&
                (MANAGER_ROLE_NAME.equals(finalRoleToAssignToUser.getRoleName()) || PROCUREMENT_MANAGER_ROLE_NAME.equals(finalRoleToAssignToUser.getRoleName()));

        if (isNowManagerOfNewAssignedDept) {
            // Check if this new department assignment is different from the old one or if they were not a manager before
            boolean newManagerAssignmentHappened = oldDepartmentOfUser == null || // Was not in a dept before
                    !oldDepartmentOfUser.getDepartmentId().equals(finalDepartmentToAssign.getDepartmentId()) || // Dept changed
                    !wasManagerOfOldDepartment; // Role changed to manager, or was not manager before

            if (newManagerAssignmentHappened || (finalDepartmentToAssign.getManagerUser() == null || !finalDepartmentToAssign.getManagerUser().getUserId().equals(userToUpdate.getUserId())) ) {
                // If new department already has a manager, and it's not this user, clear the old one.
                if (finalDepartmentToAssign.getManagerUser() != null && !finalDepartmentToAssign.getManagerUser().getUserId().equals(userToUpdate.getUserId())) {
                    log.warn("updateUser: Department {} already had manager {}. Overwriting with {}.",
                            finalDepartmentToAssign.getDepartmentName(),
                            finalDepartmentToAssign.getManagerUser().getEmail(),
                            userToUpdate.getEmail());
                    // Note: The old manager's user entity itself isn't changed here, only the department's link to them.
                    // If the old manager was reassigned, their new role/dept would handle their side.
                }
                log.debug("updateUser: Assigning user {} as manager of department {}.", userToUpdate.getEmail(), finalDepartmentToAssign.getDepartmentName());
                finalDepartmentToAssign.setManagerUser(userToUpdate);
                departmentRepository.save(finalDepartmentToAssign);
            }
        }


        userToUpdate.setFormerEmployee(userFormDto.isFormerEmployee());
        log.debug("updateUser: Set FormerEmployee to: {}", userFormDto.isFormerEmployee());

        userRepository.save(userToUpdate);
        log.info("updateUser: User {} (ID: {}) updated successfully. Role: {}, Department: {}.",
                userToUpdate.getEmail(), userToUpdate.getUserId(),
                finalRoleToAssignToUser.getRoleName(),
                finalDepartmentToAssign != null ? finalDepartmentToAssign.getDepartmentName() : "N/A");
    }


    @Override
    @Transactional
    public void toggleUserActiveStatus(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setFormerEmployee(!user.isFormerEmployee());
        userRepository.save(user);
        log.info("Toggled active status for UserID: {}. Is now former employee: {}", userId, user.isFormerEmployee());
    }
}