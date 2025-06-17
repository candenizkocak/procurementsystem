package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.AdminUserFormDto;
import com.polatholding.procurementsystem.dto.UserDto;
import com.polatholding.procurementsystem.model.Department;
import com.polatholding.procurementsystem.model.Role;
import com.polatholding.procurementsystem.model.User;
import com.polatholding.procurementsystem.repository.DepartmentRepository;
import com.polatholding.procurementsystem.repository.RoleRepository;
import com.polatholding.procurementsystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.polatholding.procurementsystem.repository.DatabaseHelperRepository dbHelper;

    // Role Names - Ensure these EXACTLY match RoleName in your Roles TABLE
    public static final String AUDITOR_ROLE_NAME = "Auditor";
    public static final String DIRECTOR_ROLE_NAME = "Director";
    public static final String ADMIN_ROLE_NAME = "Admin";
    public static final String FINANCE_ROLE_NAME = "Finance Officer"; // Adjusted based on your feedback

    // Department Names - Ensure these EXACTLY match DepartmentName in your Departments TABLE
    private static final String PROCUREMENT_DEPARTMENT_NAME = "Procurement";
    public static final String FINANCE_DEPARTMENT_NAME = "Finance";
    public static final String ADMIN_DEPARTMENT_NAME = "Administration";
    public static final String AUDIT_DEPARTMENT_NAME = "Audit";

    // Other Role Names
    static final String MANAGER_ROLE_NAME = "Manager"; // Base Manager role
    public static final String PROCUREMENT_MANAGER_ROLE_NAME = "ProcurementManager"; // Actual target role if Manager in Procurement Dept.


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
            // Since it's a single role, get the first one.
            // For display, it's fine to just show the one role name.
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
        // Returns roles suitable for admin assignment.
        // ProcurementManager is derived, not directly assigned.
        return roleRepository.findAll().stream()
                .filter(role -> !PROCUREMENT_MANAGER_ROLE_NAME.equals(role.getRoleName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createUser(AdminUserFormDto userFormDto) {
        System.out.println("DEBUG createUser: Starting user creation for email: " + userFormDto.getEmail());
        System.out.println("DEBUG createUser: DTO Department ID: " + userFormDto.getDepartmentId() + ", DTO Role ID: " + userFormDto.getRoleId());

        if (userRepository.findByEmail(userFormDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + userFormDto.getEmail());
        }
        if (userFormDto.getPassword() == null || userFormDto.getPassword().isEmpty()) {
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

        Role originallySelectedRoleOnForm = roleRepository.findById(userFormDto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Selected role not found with ID: " + userFormDto.getRoleId()));
        System.out.println("DEBUG createUser: Originally selected Role from DB - ID: " + originallySelectedRoleOnForm.getRoleId() + ", Name: '" + originallySelectedRoleOnForm.getRoleName() + "'");
        System.out.println("DEBUG createUser: FINANCE_ROLE_NAME constant: '" + FINANCE_ROLE_NAME + "'");


        Department finalDepartmentToAssign = null;
        Role finalRoleToAssign = originallySelectedRoleOnForm;
        String selectedRoleName = originallySelectedRoleOnForm.getRoleName();

        if (DIRECTOR_ROLE_NAME.equals(selectedRoleName)) {
            System.out.println("DEBUG createUser: Director role - no department.");
            finalDepartmentToAssign = null;
        }
        else if (ADMIN_ROLE_NAME.equals(selectedRoleName)) {
            Department adminDept = departmentRepository.findAll().stream()
                    .filter(d -> ADMIN_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: 'Administration' department not found."));
            finalDepartmentToAssign = adminDept;
        }
        else if (AUDITOR_ROLE_NAME.equals(selectedRoleName)) {
            Department auditDept = departmentRepository.findAll().stream()
                    .filter(d -> AUDIT_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: 'Audit' department not found."));
            finalDepartmentToAssign = auditDept;
        }
        else if (FINANCE_ROLE_NAME.equals(selectedRoleName)) {
            System.out.println("DEBUG createUser: Matched FINANCE_ROLE_NAME ('" + selectedRoleName + "'). Forcing Finance department.");
            Department financeDept = departmentRepository.findAll().stream()
                    .filter(d -> FINANCE_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: 'Finance' department not found in database. Ensure a department named '" + FINANCE_DEPARTMENT_NAME + "' exists."));
            finalDepartmentToAssign = financeDept;
            System.out.println("DEBUG createUser: Finance Department found and assigned. ID: " + financeDept.getDepartmentId() + ", Name: '" + financeDept.getDepartmentName() + "'");

            if (userFormDto.getDepartmentId() != null && !userFormDto.getDepartmentId().equals(financeDept.getDepartmentId())) {
                System.out.println("WARN createUser: Department selection overridden to 'Finance' for Finance role. Original attempted DTO dept ID: " + userFormDto.getDepartmentId());
            } else if (userFormDto.getDepartmentId() == null ) {
                System.out.println("INFO createUser: Department for Finance role was null in DTO or matched Finance, forced to Finance Dept ID: " + financeDept.getDepartmentId());
            }
        }
        else if (userFormDto.getDepartmentId() != null && MANAGER_ROLE_NAME.equals(selectedRoleName)) {
            System.out.println("DEBUG createUser: Matched MANAGER_ROLE_NAME ('" + selectedRoleName + "') with a department ID: " + userFormDto.getDepartmentId());
            Department selectedDeptForManager = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found with ID: " + userFormDto.getDepartmentId() + " for Manager role."));
            System.out.println("DEBUG createUser: Manager's selected department from DB - Name: '" + selectedDeptForManager.getDepartmentName() + "'");

            if (PROCUREMENT_DEPARTMENT_NAME.equals(selectedDeptForManager.getDepartmentName())) {
                System.out.println("DEBUG createUser: Manager in Procurement Department. Assigning ProcurementManager role.");
                Role procurementManagerActualRole = roleRepository.findAll().stream()
                        .filter(r -> PROCUREMENT_MANAGER_ROLE_NAME.equals(r.getRoleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Critical: '" + PROCUREMENT_MANAGER_ROLE_NAME + "' role not found in database."));
                finalRoleToAssign = procurementManagerActualRole;
                System.out.println("DEBUG createUser: Actual ProcurementManager role assigned - ID: " + procurementManagerActualRole.getRoleId() + ", Name: '" + procurementManagerActualRole.getRoleName() + "'");
            }
            finalDepartmentToAssign = selectedDeptForManager;
        }
        else {
            System.out.println("DEBUG createUser: Did NOT match specific role logic. Role: '" + selectedRoleName + "'. Applying default department assignment.");
            if (userFormDto.getDepartmentId() == null) {
                throw new IllegalArgumentException("Department is required for the selected role: " + selectedRoleName + ". Please ensure it's not a global role if no department is intended.");
            }
            finalDepartmentToAssign = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found with ID: " + userFormDto.getDepartmentId()));
            System.out.println("DEBUG createUser: Default department assignment. ID: " + finalDepartmentToAssign.getDepartmentId() + ", Name: '" + finalDepartmentToAssign.getDepartmentName() + "'");
        }

        newUser.setDepartment(finalDepartmentToAssign);
        if (finalDepartmentToAssign != null) {
            System.out.println("DEBUG createUser: Final assignment to newUser.department - ID: " + finalDepartmentToAssign.getDepartmentId() + ", Name: '" + finalDepartmentToAssign.getDepartmentName() + "'");
        } else {
            System.out.println("DEBUG createUser: Final assignment to newUser.department - NULL");
        }

        newUser.setRoles(Collections.singleton(finalRoleToAssign)); // Assign single role
        System.out.println("DEBUG createUser: Final assignment to newUser.roles - Role ID: " + finalRoleToAssign.getRoleId() + ", Name: '" + finalRoleToAssign.getRoleName() + "'");

        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setFormerEmployee(userFormDto.isFormerEmployee());

        Integer deptId = finalDepartmentToAssign != null ? finalDepartmentToAssign.getDepartmentId() : null;
        dbHelper.addUser(newUser.getFirstName(), newUser.getLastName(), newUser.getEmail(), newUser.getPasswordHash(), deptId);

        User created = userRepository.findByEmail(newUser.getEmail())
                .orElseThrow(() -> new IllegalStateException("User creation failed"));
        dbHelper.assignUserRole(created.getUserId(), finalRoleToAssign.getRoleName());
        System.out.println("DEBUG createUser: User saved via stored procedure. Email: " + newUser.getEmail());
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
        formDto.setPassword(null);

        if (user.getDepartment() != null) {
            formDto.setDepartmentId(user.getDepartment().getDepartmentId());
        } else {
            formDto.setDepartmentId(null);
        }

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            Role userRole = user.getRoles().iterator().next();
            if (PROCUREMENT_MANAGER_ROLE_NAME.equals(userRole.getRoleName()) &&
                    user.getDepartment() != null &&
                    PROCUREMENT_DEPARTMENT_NAME.equals(user.getDepartment().getDepartmentName())) {
                Role baseManagerRole = roleRepository.findAll().stream()
                        .filter(r -> MANAGER_ROLE_NAME.equals(r.getRoleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Base 'Manager' role not found."));
                formDto.setRoleId(baseManagerRole.getRoleId());
            } else {
                formDto.setRoleId(userRole.getRoleId());
            }
        } else {
            formDto.setRoleId(null);
        }
        formDto.setFormerEmployee(user.isFormerEmployee());
        return formDto;
    }

    @Override
    @Transactional
    public void updateUser(AdminUserFormDto userFormDto) {
        System.out.println("DEBUG updateUser: Starting user update for UserID: " + userFormDto.getUserId());
        User userToUpdate = userRepository.findById(userFormDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userFormDto.getUserId() + " for update."));

        if (!userToUpdate.getEmail().equalsIgnoreCase(userFormDto.getEmail())) {
            if (userRepository.findByEmail(userFormDto.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email already exists: " + userFormDto.getEmail());
            }
            userToUpdate.setEmail(userFormDto.getEmail());
            System.out.println("DEBUG updateUser: Email changed to: " + userFormDto.getEmail());
        }

        userToUpdate.setFirstName(userFormDto.getFirstName());
        userToUpdate.setLastName(userFormDto.getLastName());

        if (userFormDto.getPassword() != null && !userFormDto.getPassword().isEmpty()) {
            if (userFormDto.getPassword().length() < 8) {
                throw new IllegalArgumentException("New password must be at least 8 characters long.");
            }
            userToUpdate.setPasswordHash(passwordEncoder.encode(userFormDto.getPassword()));
            System.out.println("DEBUG updateUser: Password updated for UserID: " + userFormDto.getUserId());
        }
        if (userFormDto.getRoleId() == null) { // Should be caught by DTO validation, but good to check
            throw new IllegalArgumentException("A role must be selected for the user.");
        }

        Role originallySelectedRoleOnForm = roleRepository.findById(userFormDto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Selected role not found with ID: " + userFormDto.getRoleId()));
        System.out.println("DEBUG updateUser: Role selected on form - ID: " + originallySelectedRoleOnForm.getRoleId() + ", Name: '" + originallySelectedRoleOnForm.getRoleName() + "'");

        Department finalDepartmentToAssign = null;
        Role finalRoleToAssign = originallySelectedRoleOnForm;
        String selectedRoleName = originallySelectedRoleOnForm.getRoleName();

        if (DIRECTOR_ROLE_NAME.equals(selectedRoleName)) {
            System.out.println("DEBUG updateUser: Director role - no department.");
            finalDepartmentToAssign = null;
        }
        else if (ADMIN_ROLE_NAME.equals(selectedRoleName)) {
            Department adminDept = departmentRepository.findAll().stream()
                    .filter(d -> ADMIN_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: 'Administration' department not found."));
            finalDepartmentToAssign = adminDept;
        }
        else if (AUDITOR_ROLE_NAME.equals(selectedRoleName)) {
            Department auditDept = departmentRepository.findAll().stream()
                    .filter(d -> AUDIT_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: 'Audit' department not found."));
            finalDepartmentToAssign = auditDept;
        }
        else if (FINANCE_ROLE_NAME.equals(selectedRoleName)) {
            System.out.println("DEBUG updateUser: Matched FINANCE_ROLE_NAME. Forcing Finance department.");
            Department financeDept = departmentRepository.findAll().stream()
                    .filter(d -> FINANCE_DEPARTMENT_NAME.equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Critical: 'Finance' department not found in database."));
            finalDepartmentToAssign = financeDept;
        }
        else if (userFormDto.getDepartmentId() != null && MANAGER_ROLE_NAME.equals(selectedRoleName)) {
            System.out.println("DEBUG updateUser: Matched MANAGER_ROLE_NAME with a department.");
            Department selectedDeptForManager = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found with ID: " + userFormDto.getDepartmentId()));
            if (PROCUREMENT_DEPARTMENT_NAME.equals(selectedDeptForManager.getDepartmentName())) {
                System.out.println("DEBUG updateUser: Manager in Procurement Dept. Assigning ProcurementManager role.");
                Role procurementManagerActualRole = roleRepository.findAll().stream()
                        .filter(r -> PROCUREMENT_MANAGER_ROLE_NAME.equals(r.getRoleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Critical: '" + PROCUREMENT_MANAGER_ROLE_NAME + "' role not found in database."));
                finalRoleToAssign = procurementManagerActualRole;
            }
            finalDepartmentToAssign = selectedDeptForManager;
        }
        else {
            System.out.println("DEBUG updateUser: Default role/dept assignment logic for role: " + selectedRoleName);
            if (userFormDto.getDepartmentId() == null) {
                throw new IllegalArgumentException("Department is required for the selected role: " + selectedRoleName + ". Please ensure it's not a global role if no department is intended.");
            }
            finalDepartmentToAssign = departmentRepository.findById(userFormDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found with ID: " + userFormDto.getDepartmentId()));
        }

        userToUpdate.setDepartment(finalDepartmentToAssign);
        userToUpdate.setRoles(Collections.singleton(finalRoleToAssign));

        userToUpdate.setFormerEmployee(userFormDto.isFormerEmployee());
        System.out.println("DEBUG updateUser: Set FormerEmployee to: " + userFormDto.isFormerEmployee());

        userRepository.save(userToUpdate);
        System.out.println("DEBUG updateUser: User updated successfully. UserID: " + userToUpdate.getUserId());
    }
}