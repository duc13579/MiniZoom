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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.border.EmptyBorder;

public class Server {
    private static final int PORT_SCREEN = 12345;
    private static final int PORT_CHAT = 12346;
    private static final int PORT_AUDIO = 12347;
    private static final int PORT_AUDIO_SEND = 12348; // Cổng để nhận âm thanh từ client
    private static List<Socket> clientSockets = new ArrayList<>();
    private static List<Socket> chatSockets = new ArrayList<>();
    private static List<Socket> clientAudioSockets = new ArrayList<>();
    private static JLabel imageLabel;
    private static boolean isMicOn = true;
    private static JTextArea chatArea;
    private static JTextField chatInput;
    private static JPanel chatPanel;
    private static volatile boolean capturing = false;
    private static volatile boolean paused = false;
    private static Thread captureThread;
    private static String serverName; // Tên của server

    public static void startServer() {
        // Nhập tên server từ người dùng
        serverName = JOptionPane.showInputDialog(null, "Nhập tên của bạn (server):", "Thông Tin", JOptionPane.QUESTION_MESSAGE);
        if (serverName == null || serverName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Tên không được để trống! Thoát chương trình.");
            return;
        }
        serverName = serverName.trim();

        // Khung chính
        JFrame frame = new JFrame("Chủ Phòng - " + serverName);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        

        // Vùng hiển thị màn hình
        JPanel screenPanel = new JPanel(new BorderLayout());
        screenPanel.setBorder(BorderFactory.createTitledBorder("Màn Hình Live"));
        imageLabel = new JLabel("Nhấn 'Chia Sẻ Màn Hình' để bắt đầu", JLabel.CENTER);
        imageLabel.setFont(new Font("Arial", Font.BOLD, 15));
        screenPanel.add(imageLabel, BorderLayout.CENTER);
        frame.add(screenPanel, BorderLayout.CENTER);

        // Phần chat
        chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Live Chat"));
        chatArea = new JTextArea(15, 30);
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.BOLD, 12));
        chatArea.setBackground(Color.WHITE);
        chatInput = new JTextField();
        chatInput.addActionListener(e -> sendMessage());
        chatInput.setBorder(BorderFactory.createLineBorder(Color.BLUE, 1));
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(chatInput, BorderLayout.SOUTH);

        // Thanh công cụ với các nút chức năng
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton toggleButton = new JButton("Chia Sẻ Màn Hình");
        toggleButton.setFont(new Font("Arial", Font.BOLD, 12));
        toggleButton.setBackground(Color.GREEN);
        toggleButton.setForeground(Color.black);
        toggleButton.addActionListener(e -> toggleCapture(toggleButton));
        controlPanel.add(toggleButton);

        // Nút toggle chat
        // Nút toggle chat
        JButton toggleChatButton = new JButton("Chat On");
        toggleChatButton.setFont(new Font("Arial", Font.BOLD, 12));
        toggleChatButton.setBackground(Color.GREEN);
        toggleChatButton.setForeground(Color.black);
        toggleChatButton.addActionListener(e -> {
            // Kiểm tra trạng thái hiển thị của khung chat
            if (chatPanel.isVisible()) {
                chatPanel.setVisible(false); // Ẩn khung chat
                toggleChatButton.setText("Chat Off");
                toggleChatButton.setBackground(Color.GRAY); // Màu khi tắt chat
            } else {
                chatPanel.setVisible(true); // Hiện khung chat
                toggleChatButton.setText("Chat On");
                toggleChatButton.setBackground(Color.GREEN); // Màu khi bật chat
            }
        });
        controlPanel.add(toggleChatButton);


        // Nút bật/tắt microphone
        JButton toggleMicButton = new JButton("Mic On");
        toggleMicButton.setFont(new Font("Arial", Font.BOLD, 12));
        toggleMicButton.setBackground(Color.GREEN);
        toggleMicButton.setForeground(Color.black);
        toggleMicButton.addActionListener(e -> {
            toggleMic();
            if (isMicOn) {
                toggleMicButton.setText("Mic On");
                toggleMicButton.setBackground(Color.GREEN);
            } else {
                toggleMicButton.setText("Mic Off");
                toggleMicButton.setBackground(Color.GRAY);
            }
        });
        controlPanel.add(toggleMicButton);

        // Thêm nút thoát
        JButton exitButton = new JButton("Thoát");
        exitButton.setFont(new Font("Arial", Font.BOLD, 12));
        exitButton.setBackground(Color.red);
        exitButton.setForeground(Color.black);
        exitButton.addActionListener(e -> System.exit(0));
        controlPanel.add(exitButton);

        frame.add(controlPanel, BorderLayout.NORTH);
        frame.add(chatPanel, BorderLayout.EAST);

        frame.setVisible(true);

        // Khởi động server chat trong luồng riêng
        new Thread(Server::startChatServer).start();

        // Khởi động server âm thanh trong luồng riêng
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT_AUDIO)) {
                System.out.println("Audio server đang chạy...");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client đã kết nối: " + clientSocket.getInetAddress());

                    // Thu âm thanh và gửi tới client
                    new Thread(() -> sendAudio(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Khởi động server nhận âm thanh từ client trong luồng riêng
        new Thread(Server::startAudioReceiver).start();
    }

    private static void toggleCapture(JButton toggleButton) {
        if (capturing) {
            paused = !paused;
            toggleButton.setText(paused ? "Tiếp Tục Chia Sẻ" : "Tạm Dừng Chia Sẻ");
            toggleButton.setBackground(paused ? Color.GREEN : Color.GREEN);
        } else {
            capturing = true;
            paused = false;
            toggleButton.setText("Tạm Dừng Chia Sẻ");
            toggleButton.setBackground(Color.GREEN);
            startCapture();
        }
    }

    private static void toggleChatPanel() {
        boolean isVisible = chatPanel.isVisible();
        chatPanel.setVisible(!isVisible);
    }

    private static void startCapture() {
        captureThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT_SCREEN)) {
                imageLabel.setText("Server Sẵn Sàng, Vui Lòng Đợi Kết Nối...");

                Robot robot = new Robot();
                Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

                while (capturing) {
                    if (paused) {
                        Thread.sleep(5); // Tạm dừng chụp
                        continue;
                    }

                    BufferedImage screenImage = robot.createScreenCapture(screenRect);
                    BufferedImage resizedImage = resizeImage(screenImage, 1000, 700);

                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setIcon(new ImageIcon(resizedImage));
                    });

                    serverSocket.setSoTimeout(50);
                    try {
                        Socket clientSocket = serverSocket.accept();
                        clientSockets.add(clientSocket);
                        imageLabel.setText("Tổng Số Người Trong Phòng: " + clientSockets.size());
                    } catch (SocketTimeoutException e) {
                        // Tiếp tục xử lý các client đã kết nối
                    }

                    for (int i = 0; i < clientSockets.size(); i++) {
                        Socket clientSocket = clientSockets.get(i);
                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(screenImage, "png", baos);
                            byte[] imageBytes = baos.toByteArray();

                            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                            dos.writeInt(imageBytes.length);
                            dos.write(imageBytes);
                            dos.flush();
                        } catch (IOException e) {
                            try {
                                clientSocket.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            clientSockets.remove(i);
                            i--;
                            System.out.println("Client Ngắt Kết Nối. Tổng Số Người Trong Phòng " + clientSockets.size());
                        }
                    }

                    Thread.sleep(5);
                }

                for (Socket clientSocket : clientSockets) {
                    clientSocket.close();
                }
                clientSockets.clear();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        captureThread.start();
    }

    private static void startChatServer() {
        try (ServerSocket chatServerSocket = new ServerSocket(PORT_CHAT)) {
            while (true) {
                Socket chatSocket = chatServerSocket.accept();
                chatSockets.add(chatSocket);
                new Thread(() -> handleChatClient(chatSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleChatClient(Socket chatSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(chatSocket.getOutputStream(), true)) {
            String message;
            while ((message = reader.readLine()) != null) {
                message = message.trim(); // Loại bỏ khoảng trắng thừa

                if (!message.isEmpty()) { // Kiểm tra message hợp lệ
                    final String finalMessage = message;

                    SwingUtilities.invokeLater(() -> chatArea.append(finalMessage + "\n"));

                    // Gửi message tới tất cả các client khác
                    for (Socket socket : chatSockets) {
                        if (socket != chatSocket) {
                            PrintWriter otherWriter = new PrintWriter(socket.getOutputStream(), true);
                            otherWriter.println(finalMessage);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            chatSockets.remove(chatSocket);
        }
    }

    private static void sendMessage() {
        String message = chatInput.getText().trim();
        chatInput.setText("");
        if (!message.isEmpty()) {
            final String fullMessage = serverName + " (Chủ Phòng): " + message;
            SwingUtilities.invokeLater(() -> chatArea.append("Tôi: " + message + "\n"));

            for (Socket chatSocket : chatSockets) {
                try {
                    PrintWriter writer = new PrintWriter(chatSocket.getOutputStream(), true);
                    writer.println(fullMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void sendAudio(Socket clientSocket) {
        try {
            // Cấu hình format âm thanh
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Microphone không được hỗ trợ.");
                return;
            }

            // Lấy dòng microphone
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            // Gửi dữ liệu âm thanh tới client
            OutputStream os = clientSocket.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(os);
            byte[] buffer = new byte[4096];

            System.out.println("Đang gửi âm thanh...");
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
            e.printStackTrace();
        }
    }

    // Hàm nhận âm thanh từ client
    private static void startAudioReceiver() {
        try (ServerSocket serverSocket = new ServerSocket(PORT_AUDIO_SEND)) {
            System.out.println("Audio receiver server đang chạy...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientAudioSockets.add(clientSocket);
                System.out.println("Client audio đã kết nối: " + clientSocket.getInetAddress());

                // Nhận âm thanh từ client và phát lại
                new Thread(() -> receiveAudio(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveAudio(Socket clientSocket) {
        try {
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

            // Nhận dữ liệu âm thanh từ client và phát lại
            InputStream is = clientSocket.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            byte[] buffer = new byte[4096];
            int bytesRead;

            System.out.println("Đang nhận âm thanh từ client...");
            while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
                speakers.write(buffer, 0, bytesRead);
            }

            speakers.drain();
            speakers.close();
        } catch (IOException | LineUnavailableException e) {
            e.printStackTrace();
        } finally {
            clientAudioSockets.remove(clientSocket);
        }
    }

    // Hàm bật/tắt microphone
    public static void toggleMic() {
        isMicOn = !isMicOn;
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        Image tmp = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resizedImage;
    }
    
    
}
