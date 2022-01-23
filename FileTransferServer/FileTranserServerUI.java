import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.ArrayList;

import javax.swing.*;
import java.io.*;
class Main{
	public static void main(String[] args) {
		new FileTranserServerUI();
	}
}

 class FileTranserServerUI extends JFrame {

	private JLabel label1;
	private JLabel label2;
	private JLabel label3;
	private JButton addBtn;
	private JLabel addedFilesText;
	private JLabel sendingText;
	private JProgressBar progressBar;
	private JButton sendBtn;
	private JButton connectBtn;
	private JLabel serverIPText;
	private JLabel statusText;
	private JScrollPane scrollPane1;
	private JTextArea selectedFilesList;
	private int port=9990;
	private Socket socket;
	private Boolean serverStarted;
	private Boolean connected;
	private ServerSocket ss;
	private JFileChooser fc;
	private ArrayList<File> selectedFileList;
	private Boolean sending;
	private BufferedReader socketInput;
    private PrintWriter socketOutput;
	 FileTranserServerUI()
	{
		serverStarted=false;
		connected=false;
		sending=false;
		selectedFileList=new ArrayList<File>();
		initComponents();
	}

	private void adjustWindow(JComponent c)
	{
		((JFrame)c.getTopLevelAncestor()).pack();
	}
	private void startServer()
	{
		try{
		ss=new ServerSocket(port);
		serverStarted=true;
		serverIPText.setText(Inet4Address.getLocalHost().getHostAddress());
		adjustWindow(serverIPText);
		new Thread() {
			@Override
			public void run() {
				try {
					statusText.setText("Waiting for client....");
					socket=ss.accept();
					statusText.setText("connected to \n"+socket.getRemoteSocketAddress());
					// adjustWindow(statusText);
					connected=true;
					connectBtn.setText("Disconnect");
					socketInput= new BufferedReader(new InputStreamReader(socket.getInputStream()));
        			socketOutput=new PrintWriter(socket.getOutputStream(),true);
					adjustWindow(statusText);
				} catch (Exception e) {
					statusText.setText("Could not connect");
					adjustWindow(statusText);
				}
			}
		}.start();
		}catch(Exception e)
		{
			serverIPText.setText(e.getMessage());
			adjustWindow(serverIPText);

		}
	}

	private void stopServer()
	{
		new Thread(){
			public void run() {
				try{
					socket.close();
					statusText.setText("");
					serverIPText.setText("");
					// adjustWindow(statusText);
					connected=false;
					ss.close();
					// serverIPText.setText("");
					adjustWindow(serverIPText);
					serverStarted=false;
					connectBtn.setText("Connect");
					adjustWindow(connectBtn);
				}
				catch(Exception e)
				{
					statusText.setText(e.getMessage());
				}
			}
		}.start();
	}
	
	public void sendFile(File finPath)
   {
       try{
		progressBar.setValue(0);
		sendingText.setText("sending : "+finPath.getAbsolutePath());
		adjustWindow(sendingText);
        // System.out.println(socket.getInetAddress().toString()+" Inside Sendfile");
		// sendingText.setText("sending : "+finPath.getAbsolutePath());
        // System.out.println("starting file transfer "+finPath.length());
        String fName=finPath.getName();
        socketOutput.println(fName);
        if (socketInput.readLine().equals("1"))
        {
            System.out.println("file size "+finPath.length());
			long size=finPath.length();
			long completed=0;
            socketOutput.println(finPath.length());
            if (finPath.length()==0)
                return;
            if(socketInput.readLine().equals("1"))
            {
                InputStream is=new FileInputStream(finPath);
                DataOutputStream dataWrite= new DataOutputStream(socket.getOutputStream());
                byte[] data=new byte[1024];
                int read=-1;
                while(true)
                {
                    read=is.read(data,0,data.length);
					completed+=read;
					progressBar.setValue((int)((completed*100)/size));
                    if(read<=-1)
                        break;
                    dataWrite.write(data,0,read);
                }
				progressBar.setValue(100);
                socketInput.readLine().equals("1");
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

	private void startSending()
	{
		sending=true;
		progressBar.setStringPainted(true);
		new Thread(){
			public void run() {
				socketOutput.println("start");
				for(int i=0;i<selectedFileList.size();i++){
					File location=selectedFileList.get(i);
					if(location.isDirectory())
					{
                        socketOutput.println("folder");
						sendFolder(location);
					}
					else if(location.isFile())
					{
						socketOutput.println("file");
						sendFile(location);
					}
					selectedFileList.remove(i);
					refreshSelectedFilesList();
				}
				socketOutput.println("stop");
				sending=false;
				progressBar.setStringPainted(false);
				sendingText.setText("");
				adjustWindow(sendingText);
			};
		}.start();
	}

	private void initComponents() {
	
		try{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Exception e){}
		label1 = new JLabel();
		label2 = new JLabel();
		label3 = new JLabel();
		addBtn = new JButton();
		addedFilesText = new JLabel();
		sendingText = new JLabel();
		progressBar = new JProgressBar();
		sendBtn = new JButton();
		connectBtn = new JButton();
		serverIPText = new JLabel();
		statusText = new JLabel();
		scrollPane1 = new JScrollPane();
		selectedFilesList = new JTextArea();

		Container contentPane = getContentPane();

		label1.setText("Connect to IP :");

		label2.setText("Status :");

		label3.setText("File/Folder :");

		addBtn.setText("Add..");
		addBtn.addActionListener(e -> addBtnPressed(e));

		sendingText.setText("Sending :");

		sendBtn.setText("Send");
		sendBtn.addActionListener(e -> sendBtnPressed(e));

		connectBtn.setText("Connect");
		connectBtn.addActionListener(e -> connectBtnPressed(e));

		scrollPane1.setViewportView(selectedFilesList);

		selectedFilesList.setEditable(false);

		progressBar.setMaximum(100);

		
		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout.setHorizontalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(contentPaneLayout.createParallelGroup()
						.addGroup(contentPaneLayout.createSequentialGroup()
							.addGroup(contentPaneLayout.createParallelGroup()
								.addGroup(contentPaneLayout.createSequentialGroup()
									.addComponent(label3, GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE)
									.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
									.addComponent(addBtn))
								.addGroup(contentPaneLayout.createSequentialGroup()
									.addGroup(contentPaneLayout.createParallelGroup()
										.addGroup(contentPaneLayout.createSequentialGroup()
											.addComponent(label1)
											.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
											.addComponent(serverIPText))
										.addGroup(contentPaneLayout.createSequentialGroup()
											.addComponent(label2)
											.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
											.addComponent(statusText))
										.addComponent(sendingText))
									.addGap(0, 100, Short.MAX_VALUE)))
							.addGap(15, 15, 15))
						.addComponent(sendBtn, GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
						.addComponent(connectBtn, GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
						.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
						.addComponent(scrollPane1, GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE))
					.addContainerGap())
		);
		contentPaneLayout.setVerticalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(label1)
						.addComponent(serverIPText))
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addGroup(contentPaneLayout.createParallelGroup()
						.addGroup(contentPaneLayout.createSequentialGroup()
							.addComponent(label2)
							.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
							.addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
								.addComponent(addBtn)
								.addComponent(label3)))
						.addComponent(statusText))
					.addGap(4, 4, 4)
					.addComponent(scrollPane1, GroupLayout.PREFERRED_SIZE, 78, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(sendingText)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(sendBtn)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(connectBtn)
					.addContainerGap(37, Short.MAX_VALUE))
		);
		pack();
		// setResizable(false);
		setTitle(getBounds().toString());
		setLocationRelativeTo(getOwner());
		setDefaultCloseOperation(1);
		setVisible(true);
		
	}	

	private void refreshSelectedFilesList()
	{
		selectedFilesList.setText("");
		for (File file : selectedFileList) {
			selectedFilesList.append(file.getAbsolutePath()+"\n");
		}
	}
	private void addBtnPressed(ActionEvent e) {
		fc=new JFileChooser();
        fc.setDialogTitle("FileTransferServer");
		fc.setMultiSelectionEnabled(true);
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		JButton open=new JButton();
        if (fc.showOpenDialog(open)==JFileChooser.APPROVE_OPTION)
        {
			File[] files=fc.getSelectedFiles();
			int cnt=0;
			for(File f : files)
             {
				 cnt=0;
				 for(int i=0;i<selectedFileList.size();i++)
				 {
				 	if (!selectedFileList.get(i).getAbsolutePath().equals(f.getAbsolutePath()))
					 {
						cnt+=1;
					 }
				}
				if (cnt==selectedFileList.size())
				{
					selectedFileList.add(f);
				}

			 }
			 refreshSelectedFilesList();
        }
	}

	private void sendBtnPressed(ActionEvent e){
		if(!sending)
		{
			startSending();
		}
	}
	private void connectBtnPressed(ActionEvent e){
		if(!serverStarted)
			startServer();
		else if (connected)
			stopServer();
		
	}
}
