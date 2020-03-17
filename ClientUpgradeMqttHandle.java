package com.chinaredstar.robot.handle.mqtt;

import com.alibaba.fastjson.JSONObject;
import com.chinaredstar.robot.config.CommonProperties;
import com.chinaredstar.robot.util.ExceptionUtil;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * client 升级
 * @author liu.hongwei
 */
@Component
@Slf4j
public class ClientUpgradeMqttHandle implements MqttMessageHandle {

    private final CommonProperties commonProperties;
    private final ExceptionUtil exceptionUtil;

    public ClientUpgradeMqttHandle(CommonProperties commonProperties, ExceptionUtil exceptionUtil) {
        this.commonProperties = commonProperties;
        this.exceptionUtil = exceptionUtil;
    }

    @Override
    public synchronized void doHandle(JSONObject jsonObject) {
        String version = jsonObject.getString("version");
        String downloadUrl = jsonObject.getString("downloadUrl");

        log.info("接收到升级client的消息, version:[{}], downloadUrl: [{}]", version, downloadUrl);
        if (StringUtils.isEmpty(downloadUrl)) {
            log.error("downloadUrl {} 不能为空", downloadUrl);
            return;
        }

        log.info("开始升级client, version: {}, downloadUrl: {}",  version, downloadUrl);
        clientUpgrade(downloadUrl);
    }

    /**
     * 客户端升级
     * @param downloadUrl client文件下载地址
     */
    private void clientUpgrade(String downloadUrl) {
        InputStream inputStream = null;
        try {
            String upgradeScriptFilePath = System.getProperty("user.home") + "/robot/robot_client/" + commonProperties.getUpgradeScriptFile();
            log.info("升级脚本文件位置: [{}]", upgradeScriptFilePath);

            File scriptFile = processScriptFile(upgradeScriptFilePath);
            if (scriptFile == null) {
                return;
            }

            log.info("开始执行升级脚本.");
            inputStream = executeScriptFile(downloadUrl, upgradeScriptFilePath, scriptFile);
            log.info("脚本执行完成");
        } catch (IOException e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            String exception = baos.toString();
            exceptionUtil.saveExceptionToMq(exception);
            log.error("执行升级脚本时异常: ", e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            log.info("升级client downloadUrl: {} 完成.", downloadUrl);
            } catch (IOException e) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream(baos));
                String exception = baos.toString();
                exceptionUtil.saveExceptionToMq(exception);
                inputStream = null;
            }
        }
    }

    private InputStream executeScriptFile(String downloadUrl, String upgradeScriptFilePath, File scriptFile) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(upgradeScriptFilePath);
        command.add(downloadUrl);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectOutput(new File(scriptFile.getParent() + "/logs/stdout.log"));
        Process start = processBuilder.start();
        return start.getInputStream();
    }

    private File processScriptFile(String upgradeScriptFilePath) throws IOException {
        File scriptFile = new File(upgradeScriptFilePath);
        log.info("[{}], 开始生成该文件并赋予可执行权限.", upgradeScriptFilePath);
        ClassPathResource classPathResource = new ClassPathResource("upgrade.sh");
        @Cleanup FileOutputStream output = new FileOutputStream(upgradeScriptFilePath);
        OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(classPathResource.getInputStream()));
        StringBuilder fileContent = new StringBuilder();
        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
            fileContent.append(line).append(System.getProperty("line.separator"));
        }
        writer.write(fileContent.toString());
        writer.close();
        output.flush();

        boolean writeResult = scriptFile.setExecutable(true, false);
        if (!writeResult) {
            log.error("在设置[{}]可执行权限时失败!", upgradeScriptFilePath);
            return null;
        }
        return scriptFile;
    }
}
