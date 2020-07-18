import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;

public class ChatClient {
	private ChatFrame frame;		 
	private Socket conn;			  
	private PrintWriter serverOut;	 
	private BufferedReader serverIn; 
	private boolean isConnected;	 	
	private String sysMessage;
	private Map<String, JTextArea> output;
	
	public ChatClient (String ip, int port) {
		try {
			// Creating a client socket and trying to connect to a server
			conn = new Socket(ip, port);
			isConnected = true;
				
			// Creating input and output streams to a server
			serverOut = new PrintWriter(conn.getOutputStream(), true);
			serverIn = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
			// Creating GUI
			setUpGUI();
	
			// Getting default client name generated from server and customizing it
			setUpName();
					
			// GUI visible after setting up client name
			frame.setVisible(true); 
			frame.input.requestFocusInWindow();
						
			// Getting list of all connected clients
			getClientList();
			
			// Listening JTextField for user input 
			frame.input.addActionListener((e) -> sendUserIn(null));
			frame.send.addActionListener((e) -> sendUserIn(null));
			
			// Listening server and printing response to JTextArea
			sysMessage = Handler.getSysMessage();
			listenToServer();
			
		} catch (IOException e) {
			if (frame == null) {
				int answer = JOptionPane.showConfirmDialog(null, "Couldn't connect to a server, try again?", 
															"Connection failed", JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.YES_OPTION)
					reconnect();
				else
					System.exit(0);
			}
			else {
				output.keySet().forEach((tabName) -> display(tabName, "\nSERVER CLOSED, TRY TO RECONNECT LATER\n"));
				isConnected = false;
			}
		} finally {
			// Closing resources
			try {
				serverIn.close();
				serverOut.close();
				conn.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		new ChatClient(ChatServer.getIp(), ChatServer.getPort());
	}

	// Method to customize GUI (it will become visible after client name is set)
	private void setUpGUI() {
		frame = new ChatFrame();
		// Setting up menu
		JMenuItem priv = new JMenuItem("Open private chat");
		priv.addActionListener((e) -> sendUserIn("/priv"));
		priv.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
		JMenuItem whisp = new JMenuItem("Whisper");
		whisp.addActionListener((e) -> serverOut.println("/whisper"));
		whisp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
		JMenuItem reconn = new JMenuItem("Reconnect");
		reconn.addActionListener((e) -> reconnect());
		reconn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
		JMenuItem disconn = new JMenuItem("Disconnect");
		disconn.addActionListener((e) -> serverOut.println("/disconnect"));
		disconn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK));
		JMenuItem userName = new JMenuItem("Change username");
		userName.addActionListener((e) -> serverOut.println("/changename"));
		userName.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		JMenuItem theme = new JMenuItem("Change theme");
		theme.addActionListener((e) -> frame.setLookAndFeel());
		theme.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		JMenuItem exit = new JMenuItem("Exit program");
		exit.addActionListener((e) -> System.exit(0));
		frame.opts.add(priv);
		frame.opts.add(whisp);
		frame.opts.addSeparator();
		frame.opts.add(reconn);
		frame.opts.add(disconn);
		frame.opts.addSeparator();
		frame.opts.add(userName);
		frame.opts.add(theme);
		frame.opts.addSeparator();
		frame.opts.add(exit);
		
		// Setting up pop up menu
		JPopupMenu popUp = new JPopupMenu();
		JMenuItem popUpPriv = new JMenuItem("Open private chat");
		popUpPriv.addActionListener((e) -> sendUserIn("/priv"));
		popUp.add(popUpPriv);
		frame.listOfClients.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					popUp.show(frame.listOfClients, e.getX(), e.getY());;
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					popUp.show(frame.listOfClients, e.getX(), e.getY());;
			}
		});
		
		// Initializing map to store opened tabs
		output = new HashMap<>();
		output.put("Global Chat", frame.chat);
	}

	// Method to display text on client screen
	private void display(String where, String textToDispl) {
		output.get(where).append(textToDispl + "\n");
	}
	
	// Method to interact with server and confirm user name
	private void setUpName() throws IOException {
		String defaultName = serverIn.readLine(); // Getting server suggested default name
		String response;
		String msg = null;
		
		// Getting name from client input and sending it back to server, until response isn't "taken"
		do {
			String inputName = (String) JOptionPane.showInputDialog(null, msg, "Enter your username", JOptionPane.PLAIN_MESSAGE, null, null, defaultName);
			if (inputName != null) 
				serverOut.println(inputName);
			else 
				serverOut.println(defaultName);
			response = serverIn.readLine();
			msg = "Username you have entered is already taken.\nPlease, choose another:";
		} while (response.equals("taken"));
		
		((TitledBorder)frame.inputPane.getBorder()).setTitle(response);
		frame.repaint();
	}

	// Method to set up JList to display all connected clients
	private void getClientList() throws IOException {
		while (true) {
			String client = serverIn.readLine();
			if (client.equals("end of clients"))
				break;
			frame.listModel.addElement(client);
		}
		ChatFrame.sortList(frame.listModel);
	}
	
	// Method to send user input text to a server
	private void sendUserIn(String text) {
		String userIn = text == null ? frame.input.getText() : text;
		// Checking if input is from global or private tab
		String prefix = frame.tabs.getSelectedIndex() == frame.tabs.indexOfTab("Global Chat") ? 
						"" : (frame.tabs.getTitleAt(frame.tabs.getSelectedIndex()) + "/priv ");
		switch (userIn) {
		case "/disconnect": serverOut.println(userIn);
							frame.input.setText(""); 
							isConnected = false;
							break;
		case "/reconnect": 	reconnect();
							frame.input.setText(""); 
							break;
		case "/quit": 		System.exit(0); 
							break;
		case "/priv":		String to = frame.listOfClients.getSelectedValue();
							if (to == null) {frame.chat.append("Select someone\n"); break;}
							if (output.containsKey(to)) break;
							JTextArea priv =  frame.createPrivateTab(to);
							// Button to close a tab
							frame.close.addActionListener((e) -> {
								output.remove(frame.tabs.getTitleAt(frame.tabs.getSelectedIndex()));
								frame.tabs.remove(frame.tabs.getSelectedIndex());
							});
							output.put(to, priv);
							frame.input.setText("");
							break;
		default:			serverOut.println(prefix + userIn);
							frame.input.setText(""); 
							break;
		}
	}
	
	// Method to get, process and display data from server
	private void listenToServer() throws IOException {
		while (true) {
			String response = serverIn.readLine();
			if (response == null) {
				isConnected = false;
				break;
			}
			
			// Change user name? (clientName + sysMessage recived as server response to /changename)
			if (response.equals(((TitledBorder)frame.inputPane.getBorder()).getTitle() + sysMessage)) 
				setUpName();
			
			// You are sending a whisper message to someone? (server response to /whisper)
			else if (response.equals("/whisper" + sysMessage)) {
				String to = (String) JOptionPane.showInputDialog(frame, "Select user","Send private mesage to", 
												JOptionPane.PLAIN_MESSAGE, null, frame.listModel.toArray(), null);
				if (to == null) {
					serverOut.println("");
					continue;
				}
				serverOut.println(to);
				String msg = JOptionPane.showInputDialog(frame, "Write a message: ");
				if (msg == null) {
					serverOut.println("");
					continue;
				}
				serverOut.println(msg);
				display("Global Chat", "[" + new Date() + "] You have whispered to " + to + ": " + msg);
			} 
			
			// Someone sent private message? (server response to clientName + /priv + message)
			else if (response.contains("/priv") && response.contains(sysMessage)) {
				String from = response.substring(6, response.indexOf(sysMessage));
				JTextArea priv;
				if (frame.tabs.indexOfTab(from) == -1) {
					priv = frame.createPrivateTab(from);
					// Button to close a tab
					frame.close.addActionListener((e) -> {
						output.remove(frame.tabs.getTitleAt(frame.tabs.getSelectedIndex()));
						frame.tabs.remove(frame.tabs.getSelectedIndex());
					});
					output.put(from, priv);
				} 
				
				if (from.equals(((TitledBorder)frame.inputPane.getBorder()).getTitle())) 
					display(from, "You are talking to yourself!");
				else
					display(from, serverIn.readLine());
			}
			
			// In all other cases print server respond
			else
				display("Global Chat", response);
			
			// and then check:
			// Add new client?
			if (response.contains("connected") && response.contains(sysMessage)) {
				frame.listModel.addElement(response.substring(response.indexOf("]")+2, response.lastIndexOf(":")));
				ChatFrame.sortList(frame.listModel);
			}
			
			// Remove client?
			if ((response.contains("left") || response.contains("kicked")) && response.contains(sysMessage)) {
				String clientToRemove = response.substring(response.indexOf("]")+2, response.lastIndexOf(":"));
				frame.listModel.removeElement(clientToRemove);
				if (output.containsKey(clientToRemove))
					display(clientToRemove, response);
			}
			
			// Someone changed user name?
			if (response.contains("changed name to") && response.contains(sysMessage)) {
				String oldName = response.substring(response.indexOf("]")+2, response.lastIndexOf(":"));
				String newName = response.substring(response.lastIndexOf(":")+18, response.indexOf(sysMessage));
				frame.listModel.removeElement(oldName);
				frame.listModel.addElement(newName);
				ChatFrame.sortList(frame.listModel);
				if (output.containsKey(oldName)) {
					output.put(newName, output.get(oldName));
					output.remove(oldName);
					frame.tabs.setTitleAt(frame.tabs.indexOfTab(oldName), newName);
					display(newName, response);
				}
			}
		}
	}
	
	// Method to reconnect to a server
	private void reconnect() {
		if (isConnected)
			JOptionPane.showMessageDialog(frame, "You are allready connected");
		else {
			try {
				conn = new Socket(ChatServer.getIp(), ChatServer.getPort());
				serverOut = new PrintWriter(conn.getOutputStream(), true);
				serverIn = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				isConnected = true;
				
				setUpName();
				
				// Getting list of all connected clients
				frame.listModel.clear();
				getClientList();
				
				// Start to recive and display data from server again
				new Thread(() -> {
					try {
						listenToServer();						
					} catch (IOException e) {
						output.keySet().forEach((tabName) -> display(tabName, "\nSERVER CLOSED, TRY TO RECONNECT LATER\n"));
						isConnected = false;
					}
				}).start();
				
			} catch (IOException e) {
				int answer = JOptionPane.showConfirmDialog(null, "Couldn't connect to a server, try again?", 
						"Connection failed", JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.YES_OPTION)
					reconnect();
			}
		}
	}
}