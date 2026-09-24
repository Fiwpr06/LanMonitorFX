package com.fiwpr06.lanmonitorfx;

import com.fiwpr06.lanmonitorfx.client.CommandExecutor;
import com.fiwpr06.lanmonitorfx.client.TcpClient;
import com.fiwpr06.lanmonitorfx.network.Message;
import com.fiwpr06.lanmonitorfx.network.Protocol;
import com.fiwpr06.lanmonitorfx.server.ClientRegistry;
import com.fiwpr06.lanmonitorfx.server.TcpServer;
import com.fiwpr06.lanmonitorfx.ui.ServerMainController;
import com.fiwpr06.lanmonitorfx.util.FileLogger;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SystemIntegrationTest {

    private static TcpServer server;
    private static ClientRegistry registry;

    @BeforeAll
    public static void setup() throws Exception {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        registry = new ClientRegistry();
        server = new TcpServer(Protocol.PORT, registry, session -> {});
        server.start();

        Thread.sleep(200);
    }

    @AfterAll
    public static void teardown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testMultiClientConnectionAndCommands() throws Exception {
        BlockingQueue<Message> client1Commands = new LinkedBlockingQueue<>();
        BlockingQueue<Message> client2Commands = new LinkedBlockingQueue<>();

        TcpClient client1 = new TcpClient(client1Commands::add);
        TcpClient client2 = new TcpClient(client2Commands::add);

        // 1. Kết nối đồng thời 2 Client
        boolean c1Connected = client1.connect("127.0.0.1", "23DA001", "Nguyen Van A");
        boolean c2Connected = client2.connect("127.0.0.1", "23DA002", "Tran Thi B");

        assertTrue(c1Connected, "Client 1 phải kết nối thành công");
        assertTrue(c2Connected, "Client 2 phải kết nối thành công");

        Thread.sleep(400);
        assertEquals(2, registry.getOnlineCount(), "Server phải ghi nhận chính xác 2 Client Online");

        // 2. Client gửi SCREEN_DATA
        String fakeBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        client1.sendScreenData(fakeBase64);
        Thread.sleep(200);

        // 3. Server phát lệnh LOCK
        registry.sendToAll(Protocol.CMD_LOCK);
        Message c1Msg1 = client1Commands.poll(2, TimeUnit.SECONDS);
        Message c2Msg1 = client2Commands.poll(2, TimeUnit.SECONDS);

        assertNotNull(c1Msg1);
        assertEquals(Protocol.CMD_LOCK, c1Msg1.command());
        assertNotNull(c2Msg1);
        assertEquals(Protocol.CMD_LOCK, c2Msg1.command());

        // 4. Server phát lệnh NOTIFY
        String noticeText = "Nhắc nhở: Giữ trật tự phòng thi";
        registry.sendToAll(Protocol.buildNotify(noticeText));
        Message c1Msg2 = client1Commands.poll(2, TimeUnit.SECONDS);
        Message c2Msg2 = client2Commands.poll(2, TimeUnit.SECONDS);

        assertNotNull(c1Msg2);
        assertEquals(Protocol.CMD_NOTIFY, c1Msg2.command());
        assertEquals(noticeText, c1Msg2.param(0));

        // 5. Server phát lệnh UNLOCK
        registry.sendToAll(Protocol.CMD_UNLOCK);
        Message c1Msg3 = client1Commands.poll(2, TimeUnit.SECONDS);
        assertNotNull(c1Msg3);
        assertEquals(Protocol.CMD_UNLOCK, c1Msg3.command());

        // 6. Server phát lệnh SHUTDOWN, RESTART, LOGOUT
        registry.sendToAll(Protocol.CMD_SHUTDOWN);
        Message c1Msg4 = client1Commands.poll(2, TimeUnit.SECONDS);
        assertNotNull(c1Msg4);
        assertEquals(Protocol.CMD_SHUTDOWN, c1Msg4.command());

        registry.sendToAll(Protocol.CMD_RESTART);
        Message c1Msg5 = client1Commands.poll(2, TimeUnit.SECONDS);
        assertNotNull(c1Msg5);
        assertEquals(Protocol.CMD_RESTART, c1Msg5.command());

        registry.sendToAll(Protocol.CMD_LOGOUT);
        Message c1Msg6 = client1Commands.poll(2, TimeUnit.SECONDS);
        assertNotNull(c1Msg6);
        assertEquals(Protocol.CMD_LOGOUT, c1Msg6.command());

        // 7. Ngắt kết nối
        client1.disconnect();
        Thread.sleep(300);
        assertEquals(1, registry.getOnlineCount());

        client2.disconnect();
        Thread.sleep(200);
        assertEquals(0, registry.getOnlineCount());

        // 8. Kiểm tra file nhật ký theo ngày
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy"));
        File logFile = new File("logs", "NhatKy_" + today + ".txt");
        assertTrue(logFile.exists());

        String logContent = FileLogger.readTodayLog();
        assertTrue(logContent.contains("Nguyen Van A"));
        assertTrue(logContent.contains("Tran Thi B"));
    }

    @Test
    public void testClientReconnectionSocketUpdate() throws Exception {
        BlockingQueue<Message> clientMsgs1 = new LinkedBlockingQueue<>();
        TcpClient client = new TcpClient(clientMsgs1::add);

        // Lần 1: Kết nối
        assertTrue(client.connect("127.0.0.1", "23DA999", "Le Thi Test"));
        Thread.sleep(300);
        assertEquals(1, registry.getOnlineCount());

        // Ngắt kết nối
        client.disconnect();
        Thread.sleep(300);
        assertEquals(0, registry.getOnlineCount());

        // Lần 2: Tái kết nối (Reconnect) cùng MSSV
        BlockingQueue<Message> clientMsgs2 = new LinkedBlockingQueue<>();
        TcpClient clientReconnected = new TcpClient(clientMsgs2::add);
        assertTrue(clientReconnected.connect("127.0.0.1", "23DA999", "Le Thi Test"));
        Thread.sleep(300);
        assertEquals(1, registry.getOnlineCount());

        // Server phát lệnh NOTIFY sau khi sinh viên đã reconnect
        String notice = "Thông báo kiểm tra sau khi kết nối lại";
        registry.sendToAll(Protocol.buildNotify(notice));

        Message received = clientMsgs2.poll(2, TimeUnit.SECONDS);
        assertNotNull(received, "Client sau khi Reconnect phải nhận được thông báo thành công!");
        assertEquals(Protocol.CMD_NOTIFY, received.command());
        assertEquals(notice, received.param(0));

        clientReconnected.disconnect();
        Thread.sleep(200);
    }

    @Test
    public void testCommandExecutorProcessArguments() {
        BlockingQueue<String[]> executedCommands = new LinkedBlockingQueue<>();
        CommandExecutor executor = new CommandExecutor(executedCommands::add, false);

        // Kiểm tra lệnh SHUTDOWN
        executor.execute(new Message(Protocol.CMD_SHUTDOWN));
        String[] shutdownArgs = executedCommands.poll();
        assertNotNull(shutdownArgs);
        assertEquals("shutdown.exe", shutdownArgs[0]);
        assertEquals("/s", shutdownArgs[1]);
        assertEquals("/f", shutdownArgs[2]);
        assertEquals("/t", shutdownArgs[3]);
        assertEquals("10", shutdownArgs[4]);
        assertEquals("/c", shutdownArgs[5]);

        // Kiểm tra lệnh RESTART
        executor.execute(new Message(Protocol.CMD_RESTART));
        String[] restartArgs = executedCommands.poll();
        assertNotNull(restartArgs);
        assertEquals("shutdown.exe", restartArgs[0]);
        assertEquals("/r", restartArgs[1]);
        assertEquals("/f", restartArgs[2]);
        assertEquals("/t", restartArgs[3]);
        assertEquals("10", restartArgs[4]);
        assertEquals("/c", restartArgs[5]);

        // Kiểm tra lệnh LOGOUT
        executor.execute(new Message(Protocol.CMD_LOGOUT));
        String[] logoutArgs = executedCommands.poll();
        assertNotNull(logoutArgs);
        assertEquals("shutdown.exe", logoutArgs[0]);
        assertEquals("/l", logoutArgs[1]);
        assertEquals("/f", logoutArgs[2]);
    }

    @Test
    public void testFileLoggerVietnameseEncodingAndListener() throws Exception {
        BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
        java.util.function.Consumer<String> listener = logQueue::add;
        FileLogger.addListener(listener);

        String sampleVietnamese = "🟢 Thử nghiệm Tiếng Việt có dấu: Nguyễn Văn Đạt - Phòng thực hành số 5 - Đã kết nối!";
        FileLogger.log(sampleVietnamese);

        String received = logQueue.poll(2, TimeUnit.SECONDS);
        assertNotNull(received);
        assertTrue(received.contains("Nguyễn Văn Đạt"));
        assertTrue(received.contains("Phòng thực hành số 5"));

        String todayLog = FileLogger.readTodayLog();
        assertTrue(todayLog.contains("Nguyễn Văn Đạt"));

        FileLogger.removeListener(listener);
    }

    @Test
    public void testServerIpDetection() {
        String ip = ServerMainController.detectLocalLanIp();
        assertNotNull(ip);
        assertFalse(ip.isBlank());
        assertFalse(ip.equals("0.0.0.0"));
    }

    @Test
    public void testPrivateMessageIsolationAndScope() throws Exception {
        BlockingQueue<Message> c1Commands = new LinkedBlockingQueue<>();
        BlockingQueue<Message> c2Commands = new LinkedBlockingQueue<>();

        TcpClient client1 = new TcpClient(c1Commands::add);
        TcpClient client2 = new TcpClient(c2Commands::add);

        assertTrue(client1.connect("127.0.0.1", "23DA101", "Sinh Vien 1"));
        assertTrue(client2.connect("127.0.0.1", "23DA102", "Sinh Vien 2"));
        Thread.sleep(300);

        // 1. Giáo viên gửi tin nhắn RIÊNG tới Sinh Vien 1
        String privateMsg = "Em chú ý hoàn thành câu 3 nhé!";
        registry.sendTo("23DA101", Protocol.buildPrivateNotify(privateMsg));

        // Sinh viên 1 phải nhận được tin nhắn riêng
        Message c1Received = c1Commands.poll(2, TimeUnit.SECONDS);
        assertNotNull(c1Received, "Sinh viên 1 phải nhận được tin nhắn riêng!");
        assertEquals(Protocol.CMD_NOTIFY, c1Received.command());
        assertTrue(c1Received.param(0).contains("[GỬI RIÊNG]"));
        assertTrue(c1Received.param(0).contains(privateMsg));

        // Sinh viên 2 KHÔNG ĐƯỢC NHẬN BẤT KỲ GÓI TIN NÀO (Bảo mật 100%)
        Message c2Received = c2Commands.poll(1, TimeUnit.SECONDS);
        assertNull(c2Received, "Sinh viên 2 TUYỆT ĐỐI KHÔNG được nhận tin nhắn riêng của Sinh viên 1!");

        // 2. Giáo viên gửi thông báo CẢ LỚP
        String broadcastMsg = "Còn 15 phút nữa hết giờ!";
        registry.sendToAll(Protocol.buildBroadcastNotify(broadcastMsg));

        Message c1Broadcast = c1Commands.poll(2, TimeUnit.SECONDS);
        Message c2Broadcast = c2Commands.poll(2, TimeUnit.SECONDS);

        assertNotNull(c1Broadcast, "Sinh viên 1 phải nhận được thông báo cả lớp!");
        assertNotNull(c2Broadcast, "Sinh viên 2 phải nhận được thông báo cả lớp!");
        assertTrue(c1Broadcast.param(0).contains("[CẢ LỚP]"));
        assertTrue(c2Broadcast.param(0).contains("[CẢ LỚP]"));

        client1.disconnect();
        client2.disconnect();
        Thread.sleep(200);
    }
}
