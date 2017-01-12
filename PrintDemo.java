package org.linuxsogood.reference.chp1.pdf;


import org.springframework.util.ResourceUtils;

import javax.print.PrintException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by honway on 2017/1/12.
 */
public class PrintDemo {
    public static void main(String[] args) throws IOException, PrintException {


        FileInputStream textStream = new FileInputStream(ResourceUtils.getFile("classpath:test.pdf"));
        String pdfPath = ResourceUtils.getFile("classpath:test.pdf").getAbsolutePath();
        Runtime.getRuntime().exec("cmd.exe /C start acrord32 /P /H " + pdfPath);

    }

    private static void getLocalIPs() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        List<String> ipAddress = new ArrayList<>();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (inetAddress.isLinkLocalAddress() || inetAddress.isLoopbackAddress()) {
                    continue;
                }
                String ipReg = "((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))";
                Pattern compile = Pattern.compile(ipReg);
                if (compile.matcher(inetAddress.getHostAddress()).matches()) {
                    ipAddress.add(inetAddress.getHostAddress());
                    System.out.println(inetAddress.getHostAddress());
                }
            }
        }
    }
}
