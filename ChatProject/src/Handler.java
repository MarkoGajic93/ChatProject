import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Map;
import javax.swing.JOptionPane;

// Handler to manage every client in a separate thread
public class Handler implements Runnable {
	private Socket client;
	private BufferedReader in;
	private PrintWriter out;
	private Map<Handler, String> allClients;
	private String clientName;
	private static final String sysMessage = "\t[*System message*]";
	
	Handler(Socket clientSock, Map<Handler, String> clMap) throws IOException {
		client = clientSock; // Client to manage
		allClients = clMap;	 // Map of all client <names,handlers>
	
		// Creating input and output streams to a client
		in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		out = new PrintWriter(client.getOutputStream(), true);
				
		// Creating client name
		clientName = genUniqueName("Guest");			
		setUpName(true);
		
		// Sending client a list of all other clients
		for (String name : allClients.values())
			out.println(name);
		out.println("end of clients");
		
		// Messages before entering in ExecutorService pool
		out.println("Wait for a server to become available");
		ChatServer.frame.chat.append(clientName + " waiting in queue\n");
	}

	// Handling client in a separate thread, transfering data from client to server and otherwise
	@Override
	public void run() {
		
		// Adding new client to server list (other clients will update JList after reciving ...connected)
		allClients.put(this, this.toString());
		ChatServer.frame.listModel.addElement(clientName);
		ChatFrame.sortList(ChatServer.frame.listModel);
		
		// Notifying server and all clients about new connection
		ChatServer.frame.chat.append("[" + new Date() + "] " + clientName + " connected\n");
		sendToAll(clientName, "connected!" + sysMessage);
		
		// Listening JTextField for server admin input 
		ChatServer.frame.input.addActionListener((e) ->	sendAdminIn());
		ChatServer.frame.send.addActionListener((e) -> sendAdminIn());
					
		// Transfer data
		String txt;
		while (true) {
			try {
				txt = in.readLine(); // Waiting for client to send something 
				ChatServer.frame.chat.append("[" + new Date() + "] " + clientName + ": " + txt + "\n");
				// Check for special requests
				if (txt.contains("/priv")) {
					String to = txt.substring(0, txt.indexOf("/"));
					String msg = txt.substring(txt.indexOf("/")+6);
					if (allClients.containsValue(to)) {
						allClients.forEach((handler, name) -> {
							if (name.equals(to)) {
								handler.out.println("/priv " + this.toString() + sysMessage);
								handler.out.println("[" + new Date() + "] " + this.toString() + ": " + msg);
							}
						});
						out.println("/priv " + to + sysMessage);
						out.println("[" + new Date() + "] " + this.toString() + ": " + msg);
					} else {
						out.println("/priv " + to + sysMessage);
						out.println(to + " is not online");
					}
				} else {
					switch (txt) {
					case "/disconnect": throw new IOException();
					case "/changename": out.println(clientName + sysMessage);
										setUpName(false);
										break;
					case "/whisper":	out.println("/whisper" + sysMessage);
										String from = this.toString();
										String to = in.readLine();
										if (to.equals(""))
											break;
										String msg = in.readLine();
										if (msg.equals(""))
											break;
										ChatServer.frame.chat.append("[" + new Date() + "] " + from + 
																	" whispered to " + to + ": " + msg + "\n");
										allClients.forEach((handler, clName) -> {
											if (clName.equals(to))
												handler.out.println("[" + new Date() + "] " + from + 
																	" whispered to you: " + msg);
										});
										break;
					default:			if (!txt.contains(sysMessage))
											sendToAll(clientName, txt);		 // Pass it to all other clients
										break;
					}	
				}
			} catch (IOException e) {
				break; 				 // When client exits or gets kicked, exit the loop
			}
		}
		
		// Notify server and all other clients about disconnection
		// (if client socket is closed, everyone has been notified about kick in terminate() method)
		if (!client.isClosed()) {
			ChatServer.frame.chat.append("[" + new Date() + "] " + clientName + " left\n");
			sendToAll(clientName, "left" + sysMessage);
			try {
				in.close();
				out.close();
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Remove client from server list (other clients will update JList after reciving ...left/kicked)
		allClients.remove(this, this.toString());
		ChatServer.frame.listModel.removeElement(this.toString());
	}

	// Method to generate default client name
	private String genUniqueName(String base) {
		int sufix = 1;

		if (!allClients.containsValue(base))
			return base;
		
		while (allClients.containsValue(base + sufix))
			sufix++;
	
		return base+sufix;
	}

	// Method to set up clients name (NOTE - clientName is initialized before calling this method)
	private void setUpName(boolean firstTime) throws IOException {
		out.println(clientName);			
		String name = in.readLine();						
		while ((!name.equals(clientName) && allClients.containsValue(name)) || name.contains("Admin")) {
			out.println("taken");					
			name = in.readLine();				
		} 		
		out.println(name);
		
		if (!firstTime && !name.equals(clientName)) {
			ChatServer.frame.chat.append("[" + new Date() + "] " + clientName + 
										 " changed name to " + name + "\n");
			ChatServer.frame.listModel.removeElement(clientName);
			ChatServer.frame.listModel.addElement(name);
			ChatFrame.sortList(ChatServer.frame.listModel);
			sendToAll(clientName, "changed name to " + name + sysMessage);
			allClients.replace(this, clientName, name);
			clientName = name;
		}
		
		clientName = name;	
	}	
	
	// Method to send admin input to clients
	private void sendAdminIn() {
		String adminIn = ChatServer.frame.input.getText();
		ChatServer.frame.input.setText("");
		if (adminIn.equals("/kick")) {
			String clientToKick = ChatServer.frame.listOfClients.getSelectedValue();
			ChatServer.kick(clientToKick);
		} else if (adminIn.equals("/whisper")) {
			sendPrivateAdminIn();
		} else if (!adminIn.equals("")) {
			ChatServer.frame.chat.append("[" + new Date() + "] Admin: " + adminIn + "\n");
			sendToAll("Admin", adminIn);
		}
	}

	// Method to send admin input to a specific client
	protected static void sendPrivateAdminIn() {
		String to = (String) JOptionPane.showInputDialog(ChatServer.frame, "Select user","Send private mesage to", 
				JOptionPane.PLAIN_MESSAGE, null, ChatServer.frame.listModel.toArray(), null);
		if (to != null) {				
			String msg = JOptionPane.showInputDialog(ChatServer.frame, "Write a message: ");
			if (msg != null) {				
				ChatServer.frame.chat.append("[" + new Date() + "] Admin whispered to " + to + ": " + msg + "\n");
				ChatServer.allClients.forEach((handler, clName) -> {
					if (clName.equals(to))
						handler.out.println("[" + new Date() + "] Admin whispered to you: " + msg);
				});
			}
		}
	}
	
	// Method to send a String to all clients from clientList
	private void sendToAll(String sender, String txt) {
		for (Handler name : allClients.keySet()) 
			name.out.println("[" + new Date() + "] " + sender + ": " + txt);		
	}
	
	// Method to close specific client
	public void terminate() {
		ChatServer.frame.chat.append("[" + new Date() + "] " + clientName + " kicked!\n");
		sendToAll(clientName, "kicked!" + sysMessage);
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Getter method for String sysMessage, so client could obtain it
	public static String getSysMessage() {
			return sysMessage;
		}

	@Override
	public String toString() {
		return clientName;
	}
}
