package backend;

import java.util.Objects;

/**
 * Base Employee class shared by all employee types (Cashier, Manager, etc.).
 * Additional specialized behavior belongs in subclasses or service layer classes.
 */
public abstract class Employee {
    private final int id;                 // Unique numeric identifier (e.g., from DB sequence)
    private String firstName;
    private String lastName;
    private String email;
    private EmployeeRole role;            // CASHIER or MANAGER (extendable)
    private boolean active;               // Soft delete / employment status
    
    protected Employee(int id, String firstName, String lastName, String email, EmployeeRole role) {
        this.id = id;
        this.firstName = Objects.requireNonNull(firstName, "firstName");
        this.lastName = Objects.requireNonNull(lastName, "lastName");
        this.email = Objects.requireNonNull(email, "email");
        this.role = Objects.requireNonNull(role, "role");
        this.active = true;
    }

    // -------- Getters / Setters --------

    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = Objects.requireNonNull(firstName); }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = Objects.requireNonNull(lastName); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = Objects.requireNonNull(email); }
    public EmployeeRole getRole() { return role; }
    protected void setRole(EmployeeRole role) { this.role = Objects.requireNonNull(role); }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // -------- Convenience --------
    public String getFullName() { return firstName + " " + lastName; }

    @Override
    public String toString() {
        return "Employee{" +
            "id=" + id +
            ", name='" + getFullName() + '\'' +
            ", email='" + email + '\'' +
            ", role=" + role +
            ", active=" + active +
            '}';
    }
}
