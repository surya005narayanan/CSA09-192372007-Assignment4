import java.awt.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

// Domain Model
class Registration {
    int regId;
    String studentId, studentName, department, eventName, timestamp;

    Registration(int regId, String studentId, String studentName, String department, String eventName, String timestamp) {
        this.regId = regId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.department = department;
        this.eventName = eventName;
        this.timestamp = timestamp;
    }
}

// Data Access Layer (JDBC)
class RegistrationDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/campus_events";
    private static final String USER = "root";
    private static final String PWD = "password";

    public boolean isDuplicate(String studentId, String eventName) throws SQLException {
        String sql = "SELECT 1 FROM registrations WHERE student_id = ? AND event_name = ?";
        try (Connection c = DriverManager.getConnection(URL, USER, PWD);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, eventName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(String studentId, String name, String dept, String event) throws SQLException {
        String sql = "INSERT INTO registrations (student_id, student_name, department, event_name) VALUES (?, ?, ?, ?)";
        try (Connection c = DriverManager.getConnection(URL, USER, PWD);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, name);
            ps.setString(3, dept);
            ps.setString(4, event);
            ps.executeUpdate();
        }
    }

    public List<Registration> fetchByEvent(String eventFilter) throws SQLException {
        boolean all = "All Events".equals(eventFilter);
        String sql = all
            ? "SELECT * FROM registrations ORDER BY reg_id DESC"
            : "SELECT * FROM registrations WHERE event_name = ? ORDER BY reg_id DESC";

        List<Registration> list = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(URL, USER, PWD);
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (!all) ps.setString(1, eventFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Registration(
                        rs.getInt("reg_id"),
                        rs.getString("student_id"),
                        rs.getString("student_name"),
                        rs.getString("department"),
                        rs.getString("event_name"),
                        rs.getString("registered_at")
                    ));
                }
            }
        }
        return list;
    }
}

// Service Layer
class RegistrationService {
    private final RegistrationDAO dao = new RegistrationDAO();

    public void register(String studentId, String name, String dept, String event) throws Exception {
        if (studentId == null || !studentId.trim().matches("[A-Za-z0-9]{5,15}")) {
            throw new IllegalArgumentException("Student ID must be 5-15 alphanumeric characters.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Student Name cannot be empty.");
        }
        if (dao.isDuplicate(studentId.trim(), event)) {
            throw new IllegalStateException("Duplicate Registration: Student " + studentId.trim() + " is already registered for " + event + ".");
        }
        dao.insert(studentId.trim(), name.trim(), dept, event);
    }

    public List<Registration> getRegistrations(String eventFilter) throws SQLException {
        return dao.fetchByEvent(eventFilter);
    }

    public void exportCsv(DefaultTableModel model, File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                pw.print(model.getColumnName(c) + (c < model.getColumnCount() - 1 ? "," : ""));
            }
            pw.println();
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    pw.print("\"" + model.getValueAt(r, c) + "\"" + (c < model.getColumnCount() - 1 ? "," : ""));
                }
                pw.println();
            }
        }
    }
}

// Presentation Layer (Swing UI)
public class CampusEventApp extends JFrame {
    private final RegistrationService service = new RegistrationService();
    private final JTextField idField = new JTextField(12);
    private final JTextField nameField = new JTextField(15);
    private final JComboBox<String> deptBox = new JComboBox<>(new String[]{"CSE", "ECE", "EEE", "MECH", "IT"});
    private final JComboBox<String> eventBox = new JComboBox<>(new String[]{"Hackathon", "RoboWars", "Debate", "Cultural Dance"});
    private final JComboBox<String> filterBox = new JComboBox<>(new String[]{"All Events", "Hackathon", "RoboWars", "Debate", "Cultural Dance"});
    private final JButton registerBtn = new JButton("Register Student");
    private final JLabel statusLabel = new JLabel(" Ready");
    private final DefaultTableModel tableModel;

    public CampusEventApp() {
        super("Campus Event Registration Manager");
        setLayout(new BorderLayout(8, 8));

        // Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportItem = new JMenuItem("Export CSV");
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Top Form Panel
        JPanel formPanel = new JPanel(new GridLayout(2, 1));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Student ID:")); row1.add(idField);
        row1.add(new JLabel("Name:"));       row1.add(nameField);
        row1.add(new JLabel("Dept:"));       row1.add(deptBox);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("Event:"));      row2.add(eventBox);
        row2.add(registerBtn);
        row2.add(new JLabel("   |   Filter:")); row2.add(filterBox);

        formPanel.add(row1);
        formPanel.add(row2);
        add(formPanel, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"Reg ID", "Student ID", "Student Name", "Department", "Event", "Registered At"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        add(new JScrollPane(new JTable(tableModel)), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Listeners
        registerBtn.addActionListener(e -> handleRegister());
        filterBox.addActionListener(e -> loadTableData());
        exportItem.addActionListener(e -> handleExport());
        exitItem.addActionListener(e -> dispose());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 480);
        loadTableData();
    }

    private void handleRegister() {
        registerBtn.setEnabled(false);
        statusLabel.setText(" Registering student...");
        String id = idField.getText(), name = nameField.getText();
        String dept = (String) deptBox.getSelectedItem();
        String event = (String) eventBox.getSelectedItem();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.register(id, name, dept, event);
                return null;
            }
            @Override
            protected void done() {
                registerBtn.setEnabled(true);
                try {
                    get();
                    idField.setText("");
                    nameField.setText("");
                    statusLabel.setText(" Registration confirmed for " + id);
                    loadTableData();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(CampusEventApp.this, msg, "Registration Rejected", JOptionPane.WARNING_MESSAGE);
                    statusLabel.setText(" Error: " + msg);
                }
            }
        }.execute();
    }

    private void loadTableData() {
        String selectedEvent = (String) filterBox.getSelectedItem();
        statusLabel.setText(" Loading registrations...");

        new SwingWorker<List<Registration>, Void>() {
            @Override
            protected List<Registration> doInBackground() throws Exception {
                return service.getRegistrations(selectedEvent);
            }
            @Override
            protected void done() {
                try {
                    List<Registration> rows = get();
                    tableModel.setRowCount(0);
                    for (Registration r : rows) {
                        tableModel.addRow(new Object[]{r.regId, r.studentId, r.studentName, r.department, r.eventName, r.timestamp});
                    }
                    statusLabel.setText(" Displaying " + rows.size() + " registration(s).");
                } catch (Exception ex) {
                    statusLabel.setText(" Database load error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void handleExport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("registrations.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    service.exportCsv(tableModel, file);
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(CampusEventApp.this, "Exported to " + file.getAbsolutePath());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(CampusEventApp.this, "Export failed: " + ex.getMessage());
                    }
                }
            }.execute();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CampusEventApp().setVisible(true));
    }
}
