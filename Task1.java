import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.SwingWorker;

// 1. Service Layer (Business Logic & JDBC)
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
            if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
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
        return "Payment of Rs." + String.format("%.2f", amount) + " confirmed for " + regNo;
    }

    public String generateReceipt(String regNo) {
        if (regNo == null || regNo.trim().isEmpty()) return "Enter Register No for receipt.";
        return "Receipt generated for Register No: " + regNo.trim();
    }
}

// 2. Presentation Layer (AWT GUI)
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
        setLayout(new GridLayout(5, 2, 5, 5));

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
            public void windowClosing(WindowEvent e) { dispose(); }
        });

        setSize(380, 220);
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
                    statusLabel.setText("Error: " + ex.getCause().getMessage());
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
