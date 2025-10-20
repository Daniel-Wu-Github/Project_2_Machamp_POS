import java.util.Objects;

/**
 * @author Ayad Masud
 */
public abstract class Employee {
    private final int id;                 
    private String firstName;
    private String lastName;
    private String email;
    // private EmployeeRole role;            
    private boolean active;               
    
    /**
     * Create a new Employee.
     *
     * @param id        unique numeric identifier for the employee (usually from DB)
     * @param firstName employee's first name
     * @param lastName  employee's last name
     * @param email     employee's email address
     * @param role      employee's role
     *
     * @throws NullPointerException
     */
    protected Employee(int id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = Objects.requireNonNull(firstName, "firstName");
        this.lastName = Objects.requireNonNull(lastName, "lastName");
        this.email = Objects.requireNonNull(email, "email");
        // this.role = Objects.requireNonNull(role, "role");
        this.active = true;
    }

    // -------- Getters / Setters --------

    /**
     * Returns the unique identifier for this employee.
     *
     * @return the employee id
     */
    public int getId() { return id; }

    /**
     * Set the employee's first name.
     *
     * @param firstName the new first name
     * @throws NullPointerException
     */
    public void setFirstName(String firstName) { this.firstName = Objects.requireNonNull(firstName); }

    /**
     * Returns the employee's last name.
     *
     * @return the last name
     */
    public String getLastName() { return lastName; }

    /**
     * Set the employee's last name.
     *
     * @param lastName the new last name
     * @throws NullPointerException
     */
    public void setLastName(String lastName) { this.lastName = Objects.requireNonNull(lastName); }

    /**
     * Returns the employee's email address.
     *
     * @return the email address
     */
    public String getEmail() { return email; }

    /**
     * Set the employee's email address.
     *
     * @param email the new email
     * @throws NullPointerException
     */
    public void setEmail(String email) { this.email = Objects.requireNonNull(email); }

    // /**
    //  * Returns the employee's role.
    //  *
    //  * @return the role
    //  */
    // public EmployeeRole getRole() { return role; }

    // /**
    //  *
    //  * @param role new role
    //  * @throws NullPointerException
    //  */
    // protected void setRole(EmployeeRole role) { this.role = Objects.requireNonNull(role); }

    /**
     * Returns whether the employee is active (not soft-deleted).
     *
     * @return {@code true} if active, {@code false} otherwise
     */
    public boolean isActive() { return active; }

    /**
     * Set the employee active flag.
     *
     * @param active {@code true} to mark active, {@code false} to mark inactive
     */
    public void setActive(boolean active) { this.active = active; }

    // -------- Convenience --------

    /**
     * Returns the employee's full name as "{firstName} {lastName}".
     *
     * @return the full name composed of first and last name
     */
    public String getFullName() { return firstName + " " + lastName; }

    /**
     * Returns a string representation of this employee suitable for logging.
     *
     * @return a human-readable representation of the employee
     */
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
