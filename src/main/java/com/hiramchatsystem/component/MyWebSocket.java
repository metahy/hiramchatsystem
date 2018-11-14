package com.hiramchatsystem.component;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint(value = "/websocket")
@Component
public class MyWebSocket {

    private final Logger logger = LoggerFactory.getLogger(MyWebSocket.class);
    private static int onlineCount = 0;
    private static Map<Session, String> userSessionList = new LinkedHashMap<>();
    private static List<String> userList = new ArrayList<>();
    private static CopyOnWriteArraySet<MyWebSocket> webSocketSet = new CopyOnWriteArraySet<>();
    private Session session;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        webSocketSet.add(this);
        addOnlineCount();
        logger.info("新连接加入！当前在线人数：" + getOnlineCount());
        try {
            JSONObject msg = new JSONObject();
            msg.put("msg", "您已加入聊天室，请文明发言...");
            msg.put("nickName", "系统提示：");
            msg.put("type", "system");
            msg.put("time", sdf.format(new Date()));
            sendMessage(msg.toJSONString());
            msg.put("msg", "");
            msg.put("nickName", userList);
            msg.put("time", sdf.format(new Date()));
            msg.put("type", "showUser");
            sendMessage(msg.toJSONString());
        } catch (IOException e) {
            logger.info("IO异常...");
        }
    }

    @OnClose
    public void onClose() {
        webSocketSet.remove(this);
        String nickName = userSessionList.get(this.session);
        if (nickName != null) {
            logger.info(nickName + "失去连接！");
            userSessionList.remove(this.session);
            userList.remove(nickName);
            JSONObject msg = new JSONObject();
            msg.put("msg", "用户" + nickName + "离开本次聊天");
            msg.put("nickName", userList);
            msg.put("time", sdf.format(new Date()));
            msg.put("type", "loseUser");
            for (MyWebSocket item : webSocketSet) {
                try {
                    item.sendMessage(msg.toJSONString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return;
        }
        subOnlineCount();
        logger.info("一个连接关闭！当前在线人数：" + getOnlineCount());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("来自客户端的消息：" + message);
        JSONObject getMsgJson = JSONObject.parseObject(message);
        JSONObject msg = new JSONObject();
        if (getMsgJson.getString("sendType").equals("setNick")) {
            if (userList.contains(getMsgJson.getString("nickName"))) {
                msg.put("msg", -1);
                msg.put("nickName", getMsgJson.getString("nickName"));
                msg.put("time", sdf.format(new Date()));
                msg.put("type", "setResult");
                try {
                    sendMessage(msg.toJSONString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                userSessionList.put(this.session, getMsgJson.getString("nickName"));
                userList.add(getMsgJson.getString("nickName"));
                msg.put("msg", 0);
                msg.put("nickName", getMsgJson.getString("nickName"));
                msg.put("time", sdf.format(new Date()));
                msg.put("type", "setResult");
                try {
                    sendMessage(msg.toJSONString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                msg.put("msg", "用户" + getMsgJson.getString("nickName") + "加入本次聊天");
                msg.put("nickName", userList);
                msg.put("time", sdf.format(new Date()));
                msg.put("type", "newUser");
                for (MyWebSocket item : webSocketSet) {
                    try {
                        item.sendMessage(msg.toJSONString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
        }
        msg.put("msg", getMsgJson.getString("msg"));
        msg.put("nickName", getMsgJson.getString("nickName"));
        msg.put("time", sdf.format(new Date()));
        msg.put("type", "chat");
        for (MyWebSocket item : webSocketSet) {
            try {
                item.sendMessage(msg.toJSONString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.info("发生错误");
        error.printStackTrace();
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    public static void sendInfo(String message) throws IOException {
        for (MyWebSocket item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                continue;
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        MyWebSocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        MyWebSocket.onlineCount--;
    }
}
