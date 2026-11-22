import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class LibrarySearchScreen extends JFrame {
    private final JTextField searchField = new JTextField(20);
    private final Choice categoryChoice = new Choice();
    private final JButton searchBtn = new JButton("Search");
    private final JButton prevBtn = new JButton("Prev");
    private final JButton nextBtn = new JButton("Next");
    private final JButton reserveBtn = new JButton("Reserve");
    private final JLabel statusLabel = new JLabel(" Ready - Enter keyword to search.");
    private final JTable resultsTable;

    public LibrarySearchScreen() {
        super("Campus Library Search");
        setLayout(new BorderLayout(8, 8));

        // 1. NORTH: Title + Search Row
        JPanel topContainer = new JPanel(new GridLayout(2, 1));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel title = new JLabel("Campus Library Search");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        titlePanel.add(title);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        categoryChoice.add("All Categories");
        categoryChoice.add("Computer Science");
        categoryChoice.add("Mathematics");
        categoryChoice.add("Physics");
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("Category:"));
        searchPanel.add(categoryChoice);
        searchPanel.add(searchBtn);

        topContainer.add(titlePanel);
        topContainer.add(searchPanel);
        add(topContainer, BorderLayout.NORTH);

        // 2. CENTER: Expanding Results Area
        String[] cols = {"Book ID", "Title", "Author", "Category", "Availability"};
        Object[][] sampleData = {
            {"B101", "Effective Java", "Joshua Bloch", "Computer Science", "Available"},
            {"B102", "Clean Code", "Robert C. Martin", "Computer Science", "Checked Out"},
            {"B103", "Linear Algebra", "Gilbert Strang", "Mathematics", "Available"}
        };
        resultsTable = new JTable(new DefaultTableModel(sampleData, cols));
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        add(scrollPane, BorderLayout.CENTER);

        // 3. SOUTH: Navigation Buttons + Status Bar
        JPanel bottomContainer = new JPanel(new BorderLayout());
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        navPanel.add(prevBtn);
        navPanel.add(nextBtn);
        navPanel.add(reserveBtn);

        bottomContainer.add(navPanel, BorderLayout.NORTH);
        bottomContainer.add(statusLabel, BorderLayout.SOUTH);
        add(bottomContainer, BorderLayout.SOUTH);

        // Event handlers
        searchBtn.addActionListener(e -> statusLabel.setText(
            " Searching for '" + searchField.getText() + "' in " + categoryChoice.getSelectedItem()));
        reserveBtn.addActionListener(e -> {
            int row = resultsTable.getSelectedRow();
            statusLabel.setText(row >= 0
                ? " Reserved: " + resultsTable.getValueAt(row, 1)
                : " Select a book row first.");
        });

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(650, 400);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibrarySearchScreen().setVisible(true));
    }
}
