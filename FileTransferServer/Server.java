import java.util.*;
import java.net.*;
import java.io.*;
class ServerRun{
     Socket socket;
     BufferedReader socketInput;
     PrintWriter socketOutput;
     File location;
    ServerRun(Socket s,File f) throws Exception
    {
        socket=s;
        socketInput= new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socketOutput=new PrintWriter(socket.getOutputStream(),true);
        location=f;
    }
    
   public void startSending()
   {
       System.out.println(socket.getInetAddress().toString()+" connected");

        if(location.isDirectory())
        {
            socketOutput.println("folder");
            System.out.println(socket.getInetAddress().toString()+" sending folder");

            sendFolder(location);
        }
        else if(location.isFile())
        {
            socketOutput.println("file");
            System.out.println(socket.getInetAddress().toString()+" sending file");

            sendFile(location);
        }

   }

   public void sendFile(File finPath)
   {
       try{
        System.out.println(socket.getInetAddress().toString()+" Inside Sendfile");

        System.out.println("starting file transfer "+finPath.length());
        String fName=finPath.getName();
        socketOutput.println(fName);
        if (socketInput.readLine().equals("1"))
        {
            System.out.println("file size "+finPath.length());
            socketOutput.println(finPath.length());
            if(socketInput.readLine().equals("1"))
            {
                InputStream is=new FileInputStream(finPath);
                DataOutputStream dataWrite= new DataOutputStream(socket.getOutputStream());
                byte[] data=new byte[1024];
                int read=-1;
                while(true)
                {
                    read=is.read(data,0,data.length);
                    if(read<=-1)
                        break;
                    dataWrite.write(data,0,read);
                }
                socketInput.readLine().equals("1");
                System.out.println(fName+" sent successfully");
            }
        }

    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
   }
   public void sendFolder(File finPath)
   {
        try{
            
            String folName=finPath.getName();
            socketOutput.println(folName);
            if(socketInput.readLine().equals("1"))
            {
                File files[]=finPath.listFiles();
                for(File f:files)
                {
                    if(f.isDirectory())
                    {
                        socketOutput.println("folder");
                        sendFolder(f);
                    }
                    else if(f.isFile())
                    {
                        socketOutput.println("file");
                        sendFile(f);
                    }
                }
                socketOutput.println("end");
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
   }
    
}
class Server
{
static void getIPAddress() throws SocketException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets))
            displayInterfaceInformation(netint);
    }

    static void displayInterfaceInformation(NetworkInterface netint) throws SocketException {
        System.out.printf("Display name: %s\n", netint.getDisplayName());
        System.out.printf("Name: %s\n", netint.getName());
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            System.out.printf("InetAddress: %s\n", inetAddress);
        }
        System.out.printf("\n");
    }
    public static void main(String[] args)  {
        int port =9990;
        try{
        getIPAddress();
        }catch(Exception e)
        {
        }
        System.out.println("enter entire path of file/folder: ");
        Scanner sc=new Scanner(System.in);
        String filePath=sc.nextLine();
        try{
        ServerSocket ss=new ServerSocket(port);
        new ServerRun (ss.accept(),new File(filePath)).startSending();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
