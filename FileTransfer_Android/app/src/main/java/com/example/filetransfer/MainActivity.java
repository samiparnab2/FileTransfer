package com.example.filetransfer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private int port =9990;
    private Boolean storagePermissionGranted=false;
    public Boolean isConnected=false,isReceiving=false;
    Socket socket;
    Button connect,receive;
    TextView display,progress;
    EditText ip_text;
    File defaultLocation;
    ProgressBar progressBar;
    String ip;
    BufferedReader socketBr;
    PrintWriter socketPr;
    DataInputStream socketData;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connect = findViewById(R.id.connect);
        receive= findViewById(R.id.receive);
        display = findViewById(R.id.display);
        ip_text= findViewById(R.id.ip_text);
        progress=findViewById(R.id.progress);
        progressBar=findViewById(R.id.progressBar);
        requestStoragePermission();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ip=ip_text.getText().toString();
                if(ip=="")
                    display.setText("IP can't be blank");
                else {
                    connectToServer();
                }
            }
        });
        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isConnected)
                {
                    if(!isReceiving)
                    {
                        isReceiving=true;

                        defaultLocation=new File(getExternalMediaDirs()[0].getAbsolutePath());
                        if (!defaultLocation.exists()) {
                            defaultLocation.mkdir();
                            //Log.d("startService","folder created");
                        }
                        new Thread(){
                            public void run()
                            {
                                connect.post(new Runnable() {
                                                 @Override
                                                 public void run() {
                                                 connect.setEnabled(false);
                                                 }
                                             });

                                startReceiving();
                                isReceiving=false;
                                connect.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        connect.setEnabled(true);
                                    }
                                });

                            }

                        }.start();

                    }
                    else
                    {
                        display.setText("Already receiving");
                    }
                }
                else{
                    display.setText("Not connected to Server");
                }
            }
        });
    }
    private void connectToServer() {
        if(!isConnected) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        socket = new Socket(ip, port);
                        socketBr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        socketPr = new PrintWriter(socket.getOutputStream(), true);
                        socketData = new DataInputStream(socket.getInputStream());
                        isConnected = true;
                        connect.post(new Runnable() {
                            @Override
                            public void run() {
                                connect.setText("DISCONNECT");
                            }
                        });
                    } catch (Exception e) {
                        display.post(new Runnable() {
                            @Override
                            public void run() {
                                display.setText("Wrong IP");
                            }
                        });
                    }
                }
            }.start();
        }
        else if(isConnected)
        {
            new Thread(){
                @Override
                public void run() {
                    try {
                        socket.close();
                        isConnected = false;
                        connect.post(new Runnable() {
                            @Override
                            public void run() {
                                connect.setText("CONNECT");
                            }
                        });
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }.start();

        }

    }
    public void startReceiving()
    {
        try {
            String command = socketBr.readLine();
            //Log.d("startService", "readLine "+command);
            if (command.equals("folder"))
                receiveFolder(defaultLocation);
            else if (command.equals("file"))
                receiveFile(defaultLocation);
            else if (command.equals("end"))
                return;

        }
        catch (Exception e)
        {

           e.printStackTrace();
        }
    }

    public void receiveFile(File foutPath) throws Exception
    {
        String fileName = socketBr.readLine();
        display.post(new Runnable() {
            @Override
            public void run() {
                display.setText(fileName);
            }
        });
        //Log.d("receiveFile","file Name received "+foutPath.getAbsolutePath()+" "+fileName);
        File file=new File(foutPath.getAbsolutePath()+"/"+fileName);
        if(!file.exists())
            file.createNewFile();
        //Log.d("receiveFile","file created");
        OutputStream fileWriter=new FileOutputStream(file);
        socketPr.println("1");
        //Log.d("receiveFile","sent signal to start receiving");
        String strSize=socketBr.readLine();
        //Log.d("receiveFile",strSize);
        long size=Long.parseLong(strSize);
        //Log.d("receiveFile","file size received");

        long totalSize=size;
        long completed=0;
        long read=-1;
        byte[] fileData=new byte[1024];
        socketPr.println("1");
        //Log.d("receiveFile","sent signal to start receiving-1");

        while(true)
            {
                read=socketData.read(fileData,0,fileData.length);
                fileWriter.write(fileData,0,(int)read);
                size-=read;
                completed+=read;
                updateProgress((int)((completed*100)/totalSize),true);

                if(size<=0)
                    break;
            }
            fileWriter.close();
            socketPr.println("1");
    }
    public void updateProgress(Integer s,Boolean doPost)
    {
        if(doPost) {
            progress.post(new Runnable() {
                @Override
                public void run() {
                    progress.setText(s.toString()+" %");
                }
            });
        progressBar.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(s);
            }
        });
        }
        else{
            progress.setText(s.toString()+" %");
            progressBar.setProgress(s);
        }
    }
    public void receiveFolder(File foutPath)
    {
        try {
            String folderName = socketBr.readLine();
            foutPath=new File(foutPath.getAbsolutePath()+"/"+folderName);
            if(!foutPath.exists())
                foutPath.mkdir();
            socketPr.println("1");
            String command=socketBr.readLine();
            while(!command.equals("end"))
            {
                if(command.equals("folder"))
                    receiveFolder(foutPath);
                else if(command.equals("file"))
                    receiveFile(foutPath);
                command=socketBr.readLine();
            }
        }catch (Exception e)
        {
            //Log.d("Folder Write",e.getCause().toString());
        }
    }

    public void requestStoragePermission()
    {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_DENIED)
        {
            String[] permissions={Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions,1);
            }
        }
        else if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)
        {
            storagePermissionGranted=true;
            File f= Environment.getExternalStorageDirectory();
            Toast.makeText(this,f.toString(),Toast.LENGTH_SHORT).show();
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_DENIED)
        {
            String[] permissions={Manifest.permission.READ_EXTERNAL_STORAGE};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions,1);
            }
        }
        else if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)
        {
            storagePermissionGranted=true;
            File f= Environment.getExternalStorageDirectory();
            Toast.makeText(this,f.toString(),Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults)
    {
        if(requestCode==1)
        {
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED)
            {
                storagePermissionGranted=true;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


}