package network_donwload_booster;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.Math;
import java.text.DecimalFormat;

public class Booster {
    private static int threadNum = 5;
    private ThreadDownloader[] threads = new ThreadDownloader[threadNum];
    private int fileSize;
    private JFrame frame;
    public static JTextArea textArea;
    private JLabel urlLabel, dirLabel;
    private JTextField urlTF, dirTF;
    private JButton dirBt, downloadBt;
    DecimalFormat df = new DecimalFormat("##.00");
    
    Booster() {
        frame = new JFrame("Network Download Booster");
        frame.setSize(800,400);
        frame.setLayout(new FlowLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        textArea = new JTextArea(10,60);
        
        urlLabel = new JLabel("Download url");
        dirLabel = new JLabel("save dir");
        
        urlTF = new JTextField(20);
        dirTF = new JTextField(20);
        dirTF.setEditable(false);
        
        dirBt = new JButton("save url");
        dirBt.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent ae)
           {
               JFileChooser fileChooser = new JFileChooser();
               fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
               int returnValue = fileChooser.showOpenDialog(null);
               if (returnValue == JFileChooser.APPROVE_OPTION){
                   File selectedFile = fileChooser.getSelectedFile();
                   dirTF.setText(selectedFile.getAbsolutePath());
               }
           }
        });
        downloadBt = new JButton("download");
        downloadBt.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent ae)
           {
               try{
                   String path = urlTF.getText();
                   URL url = new URL(path);
                   HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                   conn.setRequestMethod("GET");
                   conn.setConnectTimeout(5000);
                   String savePath = dirTF.getText() + "\\" + path.substring(path.lastIndexOf("/")+1);
                   
                   int code = conn.getResponseCode();
                   if (code == 200) {
                       fileSize = conn.getContentLength();
                       RandomAccessFile file = new RandomAccessFile(savePath, "rw");
                       file.setLength(fileSize);
                       file.close();
                       int blockSize = fileSize/threadNum;
                       
                       textArea.setText("開始下載，檔案大小共 " + (fileSize / 1024 / 1024) + "MB");
                       for (int threadId = 0; threadId < threadNum; threadId++) {
                           int startIndex = threadId * blockSize;
                           int endIndex = 0;
                           
                           if (threadId != (threadNum - 1)){
                               endIndex = (threadId + 1) * blockSize - 1;
                           } else{
                               endIndex = fileSize - 1;
                           }
                           
                           threads[threadId] = new ThreadDownloader(path, savePath, startIndex, endIndex, threadId);
                           threads[threadId].start();
                       }
                       Thread overseer = new Overseer();
                       overseer.start();
                   }
               } catch (Exception e) {
                   textArea.setText("下載失敗");
                   e.printStackTrace();
               }
           }
        });
        
        JPanel p1 = new JPanel();
        p1.setSize(400,100);
        p1.add(urlLabel);
        p1.add(urlTF);
        JPanel p2 = new JPanel();
        p2.setSize(400,100);
        p2.add(dirLabel);
        p2.add(dirTF);
        p2.add(dirBt);
        JPanel p3 = new JPanel();
        p3.setSize(400,100);
        p3.add(downloadBt);
        JPanel p4 = new JPanel();
        p4.setSize(400,100);
        p4.add(new JScrollPane(textArea));
        
        frame.add(p1);
        frame.add(p2);
        frame.add(p3);
        frame.add(p4);
        frame.setVisible(true);
    }
    
    class Overseer extends Thread {
        @Override
        public void run() {
            double p;
            double Sum, lastSum = 0;
            while ((p = getSum() / fileSize * 100) < 100) {
                Sum = getSum();
                textArea.setText(textArea.getText() + "\n已完成" + df.format(p) + "%，下載速度 " + df.format((Sum - lastSum) / 1024 / 1024 / 3) + "MB/s");
                textArea.selectAll();
                lastSum = Sum;
                try{
                    Thread.sleep(3000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private double getSum() {
            double sum = 0;
            for (int i = 0; i < threadNum; i++) {
                sum += threads[i].total;
            }
            return sum;
        }
    }
}
