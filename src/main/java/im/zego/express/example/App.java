package im.zego.express.example;

import im.zego.express.example.constants.CliOptions;
import org.apache.commons.cli.*;

import java.util.HashMap;

public class App {

    public static void main(String[] args) {
        HashMap<String, ApplicationBase> applicationMap = new HashMap<>();
        applicationMap.put("publisher_zego", new MediaDataPublisherZego());
        applicationMap.put("publisher_tencent", new MediaDataPublisherTencent());

        Options options = new Options();
        options.addOption(Option.builder(CliOptions.APPLICATION).hasArg()
                .desc("Which example to be run? choices: " + applicationMap.keySet()).required().build());
        options.addOption(Option.builder(CliOptions.NATIVE_PATH).hasArg()
                .desc("Set the Express native shared library path, if not set, you should add '-Dsun.boot.library.path=/path/to/lib' argument to run this app!").build());
        options.addOption(Option.builder(CliOptions.LOG_PATH).hasArg().desc("Set the log path").build());

        options.addOption(Option.builder(CliOptions.APP_ID).hasArg().desc("Set ZEGO appID, you can get appID from admin console. https://console.zego.im/dashboard ").required().build());
        options.addOption(Option.builder(CliOptions.APP_SIGN).hasArg().desc("Set ZEGO appSign, you can get appSign from admin console. https://console.zego.im/dashboard ").required().build());
        options.addOption(Option.builder(CliOptions.USER_ID).hasArg().desc("Set user id").required().build());
        options.addOption(Option.builder(CliOptions.TOKEN).hasArg().desc("Set the token of for the user id, you can get token from admin console.").build());

        options.addOption(Option.builder(CliOptions.ROOM_ID).hasArg().desc("Set room id").build());
        options.addOption(Option.builder(CliOptions.STREAM_ID).hasArg().desc("Set stream id").build());

        options.addOption(Option.builder(CliOptions.MEDIA_PATH).hasArg().desc("Set the media source path, used for publishing stream (e.g. the media_player or media_data_publisher example)").build());
        options.addOption(Option.builder(CliOptions.RTMP_URL).hasArg().desc("Set the RTMP_URL").build());

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar ZegoExpressExample.jar", options);

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            String exampleName = cmd.getOptionValue(CliOptions.APPLICATION);
            if (!applicationMap.containsKey(exampleName)) {
                throw new Exception("Unknown APPLICATION");
            }

            ApplicationBase applicationBase = applicationMap.get(exampleName);
            applicationBase.run(cmd);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
