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
    private final com.polatholding.procurementsystem.repository.DatabaseHelperRepository dbHelper; // For createUser's SP calls

    // Role Names - Ensure these EXACTLY match RoleName in your Roles TABLE
    public static final String AUDITOR_ROLE_NAME = "Auditor";
    public static final String DIRECTOR_ROLE_NAME = "Director";
    public static final String ADMIN_ROLE_NAME = "Admin";
    public static final String FINANCE_ROLE_NAME = "Finance Officer";
    public static final String MANAGER_ROLE_NAME = "Manager"; // Base Manager role (selected on form)
    public static final String PROCUREMENT_MANAGER_ROLE_NAME = "ProcurementManager"; // Actual derived role

    // Department Names
    public static final String PROCUREMENT_DEPARTMENT_NAME = "Procurement";
    public static final String FINANCE_DEPARTMENT_NAME = "Finance";


    public AdminServiceImpl(UserRepository userRepository,
                            DepartmentRepository departmentRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder,
                            com.polatholding.procurementsystem.repository.DatabaseHelperRepository dbHelper) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.dbHelper = dbHelper;
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
        if (!StringUtils.hasText(userFormDto.getPassword())) { // Ensure password is not null or empty
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

        Role selectedRoleOnForm = roleRepository.findById(userFormDto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Selected role not found with ID: " + userFormDto.getRoleId()));
        String selectedRoleName = selectedRoleOnForm.getRoleName();

        Department finalDepartmentToAssign = null;
        Role finalRoleToAssign = selectedRoleOnForm;

        if (Set.of(AUDITOR_ROLE_NAME, DIRECTOR_ROLE_NAME, ADMIN_ROLE_NAME).contains(selectedRoleName)) {
            log.debug("createUser: Global role '{}' selected. Department will be null.", selectedRoleName);
            // Department is already null
        } else if (FINANCE_ROLE_NAME.equals(selectedRoleName)) {
            Department financeDept = departmentRepository.findAll().stream()
                    .filter(d -> FINANCE_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: '" + FINANCE_DEPARTMENT_NAME + "' department not found."));
            finalDepartmentToAssign = financeDept;
            log.debug("createUser: Finance role '{}' selected. Department set to '{}'.", selectedRoleName, financeDept.getDepartmentName());
        } else if (MANAGER_ROLE_NAME.equals(selectedRoleName)) {
            if (userFormDto.getDepartmentId() == null) {
                throw new IllegalArgumentException("Department is required for the Manager role.");
            }
            Department assignedDeptForManager = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found for Manager role with ID: " + userFormDto.getDepartmentId()));
            finalDepartmentToAssign = assignedDeptForManager;

            if (PROCUREMENT_DEPARTMENT_NAME.equals(assignedDeptForManager.getDepartmentName())) {
                finalRoleToAssign = roleRepository.findAll().stream()
                        .filter(r -> PROCUREMENT_MANAGER_ROLE_NAME.equals(r.getRoleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Critical: '" + PROCUREMENT_MANAGER_ROLE_NAME + "' role not found."));
                log.debug("createUser: Manager role in Procurement dept. Actual role set to '{}'.", PROCUREMENT_MANAGER_ROLE_NAME);
            } else {
                log.debug("createUser: Manager role in dept '{}'. Actual role set to '{}'.", assignedDeptForManager.getDepartmentName(), MANAGER_ROLE_NAME);
            }
        } else { // Employee or other non-special roles
            if (userFormDto.getDepartmentId() == null) {
                throw new IllegalArgumentException("Department is required for the role: " + selectedRoleName);
            }
            finalDepartmentToAssign = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found with ID: " + userFormDto.getDepartmentId()));
            log.debug("createUser: Standard role '{}' assigned to department '{}'.", selectedRoleName, finalDepartmentToAssign.getDepartmentName());
        }

        newUser.setDepartment(finalDepartmentToAssign);
        newUser.setRoles(Collections.singleton(finalRoleToAssign)); // Assign single role
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setFormerEmployee(userFormDto.isFormerEmployee());

        Integer deptIdForSp = finalDepartmentToAssign != null ? finalDepartmentToAssign.getDepartmentId() : null;
        dbHelper.addUser(newUser.getFirstName(), newUser.getLastName(), newUser.getEmail(), newUser.getPasswordHash(), deptIdForSp);

        // Fetch the user just created via SP to get its ID, then assign role via SP
        // This is because sp_AddUser does not return the ID and also might not handle the UserRoles table itself
        User createdUserBySp = userRepository.findByEmail(newUser.getEmail())
                .orElseThrow(() -> new IllegalStateException("User creation via SP failed or user not found immediately after: " + newUser.getEmail()));
        dbHelper.assignUserRole(createdUserBySp.getUserId(), finalRoleToAssign.getRoleName());

        // If the created user is a manager of the assigned department, update the department record
        if (finalDepartmentToAssign != null &&
                (MANAGER_ROLE_NAME.equals(finalRoleToAssign.getRoleName()) || PROCUREMENT_MANAGER_ROLE_NAME.equals(finalRoleToAssign.getRoleName()))) {
            log.debug("createUser: User {} is a manager. Updating department {} manager link.", createdUserBySp.getEmail(), finalDepartmentToAssign.getDepartmentName());
            finalDepartmentToAssign.setManagerUser(createdUserBySp);
            departmentRepository.save(finalDepartmentToAssign);
        }
        log.info("createUser: User {} created successfully with role {} and department {}.",
                newUser.getEmail(), finalRoleToAssign.getRoleName(),
                finalDepartmentToAssign != null ? finalDepartmentToAssign.getDepartmentName() : "N/A");
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
            // If actual role is ProcurementManager, the form should show "Manager" role selected
            // and "Procurement" department.
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

        // Store original department and if they were a manager for comparison
        Department oldDepartmentOfUser = userToUpdate.getDepartment();
        boolean wasManagerOfOldDepartment = oldDepartmentOfUser != null &&
                oldDepartmentOfUser.getManagerUser() != null &&
                oldDepartmentOfUser.getManagerUser().getUserId().equals(userToUpdate.getUserId());

        // Update basic fields
        userToUpdate.setFirstName(userFormDto.getFirstName());
        userToUpdate.setLastName(userFormDto.getLastName());

        // Update email if changed (check for uniqueness)
        if (!userToUpdate.getEmail().equalsIgnoreCase(userFormDto.getEmail())) {
            userRepository.findByEmail(userFormDto.getEmail()).ifPresent(existingUserWithNewEmail -> {
                if (!existingUserWithNewEmail.getUserId().equals(userToUpdate.getUserId())) {
                    throw new IllegalArgumentException("Email already exists: " + userFormDto.getEmail());
                }
            });
            userToUpdate.setEmail(userFormDto.getEmail());
            log.debug("updateUser: Email changed to: {}", userFormDto.getEmail());
        }

        // Update password if provided
        if (StringUtils.hasText(userFormDto.getPassword())) {
            if (userFormDto.getPassword().length() < 8) {
                throw new IllegalArgumentException("New password must be at least 8 characters long.");
            }
            userToUpdate.setPasswordHash(passwordEncoder.encode(userFormDto.getPassword()));
            log.debug("updateUser: Password updated for UserID: {}", userFormDto.getUserId());
        }

        // --- Determine Final Department and Role ---
        Role selectedRoleOnForm = roleRepository.findById(userFormDto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Selected role not found with ID: " + userFormDto.getRoleId()));
        String selectedRoleNameOnForm = selectedRoleOnForm.getRoleName();

        Department finalDepartmentToAssign = null;
        Role finalRoleToAssignToUser = selectedRoleOnForm; // Start with the role selected on the form

        if (Set.of(AUDITOR_ROLE_NAME, DIRECTOR_ROLE_NAME, ADMIN_ROLE_NAME).contains(selectedRoleNameOnForm)) {
            log.debug("updateUser: Global role '{}' selected. Department will be null.", selectedRoleNameOnForm);
            // finalDepartmentToAssign is already null
        } else if (FINANCE_ROLE_NAME.equals(selectedRoleNameOnForm)) {
            Department financeDept = departmentRepository.findAll().stream()
                    .filter(d -> FINANCE_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: '" + FINANCE_DEPARTMENT_NAME + "' department not found."));
            finalDepartmentToAssign = financeDept;
            log.debug("updateUser: Finance role '{}' selected. Department forced to '{}'.", selectedRoleNameOnForm, financeDept.getDepartmentName());
        } else if (MANAGER_ROLE_NAME.equals(selectedRoleNameOnForm)) {
            if (userFormDto.getDepartmentId() == null) {
                throw new IllegalArgumentException("Department is required for the Manager role.");
            }
            Department assignedDeptForManager = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department for Manager role not found with ID: " + userFormDto.getDepartmentId()));
            finalDepartmentToAssign = assignedDeptForManager;

            if (PROCUREMENT_DEPARTMENT_NAME.equals(assignedDeptForManager.getDepartmentName())) {
                finalRoleToAssignToUser = roleRepository.findAll().stream()
                        .filter(r -> PROCUREMENT_MANAGER_ROLE_NAME.equals(r.getRoleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Critical: '" + PROCUREMENT_MANAGER_ROLE_NAME + "' role not found."));
                log.debug("updateUser: Manager role in Procurement dept. Actual role set to '{}'.", PROCUREMENT_MANAGER_ROLE_NAME);
            } else {
                log.debug("updateUser: Manager role in dept '{}'. Actual role set to '{}'.", assignedDeptForManager.getDepartmentName(), MANAGER_ROLE_NAME);
            }
        } else { // Employee or other non-special roles
            if (userFormDto.getDepartmentId() == null) {
                throw new IllegalArgumentException("Department is required for the role: " + selectedRoleNameOnForm);
            }
            finalDepartmentToAssign = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found with ID: " + userFormDto.getDepartmentId()));
            log.debug("updateUser: Standard role '{}' assigned to department '{}'.", selectedRoleNameOnForm, finalDepartmentToAssign.getDepartmentName());
        }

        // --- Update User's Department and Role Collection ---
        userToUpdate.setDepartment(finalDepartmentToAssign);
        userToUpdate.getRoles().clear(); // Clear existing roles
        userToUpdate.getRoles().add(finalRoleToAssignToUser); // Add the new role

        // --- Update Department Manager Links ---
        // 1. If user was manager of `oldDepartmentOfUser` but no longer is (different dept OR different role)
        if (wasManagerOfOldDepartment) {
            boolean stillManagerOfThisOldDept = finalDepartmentToAssign != null &&
                    finalDepartmentToAssign.getDepartmentId().equals(oldDepartmentOfUser.getDepartmentId()) &&
                    (MANAGER_ROLE_NAME.equals(finalRoleToAssignToUser.getRoleName()) || PROCUREMENT_MANAGER_ROLE_NAME.equals(finalRoleToAssignToUser.getRoleName()));
            if (!stillManagerOfThisOldDept) {
                log.debug("updateUser: User {} is no longer manager of old department {}. Clearing manager link.", userToUpdate.getEmail(), oldDepartmentOfUser.getDepartmentName());
                oldDepartmentOfUser.setManagerUser(null);
                departmentRepository.save(oldDepartmentOfUser);
            }
        }

        // 2. If user is now manager of `finalDepartmentToAssign`
        boolean isNowManager = finalDepartmentToAssign != null &&
                (MANAGER_ROLE_NAME.equals(finalRoleToAssignToUser.getRoleName()) || PROCUREMENT_MANAGER_ROLE_NAME.equals(finalRoleToAssignToUser.getRoleName()));

        if (isNowManager) {
            // Check if the department they are now manager of is different from their old one, or if they weren't a manager before
            boolean newManagerAssignment = !wasManagerOfOldDepartment ||
                    (oldDepartmentOfUser == null || !oldDepartmentOfUser.getDepartmentId().equals(finalDepartmentToAssign.getDepartmentId()));

            if (newManagerAssignment) {
                // If `finalDepartmentToAssign` already has a manager, and it's not this user, clear the old one
                if (finalDepartmentToAssign.getManagerUser() != null && !finalDepartmentToAssign.getManagerUser().getUserId().equals(userToUpdate.getUserId())) {
                    log.warn("updateUser: Department {} already had manager {}. Overwriting with {}.",
                            finalDepartmentToAssign.getDepartmentName(),
                            finalDepartmentToAssign.getManagerUser().getEmail(),
                            userToUpdate.getEmail());
                    // No need to save the old manager's department here, it's implicitly cleared by setting new one
                }
                log.debug("updateUser: Assigning user {} as manager of department {}.", userToUpdate.getEmail(), finalDepartmentToAssign.getDepartmentName());
                finalDepartmentToAssign.setManagerUser(userToUpdate);
                departmentRepository.save(finalDepartmentToAssign);
            } else {
                log.debug("updateUser: User {} continues as manager of department {}.", userToUpdate.getEmail(), finalDepartmentToAssign.getDepartmentName());
                // Ensure it's set if it somehow got detached, though usually not needed if already manager of same dept.
                if(finalDepartmentToAssign.getManagerUser() == null || !finalDepartmentToAssign.getManagerUser().getUserId().equals(userToUpdate.getUserId())) {
                    finalDepartmentToAssign.setManagerUser(userToUpdate);
                    departmentRepository.save(finalDepartmentToAssign);
                }
            }
        }

        // Update formerEmployee status
        userToUpdate.setFormerEmployee(userFormDto.isFormerEmployee());
        log.debug("updateUser: Set FormerEmployee to: {}", userFormDto.isFormerEmployee());

        // Save the user entity (JPA handles UserRoles join table changes)
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