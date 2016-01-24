package com.bt.tool;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by xinyu.he on 2016/1/7.
 */
public class Utils {
    public static void openWebpage(String urlString) {
        try {
            openWebpage(new URL(urlString).toURI());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void openWebpage(URL url) {
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static String runCmdAndGetOutput(String[] cmd) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }
        }
        return line;
    }

    public static void runCmd(String[] cmd) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        builder.start();
    }

    public static void sendContentToClipboard(String content) {
        StringSelection stsel = new StringSelection(content);
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        systemClipboard.setContents(stsel, stsel);
    }

    public static int sift4(CharSequence s1, CharSequence s2, int maxOffset) {
        final int l1 = s1.length(), l2 = s2.length();
        if (s1 == null || l1 == 0)
            return ((s2 == null || l2 == 0) ? 0 : l2);
        if (s2 == null || l2 == 0)
            return (l1);

        int c1 = 0;
        int c2 = 0;
        int lcss = 0;
        int local_cs = 0;
        while ((c1 < l1) && (c2 < l2)) {
            if (s1.charAt(c1) == s2.charAt(c2))
                local_cs++;
            else {
                lcss += local_cs;
                local_cs = 0;
                if (c1 != c2) {
                    c1 = c2 = Math.max(c1, c2);
                }
                for (int i = 1; i < maxOffset && (c1 + i < l1 || c2 + i < l2); i++) {
                    if ((c1 + i < l1) && c2 < l2 && (s1.charAt(c1 + i) == s2.charAt(c2))) {
                        c1 += i;
                        local_cs++;
                        break;
                    }
                    if ((c2 + i < l2) && c1 < l1 && (s1.charAt(c1) == s2.charAt(c2 + i))) {
                        c2 += i;
                        local_cs++;
                        break;
                    }
                }
            }
            c1++;
            c2++;
        }
        lcss += local_cs;
        return Math.max(l1, l2) - lcss;
    }

    public static String readFileAsString(String filePath) throws IOException {
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = new BufferedReader(
                new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }


}
