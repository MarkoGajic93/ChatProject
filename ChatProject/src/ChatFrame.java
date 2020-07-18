import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.TitledBorder;


// Template JFrame that both client and server use (with a few differences, defined in those classes)
public class ChatFrame extends JFrame {
	protected JTextArea chat;					 // Area to display chat messages
	protected JTabbedPane tabs;					 // Global or private chat tabs
	protected JTextField input;					 // User input field
	protected JPanel inputPane;					 // Container for input field
	protected JButton send; 					 // Send button
	protected JButton close;					 // Button to close private tab
	protected JList<String> listOfClients;		 // List of all clients
	protected DefaultListModel<String> listModel = new DefaultListModel<>();
	protected JMenu opts = new JMenu("Options"); // Options menu
	
	public ChatFrame() {		
		super("Chat");		
		setSize(750, 550);
		int x = (Toolkit.getDefaultToolkit().getScreenSize().width - getWidth())/2;
		int y = (Toolkit.getDefaultToolkit().getScreenSize().height - getHeight())/2;
		setLocation(x, y);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// Setting up layout
		GridBagLayout gb = new GridBagLayout();
		setLayout(gb);	

		// Setting up menu
		JMenuBar menu = new JMenuBar();
		opts.setMnemonic(KeyEvent.VK_O);
		menu.add(opts);
		setJMenuBar(menu);
		
		// Setting up chat area and adding it into a scroll container
		chat = new JTextArea();
		chat.setWrapStyleWord(true);
		chat.setLineWrap(true);
		chat.setEditable(false);
		JScrollPane chatScroll = new JScrollPane(chat);
		
		// Setting up list of clients and adding it into a scroll container
		listOfClients = new JList<String>(listModel);
		JScrollPane listScroll = new JScrollPane(listOfClients);
		listScroll.setBorder(new TitledBorder("Connected users"));
		
		// Adding scrolls into split pane, then adding split pane
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScroll, listScroll);
		splitPane.setDividerLocation(super.getWidth() - 200);
		splitPane.setOneTouchExpandable(true);
		tabs = new JTabbedPane();
		tabs.addTab("Global Chat", splitPane);
		addComponent(tabs, 0, 0, 2, 1, 100, 90, GridBagConstraints.BOTH, GridBagConstraints.CENTER, new Insets(10, 10, 0, 10));

		// Setting up and adding input panel
		inputPane = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		input = new JTextField(10);
		inputPane.add(input, c);
		TitledBorder title = BorderFactory.createTitledBorder("NAME");
		title.setTitleFont(new Font("Footlight MT Light", Font.BOLD, 18));
		inputPane.setBorder(title);
		addComponent(inputPane, 0, 1, 1, 1, 100, 10, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, new Insets(10, 10, 10, 0));
		
		// Adding send button
		send = new JButton("Send");
		addComponent(send, 1, 1, 1, 1, 10, 10, GridBagConstraints.NONE, GridBagConstraints.CENTER, new Insets(10, 0, 10, 10));
		
		// Display splash screen for a few seconds (totally unnecessary :D)
		SplashScreen splash = new SplashScreen();
		try {
			Thread.sleep(2000);
			splash.dispose();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
	}

	// Method to set up constraints and add component
	protected void addComponent(Component comp, int gridx, int gridy, int gridwidth, int gridheight, int weightx, int weighty, int fill, int anchor, Insets insets) {
		GridBagConstraints con = new GridBagConstraints();
		con.insets = insets;
		con.gridx = gridx;
		con.gridy = gridy;
		con.gridwidth = gridwidth;
		con.gridheight = gridheight;
		con.weightx = weightx;
		con.weighty = weighty;
		con.fill = fill;
		con.anchor = anchor;
		add(comp, con);
	}
	
	// Panel used in private chat
	protected JTextArea createPrivateTab (String title) {
		JPanel privPane = new JPanel(new BorderLayout(10, 10));
		JTextArea privChat = new JTextArea();
		privChat.setWrapStyleWord(true);
		privChat.setLineWrap(true);
		privChat.setEditable(false);
		JScrollPane privChatScroll = new JScrollPane(privChat);
		privPane.add(privChatScroll, BorderLayout.CENTER);
		close = new JButton("close");
		privPane.add(close, BorderLayout.SOUTH);
		tabs.addTab(title, privPane);
		return privChat;
	}
	
	// Method to sort list of clients (direct insertion)
	public static void sortList(DefaultListModel<String> list) {
		for (int i = 1; i < list.size(); i++) {
			String temp = list.get(i);
			int k = i-1;
			while (k >= 0 && list.get(k).compareToIgnoreCase(temp) > 0) {
				list.set(k+1, list.get(k));
				k--;
			}
			list.set(k+1, temp);
		}
	}
	
	// Method to change theme
	public void setLookAndFeel() {
		UIManager.LookAndFeelInfo[] laf = UIManager.getInstalledLookAndFeels();
		String[] themes = new String[laf.length];
		for (int i = 0; i < laf.length; i++)
			themes[i] = laf[i].getName();
		String answer = (String) JOptionPane.showInputDialog(null, "Available themes:", "Choose theme", 
															JOptionPane.PLAIN_MESSAGE, null, themes, themes[0]);
		if (answer != null) {
			int index = 0;
			while (laf[index].getName() != answer)
				index++;
			try {
				UIManager.setLookAndFeel(laf[index].getClassName());
				SwingUtilities.updateComponentTreeUI(this);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Inner class to create splash screen
	class SplashScreen extends JWindow {
		Image splashScreen;
		ImageIcon icon;
		
		SplashScreen() {
			splashScreen = Toolkit.getDefaultToolkit().getImage("message.png");
			icon = new ImageIcon(splashScreen);
			setSize(icon.getIconWidth(), icon.getIconHeight());
			setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - getWidth())/2, 
						(Toolkit.getDefaultToolkit().getScreenSize().height - getHeight())/2);
			setVisible(true);
		}
		
		public void paint(Graphics g) {
			super.paint(g);
			g.drawImage(splashScreen, 0, 0, this);
		}
	}
}

