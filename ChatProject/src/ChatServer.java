import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;

public class ChatServer {
	private static final String IP = "localhost";
	private static final int PORT = 5230;
	protected static ChatFrame frame;
	protected static Map<Handler, String> allClients = new ConcurrentHashMap<>();
	private static ExecutorService pool = Executors.newFixedThreadPool(4); // 4 clients max for testing purpose

	public static void main(String[] args) {
		// Creating a server socket on defined port
		try (ServerSocket server = new ServerSocket(PORT)) {
			
			// Creating and setting up GUI
			setUpGUI();
			
			while (true) {
				// Waiting for client to connect
				Socket client = server.accept();
					
				// Passing the connected client to a runnable handler
				Handler clientHandler = new Handler(client, allClients);
				
				// Executing pool
				pool.execute(clientHandler);
			}
		} catch (IOException e) {
			if (frame != null)
				frame.chat.append("Server crashed");
			JOptionPane.showMessageDialog(null, "Server already running");
		}
	}

	// Method to customize and start GUI
	private static void setUpGUI() {
		frame = new ChatFrame();
		frame.setTitle("Chat Server");
		((TitledBorder)frame.inputPane.getBorder()).setTitle("Admin");
		frame.repaint();
		// Setting up menu items
		JMenuItem whisp = new JMenuItem("Whisper"); 
		whisp.addActionListener((e) -> Handler.sendPrivateAdminIn());
		whisp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
		JMenuItem kick = new JMenuItem("Kick selected user");
		kick.addActionListener((e) -> kick(frame.listOfClients.getSelectedValue()));
		kick.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK));
		JMenuItem theme = new JMenuItem("Change theme");
		theme.addActionListener((e) -> frame.setLookAndFeel());
		theme.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		JMenuItem exit = new JMenuItem("Exit program");
		exit.addActionListener((e) -> System.exit(0));
		frame.opts.add(whisp);
		frame.opts.add(kick);
		frame.opts.addSeparator();
		frame.opts.add(theme);
		frame.opts.addSeparator();
		frame.opts.add(exit);
		
		// Setting up pop up menu
		JPopupMenu popUp = new JPopupMenu();
		JMenuItem popUpKick = new JMenuItem("Kick selected user");
		popUpKick.addActionListener((e) -> kick(frame.listOfClients.getSelectedValue()));
		popUp.add(popUpKick);
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
		
		// Starting GUI
		frame.setVisible(true);
		frame.input.requestFocusInWindow();
		frame.chat.append("Server running...\n");
	}
	
	// Method to kick out client from chat
	protected static void kick(String clientToKick) {
		if (clientToKick != null)
			allClients.forEach((handler, name) -> {
					if (name.equals(clientToKick))
						handler.terminate();
				});
		else
			frame.chat.append("Select the client you want to kick out\n");
	}
	
	// Getter methods for IP adress port, so client could obtain them
	public static String getIp() {
		return IP;
	}
	public static int getPort() {
		return PORT;
	}
}
