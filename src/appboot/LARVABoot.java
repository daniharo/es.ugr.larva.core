/**
 *
 * @author lcv
 */
package appboot;

import data.Ole;
import data.OleOptions;
import disk.Logger;
import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.showConfirmDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import swing.LARVAFrame;
import static tools.Internet.getExtIPAddress;
import static tools.Internet.getLocalIPAddress;
import tools.emojis;

/**
 * A basic application launcher that abstracts the launch of Jade and the stop
 * of the associated container
 */
public class LARVABoot {

    protected boolean _connected = false, _echo = false, _debug=false;
    protected String _title, _subtitile, _version = "1.0";
    Object _args[];
    protected ArrayList<String> _tasks, _achieved;
    protected jade.core.Runtime _runtime;
    protected MicroRuntime _uruntime;
    protected ContainerController _firstContainer, _secondContainer;
    protected Profile _profile;
    protected HashMap<String, AgentController> _controllers;
    protected ArrayList<String> _agentNames;
    protected String _host, _virtualhost, _containerName, _platformId, _username, _password;
    protected final String _lockShutDownFilename = ".DeleteThisToReset.lock", _lockRebootFilename = ".Reboot.lock", _lockWaitFilename = ".Wait.lock";
    protected FileWriter _lockCloseSession, _lockReboot;
    protected int _port;
    protected double _progress;
    protected OleOptions config;
    protected String configfilename;
    protected Logger logger;

    protected enum PLATFORM {
        MAGENTIX, JADE, MICROJADE
    }
    PLATFORM _platformType;

    enum Buttons {
        Start, Shutdown
    };

    LARVAFrame fMain;
    JScrollPane pScroll;
    JTextArea taMessages;
    String title;
    JPanel pControl;
    JPanel pMain;
    JButton bStart, bExit;
    int width = 800, height = 400;
    int nlog;
    String who, name;
    String sResult;
    boolean bResult;
    String sMessages;
    Semaphore sShutdown;

    public LARVABoot() {
        _firstContainer = null;
        _containerName = "";
        _controllers = new HashMap<>();
        _agentNames = new ArrayList<>();
        logger = new Logger();
        logger.setEcho(true);
        logger.setOwner("Jade BOOT");
        logger.onTabular();
        _host = "localhost";
        _port = 1099;
        _tasks = new ArrayList<>();
        _tasks.add("ARGUMENTS");
        _tasks.add("CONFIGURE");
        _tasks.add("CONNECT");
        _tasks.add("LAUNCH");
        _achieved = new ArrayList<>();
        _args = new Component[0];
        sShutdown = new Semaphore(0);
        initGUI();
    }

    public void initGUI() {
        fMain = new LARVAFrame(e -> this.jadebootListener(e));
        pMain = new JPanel();
        BoxLayout pHBox = new BoxLayout(pMain, BoxLayout.Y_AXIS);
        pMain.setLayout(pHBox);
        pMain.setBorder(new EmptyBorder(new Insets(4, 4, 4, 4)));

        taMessages = new JTextArea(100, 100);
        taMessages.setEditable(false);
        taMessages.setWrapStyleWord(true);

        pControl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pControl.setPreferredSize(new Dimension(width, 32));
        bExit = new JButton(Buttons.Shutdown.name());
        bExit.addActionListener(fMain);
        pControl.add(bExit);
        pScroll = new JScrollPane(taMessages);
        pScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        pScroll.setPreferredSize(new Dimension(width, height - pScroll.getHeight()));
        pMain.add(pScroll);
        pMain.add(pControl);
        fMain.add(pMain);
        fMain.setSize(width, height);
        fMain.setVisible(true);
        fMain.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fMain.show();
    }

    protected void refreshGUI() {
        title = "LARVA Launcher";
        if (!_achieved.contains("ARGUMENTS")) {
            title = emojis.BLACKCIRCLE + " " + (int) (_progress * 100) + "% " + title + " processing arguments";
        } else if (!_achieved.contains("CONFIGURE")) {
            title = emojis.BLACKCIRCLE + " " + (int) (_progress * 100) + "% " + title + " loading configuration";
        } else if (!_connected) {
            title = emojis.BLACKCIRCLE + " " + (int) (_progress * 100) + "% " + title + " Connecting to JADE";
        } else {
            title = emojis.WHITECIRCLE + " " + title + "[" + this._platformType + "] " + this._host + ":" + this._port;
        }
        fMain.setTitle(title);

//        fMain.revalidate();
        fMain.repaint();
//        taMessages.revalidate();
//        taMessages.repaint();
    }

    public LARVABoot Boot(String host, int port) {
        return this.selectConnection(host, port);
    }

    protected LARVABoot doCompleted(String task) {
        if (_tasks.contains(task) && !isCompleted(task)) {
            _achieved.add(task);
            Progress();
        }
        return this;
    }

    protected boolean isCompleted(String task) {
        return _achieved.contains(task);
    }

    protected LARVABoot processArguments() {
        Info("Processing arguments:");
        if (_args.length > 0) {
            for (int i = 0; i < _args.length; i++) {
                switch ((String) _args[i]) {
                    case "-config":
                        if (i + 1 < _args.length) {
                            configfilename = (String) _args[++i];
                        } else {
                            Abort("Error, missing argument in call");
                        }
                        break;
                    case "-silent":
                        logger.setEcho(false);
                        break;
                    default:
                        Abort("Error, missing argument in call");
                }
            }
        } else {

        }
        config = new OleOptions();
        if (configfilename != null && !new File(configfilename).exists()) {
            configfilename = null;
        }
        if (configfilename != null) {
            if (config.loadFile(configfilename).isEmpty()) {
                Abort("Error loading confg file " + configfilename);
            }
        }
        config.setField("rebootjade", this._lockRebootFilename);
        config.setField("shutdownjade", this._lockShutDownFilename);
        doCompleted("ARGUMENTS");
        return this;
    }

    protected LARVABoot Configure() {
        if (!isCompleted("ARGUMENTS")) {
            processArguments();
        }
        Info("Configuring boot:");
        if (configfilename != null) {
            OleOptions cfgbasic = new OleOptions(new Ole(config.getField("basic")));
            if (cfgbasic.getFullFieldList().contains("savelog") && cfgbasic.getBoolean("savelog")) {
                if (cfgbasic.getFullFieldList().contains("logfile")) {
                    logger.setLoggerFileName(cfgbasic.getString("logfile"));
                } else {
                    logger.setLoggerFileName("default_log.json");
                }
                if (cfgbasic.getFullFieldList().contains("host")) {
                    _host = cfgbasic.getString("host");
                }
                if (cfgbasic.getFullFieldList().contains("port")) {
                    _port = cfgbasic.getInt("port");
                }
                if (cfgbasic.getFullFieldList().contains("containername")) {
                    this._containerName = cfgbasic.getString("containername");
                }
            }
            Info("%% BOOTING %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            Info("Loaded config " + configfilename);
        }
        doCompleted("CONFIGURE");
        return this;
    }

    /**
     * Inner method to set a full-p2p Jade connection
     *
     * @param host Host that contains the main container
     * @param port Port
     * @return A reference to the same instance
     */
    protected LARVABoot setupJadeConnection(String host, int port) {
        if (!isCompleted("CONFIGURE")) {
            Configure();
        }
        Info("Trying to connecto to Jade (Boot) @" + host + ":" + port);
        _platformType = PLATFORM.JADE;
        _host = host;
        _port = port;

        try {
            Info("jade.Boot Host " + _host + "["
                    + _port + "] <"
                    + _platformId + ">");
            if (_firstContainer == null) {
                _runtime = jade.core.Runtime.instance();
                _profile = new ProfileImpl();
                if (!_host.equals("")) {
                    _profile.setParameter(Profile.MAIN_HOST, _host);
                }
                if (_port != -1) {
                    _profile.setParameter(Profile.MAIN_PORT, "" + _port);
                }
                if (!_containerName.equals("")) {
                    _profile.setParameter(Profile.CONTAINER_NAME, _containerName);
                }
                _firstContainer = _runtime.createAgentContainer(_profile);
                if (_containerName == null || _containerName.equals("")) {
                    _containerName = _firstContainer.getContainerName();
                }
            }
//            _runtime.setCloseVM(true);
            _connected = true;
            this.refreshGUI();
            Info("Connected to Jade");
        } catch (Exception ex) {
            Abort("Unable to connect:");
        }

        doCompleted("CONNECT");
        return this;
    }

    /**
     * Inner method to set a restricted-p2p Jade connection
     *
     * @param host Host that contains the main container
     * @param port Port
     * @return A reference to the same instance
     */
    protected LARVABoot setupMicroJadeConnection(String host, int port) {
        if (!isCompleted("CONFIGURE")) {
            Configure();
        }
        System.out.println("Trying to connecto to Jade (MicroBoot) @" + host + ":" + port);
        _platformType = PLATFORM.MICROJADE;
        _host = host;
        _port = port;
        _controllers = new HashMap<>();
        _agentNames = new ArrayList<>();

        Info("jade.MicroBoot Host: " + _host + "["
                + _port + "] <"
                + _platformId + ">");
        jade.util.leap.Properties pr = new jade.util.leap.Properties();
        if (!_host.equals("")) {
            pr.setProperty(Profile.MAIN_HOST, _host);
        }
        if (_port != -1) {
            pr.setProperty(Profile.MAIN_PORT, "" + _port);
        }

        MicroRuntime.startJADE(pr, null);
        _containerName = MicroRuntime.getContainerName();
        doCompleted("CONNECT");
        return this;
    }

    /**
     * Analyzes the inet connection and sets upa a LARVABoot or jade.Microboot
 conection, the most appropriate one
     *
     * @param host The target host
     * @param port The target port
     * @return A reference to the same instance
     */
    protected LARVABoot selectConnection(String host, int port) {

        if (!isCompleted("CONFIGURE")) {
            Configure();
        }
        _host = host;
        _port = port;
        if (isBehindRouter()) {
            return setupMicroJadeConnection(host, port);
        } else {
            return setupJadeConnection(host, port);
        }
    }

    protected LARVABoot selectConnection() {

        if (!isCompleted("CONFIGURE")) {
            Configure();
        }

        return selectConnection(_host, _port);
    }

    public LARVABoot launchAgent(String name, Class c) {
        Info("Launching agent " + name);
        if (!isCompleted("CONNECT")) {
            Abort("Please configure the connection first");
        }
        AgentController ag;
        _agentNames.add(name);
        _args = new Object[3];
        _args[0] = fMain;
        _args[1] = this.pScroll;
        _args[2] = this.taMessages;
        if (isMicroBoot()) {
            try {
                MicroRuntime.startAgent(name, c.getName(), _args);
                ag = MicroRuntime.getAgent(name);
                _controllers.put(name, ag);
            } catch (Exception ex) {
                Error("ERROR CREATING AGENT " + name);
                Exception(ex);
            }
        } else {
            try {
                ag = _firstContainer.createNewAgent(name, c.getName(), _args);
                ag.start();
                _controllers.put(name, ag);
            } catch (Exception e) {
                Error("Error creating Agent " + name);
                Exception(e);
                ag = null;
            }
        }

        doCompleted("LAUNCH");
        return this;
    }

    protected LARVABoot Progress() {
        _progress = _achieved.size() * 1.0 / _tasks.size();
        refreshGUI();
        return this;
    }

    protected void Info(String s) {
        logger.logMessage(s);
        taMessages.append(logger.getLastlog()); //logger.getLastlog());
        refreshGUI();
        if (_debug)
            Alert(s);
    }

    protected void Error(String s) {
        logger.logError(s);
        taMessages.append(logger.getLastlog()); //logger.getLastlog());
        Alert(s);
        refreshGUI();
    }

    protected void Exception(Exception ex) {
        logger.logException(ex);
        taMessages.append(logger.getLastlog());
        refreshGUI();
    }

//    public LARVABoot WaitToClose() {
//        boolean somealive;
//        String alive;
//        Info("Waiting for agents to close");
//        do {
//            alive = "";
//            somealive = false;
//            for (String sname : _agentNames) {
//                String name = "" + sname;
//                try {
//                    if (isMicroBoot()) {
//                        somealive = MicroRuntime.size() > 0;
//                    } else {
//                        _firstContainer.getAgent(name);
//                        somealive = true;
//                    }
//                    alive += name + ". ";
//                } catch (Exception ex) {
//                    _controllers.remove(name);
////                    _agentNames.remove(name);
//                }
//            }
//            if (somealive) {
//                try {
//                    Thread.sleep(2500);
//                } catch (Exception e) {
//                }
//            }
//        } while (somealive);
//        return this;
//    }

    public LARVABoot Close() {
        // Kill all agents
        try {
            this.sShutdown.acquire();
        } catch (Exception ex) {
        };
        Info("Shutting down");
        Info("Killing all remaining agents");
        this._achieved.remove("LAUNCH");
        AgentController agc;
        for (String name : _agentNames) {
            try {
                if (isMicroBoot()) {
                    agc = MicroRuntime.getAgent(name);
                } else {
                    agc = _firstContainer.getAgent(name);
                }
                agc.kill();
                _controllers.remove(name);
            } catch (Exception ex) {
            }
        }
        return this;
    }

    public LARVABoot WaitAndShutDown() {
        Close();
        ShutDown();
        return this;
    }

    public LARVABoot ShutDown() {
        Info("Turning off JadeBoot");
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }

        turnOff(_firstContainer);
        fMain.dispatchEvent(new WindowEvent(fMain, WindowEvent.WINDOW_CLOSING));
//        System.exit(0);
        return this;
    }

    protected void turnOff(ContainerController container) {
        Info("Shutting down container " + _containerName);
        try {
            if (isMicroBoot()) {
                MicroRuntime.stopJADE();
            } else {
                try {
                    container.kill();
                } catch (Exception ex) {
//                Exception(ex);
                }
            }
        } catch (Exception ex) {
//                Exception(ex);
        }
        Info("Container " + _containerName + " shut down");
    }

    public void doSwingLater(Runnable what) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                what.run();
            });
        } else {
            what.run();
        }
    }

    public void doSwingWait(Runnable what) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    what.run();
                });
            } catch (Exception ex) {
            }
        } else {
            what.run();
        }
    }

    public void jadebootListener(ActionEvent e) {
        if (e.getActionCommand().equals(Buttons.Shutdown.name())) {
            if (Confirm("Kill all agents and exit?")) {
                this.sShutdown.release();
            }
        }
    }

    protected void Alert(String message) {
        JOptionPane.showMessageDialog(this.fMain,
                message, "LARVA Boot", JOptionPane.INFORMATION_MESSAGE);
    }

    protected String inputLine(String message) {
        sResult = JOptionPane.showInputDialog(this.fMain, message, "LARVA Boot", JOptionPane.QUESTION_MESSAGE);
        return sResult;
    }

    protected boolean Confirm(String message) {
        bResult = JOptionPane.showConfirmDialog(this.fMain,
                message, "LARVA Boot", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        return bResult;
    }

    protected boolean isJade() {
        return (_platformType == PLATFORM.JADE
                || _platformType == PLATFORM.MICROJADE);
    }

    protected boolean isMicroBoot() {
        return _platformType == PLATFORM.MICROJADE;
    }

    protected void Abort(String s) {
        Error(s);
        Exit();
    }

    protected void Exit() {
        Info("AppBoot exiting");
        ShutDown();
    }

    protected boolean isBehindRouter() {
        return !_host.equals("localhost")
                && !getExtIPAddress().equals(getLocalIPAddress());
    }

    public JFrame getMyFrame() {
        return fMain;
    }

    public JScrollPane getMyPane() {
        return pScroll;
    }

    public JTextArea getMessages() {
        return taMessages;
    }

    public boolean isDebug() {
        return _debug;
    }

    public void setDebug(boolean _debug) {
        this._debug = _debug;
    }

}