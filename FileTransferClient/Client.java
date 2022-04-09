import java.net.*;
import java.util.*;
import java.io.*;
class ClientRun
{
    BufferedReader socketBr;
    PrintWriter socketPr;
    DataInputStream socketData;
    Socket socket;
    File defaultLocation;
    ClientRun(Socket s,File f) throws Exception
    {
        socket=s;
        defaultLocation=f;
        socketBr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socketPr = new PrintWriter(socket.getOutputStream(), true);
        socketData = new DataInputStream(socket.getInputStream());
    }
    public void startReceiving()
    {
        try {
            String command = socketBr.readLine();
            while (!command.equals("stop")) {
                
            
                    if (command.equals("folder"))
                        receiveFolder(defaultLocation);
                    else if (command.equals("file"))
                        receiveFile(defaultLocation);
                    command = socketBr.readLine();
                }
            }
        catch (Exception e)
        {

        e.printStackTrace();
        }
    }

    public void receiveFile(File foutPath) throws Exception
    {
        String fileName = socketBr.readLine();
        File file=new File(foutPath.getAbsolutePath()+"/"+fileName);
        System.out.println("receiving "+fileName +": ");
        if(!file.exists())
            file.createNewFile();
        OutputStream fileWriter=new FileOutputStream(file);
        socketPr.println("1");
        String strSize=socketBr.readLine();
        long size=Long.parseLong(strSize);
        long totalSize=size;
        long completed=0;
        long read=-1;
        byte[] fileData=new byte[1024];
        socketPr.println("1");
        while(true)
            {
                read=socketData.read(fileData,0,fileData.length);
                fileWriter.write(fileData,0,(int)read);
                size-=read;
                completed+=read;
                String printString=(int)((completed*100)/totalSize)+" %\r";
                System.out.write(printString.getBytes());

                if(size<=0)
                    break;
            }
            fileWriter.close();
            socketPr.println("1");
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

        }
    }
}

class Client
{
    public static void main(String ar[])
    {
        Scanner sc=new Scanner(System.in);
        System.out.println("Enter server Address :\n");
        String ip=sc.nextLine();
        System.out.println("Enter Destination path :\n");
        String path=sc.nextLine();
        try{
        new ClientRun(new Socket(ip,9990),new File(path)).startReceiving();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}