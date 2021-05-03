package network_donwload_booster;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Date;

public class ThreadDownloader extends Thread {
    Date startTime = new Date();
    String path, savepath;
    int startIndex, endIndex, threadId;
    int total = 0;
    static int runningThreadCount = 5;
    
    ThreadDownloader(String path, String savepath, int startIndex, int endIndex, int threadId){
        this.path = path;
        this.savepath = savepath;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.threadId = threadId;
    }
    
    @Override
    public void run() {
        download(path, savepath, startIndex, endIndex, threadId);
    }
    
    public void download(String path, String savepath, int startIndex, int endIndex, int threadId){
        try{
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            
            File threadFile = new File(threadId + ".txt");
            if(threadFile.exists() && threadFile.length() > 0) {
                FileReader fr = new FileReader(threadFile);
                BufferedReader br = new BufferedReader(fr);
                String position = br.readLine();
                total = Integer.valueOf(position) - startIndex;
                
                conn.setRequestProperty("Range", "bytes=" + position + "-" + endIndex);
                startIndex = Integer.valueOf(position);
            }
            else {
                conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);
            }
            
            int code = conn.getResponseCode();
            if (code == 206) {
                InputStream is = conn.getInputStream();
                RandomAccessFile file = new RandomAccessFile(savepath, "rw");
                file.seek(startIndex);
                
                byte[] buffer = new byte[1024*1024];
                int len = -1;
                while ((len = is.read(buffer)) != -1) {
                    file.write(buffer, 0, len);
                    total += len;
                    int currentPosition = startIndex + total;
                    RandomAccessFile f = new RandomAccessFile(threadId + ".txt", "rwd");
                    f.write((currentPosition+"").getBytes());
                    f.close();
                }
                file.close();
                synchronized (ThreadDownloader.this) {
                    runningThreadCount--;
                    if (runningThreadCount == 0) {
                        Date now = new Date();
                        Booster.textArea.setText("下載完成，共花費 " + (now.getTime() - startTime.getTime())/1000 + "秒");
                        for (int i = 0; i < 5; i++) {
                            File f = new File(i + ".txt");
                            System.out.println(i + " " + f.delete());
                        }
                        System.out.println("all complete");
                    }
                }
            }
        } catch(Exception e){
            System.out.println(threadId + "下載失敗");
            e.printStackTrace();
        }
    }
}
