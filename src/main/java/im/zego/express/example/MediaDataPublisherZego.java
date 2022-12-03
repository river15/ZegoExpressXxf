package im.zego.express.example;

import im.zego.express.example.constants.CliOptions;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.*;
import im.zego.zegoexpress.constants.*;
import im.zego.zegoexpress.entity.*;
import org.apache.commons.cli.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MediaDataPublisherZego extends ApplicationBase {
    private String nativeLibPath;
    private String logPath;
    private long appID;
    private String appSign;
    private String userId;
    private String roomId = "0001";
    private String streamId = "0001";


    private ZegoExpressEngine engine;


    private final IZegoEventHandler eventHandler = new IZegoEventHandler() {


        /** 推流状态更新回调 */
        @Override
        public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, String extendedData) {
            // RTC推流成功后转推到CDN
            if (state.value()==2){

                /**
                 * setp1:生成txTime
                 * txTime:url的有效期，每次设置为当前时间+7200个小时
                 * 再将该时间转换成 Unix 时间戳格式（即 1546064025）。
                 * 然后再转换成十六进制，以进一步压缩字符长度，得到 txTime = 5c271099（十六进制）。
                 */
                Date date = getBeforeDateTime(1000);
                String txTime = Long.toHexString(date.getTime());
                System.out.println(txTime);
                /**
                 * setp2:生成txSecret
                 * txSecret 的生成方法是 txSecret = MD5(KEY + StreamName + txTime)
                 */
                MessageDigest md5 = null;
                try {
                    md5 = MessageDigest.getInstance("md5");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                String s = "KkhUz13yGBKWFQJVwKzkjBBrcdPkw0NW"+streamID+txTime;
                String hexStr = convertBytes2HexStr(md5.digest(s.getBytes()));
                /**
                 * setp3:转推的CDN
                 * txSecret 的生成方法是 txSecret = MD5(KEY + StreamName + txTime)
                 */
                String URL = "rtmp://publish-tencent1.zego.xinxinfa.cn/live/"+streamID+"?txSecret="+hexStr+"&txTime="+txTime;
                engine.addPublishCdnUrl(streamID, URL, new IZegoPublisherUpdateCdnUrlCallback() {
                    @Override
                    public void onPublisherUpdateCdnUrlResult(int errorCode) {
                        if(errorCode == 0) {
                            System.out.println("转推成功！");
                        } else {
                            System.out.println("转推失败！");
                        }
                    }
                });

            }
        }


        @Override
        public void onDebugError(int i, String s, String s1) {
            System.out.printf("[onDebugError] code:%d,func:%s,info:%s%n", i, s, s1);
        }

        @Override
        public void onEngineStateUpdate(ZegoEngineState state) {
            super.onEngineStateUpdate(state);

            System.out.printf("[onEngineStateUpdate] state:%d%n", state.value());
        }

        @Override
        public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, String extendedData) {
            super.onRoomStateUpdate(roomID, state, errorCode, extendedData);
            System.out.printf("[onRoomStateUpdate] room id:%s, state:%d%n", roomID, state.value());

            if (state == ZegoRoomState.CONNECTED && streamId != null && !streamId.isEmpty()) {
                engine.startPublishingStream(streamId);
            }
        }

        @Override
        public void onRoomUserUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoUser> userList) {
            super.onRoomUserUpdate(roomID, updateType, userList);
            System.out.printf("[onRoomUserUpdate()] room id:%s, type:%d%n", roomID, updateType.value());
        }

        @Override
        public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList, String extendedData) {
            super.onRoomStreamUpdate(roomID, updateType, streamList, extendedData);
            System.out.printf("[onRoomStreamUpdate()] room id:%s, type:%d%n", roomID, updateType.value());
        }

        @Override
        public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, String extendedData) {
            super.onPlayerStateUpdate(streamID, state, errorCode, extendedData);
            System.out.printf("[onPlayerStateUpdate()] stream id:%s, state:%d%n", streamID, state.value());
        }

        @Override
        public void onPlayerQualityUpdate(String streamID, ZegoPlayStreamQuality quality) {
            super.onPlayerQualityUpdate(streamID, quality);
            System.out.printf("[onPlayerQualityUpdate()] stream id:%s quality:%s%n", streamID, quality.toString());
        }
    };

    private void parseCommand(CommandLine cmd) {
        try {
            appID = Long.parseLong(cmd.getOptionValue(CliOptions.APP_ID));
            userId = cmd.getOptionValue(CliOptions.USER_ID);
            appSign = cmd.getOptionValue(CliOptions.APP_SIGN);

            if (cmd.hasOption(CliOptions.NATIVE_PATH)) {
                nativeLibPath = cmd.getOptionValue(CliOptions.NATIVE_PATH);
            }
            if (cmd.hasOption(CliOptions.LOG_PATH)) {
                logPath = cmd.getOptionValue(CliOptions.LOG_PATH);
            }
            if (cmd.hasOption(CliOptions.ROOM_ID)) {
                roomId = cmd.getOptionValue(CliOptions.ROOM_ID);
            }
            if (cmd.hasOption(CliOptions.STREAM_ID)) {
                streamId = cmd.getOptionValue(CliOptions.STREAM_ID);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(CommandLine cmd) {
        this.parseCommand(cmd);

        System.out.printf("%nExpress SDK Version: '%s'%n%n", ZegoExpressEngine.getVersion());

        // Monitor the SIGKILL signal
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                ZegoExpressEngine.destroyEngine(new IZegoDestroyCompletionCallback() {
                    @Override
                    public void onDestroyCompletion() {
                        System.out.printf("[destroyEngine] OK%n");
                    }
                });
            }
        });

        ZegoExpressEngine.setApiCalledCallback(new IZegoApiCalledEventHandler() {
            @Override
            public void onApiCalledResult(int i, String s, String s1) {
                System.out.printf("[onApiCalledResult] code:%d,func:%s,info:%s%n", i, s, s1);
            }
        });

        ZegoEngineConfig engineConfig = new ZegoEngineConfig();
        if (nativeLibPath != null && !nativeLibPath.isEmpty()) {
            // Set SDK native shared library path
            engineConfig.advancedConfig.put("native_library_path", nativeLibPath);
        }else{
            engineConfig.advancedConfig.put("native_library_path", "/home/pi/cli_java/ZegoExpressExample/libs/aarch64/libZegoExpressEngine.so");
        }

        ZegoExpressEngine.setEngineConfig(engineConfig);

        if (logPath != null && !logPath.isEmpty()) {
            // Set SDK log path
            ZegoLogConfig logConfig = new ZegoLogConfig();
            logConfig.logPath = logPath;
            ZegoExpressEngine.setLogConfig(logConfig);
        }

        // Create RTC engine
        ZegoEngineProfile profile = new ZegoEngineProfile();
        profile.appID = appID;
        profile.appSign = appSign;

        profile.scenario = ZegoScenario.BROADCAST;


        engine = ZegoExpressEngine.createEngine(profile, eventHandler);
        System.out.printf("[createEngine] appId:%d%n", profile.appID);

        // Set user
        ZegoUser user = new ZegoUser(userId);

        // Login room
        ZegoRoomConfig roomConfig = new ZegoRoomConfig();
        roomConfig.isUserStatusNotify = true;
        engine.loginRoom(roomId, user, roomConfig);
        System.out.printf("[loginRoom] roomId:%s, userId:%s, token:%s%n", roomId, user.userID, roomConfig.token);


        ZegoVideoConfig videoConfig = new ZegoVideoConfig();
        videoConfig.captureHeight = 1280;
        videoConfig.captureWidth = 720;
        videoConfig.encodeWidth = 1280;
        videoConfig.encodeHeight = 720;
        videoConfig.fps=24;
        videoConfig.bitrate=2500;
        engine.setVideoConfig(videoConfig);


        engine.startPublishingStream(streamId);

    }

    public static String convertBytes2HexStr(byte[] originalBytes) {
        StringBuffer sb = new StringBuffer();
        for (byte bt : originalBytes) {
            // 获取b补码后的八位
            String hex = Integer.toHexString(((int)bt)&0xff);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 返回n个小时之前的日期
     */
    public static Date getBeforeDateTime(int value){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR_OF_DAY, value);
        return  calendar.getTime();
    }
}
