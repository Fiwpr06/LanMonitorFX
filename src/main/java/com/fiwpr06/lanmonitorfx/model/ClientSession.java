package com.fiwpr06.lanmonitorfx.model;

import com.fiwpr06.lanmonitorfx.network.SocketConnection;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

/**
 * Model đại diện cho phiên kết nối của một máy sinh viên tại Server.
 * Sử dụng các JavaFX Property để các thành phần UI (TableView, ImageView) có thể tự động liên kết (binding).
 * Hỗ trợ cập nhật SocketConnection sống khi sinh viên kết nối lại (reconnect).
 */
public class ClientSession {

    private String clientId;
    private final StringProperty mssv = new SimpleStringProperty("");
    private final StringProperty hoTen = new SimpleStringProperty("");
    private final StringProperty ipAddress = new SimpleStringProperty("");
    private final StringProperty status = new SimpleStringProperty("Online");
    private final BooleanProperty warning = new SimpleBooleanProperty(false);
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final ObjectProperty<Image> lastScreen = new SimpleObjectProperty<>(null);
    private SocketConnection connection;

    public ClientSession(String clientId, String mssv, String hoTen, String ipAddress, SocketConnection connection) {
        this.clientId = clientId;
        this.mssv.set(mssv);
        this.hoTen.set(hoTen);
        this.ipAddress.set(ipAddress);
        this.status.set("Online");
        this.connection = connection;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public SocketConnection getConnection() {
        return connection;
    }

    /**
     * Cập nhật Socket kết nối mới khi sinh viên reconnect.
     * Khắc phục triệt để lỗi rớt socket cũ khiến các lệnh NOTIFY, SHUTDOWN không tới được client.
     */
    public synchronized void updateConnection(SocketConnection newConnection, String newIp, String newClientId) {
        this.connection = newConnection;
        this.ipAddress.set(newIp);
        this.clientId = newClientId;
        this.status.set("Online");
        this.warning.set(false);
    }

    // --- JavaFX Properties cho TableView & UI Binding ---

    public String getMssv() {
        return mssv.get();
    }

    public void setMssv(String value) {
        this.mssv.set(value);
    }

    public StringProperty mssvProperty() {
        return mssv;
    }

    public String getHoTen() {
        return hoTen.get();
    }

    public void setHoTen(String value) {
        this.hoTen.set(value);
    }

    public StringProperty hoTenProperty() {
        return hoTen;
    }

    public String getIpAddress() {
        return ipAddress.get();
    }

    public void setIpAddress(String value) {
        this.ipAddress.set(value);
    }

    public StringProperty ipAddressProperty() {
        return ipAddress;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String value) {
        this.status.set(value);
    }

    public StringProperty statusProperty() {
        return status;
    }

    public boolean isWarning() {
        return warning.get();
    }

    public void setWarning(boolean value) {
        this.warning.set(value);
    }

    public BooleanProperty warningProperty() {
        return warning;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        this.selected.set(value);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public Image getLastScreen() {
        return lastScreen.get();
    }

    public void setLastScreen(Image image) {
        this.lastScreen.set(image);
    }

    public ObjectProperty<Image> lastScreenProperty() {
        return lastScreen;
    }
}
