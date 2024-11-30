package minizoom;

import javax.swing.*;
import java.awt.*;

public class MiniZoom {

    public static void main(String[] args) {
        // Tạo cửa sổ chính
        JFrame frame = new JFrame("MiniZoom");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 300);
        frame.setLayout(new GridBagLayout()); // Sử dụng GridBagLayout

        // Đặt biểu tượng cho cửa sổ
        frame.setIconImage(new ImageIcon("C:\\Users\\haduc\\OneDrive\\Documents\\NetBeansProjects\\MiniZoom\\Image\\MiniZoom.PNG").getImage());

        // Tạo nút "Tạo Phòng"
        JButton serverButton = new JButton("Tạo Phòng");
        serverButton.setFont(new Font("Arial", Font.BOLD, 14));
        serverButton.setBackground(new Color(0, 255, 0));
        serverButton.setForeground(Color.BLACK);
        serverButton.addActionListener(e -> {
            frame.dispose();
            Server.startServer();
        });

        // Tạo nút "Tham gia Phòng"
        JButton clientButton = new JButton("Tham gia Phòng");
        clientButton.setFont(new Font("Arial", Font.BOLD, 14));
        clientButton.setBackground(new Color(0, 255, 0));
        clientButton.setForeground(Color.BLACK);
        clientButton.addActionListener(e -> {
            frame.dispose();
            Client.startClient();
        });

        // Tạo lưu ý
        JLabel noteLabel = new JLabel("Lưu ý: Tạm Thời Chỉ Dùng Cho Mạng LAN Hoặc Dùng Cùng Một Mạng Mới Có Thể Kết Nối!");
        noteLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        noteLabel.setForeground(Color.RED);

        // Thêm các nút và lưu ý vào frame
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0); // Khoảng cách giữa các thành phần

        // Thêm lưu ý ở trên
        gbc.gridy = 0;
        frame.add(noteLabel, gbc); // Thêm lưu ý

        // Thêm nút "Tạo Phòng"
        gbc.gridy = 1;
        frame.add(serverButton, gbc);

        // Thêm nút "Tham gia Phòng"
        gbc.gridy = 2;
        frame.add(clientButton, gbc);

        // Hiển thị cửa sổ
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
