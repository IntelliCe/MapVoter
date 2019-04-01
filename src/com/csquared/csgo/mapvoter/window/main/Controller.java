package com.csquared.csgo.mapvoter.window.main;

import com.csquared.csgo.mapvoter.Launcher;
import com.csquared.csgo.mapvoter.data.*;
import com.csquared.csgo.mapvoter.entity.Info;
import com.csquared.csgo.mapvoter.util.StringEncode;
import com.csquared.csgo.mapvoter.util.StringUtil;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

import com.csquared.csgo.mapvoter.data.ServerTick;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;


public class Controller {

    //private String serverAddress = "178.128.111.109";
    private String serverAddress = "127.0.0.1";
    private final int port = 27010;

    private Socket socket = null;
    private BufferedReader reader = null;

    @FXML private Label lbTitle;

    @FXML private Label lbInfo;
    @FXML private Label lbIntroduction;

    @FXML private Label lbNicknames;
    @FXML private Label lbAuths;

    @FXML private TextField tfInput;
    @FXML private TextArea taLog;
    @FXML private Button btSend;

    @FXML private ImageView imgDustStatus;

    public void initialize() {
        //test
        System.out.println(System.getProperty("file.encoding"));

        //Style init
        lbIntroduction.setWrapText(true);

        //init
        Info info = new Info();
        lbInfo.setText(info.toComponent());
        lbIntroduction.setText(StringUtil.INTRODUCTION_NULL);

        //client init
        if (!Launcher.ip.equals("")) {
            serverAddress = Launcher.ip;
        }
        try {
            socket = new Socket(serverAddress, port);
            addLog("成功连接到 BAN/PICK 服务器。");
            System.out.println("Connected to " + serverAddress + ":" + port);
            new ClientThread().start();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "建立到服务器的连接出错。");
            alert.setHeaderText("连接错误");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                System.exit(0);
            }
        }
    }

    @FXML private void onChangeIdButtonClicked() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("修改 ID");
        dialog.setHeaderText("修改 ID");
        dialog.setContentText("输入你想要应用的 ID:");
        Optional result = dialog.showAndWait();
        if (result.isPresent()) {
            if (!result.get().toString().trim().equals("")) {
                ClientTick tick = new ClientTick(C.TYPE_SET_NICKNAME);
                tick.setNickname(result.get().toString());
                sendData(socket, new Gson().toJson(tick, ClientTick.class));
            } else {
                new Alert(Alert.AlertType.INFORMATION, "输入的内容不能为空。").show();
            }
        }
    }

    @FXML private void onSendButtonClicked() {
        String string = tfInput.getText();
        if (!string.trim().equals("")) {
            tfInput.setText("");
            ClientTick tick = new ClientTick(C.TYPE_MESSAGE);
            tick.setMessage(string);
            sendData(socket, new Gson().toJson(tick, ClientTick.class));
        } else {
            new Alert(Alert.AlertType.INFORMATION, "输入的内容不能为空。").show();
        }
    }

    @FXML private void onInputBoxKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            onSendButtonClicked();
        }
    }

    private void addLog(String s) {
        taLog.setText(taLog.getText() + "\n" + s);
    }

    private void handleServerTick(ServerTick tick) {

        int tickType = tick.getType();

        switch (tickType) {

            case C.TYPE_SERVER_INFO_CHANGED:
                ServerInfo serverInfo = tick.getserverInfo();
                String name = serverInfo.getName();
                int type = serverInfo.getType();
                int left = serverInfo.getLeft();
                String typeStr = "";
                int auth = serverInfo.getAuth();
                String authStr = "";
                int amount = serverInfo.getAmount();
                switch (type) {
                    case C.VOTE_TYPE_STANDARD:
                        typeStr = "标准 BAN/PICK";
                        break;
                    case C.VOTE_TYPE_BAN:
                        typeStr = String.format("常规 BAN（保留%d图）", left);
                        break;
                    case C.VOTE_TYPE_PICK:
                        typeStr = String.format("常规 PICK（选择%d图）", left);
                        break;
                    case C.VOTE_TYPE_VOTE_TO_BAN:
                        typeStr = String.format("投票 BAN（保留%d图）", left);
                        break;
                    case C.VOTE_TYPE_VOTE_TO_PICK:
                        typeStr = String.format("投票 PICK（保留%d图）", left);
                        break;
                }
                switch (auth) {
                    case C.AUTH_COMMON:
                        authStr = "普通权限";
                        break;
                    case C.AUTH_FULL:
                        authStr = "优先权限";
                        break;
                }
                if (amount != 0) {
                    authStr = authStr + " - 每人" + amount + "票数";
                }
                lbInfo.setText(new Info(serverAddress + " [" + name + "]", typeStr, authStr).toComponent());
                break;

            case C.TYPE_USERS_CHANGED:
                List<User> users = tick.getUsers();
                StringBuilder nicknames = new StringBuilder();
                StringBuilder auths = new StringBuilder();
                for (User user : users) {
                    nicknames.append(user.getNickname()).append("\n");
                    auths.append(user.getAuthString()).append("\n");
                }
                lbNicknames.setText(nicknames.toString());
                lbAuths.setText(auths.toString());
                break;

            case C.TYPE_MESSAGE:
                addLog(tick.getMessage());
                break;

            case C.TYPE_HEARTBEAT:
                System.out.println("[♥] Heartbeat package received from server.");
                break;

            case C.TYPE_VOTING_STATE_CHANGED:
                VotingState state = tick.getVotingState();
                String title = "";
                int phase = state.getPhase();
                int counter = state.getCounter();
                String nickname = state.getNickname();
                if (phase == C.PHASE_READY) {
                    title = String.format("地图 BAN/PICK 将于 %d 秒后开始……", counter);

                } else if (phase == C.PHASE_VOTING) {
                    title = String.format("现在轮到 %s 进行选择……（%d）", nickname, counter);
                }
                lbTitle.setText(title);
        }

    }

    private void sendData(Socket socket, String data) {

        PrintWriter writer;

        try {
            System.out.println("[>] Sending data to server");
            writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(data);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    class ClientThread extends Thread {

        private BufferedReader reader = null;

        @Override
        public void run() {

            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String data;
                while ((data = reader.readLine()) != null) {
                    System.out.println("[<] Received: " + data);
                    ServerTick tick = new Gson().fromJson(data, ServerTick.class);
                    Platform.runLater(() ->
                        handleServerTick(tick)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "与服务器断开连接。");
                    alert.setHeaderText("连接错误");
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        System.exit(0);
                    }
                });
            }

        }

    }

}
