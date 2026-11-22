# SIMATS ENGINEERING — PROGRAMMING IN JAVA (CSA09)
## Assignment 4 – CO4: Event-Driven GUI Design (Comprehensive Report & Code)

* **Course:** Programming in Java (CSA09)
* **Course Outcome:** CO4 – Applets, Event Delegation Model, Listeners & Adapter Classes, Inner & Anonymous Classes, AWT & Swing Components, Layout Managers, Menus & Dialogs, and GUI–JDBC Integration
* **Student Name:** `<Your Name>`
* **Register Number:** `<Your Register Number>`
* **GitHub Repository:** `https://github.com/<your-username>/CSA09-<RegisterNumber>-Assignment4`
* **Java Version:** JDK 17+[cite: 1]

---

## Table of Contents
1. [Task 1: GUI Design Review (`FeeWindow`)](#task-1-gui-design-review-feewindow)
2. [Task 2: From Requirement to Event-Driven Design (Outpatient Desk)](#task-2-from-requirement-to-event-driven-design-outpatient-desk)
3. [Task 3: From Sketch to Layout (Campus Library Search)](#task-3-from-sketch-to-layout-campus-library-search)
4. [Task 4: Event-Handling Forensics (`DrawPad`)](#task-4-event-handling-forensics-drawpad)
5. [Task 5: Same Screen, Different Event-Handling Styles (`Converter`)](#task-5-same-screen-different-event-handling-styles-converter)
6. [Task 6: Design Challenge (Campus Event Registration System)](#task-6-design-challenge-campus-event-registration-system)

---

## Task 1: GUI Design Review (`FeeWindow`)

### Task 1.1 – Diagnose the Design

1. **Lack of Separation of Concerns (Tight Coupling):**
   * *Defect:* UI component creation, input parsing, JDBC database connections, and business logic all reside inside `FeeWindow.actionPerformed()`.
   * *Impact:* Modifying database credentials or SQL tables forces changes to the GUI class, and business logic cannot be unit-tested without launching a display window.
2. **String Reference Equality (`==`) Instead of Value Equality (`.equals()`):**
   * *Defect:* `if (e.getActionCommand() == "Pay")` compares object memory references rather than string contents.
   * *Impact:* If an action command string is constructed dynamically rather than interned, the comparison evaluates to `false` and clicking **Pay** silently does nothing.
3. **Blocking the Event-Dispatch Thread (EDT):**
   * *Defect:* `DriverManager.getConnection()`, `executeUpdate()`, and `Thread.sleep(3000)` execute directly on the UI event thread[cite: 2].
   * *Impact:* The entire window freezes for over 3 seconds—buttons become unclickable, text fields ignore typing, and the OS may mark the application as unresponsive.
4. **SQL Injection Vulnerability:**
   * *Defect:* User input (`regNo.getText()`) is concatenated directly into the SQL string (`"INSERT INTO fees VALUES('" + regNo.getText() + ...`)[cite: 2].
   * *Impact:* Malicious input (e.g., `'; DROP TABLE fees; --`) can manipulate the SQL query structure and corrupt or destroy database records.
5. **Resource Leaks & Unhandled Checked Exceptions:**
   * *Defect:* `getConnection()`, `executeUpdate()`, and `Thread.sleep()` throw checked exceptions (`SQLException`, `InterruptedException`) that are neither caught nor declared, causing compilation errors[cite: 2]. Furthermore, `Connection` and `Statement` are never closed.
   * *Impact:* The program fails to compile as written, and if `throws` were added, unclosed JDBC connections would exhaust the database connection pool.
6. **Unhandled Invalid/Empty Input & Broken `Receipt` Button:**
   * *Defect:* `Double.parseDouble(amount.getText())` throws an uncaught `NumberFormatException` on empty or alphabetic input[cite: 2]. Additionally, the `else` branch catches *every* button other than `"Pay"`[cite: 2].
   * *Impact:* Clicking **Pay** with an empty box crashes the event handler; clicking **Receipt** erroneously clears both text fields instead of generating a receipt[cite: 2].
7. **Poor User Feedback & Unclosable Window:**
   * *Defect:* `"Paid"` is printed only to `System.out` (the developer console), and no `WindowListener` is attached to the `Frame`[cite: 2].
   * *Impact:* End users see no confirmation on the GUI window, and clicking the window's close (`X`) button does nothing.

---

### Task 1.2 – Repair the Design

* **`FeeService` (Service & Data Access Layer):** Validates inputs, uses parameterized `PreparedStatement` queries, manages JDBC resources with `try-with-resources`, and returns status strings.
* **`FeeWindow` (Presentation Layer):** Collects inputs, offloads payment processing to a background `SwingWorker`, updates a GUI `Label`, and handles window closing cleanly.

```java
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.SwingWorker;

// Service Layer: Validation & JDBC Persistence
class FeeService {
    private static final String URL = "jdbc:mysql://localhost:3306/college_db";
    private static final String USER = "root";
    private static final String PWD = "password";

    public String processPayment(String regNo, String amountText) throws Exception {
        if (regNo == null || regNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Register number cannot be empty.");
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText.trim());
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric amount.");
        }

        String sql = "INSERT INTO fees (reg_no, amount) VALUES (?, ?)";
        try (Connection c = DriverManager.getConnection(URL, USER, PWD);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, regNo.trim());
            ps.setDouble(2, amount);
            ps.executeUpdate();
            Thread.sleep(3000); // Simulated bank confirmation off the EDT
        }
        return "Payment of Rs." + String.format("%.2f", amount) + " confirmed for " + regNo.trim();
    }

    public String generateReceipt(String regNo) {
        if (regNo == null || regNo.trim().isEmpty()) {
            return "Enter Register No to generate receipt.";
        }
        return "Receipt generated for Register No: " + regNo.trim();
    }
}

// Presentation Layer: AWT Window
public class FeeWindow extends Frame {
    private final TextField regNo = new TextField(15);
    private final TextField amount = new TextField(15);
    private final Label statusLabel = new Label("Ready");
    private final Button pay = new Button("Pay");
    private final Button clear = new Button("Clear");
    private final Button receipt = new Button("Receipt");
    private final FeeService feeService = new FeeService();

    public FeeWindow() {
        super("Student Fee Payment");
        setLayout(new GridLayout(5, 2, 6, 6));

        add(new Label("Register No:")); add(regNo);
        add(new Label("Amount:"));      add(amount);
        add(pay);                       add(clear);
        add(receipt);                   add(statusLabel);

        pay.addActionListener(e -> handlePay());
        clear.addActionListener(e -> {
            regNo.setText("");
            amount.setText("");
            statusLabel.setText("Cleared");
        });
        receipt.addActionListener(e -> statusLabel.setText(feeService.generateReceipt(regNo.getText())));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        setSize(400, 220);
    }

    private void handlePay() {
        pay.setEnabled(false);
        statusLabel.setText("Processing payment...");
        String reg = regNo.getText();
        String amt = amount.getText();

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return feeService.processPayment(reg, amt);
            }

            @Override
            protected void done() {
                try {
                    statusLabel.setText(get());
                } catch (Exception ex) {
                    String msg = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
                    statusLabel.setText("Error: " + msg);
                } finally {
                    pay.setEnabled(true);
                }
            }
        }.execute();
    }

    public static void main(String[] args) {
        new FeeWindow().setVisible(true);
    }
}
