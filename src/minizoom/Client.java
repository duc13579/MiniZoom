/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package minizoom;

/**
 *
 * @author haduc
 */
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

public class Client {
    private static String SERVER_IP; // Địa chỉ IP nhập từ bàn phím
    private static String clientName; // Tên của client
    private static final int PORT_SCREEN = 12345;
    private static final int PORT_CHAT = 12346;
    private static final int PORT_AUDIO = 12347;
    private static final int PORT_AUDIO_SEND = 12348; // Cổng để gửi âm thanh tới server
    private static JTextArea chatArea;
    private static JTextField chatInput;
    private static JPanel chatPanel;
    private static JLabel imageLabel; // Dùng để hiển thị hình ảnh
    private static JFrame frame;
    private static JButton toggleChatButton; // Nút bật/tắt chat
    private static boolean isMicOn = false; // Trạng thái của microphone
    private static JButton toggleMicButton; // Nút bật/tắt microphone

    public static void main(String[] args) {
        startClient();
    }

    public static void startClient() {
        // Nhập IP và tên từ người dùng
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField ipField = new JTextField();
        JTextField nameField = new JTextField();
        inputPanel.add(new JLabel("Nhập địa chỉ IP Cần Kết Nối:"));
        inputPanel.add(ipField);
        inputPanel.add(new JLabel("Nhập tên của bạn:"));
        inputPanel.add(nameField);

        int result = JOptionPane.showConfirmDialog(null, inputPanel, "Thông tin kết nối", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            SERVER_IP = ipField.getText().trim();
            clientName = nameField.getText().trim();
            if (SERVER_IP.isEmpty() || clientName.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Địa chỉ IP và tên không được để trống! Thoát chương trình.");
                return;
            }
        } else {
            return; // Thoát nếu người dùng nhấn Cancel
        }

        frame = new JFrame("Người Tham Gia - " + clientName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);
        frame.setLayout(new BorderLayout());

        // Tạo phần hiển thị hình ảnh
        imageLabel = new JLabel("Đang Chờ Chủ Phòng Chia Sẻ Màn Hình", JLabel.CENTER);
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Color.BLACK);
        imageLabel.setForeground(Color.WHITE);
        imageLabel.setFont(new Font("Arial", Font.BOLD, 16));
        imageLabel.setBorder(new EmptyBorder(5, 5, 5, 5)); // Viền xung quanh
        frame.getContentPane().add(imageLabel, BorderLayout.CENTER);
        
        frame.setIconImage(new ImageIcon("C:\\Users\\haduc\\OneDrive\\Documents\\NetBeansProjects\\MiniZoom\\Image\\MiniZoom.PNG").getImage());

        // Tạo phần chat
        chatPanel = new JPanel(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(250, 0)); // Chiều rộng cố định cho phần chat
        chatPanel.setBorder(BorderFactory.createTitledBorder("Live Chat"));
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(Color.WHITE);
        chatArea.setFont(new Font("Arial", Font.BOLD, 12));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(new EmptyBorder(5, 5, 5, 5));
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        chatInput = new JTextField();
        chatInput.addActionListener(e -> sendMessage());
        chatInput.setBorder(new EmptyBorder(5, 5, 5, 5));
        chatPanel.add(chatInput, BorderLayout.SOUTH);

        frame.getContentPane().add(chatPanel, BorderLayout.EAST);

        // Tạo thanh công cụ
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        // Nút bật/tắt chat
        toggleChatButton = new JButton("Bật/Tắt Chat");
        toggleChatButton.setFont(new Font("Arial", Font.BOLD, 12));
        toggleChatButton.setBackground(Color.green);
        toggleChatButton.setForeground(Color.black);
        toggleChatButton.addActionListener(e -> toggleChatPanel());
        controlPanel.add(toggleChatButton);

        // Nút bật/tắt microphone
        toggleMicButton = new JButton("Mic Off");
        toggleMicButton.setFont(new Font("Arial", Font.BOLD, 12));
        toggleMicButton.setBackground(Color.gray);
        toggleMicButton.setForeground(Color.black);
        toggleMicButton.addActionListener(e -> toggleMic());
        controlPanel.add(toggleMicButton);

        frame.getContentPane().add(controlPanel, BorderLayout.NORTH);

        frame.setVisible(true);

        // Bắt đầu nhận màn hình và chat
        new Thread(Client::startScreenReceiver).start();
        new Thread(Client::startChatReceiver).start();
        new Thread(Client::startAudioReceiver).start();

        // Bắt đầu gửi âm thanh từ micro tới server
        new Thread(Client::startAudioSender).start();
    }

    // Nhận và hiển thị video
    private static void startScreenReceiver() {
        while (true) {
            try (Socket socket = new Socket(SERVER_IP, PORT_SCREEN)) {
                InputStream is = socket.getInputStream();
                DataInputStream dis = new DataInputStream(is);

                while (true) {
                    int imageSize = dis.readInt();
                    byte[] imageBytes = new byte[imageSize];
                    dis.readFully(imageBytes);

                    ImageIcon imageIcon = new ImageIcon(imageBytes);

                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setIcon(imageIcon);
                        imageLabel.setText(null); // Xóa dòng chữ "Đang chờ video..." khi có dữ liệu
                    });
                }
            } catch (IOException e) {
                System.err.println("Không thể kết nối đến server để nhận video. Thử lại sau 3 giây...");
                try {
                    Thread.sleep(3000); // Đợi 3 giây trước khi thử lại
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    // Nhận và hiển thị tin nhắn chat
    private static void startChatReceiver() {
        while (true) {
            try (Socket chatSocket = new Socket(SERVER_IP, PORT_CHAT)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));

                String message;
                while ((message = reader.readLine()) != null) {
                    final String finalMessage = message;
                    SwingUtilities.invokeLater(() -> chatArea.append(finalMessage + "\n"));
                }
            } catch (IOException e) {
                System.err.println("Không thể kết nối đến server để nhận tin nhắn chat. Thử lại sau 3 giây...");
                try {
                    Thread.sleep(3000); // Đợi 3 giây trước khi thử lại
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    // Gửi tin nhắn khi người dùng nhấn Enter
    private static void sendMessage() {
        String message = chatInput.getText().trim();
        chatInput.setText("");

        if (!message.isEmpty()) {
            SwingUtilities.invokeLater(() -> chatInput.requestFocusInWindow());

            new Thread(() -> {
                while (true) {
                    try (Socket chatSocket = new Socket(SERVER_IP, PORT_CHAT);
                         PrintWriter writer = new PrintWriter(chatSocket.getOutputStream(), true)) {
                        writer.println(clientName + ": " + message);
                        break; // Gửi thành công, thoát vòng lặp
                    } catch (IOException e) {
                        System.err.println("Không thể kết nối đến server để gửi tin nhắn chat. Thử lại sau 3 giây...");
                        try {
                            Thread.sleep(3000); // Đợi 3 giây trước khi thử lại
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                    }
                }
            }).start();

            SwingUtilities.invokeLater(() -> chatArea.append("Me: " + message + "\n"));
        }
    }

    // Hàm bật/tắt khung chat
    private static void toggleChatPanel() {
        if (chatPanel.isVisible()) {
        chatPanel.setVisible(false); // Ẩn khung chat
        toggleChatButton.setText("Chat Off");
        toggleChatButton.setBackground(Color.GRAY); // Màu khi tắt chat
        } else {
            chatPanel.setVisible(true); // Hiện khung chat
            toggleChatButton.setText("Chat On");
            toggleChatButton.setBackground(Color.green); // Màu khi bật chat
        }    
    }
    
    // Hàm bật/tắt microphone
    private static void toggleMic() {
        isMicOn = !isMicOn;
        if (isMicOn) {
            toggleMicButton.setText("Mic On");
            toggleMicButton.setBackground(Color.green);
        } else {
            toggleMicButton.setText("Mic Off");
            toggleMicButton.setBackground(Color.GRAY);
        }
    }

    // Nhận và phát âm thanh từ server
    private static void startAudioReceiver() {
        while (true) {
            try (Socket socket = new Socket(SERVER_IP, PORT_AUDIO)) {
                System.out.println("Kết nối đến audio server...");

                // Cấu hình format âm thanh
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    System.err.println("Loa không được hỗ trợ.");
                    return;
                }

                // Mở dòng phát âm thanh
                SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(format);
                speakers.start();

                // Nhận dữ liệu âm thanh từ server và phát lại
                InputStream is = socket.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                byte[] buffer = new byte[4096];
                int bytesRead;

                System.out.println("Đang nhận âm thanh...");
                while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
                    speakers.write(buffer, 0, bytesRead);
                }

                speakers.drain();
                speakers.close();
            } catch (IOException | LineUnavailableException e) {
                System.err.println("Không thể kết nối đến server để nhận âm thanh. Thử lại sau 3 giây...");
                try {
                    Thread.sleep(3000); // Đợi 3 giây trước khi thử lại
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    // Gửi âm thanh từ micro tới server
    private static void startAudioSender() {
        while (true) {
            try (Socket socket = new Socket(SERVER_IP, PORT_AUDIO_SEND)) {
                System.out.println("Kết nối đến audio receiver trên server...");

                // Cấu hình format âm thanh
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    System.err.println("Microphone không được hỗ trợ.");
                    return;
                }

                // Mở dòng thu âm thanh từ micro
                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                // Gửi dữ liệu âm thanh tới server
                OutputStream os = socket.getOutputStream();
                BufferedOutputStream bos = new BufferedOutputStream(os);
                byte[] buffer = new byte[4096];

                System.out.println("Đang gửi âm thanh từ micro...");
                while (true) {
                    if (isMicOn) {
                        int bytesRead = microphone.read(buffer, 0, buffer.length);
                        if (bytesRead != -1) {
                            bos.write(buffer, 0, bytesRead);
                            bos.flush();
                        }
                    } else {
                        // Nếu mic tắt, gửi dữ liệu silence
                        byte[] silence = new byte[buffer.length];
                        bos.write(silence, 0, silence.length);
                        bos.flush();
                        Thread.sleep(100);  // Đợi một chút trước khi kiểm tra lại
                    }
                }
            } catch (IOException | LineUnavailableException | InterruptedException e) {
                try {
                    Thread.sleep(5000); // Đợi 5 giây trước khi thử lại
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }
}
