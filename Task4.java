import java.awt.FlowLayout;
import java.awt.event.*;
import javax.swing.*;

public class ConverterStyleA extends JFrame implements ActionListener {
    private final JTextField amount = new JTextField(10);
    private final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
    private final JLabel result = new JLabel(" ");

    public ConverterStyleA() {
        super("Converter - Style A (Frame Listener)");
        setLayout(new FlowLayout());
        JButton convertButton = new JButton("Convert");
        add(new JLabel("INR")); add(amount); add(target); add(convertButton); add(result);

        convertButton.addActionListener(this);
        amount.addActionListener(this); // Task 5.2 change: +1 line

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
        pack();
    }

    @Override
    public void actionPerformed(ActionEvent e) { convert(); }

    private void convert() {
        try {
            double inr = Double.parseDouble(amount.getText().trim());
            String cur = (String) target.getSelectedItem();
            double val = inr * (cur.equals("USD") ? 0.012 : 0.011);
            String msg = String.format("%.2f %s", val, cur);
            result.setText(msg);
            JOptionPane.showMessageDialog(this, msg); // Task 5.2 dialog
        } catch (NumberFormatException ex) {
            result.setText("Invalid amount");
            JOptionPane.showMessageDialog(this, "Invalid amount", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
