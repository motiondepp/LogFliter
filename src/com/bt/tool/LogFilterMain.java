package com.bt.tool;

import com.bt.tool.annotation.CheckBoxSaveState;
import com.bt.tool.annotation.FieldSaveState;
import com.bt.tool.annotation.StateSaver;
import com.bt.tool.annotation.TextFieldSaveState;
import com.bt.tool.diff.DiffService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LogFilterMain extends JFrame implements INotiEvent {
    private static final long serialVersionUID = 1L;

    static final String LOGFILTER = "LogFilter";
    static final String VERSION = "Version 1.8.1";
    static final String OUTPUT_LOG_DIR = "log";
    static final String CONFIG_BASE_DIR = "conf";
    final String COMBO_ANDROID = "Android          ";
    final String COMBO_IOS = "ios";
    final String COMBO_CUSTOM_COMMAND = "custom command";
    final String IOS_DEFAULT_CMD = "adb logcat -v time ";
    final String IOS_SELECTED_CMD_FIRST = "adb -s ";
    final String IOS_SELECTED_CMD_LAST = " logcat -v time ";
    final String ANDROID_DEFAULT_CMD = "logcat -v time";
    final String ANDROID_THREAD_CMD = "logcat -v threadtime";
    final String ANDROID_EVENT_CMD = "logcat -b events -v time";
    final String ANDROID_RADIO_CMD = "logcat -b radio -v time";
    final String ANDROID_CUSTOM_CMD = "shell cat /proc/kmsg";
    final String ANDROID_DEFAULT_CMD_FIRST = "adb ";
    final String ANDROID_SELECTED_CMD_FIRST = "adb -s ";
    // final String ANDROID_SELECTED_CMD_LAST = " logcat -v time ";
    final String[] DEVICES_CMD = {"adb devices -l", "", ""};

    static final int DEFAULT_WIDTH = 1200;
    static final int DEFAULT_HEIGHT = 720;
    static final int MIN_WIDTH = 1100;
    static final int MIN_HEIGHT = 500;

    static final int DEVICES_ANDROID = 0;
    static final int DEVICES_IOS = 1;
    static final int DEVICES_CUSTOM = 2;

    static final int STATUS_CHANGE = 1;
    static final int STATUS_PARSING = 2;
    static final int STATUS_READY = 4;

    private static final String DIFF_PROGRAM_PATH = "C:\\Program Files\\KDiff3\\kdiff3.exe";

    JTabbedPane m_tpTab;
    JLabel m_tfStatus;
    IndicatorPanel m_ipIndicator;
    ArrayList<LogInfo> m_arLogInfoAll;
    ArrayList<LogInfo> m_arLogInfoFiltered;
    HashMap<Integer, Integer> m_hmBookmarkAll;
    HashMap<Integer, Integer> m_hmBookmarkFiltered;
    ConcurrentHashMap<Integer, Integer> m_hmErrorAll;
    ConcurrentHashMap<Integer, Integer> m_hmErrorFiltered;
    ILogParser m_iLogParser;
    LogTable m_tbLogTable;
    JScrollPane m_scrollVBar;
    LogFilterTableModel m_tmLogTableModel;
    boolean m_bUserFilter;

    // Word Filter, tag filter
    JTextField m_tfSearch;
    @TextFieldSaveState
    JTextField m_tfHighlight;

    @TextFieldSaveState
    JTextField m_tfFindWord;
    @TextFieldSaveState
    JTextField m_tfRemoveWord;
    @TextFieldSaveState
    JTextField m_tfShowTag;
    @TextFieldSaveState
    JTextField m_tfRemoveTag;
    @TextFieldSaveState
    JTextField m_tfShowPid;
    @TextFieldSaveState
    JTextField m_tfShowTid;
    @TextFieldSaveState
    JTextField m_tfBookmarkTag;

    // Device
    JButton m_btnDevice;
    JList<TargetDevice> m_lDeviceList;
    JComboBox<String> m_comboDeviceCmd;
    JComboBox<String> m_comboCmd;
    JButton m_btnSetFont;

    // Log filter enable/disable
    @CheckBoxSaveState
    JCheckBox m_chkEnableFind;
    @CheckBoxSaveState
    JCheckBox m_chkEnableRemove;
    @CheckBoxSaveState
    JCheckBox m_chkEnableShowTag;
    @CheckBoxSaveState
    JCheckBox m_chkEnableRemoveTag;
    @CheckBoxSaveState
    JCheckBox m_chkEnableShowPid;
    @CheckBoxSaveState
    JCheckBox m_chkEnableShowTid;
    @CheckBoxSaveState
    JCheckBox m_chkEnableHighlight;
    @CheckBoxSaveState
    JCheckBox m_chkEnableBookmarkTag;

    private JCheckBox m_chkEnableTimeTag;

    // Log filter
    @CheckBoxSaveState
    JCheckBox m_chkVerbose;
    @CheckBoxSaveState
    JCheckBox m_chkDebug;
    @CheckBoxSaveState
    JCheckBox m_chkInfo;
    @CheckBoxSaveState
    JCheckBox m_chkWarn;
    @CheckBoxSaveState
    JCheckBox m_chkError;
    @CheckBoxSaveState
    JCheckBox m_chkFatal;

    // Show column
    @CheckBoxSaveState
    JCheckBox m_chkClmBookmark;
    @CheckBoxSaveState
    JCheckBox m_chkClmLine;
    @CheckBoxSaveState
    JCheckBox m_chkClmDate;
    @CheckBoxSaveState
    JCheckBox m_chkClmTime;
    @CheckBoxSaveState
    JCheckBox m_chkClmLogLV;
    @CheckBoxSaveState
    JCheckBox m_chkClmPid;
    @CheckBoxSaveState
    JCheckBox m_chkClmThread;
    @CheckBoxSaveState
    JCheckBox m_chkClmTag;
    @CheckBoxSaveState
    JCheckBox m_chkClmMessage;

    @TextFieldSaveState
    JTextField m_tfFontSize;
    // JTextField m_tfProcessCmd;

    JComboBox<String> m_comboEncode;
    //    JComboBox m_jcFontType;
    JButton m_btnRun;
    JButton m_btnClear;
    JToggleButton m_tbtnPause;
    JButton m_btnStop;

    String m_strLogFileName;
    String m_strSelectedDevice;
    // String m_strProcessCmd;
    Process m_Process;
    Thread m_thProcess;
    Thread m_thWatchFile;
    Thread m_thFilterParse;
    boolean m_bPauseADB;

    Object FILE_LOCK;
    Object FILTER_LOCK;
    volatile int m_nChangedFilter;
    int m_nFilterLogLV;
    @FieldSaveState
    int m_nWinWidth = DEFAULT_WIDTH;
    @FieldSaveState
    int m_nWinHeight = DEFAULT_HEIGHT;
    int m_nLastWidth;
    int m_nLastHeight;
    @FieldSaveState
    int m_nWindState;
    static RecentFileMenu m_recentMenu;

    @FieldSaveState
    int[] m_colWidths = LogFilterTableModel.DEFULT_WIDTH;

    @FieldSaveState
    private String m_strLastDir;

    private static JMenuItem sDisconnectDiffMenuItem;
    private static JMenuItem sConnectDiffMenuItem;

    public DiffService mDiffService;
    private JLabel m_tfDiffPort;
    private JLabel m_tfDiffState;
    private JCheckBox mSyncScrollCheckBox;
    private JCheckBox mSyncSelectedCheckBox;
    JComboBox<DumpOfServiceInfo> mDumpOfServiceBomboBox;
    private boolean mSyncScrollEnable;
    private boolean mSyncScrollSelected;

    private final StateSaver mStateSaver;
    private JTextField m_tfFromTimeTag;
    private JTextField m_tfToTimeTag;

    public static void main(final String args[]) {
        // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final LogFilterMain mainFrame = new LogFilterMain();
        mainFrame.setTitle(LOGFILTER + " " + VERSION);
        // mainFrame.addWindowListener(new com.bt.tool.WindowEventHandler());

        JMenuBar menubar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem fileOpen = new JMenuItem("Open");
        fileOpen.setMnemonic(KeyEvent.VK_O);
        fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                ActionEvent.ALT_MASK));
        fileOpen.setToolTipText("Open log file");
        fileOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openFileBrowserToLoad(FileType.LOGFILE);
            }
        });

        JMenu modeMenu = new JMenu("Mode");

        JMenuItem modeOpen = new JMenuItem("Open Mode File");
        modeOpen.setToolTipText("Open .mode file");
        modeOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openFileBrowserToLoad(FileType.MODEFILE);
            }
        });
        JMenuItem modeSave = new JMenuItem("Save Mode File");
        modeSave.setToolTipText("Save .mode file");
        modeSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.openFileBrowserToSave(FileType.MODEFILE);
            }
        });

        modeMenu.add(modeOpen);
        modeMenu.add(modeSave);

        m_recentMenu = new RecentFileMenu("RecentFile", 10) {
            public void onSelectFile(String filePath) {
                mainFrame.parseFile(new File(filePath));
            }
        };

        fileMenu.add(fileOpen);
        fileMenu.add(modeMenu);
        fileMenu.add(m_recentMenu);

        JMenu netMenu = new JMenu("Net");
        JMenu diffMenu = new JMenu("Diff Service");

        sDisconnectDiffMenuItem = new JMenuItem("disconnect");
        sDisconnectDiffMenuItem.setEnabled(false);
        sConnectDiffMenuItem = new JMenuItem("connect to diff server");

        sDisconnectDiffMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainFrame.mDiffService.disconnectDiffClient();
                sConnectDiffMenuItem.setEnabled(true);
                sDisconnectDiffMenuItem.setEnabled(false);
            }
        });
        sConnectDiffMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String serverPort = JOptionPane.showInputDialog(
                        mainFrame,
                        "Enter Server Port",
                        "",
                        JOptionPane.QUESTION_MESSAGE
                );
                if (serverPort != null && serverPort.length() != 0) {
                    if (mainFrame.mDiffService.setupDiffClient(serverPort)) {
                        sConnectDiffMenuItem.setEnabled(false);
                        sDisconnectDiffMenuItem.setEnabled(true);
                    }
                }
            }
        });


        diffMenu.add(sConnectDiffMenuItem);
        diffMenu.add(sDisconnectDiffMenuItem);
        netMenu.add(diffMenu);

        menubar.add(fileMenu);
        menubar.add(netMenu);
        mainFrame.setJMenuBar(menubar);
        mainFrame.pack();

        if (args != null && args.length > 0) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    mainFrame.parseFile(new File(args[0]));
                }
            });
        }
    }

    String makeFilename() {
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return OUTPUT_LOG_DIR + File.separator + "LogFilter_" + format.format(now) + ".txt";
    }

    void exit() {
        if (m_Process != null)
            m_Process.destroy();
        if (m_thProcess != null)
            m_thProcess.interrupt();
        if (m_thWatchFile != null)
            m_thWatchFile.interrupt();
        if (m_thFilterParse != null)
            m_thFilterParse.interrupt();

        saveColor();
        mStateSaver.save();
        System.exit(0);
    }

    /**
     * @throws HeadlessException
     */
    public LogFilterMain() {
        super();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        initValue();
        createComponent();

        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());

        pane.add(getOptionPanel(), BorderLayout.NORTH);
        pane.add(getBookmarkPanel(), BorderLayout.WEST);
        pane.add(getStatusPanel(), BorderLayout.SOUTH);
        pane.add(getTabPanel(), BorderLayout.CENTER);

        setDnDListener();
        addChangeListener();
        startFilterParse();

        setVisible(true);
        addDesc();

        // register state saver
        mStateSaver = new StateSaver(this, INI_FILE_STATE);
        mStateSaver.load();

        loadUI();
        loadColor();
        loadCmd();
        initDiffService();

        setSize(m_nWinWidth, m_nWinHeight);
        setExtendedState(m_nWindState);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(mKeyEventDispatcher);
    }

    private void loadUI() {
        loadTableColumnState();
        getLogTable().setFontSize(Integer.parseInt(m_tfFontSize
                .getText()));
        updateTable(-1, false);
    }

    final String INI_FILE = CONFIG_BASE_DIR + File.separator + "LogFilter.ini";
    final String INI_FILE_CMD = CONFIG_BASE_DIR + File.separator + "LogFilterCmd.ini";
    final String INI_FILE_COLOR = CONFIG_BASE_DIR + File.separator + "LogFilterColor.ini";
    final String INI_FILE_STATE = CONFIG_BASE_DIR + File.separator + "LogFilterState.ser";
    final String INI_LAST_DIR = "LAST_DIR";
    final String INI_CMD_COUNT = "CMD_COUNT";
    final String INI_CMD = "CMD_";
    final String INI_FONT_TYPE = "FONT_TYPE";
    final String INI_WORD_FIND = "WORD_FIND";
    final String INI_WORD_REMOVE = "WORD_REMOVE";
    final String INI_TAG_SHOW = "TAG_SHOW";
    final String INI_TAG_REMOVE = "TAG_REMOVE";
    final String INI_HIGHLIGHT = "HIGHLIGHT";
    final String INI_PID_SHOW = "PID_SHOW";
    final String INI_TID_SHOW = "TID_SHOW";
    final String INI_COLOR_0 = "INI_COLOR_0";
    final String INI_COLOR_1 = "INI_COLOR_1";
    final String INI_COLOR_2 = "INI_COLOR_2";
    final String INI_COLOR_3 = "INI_COLOR_3(E)";
    final String INI_COLOR_4 = "INI_COLOR_4(W)";
    final String INI_COLOR_5 = "INI_COLOR_5";
    final String INI_COLOR_6 = "INI_COLOR_6(I)";
    final String INI_COLOR_7 = "INI_COLOR_7(D)";
    final String INI_COLOR_8 = "INI_COLOR_8(F)";
    final String INI_HIGILIGHT_COUNT = "INI_HIGILIGHT_COUNT";
    final String INI_HIGILIGHT_ = "INI_HIGILIGHT_";
    final String INI_WIDTH = "INI_WIDTH";
    final String INI_HEIGHT = "INI_HEIGHT";
    final String INI_WINDOW_STATE = "INI_WINDOW_STATE";

    final String INI_FILTER_LEVEL_V = "INI_FILTER_LEVEL_V";
    final String INI_FILTER_LEVEL_D = "INI_FILTER_LEVEL_D";
    final String INI_FILTER_LEVEL_I = "INI_FILTER_LEVEL_I";
    final String INI_FILTER_LEVEL_W = "INI_FILTER_LEVEL_W";
    final String INI_FILTER_LEVEL_E = "INI_FILTER_LEVEL_E";
    final String INI_FILTER_LEVEL_F = "INI_FILTER_LEVEL_F";

    final String INI_SHOW_COLUMN_MARK = "INI_SHOW_COLUMN_MARK";
    final String INI_SHOW_COLUMN_LINE = "INI_SHOW_COLUMN_LINE";
    final String INI_SHOW_COLUMN_DATE = "INI_SHOW_COLUMN_DATE";
    final String INI_SHOW_COLUMN_TIME = "INI_SHOW_COLUMN_TIME";
    final String INI_SHOW_COLUMN_LV = "INI_SHOW_COLUMN_LV";
    final String INI_SHOW_COLUMN_PID = "INI_SHOW_COLUMN_PID";
    final String INI_SHOW_COLUMN_TID = "INI_SHOW_COLUMN_TID";
    final String INI_SHOW_COLUMN_TAG = "INI_SHOW_COLUMN_TAG";
    final String INI_SHOW_COLUMN_MSG = "INI_SHOW_COLUMN_MSG";


    final String INI_COMUMN = "INI_COMUMN_";

    void loadCmd() {
        try {
            Properties p = new Properties();

            try {
                p.load(new FileInputStream(INI_FILE_CMD));
            } catch (FileNotFoundException e) {
                T.d(INI_FILE_CMD + " not exist!");
            }

            if (p.getProperty(INI_CMD_COUNT) == null) {
                p.setProperty(INI_CMD + "0", ANDROID_THREAD_CMD);
                p.setProperty(INI_CMD + "1", ANDROID_DEFAULT_CMD);
                p.setProperty(INI_CMD + "2", ANDROID_RADIO_CMD);
                p.setProperty(INI_CMD + "3", ANDROID_EVENT_CMD);
                p.setProperty(INI_CMD + "4", ANDROID_CUSTOM_CMD);
                p.setProperty(INI_CMD_COUNT, "5");
                p.store(new FileOutputStream(INI_FILE_CMD), null);
            }

            T.d("p.getProperty(INI_CMD_COUNT) = "
                    + p.getProperty(INI_CMD_COUNT));
            int nCount = Integer.parseInt(p.getProperty(INI_CMD_COUNT));
            T.d("nCount = " + nCount);
            for (int nIndex = 0; nIndex < nCount; nIndex++) {
                T.d("CMD = " + INI_CMD + nIndex);
                m_comboCmd.addItem(p.getProperty(INI_CMD + nIndex));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    void loadColor() {
        try {
            Properties p = new Properties();

            p.load(new FileInputStream(INI_FILE_COLOR));

            LogColor.COLOR_0 = Integer.parseInt(p.getProperty(INI_COLOR_0)
                    .replace("0x", ""), 16);
            LogColor.COLOR_1 = Integer.parseInt(p.getProperty(INI_COLOR_1)
                    .replace("0x", ""), 16);
            LogColor.COLOR_2 = Integer.parseInt(p.getProperty(INI_COLOR_2)
                    .replace("0x", ""), 16);
            LogColor.COLOR_ERROR = LogColor.COLOR_3 = Integer.parseInt(p
                    .getProperty(INI_COLOR_3).replace("0x", ""), 16);
            LogColor.COLOR_WARN = LogColor.COLOR_4 = Integer.parseInt(p
                    .getProperty(INI_COLOR_4).replace("0x", ""), 16);
            LogColor.COLOR_5 = Integer.parseInt(p.getProperty(INI_COLOR_5)
                    .replace("0x", ""), 16);
            LogColor.COLOR_INFO = LogColor.COLOR_6 = Integer.parseInt(p
                    .getProperty(INI_COLOR_6).replace("0x", ""), 16);
            LogColor.COLOR_DEBUG = LogColor.COLOR_7 = Integer.parseInt(p
                    .getProperty(INI_COLOR_7).replace("0x", ""), 16);
            LogColor.COLOR_FATAL = LogColor.COLOR_8 = Integer.parseInt(p
                    .getProperty(INI_COLOR_8).replace("0x", ""), 16);

            int nCount = Integer.parseInt(p.getProperty(INI_HIGILIGHT_COUNT,
                    "0"));
            if (nCount > 0) {
                LogColor.COLOR_HIGHLIGHT = new String[nCount];
                for (int nIndex = 0; nIndex < nCount; nIndex++)
                    LogColor.COLOR_HIGHLIGHT[nIndex] = p.getProperty(
                            INI_HIGILIGHT_ + nIndex).replace("0x", "");
            } else {
                LogColor.COLOR_HIGHLIGHT = new String[1];
                LogColor.COLOR_HIGHLIGHT[0] = "ffff";
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    void saveColor() {
        try {
            Properties p = new Properties();

            p.setProperty(INI_COLOR_0,
                    "0x" + Integer.toHexString(LogColor.COLOR_0).toUpperCase());
            p.setProperty(INI_COLOR_1,
                    "0x" + Integer.toHexString(LogColor.COLOR_1).toUpperCase());
            p.setProperty(INI_COLOR_2,
                    "0x" + Integer.toHexString(LogColor.COLOR_2).toUpperCase());
            p.setProperty(INI_COLOR_3,
                    "0x" + Integer.toHexString(LogColor.COLOR_3).toUpperCase());
            p.setProperty(INI_COLOR_4,
                    "0x" + Integer.toHexString(LogColor.COLOR_4).toUpperCase());
            p.setProperty(INI_COLOR_5,
                    "0x" + Integer.toHexString(LogColor.COLOR_5).toUpperCase());
            p.setProperty(INI_COLOR_6,
                    "0x" + Integer.toHexString(LogColor.COLOR_6).toUpperCase());
            p.setProperty(INI_COLOR_7,
                    "0x" + Integer.toHexString(LogColor.COLOR_7).toUpperCase());
            p.setProperty(INI_COLOR_8,
                    "0x" + Integer.toHexString(LogColor.COLOR_8).toUpperCase());

            if (LogColor.COLOR_HIGHLIGHT != null) {
                p.setProperty(INI_HIGILIGHT_COUNT, ""
                        + LogColor.COLOR_HIGHLIGHT.length);
                for (int nIndex = 0; nIndex < LogColor.COLOR_HIGHLIGHT.length; nIndex++)
                    p.setProperty(INI_HIGILIGHT_ + nIndex, "0x"
                            + LogColor.COLOR_HIGHLIGHT[nIndex].toUpperCase());
            }

            p.store(new FileOutputStream(INI_FILE_COLOR), "done.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void addDesc(String strMessage) {
        LogInfo logInfo = new LogInfo();
        logInfo.m_intLine = m_arLogInfoAll.size() + 1;
        logInfo.m_strMessage = strMessage;
        m_arLogInfoAll.add(logInfo);
    }

    void addDesc() {
        addDesc(VERSION);
        addDesc("");
        addDesc("Xinyu.he fork from https://github.com/iookill/LogFilter");
    }

    void bookmarkItem(int nIndex, int nLine, boolean bBookmark) {
        synchronized (FILTER_LOCK) {
            LogInfo logInfo = m_arLogInfoAll.get(nLine);
            logInfo.m_bMarked = bBookmark;
            m_arLogInfoAll.set(nLine, logInfo);

            if (logInfo.m_bMarked) {
                m_hmBookmarkAll.put(nLine, nLine);
                if (m_bUserFilter)
                    m_hmBookmarkFiltered.put(nLine, nIndex);
            } else {
                m_hmBookmarkAll.remove(nLine);
                if (m_bUserFilter)
                    m_hmBookmarkFiltered.remove(nLine);
            }
        }
        m_ipIndicator.repaint();
    }

    void clearData() {
        m_arLogInfoAll.clear();
        m_arLogInfoFiltered.clear();
        m_hmBookmarkAll.clear();
        m_hmBookmarkFiltered.clear();
        m_hmErrorAll.clear();
        m_hmErrorFiltered.clear();
    }

    void createComponent() {
    }

    Component getBookmarkPanel() {
        JPanel jp = new JPanel();
        jp.setLayout(new BorderLayout());

        m_ipIndicator = new IndicatorPanel(this);
        m_ipIndicator.setData(m_arLogInfoAll, m_hmBookmarkAll, m_hmErrorAll);
        jp.add(m_ipIndicator, BorderLayout.CENTER);
        return jp;
    }

    Component getDevicePanel() {
        JPanel jpOptionDevice = new JPanel();
        jpOptionDevice.setBorder(BorderFactory
                .createTitledBorder("Device select"));
        jpOptionDevice.setLayout(new BorderLayout());
        // jpOptionDevice.setPreferredSize(new Dimension(200, 100));

        JPanel jpCmd = new JPanel();
        m_comboDeviceCmd = new JComboBox<String>();
        m_comboDeviceCmd.addItem(COMBO_ANDROID);
        // m_comboDeviceCmd.addItem(COMBO_IOS);
        // m_comboDeviceCmd.addItem(CUSTOM_COMMAND);
        m_comboDeviceCmd.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED)
                    return;

                DefaultListModel listModel = (DefaultListModel) m_lDeviceList
                        .getModel();
                listModel.clear();
                if (e.getItem().equals(COMBO_CUSTOM_COMMAND)) {
                    m_comboDeviceCmd.setEditable(true);
                } else {
                    m_comboDeviceCmd.setEditable(false);
                }
            }
        });

        m_btnDevice = new JButton("OK");
        m_btnDevice.setMargin(new Insets(0, 0, 0, 0));
        m_btnDevice.addActionListener(m_alButtonListener);

        jpCmd.add(m_comboDeviceCmd);
        jpCmd.add(m_btnDevice);

        jpOptionDevice.add(jpCmd, BorderLayout.NORTH);

        final DefaultListModel<TargetDevice> listModel = new DefaultListModel<>();
        m_lDeviceList = new JList<>(listModel);
        JScrollPane vbar = new JScrollPane(m_lDeviceList);
        vbar.setPreferredSize(new Dimension(100, 50));
        m_lDeviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_lDeviceList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                JList deviceList = (JList) e.getSource();
                TargetDevice selectedItem = (TargetDevice) deviceList.getSelectedValue();
                m_strSelectedDevice = "";
                if (selectedItem != null) {
                    m_strSelectedDevice = selectedItem.code;
                }
            }
        });
        jpOptionDevice.add(vbar);

        return jpOptionDevice;
    }

    void addLogInfo(LogInfo logInfo) {
        synchronized (FILTER_LOCK) {
            getLogTable().setTagLength(logInfo.m_strTag.length());
            m_arLogInfoAll.add(logInfo);
            // addTagList(logInfo.m_strTag);
            if (logInfo.m_strLogLV.equals("E")
                    || logInfo.m_strLogLV.equals("ERROR"))
                m_hmErrorAll.put(logInfo.m_intLine - 1,
                        logInfo.m_intLine - 1);

            if (m_bUserFilter) {
                if (m_ipIndicator.m_chBookmark.isSelected()
                        || m_ipIndicator.m_chError.isSelected()) {
                    boolean bAddFilteredArray = false;
                    if (logInfo.m_bMarked
                            && m_ipIndicator.m_chBookmark.isSelected()) {
                        bAddFilteredArray = true;
                        m_hmBookmarkFiltered.put(logInfo.m_intLine - 1,
                                m_arLogInfoFiltered.size());
                        if (logInfo.m_strLogLV.equals("E")
                                || logInfo.m_strLogLV.equals("ERROR"))
                            m_hmErrorFiltered.put(logInfo.m_intLine - 1,
                                    m_arLogInfoFiltered.size());
                    }
                    if ((logInfo.m_strLogLV.equals("E") || logInfo.m_strLogLV
                            .equals("ERROR"))
                            && m_ipIndicator.m_chError.isSelected()) {
                        bAddFilteredArray = true;
                        m_hmErrorFiltered.put(logInfo.m_intLine - 1,
                                m_arLogInfoFiltered.size());
                        if (logInfo.m_bMarked)
                            m_hmBookmarkFiltered.put(logInfo.m_intLine - 1,
                                    m_arLogInfoFiltered.size());
                    }

                    if (bAddFilteredArray)
                        m_arLogInfoFiltered.add(logInfo);
                } else if (checkLogLVFilter(logInfo) && checkPidFilter(logInfo)
                        && checkTidFilter(logInfo)
                        && checkShowTagFilter(logInfo)
                        && checkRemoveTagFilter(logInfo)
                        && checkFindFilter(logInfo)
                        && checkRemoveFilter(logInfo)
                        && checkFromTimeFilter(logInfo)
                        && checkToTimeFilter(logInfo)
                        && checkBookmarkFilter(logInfo)) {
                    m_arLogInfoFiltered.add(logInfo);
                    if (logInfo.m_bMarked)
                        m_hmBookmarkFiltered.put(logInfo.m_intLine - 1,
                                m_arLogInfoFiltered.size());
                    if (logInfo.m_strLogLV == "E"
                            || logInfo.m_strLogLV == "ERROR")
                        if (logInfo.m_strLogLV.equals("E")
                                || logInfo.m_strLogLV.equals("ERROR"))
                            m_hmErrorFiltered.put(logInfo.m_intLine - 1,
                                    m_arLogInfoFiltered.size());
                }
            }
        }
    }

    void addChangeListener() {
        m_tfSearch.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfHighlight.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfFindWord.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfRemoveWord.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfShowTag.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfRemoveTag.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfBookmarkTag.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfShowPid.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfShowTid.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfFromTimeTag.getDocument().addDocumentListener(m_dlFilterListener);
        m_tfToTimeTag.getDocument().addDocumentListener(m_dlFilterListener);

        m_chkEnableFind.addItemListener(m_itemListener);
        m_chkEnableRemove.addItemListener(m_itemListener);
        m_chkEnableShowPid.addItemListener(m_itemListener);
        m_chkEnableShowTid.addItemListener(m_itemListener);
        m_chkEnableShowTag.addItemListener(m_itemListener);
        m_chkEnableRemoveTag.addItemListener(m_itemListener);
        m_chkEnableBookmarkTag.addItemListener(m_itemListener);
        m_chkEnableHighlight.addItemListener(m_itemListener);
        m_chkEnableTimeTag.addItemListener(m_itemListener);

        m_chkVerbose.addItemListener(m_itemListener);
        m_chkDebug.addItemListener(m_itemListener);
        m_chkInfo.addItemListener(m_itemListener);
        m_chkWarn.addItemListener(m_itemListener);
        m_chkError.addItemListener(m_itemListener);
        m_chkFatal.addItemListener(m_itemListener);
        m_chkClmBookmark.addItemListener(m_itemListener);
        m_chkClmLine.addItemListener(m_itemListener);
        m_chkClmDate.addItemListener(m_itemListener);
        m_chkClmTime.addItemListener(m_itemListener);
        m_chkClmLogLV.addItemListener(m_itemListener);
        m_chkClmPid.addItemListener(m_itemListener);
        m_chkClmThread.addItemListener(m_itemListener);
        m_chkClmTag.addItemListener(m_itemListener);
        m_chkClmMessage.addItemListener(m_itemListener);

        m_scrollVBar.getViewport().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                // m_ipIndicator.m_bDrawFull = false;
                if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                    m_nLastWidth = getWidth();
                    m_nLastHeight = getHeight();
                }
                m_ipIndicator.repaint();
            }
        });
    }

    Component getFilterPanel() {
        m_chkEnableFind = new JCheckBox();
        m_chkEnableRemove = new JCheckBox();
        m_chkEnableShowTag = new JCheckBox();
        m_chkEnableRemoveTag = new JCheckBox();
        m_chkEnableShowPid = new JCheckBox();
        m_chkEnableShowTid = new JCheckBox();
        m_chkEnableBookmarkTag = new JCheckBox();
        m_chkEnableTimeTag = new JCheckBox();
        m_chkEnableFind.setSelected(true);
        m_chkEnableRemove.setSelected(true);
        m_chkEnableShowTag.setSelected(true);
        m_chkEnableRemoveTag.setSelected(true);
        m_chkEnableShowPid.setSelected(true);
        m_chkEnableShowTid.setSelected(true);
        m_chkEnableBookmarkTag.setSelected(false);
        m_chkEnableTimeTag.setSelected(true);

        m_tfFindWord = new JTextField();
        m_tfRemoveWord = new JTextField();
        m_tfShowTag = new JTextField();
        m_tfRemoveTag = new JTextField();
        m_tfShowPid = new JTextField();
        m_tfShowTid = new JTextField();
        m_tfBookmarkTag = new JTextField();
        m_tfFromTimeTag = new JTextField();
        m_tfToTimeTag = new JTextField();

        JPanel jpMain = new JPanel(new BorderLayout());

        JPanel jpWordFilter = new JPanel(new BorderLayout());
        jpWordFilter.setBorder(BorderFactory.createTitledBorder("Word filter"));

        JPanel jpFind = new JPanel(new BorderLayout());
        JLabel find = new JLabel();
        find.setText("include:");
        jpFind.add(find, BorderLayout.WEST);
        jpFind.add(m_tfFindWord, BorderLayout.CENTER);
        jpFind.add(m_chkEnableFind, BorderLayout.EAST);

        JPanel jpRemove = new JPanel(new BorderLayout());
        JLabel remove = new JLabel();
        remove.setText("exclude:");
        jpRemove.add(remove, BorderLayout.WEST);
        jpRemove.add(m_tfRemoveWord, BorderLayout.CENTER);
        jpRemove.add(m_chkEnableRemove, BorderLayout.EAST);

        jpWordFilter.add(jpFind, BorderLayout.NORTH);
        jpWordFilter.add(jpRemove);

        jpMain.add(jpWordFilter, BorderLayout.NORTH);

        JPanel jpTagFilter = new JPanel(new GridLayout(5, 1));
        jpTagFilter.setBorder(BorderFactory.createTitledBorder("Tag filter"));

        JPanel jpPidTid = new JPanel(new GridLayout(1, 2));

        JPanel jpPid = new JPanel(new BorderLayout());
        JLabel pid = new JLabel();
        pid.setText("Pid : ");
        jpPid.add(pid, BorderLayout.WEST);
        jpPid.add(m_tfShowPid, BorderLayout.CENTER);
        jpPid.add(m_chkEnableShowPid, BorderLayout.EAST);

        JPanel jpTid = new JPanel(new BorderLayout());
        JLabel tid = new JLabel();
        tid.setText("Tid : ");
        jpTid.add(tid, BorderLayout.WEST);
        jpTid.add(m_tfShowTid, BorderLayout.CENTER);
        jpTid.add(m_chkEnableShowTid, BorderLayout.EAST);

        jpPidTid.add(jpPid);
        jpPidTid.add(jpTid);

        JPanel jpShow = new JPanel(new BorderLayout());
        JLabel show = new JLabel();
        show.setText("Tag Include: ");
        jpShow.add(show, BorderLayout.WEST);
        jpShow.add(m_tfShowTag, BorderLayout.CENTER);
        jpShow.add(m_chkEnableShowTag, BorderLayout.EAST);

        JPanel jpRemoveTag = new JPanel(new BorderLayout());
        JLabel removeTag = new JLabel();
        removeTag.setText("Tag Exclude: ");
        jpRemoveTag.add(removeTag, BorderLayout.WEST);
        jpRemoveTag.add(m_tfRemoveTag, BorderLayout.CENTER);
        jpRemoveTag.add(m_chkEnableRemoveTag, BorderLayout.EAST);

        JPanel jpBmTag = new JPanel(new BorderLayout());
        JLabel bkTag = new JLabel();
        bkTag.setText("Bookmark: ");
        jpBmTag.add(bkTag, BorderLayout.WEST);
        jpBmTag.add(m_tfBookmarkTag, BorderLayout.CENTER);
        jpBmTag.add(m_chkEnableBookmarkTag, BorderLayout.EAST);

        JPanel jpFromTimeTag = new JPanel(new BorderLayout());
        JLabel jlFromTimeTag = new JLabel();
        jlFromTimeTag.setText("from time");
        jpFromTimeTag.add(jlFromTimeTag, BorderLayout.WEST);
        jpFromTimeTag.add(m_tfFromTimeTag, BorderLayout.CENTER);

        JPanel jpToTimeTag = new JPanel(new BorderLayout());
        JLabel jlToTimeTag = new JLabel();
        jlToTimeTag.setText("to");
        jpToTimeTag.add(jlToTimeTag, BorderLayout.WEST);
        jpToTimeTag.add(m_tfToTimeTag, BorderLayout.CENTER);

        JPanel jpTimeTag = new JPanel(new GridLayout(1, 2));
        jpTimeTag.add(jpFromTimeTag);
        jpTimeTag.add(jpToTimeTag);
        JPanel jpTimeMainTag = new JPanel(new BorderLayout());
        jpTimeMainTag.add(jpTimeTag, BorderLayout.CENTER);
        jpTimeMainTag.add(m_chkEnableTimeTag, BorderLayout.EAST);

        jpTagFilter.add(jpPidTid);
        jpTagFilter.add(jpShow);
        jpTagFilter.add(jpRemoveTag);
        jpTagFilter.add(jpBmTag);
        jpTagFilter.add(jpTimeMainTag);

        jpMain.add(jpTagFilter, BorderLayout.CENTER);

        JPanel jpActionPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JButton clearFieldBtn = new JButton("Clean");
        clearFieldBtn.setMargin(new Insets(0, 0, 0, 0));
        clearFieldBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_tfFindWord.setText("");
                m_tfRemoveWord.setText("");
                m_tfShowTag.setText("");
                m_tfRemoveTag.setText("");
                m_tfShowPid.setText("");
                m_tfShowTid.setText("");
                m_tfBookmarkTag.setText("");
            }
        });
        jpActionPanel.add(clearFieldBtn);

        JButton followBtn = new JButton("Follow");
        followBtn.setMargin(new Insets(0, 0, 0, 0));
        followBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int endLine = m_tmLogTableModel.getRowCount();
                updateTable(endLine - 1, true);
            }
        });
        jpActionPanel.add(followBtn);

        jpMain.add(jpActionPanel, BorderLayout.SOUTH);

        return jpMain;
    }

    Component getHighlightPanel() {
        m_chkEnableHighlight = new JCheckBox();
        m_chkEnableHighlight.setSelected(true);

        m_tfHighlight = new JTextField();

        JPanel jpMain = new JPanel(new BorderLayout());
        jpMain.setBorder(BorderFactory.createTitledBorder("Highlight"));
        jpMain.add(m_tfHighlight);
        jpMain.add(m_chkEnableHighlight, BorderLayout.EAST);

        return jpMain;
    }

    Component getSearchPanel() {
        m_tfSearch = new JTextField();
        m_tfSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLogTable().gotoNextSearchResult();
            }
        });

        JPanel jpMain = new JPanel(new BorderLayout());
        jpMain.setBorder(BorderFactory.createTitledBorder("Search"));
        jpMain.add(m_tfSearch, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        JButton preButton = new JButton();
        preButton.setMargin(new Insets(0, 0, 0, 0));
        preButton.setText("<=");
        preButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLogTable().gotoPreSearchResult();
            }
        });
        buttonPanel.add(preButton);

        JButton nextButton = new JButton();
        nextButton.setMargin(new Insets(0, 0, 0, 0));
        nextButton.setText("=>");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLogTable().gotoNextSearchResult();
            }
        });
        buttonPanel.add(nextButton);

        jpMain.add(buttonPanel, BorderLayout.EAST);

        return jpMain;
    }

    public Component getDumpOfServicePanel() {
        JPanel jpMain = new JPanel(new BorderLayout());
        jpMain.setBorder(BorderFactory.createTitledBorder("Dump of Service"));

        mDumpOfServiceBomboBox = new JComboBox<>();
        mDumpOfServiceBomboBox.addItem(new DumpOfServiceInfo("", -1));
        mDumpOfServiceBomboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED) return;
                DumpOfServiceInfo dumpOfServiceInfo = (DumpOfServiceInfo) e.getItem();
                m_tbLogTable.showRow(dumpOfServiceInfo.row, true);
            }
        });

        jpMain.add(mDumpOfServiceBomboBox, BorderLayout.CENTER);
        return jpMain;
    }

    Component getCheckPanel() {
        m_chkVerbose = new JCheckBox();
        m_chkDebug = new JCheckBox();
        m_chkInfo = new JCheckBox();
        m_chkWarn = new JCheckBox();
        m_chkError = new JCheckBox();
        m_chkFatal = new JCheckBox();

        m_chkClmBookmark = new JCheckBox();
        m_chkClmLine = new JCheckBox();
        m_chkClmDate = new JCheckBox();
        m_chkClmTime = new JCheckBox();
        m_chkClmLogLV = new JCheckBox();
        m_chkClmPid = new JCheckBox();
        m_chkClmThread = new JCheckBox();
        m_chkClmTag = new JCheckBox();
        m_chkClmMessage = new JCheckBox();

        JPanel jpMain = new JPanel();
        jpMain.setLayout(new BoxLayout(jpMain, BoxLayout.PAGE_AXIS));

        JPanel jpLogFilter = new JPanel();
        jpLogFilter.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        jpLogFilter.setBorder(BorderFactory.createTitledBorder("Log filter"));
        m_chkVerbose.setText("Verbose");
        m_chkVerbose.setSelected(true);
        m_chkDebug.setText("Debug");
        m_chkDebug.setSelected(true);
        m_chkInfo.setText("Info");
        m_chkInfo.setSelected(true);
        m_chkWarn.setText("Warn");
        m_chkWarn.setSelected(true);
        m_chkError.setText("Error");
        m_chkError.setSelected(true);
        m_chkFatal.setText("Fatal");
        m_chkFatal.setSelected(true);
        jpLogFilter.add(m_chkVerbose);
        jpLogFilter.add(m_chkDebug);
        jpLogFilter.add(m_chkInfo);
        jpLogFilter.add(m_chkWarn);
        jpLogFilter.add(m_chkError);
        jpLogFilter.add(m_chkFatal);

        JPanel jpShowColumn = new JPanel();
        jpShowColumn.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        jpShowColumn.setBorder(BorderFactory.createTitledBorder("Show column"));
        m_chkClmBookmark.setText("Mark");
        m_chkClmBookmark.setToolTipText("Bookmark");
        m_chkClmLine.setText("Line");
        m_chkClmLine.setSelected(true);
        m_chkClmDate.setText("Date");
        m_chkClmDate.setSelected(true);
        m_chkClmTime.setText("Time");
        m_chkClmTime.setSelected(true);
        m_chkClmLogLV.setText("LogLV");
        m_chkClmLogLV.setSelected(true);
        m_chkClmPid.setText("Pid");
        m_chkClmPid.setSelected(true);
        m_chkClmThread.setText("Thread");
        m_chkClmThread.setSelected(true);
        m_chkClmTag.setText("Tag");
        m_chkClmTag.setSelected(true);
        m_chkClmMessage.setText("Msg");
        m_chkClmMessage.setSelected(true);
        jpShowColumn.add(m_chkClmBookmark);
        jpShowColumn.add(m_chkClmLine);
        jpShowColumn.add(m_chkClmDate);
        jpShowColumn.add(m_chkClmTime);
        jpShowColumn.add(m_chkClmLogLV);
        jpShowColumn.add(m_chkClmPid);
        jpShowColumn.add(m_chkClmThread);
        jpShowColumn.add(m_chkClmTag);
        jpShowColumn.add(m_chkClmMessage);

        jpMain.add(jpLogFilter);
        jpMain.add(jpShowColumn);
        jpMain.add(getHighlightPanel());
        jpMain.add(getSearchPanel());
        jpMain.add(getDumpOfServicePanel());
        return jpMain;
    }

    JPanel getToolPanel() {
        JPanel jpTool = new JPanel();
        jpTool.setLayout(new GridLayout(5, 1, 2, 2));
        jpTool.setBorder(BorderFactory.createTitledBorder("Tools"));

        JButton runOGButton = new JButton("OpenGrok");
        runOGButton.setMargin(new Insets(0, 0, 0, 0));
        runOGButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.openWebpage("http://mobilerndhub.sec.samsung.net/portal/og/index/");
            }
        });

        JButton runCLIndexButton = new JButton("CL Index");
        runCLIndexButton.setMargin(new Insets(0, 0, 0, 0));
        runCLIndexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.openWebpage("http://mobilerndhub.sec.samsung.net/portal/clindex/search/");
            }
        });

        JButton plmButton = new JButton("PLM");
        plmButton.setMargin(new Insets(0, 0, 0, 0));
        plmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.openWebpage("http://splm.sec.samsung.net/portal/com/shared/main.do");
            }
        });

        JButton diffButton = new JButton("Diff");
        diffButton.setMargin(new Insets(0, 0, 0, 0));
        diffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Utils.runCmd(new String[]{DIFF_PROGRAM_PATH});
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        jpTool.add(runOGButton);
        jpTool.add(runCLIndexButton);
        jpTool.add(plmButton);
        jpTool.add(diffButton);
        return jpTool;
    }

    Component getOptionFilter() {
        JPanel optionFilter = new JPanel(new BorderLayout());

        optionFilter.add(getDevicePanel(), BorderLayout.WEST);
        optionFilter.add(getFilterPanel(), BorderLayout.CENTER);

        JPanel aPanel = new JPanel(new BorderLayout());
        aPanel.add(getCheckPanel(), BorderLayout.WEST);
        aPanel.add(getToolPanel(), BorderLayout.EAST);
        optionFilter.add(aPanel, BorderLayout.EAST);

        return optionFilter;
    }

    Component getOptionMenu() {
        JPanel optionMenu = new JPanel(new BorderLayout());
        JPanel optionWest = new JPanel();

//        JLabel jlFontType = new JLabel("Font Type : ");
//        m_jcFontType = new JComboBox();
//        String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment()
//                .getAvailableFontFamilyNames();
//        m_jcFontType.addItem("Dialog");
//        for (int i = 0; i < fonts.length; i++) {
//            m_jcFontType.addItem(fonts[i]);
//        }
//        m_jcFontType.addActionListener(m_alButtonListener);

        JLabel jlFont = new JLabel("Font Size : ");
        m_tfFontSize = new JTextField(2);
        m_tfFontSize.setHorizontalAlignment(SwingConstants.RIGHT);
        m_tfFontSize.setText("12");

        m_btnSetFont = new JButton("OK");
        m_btnSetFont.setMargin(new Insets(0, 0, 0, 0));
        m_btnSetFont.addActionListener(m_alButtonListener);

        JLabel jlEncode = new JLabel("Text Encode : ");
        m_comboEncode = new JComboBox<>();
        m_comboEncode.addItem("UTF-8");
        m_comboEncode.addItem("Local");

        JLabel jlGoto = new JLabel("Goto : ");
        final JTextField tfGoto = new JTextField(6);
        tfGoto.setHorizontalAlignment(SwingConstants.RIGHT);
        tfGoto.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                try {
                    int nIndex = Integer.parseInt(tfGoto.getText()) - 1;
                    getLogTable().showRow(nIndex, false);
                } catch (Exception err) {
                }
            }
        });

        JLabel jlProcessCmd = new JLabel("Cmd : ");
        m_comboCmd = new JComboBox<>();
        m_comboCmd.setPreferredSize(new Dimension(180, 25));
        // m_comboCmd.setMaximumSize( m_comboCmd.getPreferredSize() );
        // m_comboCmd.setSize( 20000, m_comboCmd.getHeight() );
        // m_comboCmd.addItem(ANDROID_THREAD_CMD);
        // m_comboCmd.addItem(ANDROID_DEFAULT_CMD);
        // m_comboCmd.addItem(ANDROID_RADIO_CMD);
        // m_comboCmd.addItem(ANDROID_EVENT_CMD);
        // m_comboCmd.addItem(ANDROID_CUSTOM_CMD);
        // m_comboCmd.addItemListener(new ItemListener()
        // {
        // public void itemStateChanged(ItemEvent e)
        // {
        // if(e.getStateChange() != ItemEvent.SELECTED) return;
        //
        // if (e.getItem().equals(ANDROID_CUSTOM_CMD)) {
        // m_comboCmd.setEditable(true);
        // } else {
        // m_comboCmd.setEditable(false);
        // }
        // // setProcessCmd(m_comboDeviceCmd.getSelectedIndex(),
        // m_strSelectedDevice);
        // }
        // });

        m_btnClear = new JButton("Clear");
        m_btnClear.setMargin(new Insets(0, 0, 0, 0));
        m_btnClear.setEnabled(false);
        m_btnRun = new JButton("Run");
        m_btnRun.setMargin(new Insets(0, 0, 0, 0));

        m_tbtnPause = new JToggleButton("Pause");
        m_tbtnPause.setMargin(new Insets(0, 0, 0, 0));
        m_tbtnPause.setEnabled(false);
        m_btnStop = new JButton("Stop");
        m_btnStop.setMargin(new Insets(0, 0, 0, 0));
        m_btnStop.setEnabled(false);
        m_btnRun.addActionListener(m_alButtonListener);
        m_btnStop.addActionListener(m_alButtonListener);
        m_btnClear.addActionListener(m_alButtonListener);
        m_tbtnPause.addActionListener(m_alButtonListener);

        mSyncScrollCheckBox = new JCheckBox("sync scroll");
        mSyncScrollCheckBox.setEnabled(false);
        mSyncScrollCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                JCheckBox check = (JCheckBox) e.getSource();
                enableSyncScroll(check.isSelected());
            }
        });

        mSyncSelectedCheckBox = new JCheckBox("sync selected");
        mSyncSelectedCheckBox.setEnabled(false);
        mSyncSelectedCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                JCheckBox check = (JCheckBox) e.getSource();
                enableSyncSelected(check.isSelected());
            }
        });

        JButton preHistoryButton = new JButton("<");
        preHistoryButton.setMargin(new Insets(0, 5, 0, 5));
        preHistoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_tbLogTable.historyBack();
            }
        });

        JButton nextHistoryButton = new JButton(">");
        nextHistoryButton.setMargin(new Insets(0, 5, 0, 5));
        nextHistoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_tbLogTable.historyForward();
            }
        });

        optionWest.add(mSyncScrollCheckBox);
        optionWest.add(mSyncSelectedCheckBox);
        optionWest.add(jlFont);
        optionWest.add(m_tfFontSize);
        optionWest.add(m_btnSetFont);
        optionWest.add(jlEncode);
        optionWest.add(m_comboEncode);
        optionWest.add(jlGoto);
        optionWest.add(tfGoto);
        optionWest.add(jlProcessCmd);
        optionWest.add(m_comboCmd);
        optionWest.add(m_btnClear);
        optionWest.add(m_btnRun);
        optionWest.add(m_tbtnPause);
        optionWest.add(m_btnStop);

        optionWest.add(preHistoryButton);
        optionWest.add(nextHistoryButton);

        optionMenu.add(optionWest, BorderLayout.WEST);
        return optionMenu;
    }

    Component getOptionPanel() {
        JPanel optionMain = new JPanel(new BorderLayout());

        optionMain.add(getOptionFilter(), BorderLayout.CENTER);
        optionMain.add(getOptionMenu(), BorderLayout.SOUTH);

        return optionMain;
    }

    Component getStatusPanel() {
        JPanel mainP = new JPanel(new BorderLayout());

        JPanel tfPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        Border border = BorderFactory.createCompoundBorder(new EmptyBorder(0, 4, 0, 0), new EtchedBorder());

        m_tfStatus = new JLabel("ready");
        m_tfStatus.setBorder(border);
        m_tfDiffPort = new JLabel("not bind");
        m_tfDiffPort.setBorder(border);
        m_tfDiffState = new JLabel("disconnected");
        m_tfDiffState.setBorder(border);


        tfPanel.add(m_tfDiffState, constraints);
        tfPanel.add(m_tfDiffPort, constraints);
        tfPanel.add(m_tfStatus, constraints);

        mainP.add(tfPanel, BorderLayout.EAST);

        return mainP;
    }

    Component getTabPanel() {
        m_tpTab = new JTabbedPane();
        m_tmLogTableModel = new LogFilterTableModel();
        m_tmLogTableModel.setData(m_arLogInfoAll);
        m_tbLogTable = new LogTable(m_tmLogTableModel, this);
        m_iLogParser = new LogCatParser();
        getLogTable().setLogParser(m_iLogParser);

        m_scrollVBar = new JScrollPane(getLogTable());

        m_tpTab.addTab("Log", m_scrollVBar);

        return m_scrollVBar;
    }

    void initValue() {
        m_bPauseADB = false;
        FILE_LOCK = new Object();
        FILTER_LOCK = new Object();
        m_nChangedFilter = STATUS_READY;
        m_nFilterLogLV = LogInfo.LOG_LV_ALL;

        m_arLogInfoAll = new ArrayList<LogInfo>();
        m_arLogInfoFiltered = new ArrayList<LogInfo>();
        m_hmBookmarkAll = new HashMap<Integer, Integer>();
        m_hmBookmarkFiltered = new HashMap<Integer, Integer>();
        m_hmErrorAll = new ConcurrentHashMap<Integer, Integer>();
        m_hmErrorFiltered = new ConcurrentHashMap<Integer, Integer>();

        File confDir = new File(CONFIG_BASE_DIR);
        if (!confDir.exists()) {
            confDir.mkdirs();
            T.d("create conf directory: " + confDir.getAbsolutePath());
        }
        File outputDir = new File(OUTPUT_LOG_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            T.d("create log directory: " + outputDir.getAbsolutePath());
        }
        m_strLogFileName = makeFilename();
        // m_strProcessCmd = ANDROID_DEFAULT_CMD + m_strLogFileName;
    }

    void parseFile(final File file) {
        if (file == null) {
            T.e("file == null");
            return;
        }

        setTitle(file.getPath());
        new Thread(new Runnable() {
            public void run() {
                FileInputStream fstream = null;
                DataInputStream in = null;
                BufferedReader br = null;
                int nIndex = 1;

                try {
                    fstream = new FileInputStream(file);
                    in = new DataInputStream(fstream);
                    if (m_comboEncode.getSelectedItem().equals("UTF-8"))
                        br = new BufferedReader(new InputStreamReader(in,
                                "UTF-8"));
                    else
                        br = new BufferedReader(new InputStreamReader(in));

                    String strLine;

                    setStatus("Parsing");
                    clearData();
                    getLogTable().clearSelection();

                    boolean inMainLog = false;
                    boolean inDumpServiceLog = false;
                    while ((strLine = br.readLine()) != null) {
                        if (!inMainLog && strLine.endsWith("beginning of main")) {
                            inMainLog = true;
                            continue;
                        } else if (inMainLog && strLine.startsWith("[logcat:")) {
                            inMainLog = false;
                            continue;
                        }

                        if (!inDumpServiceLog && strLine.startsWith("DUMP OF SERVICE")) {
                            inDumpServiceLog = true;

                            LogInfo dumpHeader = new LogInfo();
                            dumpHeader.m_strMessage = "===================================";
                            dumpHeader.m_strLogLV = "D";
                            dumpHeader.m_TextColor = m_iLogParser.getColor(dumpHeader);
                            dumpHeader.m_intLine = nIndex++;
                            addLogInfo(dumpHeader);

                            LogInfo dumpName = new LogInfo();
                            dumpName.m_strMessage = strLine;
                            dumpName.m_strLogLV = "D";
                            dumpName.m_TextColor = m_iLogParser.getColor(dumpName);
                            dumpName.m_intLine = nIndex++;
                            addLogInfo(dumpName);

                            DumpOfServiceInfo dumpOfServiceInfo = new DumpOfServiceInfo(strLine, nIndex - 2);
                            mDumpOfServiceBomboBox.addItem(dumpOfServiceInfo);
                            continue;
                        } else if (inDumpServiceLog && strLine.startsWith("-------------------------------------------------------------------------------")) {
                            inDumpServiceLog = false;
                            continue;
                        }

                        if (inDumpServiceLog && !"".equals(strLine.trim())) {
                            LogInfo logInfo = new LogInfo();
                            logInfo.m_strMessage = strLine;
                            logInfo.m_intLine = nIndex++;
                            addLogInfo(logInfo);
                        } else if (inMainLog && !"".equals(strLine.trim())) {
                            LogInfo logInfo = m_iLogParser.parseLog(strLine);
                            logInfo.m_intLine = nIndex++;
                            addLogInfo(logInfo);
                        }
                    }
                    runFilter();
                    setStatus("Parse complete");
                } catch (Exception ioe) {
                    T.e(ioe);
                }
                try {
                    if (br != null)
                        br.close();
                    if (in != null)
                        in.close();
                    if (fstream != null)
                        fstream.close();
                } catch (Exception e) {
                    T.e(e);
                }
            }
        }).start();
    }

    void pauseProcess() {
        if (m_tbtnPause.isSelected()) {
            m_bPauseADB = true;
            m_tbtnPause.setText("Resume");
        } else {
            m_bPauseADB = false;
            m_tbtnPause.setText("Pause");
        }
    }

    void setBookmark(int nLine, String strBookmark) {
        LogInfo logInfo = m_arLogInfoAll.get(nLine);
        logInfo.m_strBookmark = strBookmark;
        m_arLogInfoAll.set(nLine, logInfo);
    }

    void setDeviceList() {
        m_strSelectedDevice = "";

        DefaultListModel<TargetDevice> listModel = (DefaultListModel<TargetDevice>) m_lDeviceList
                .getModel();
        try {
            listModel.clear();
            String s;
            String strCommand = DEVICES_CMD[m_comboDeviceCmd.getSelectedIndex()];
            if (m_comboDeviceCmd.getSelectedIndex() == DEVICES_CUSTOM)
                strCommand = (String) m_comboDeviceCmd.getSelectedItem();
            Process oProcess = Runtime.getRuntime().exec(strCommand);

            //
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(
                    oProcess.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(
                    oProcess.getErrorStream()));

            //
            while ((s = stdOut.readLine()) != null) {
                if (!s.equals("List of devices attached ") && s.length() != 0) {
                    listModel.addElement(new TargetDevice(s));
                }
            }
            while ((s = stdError.readLine()) != null) {
                if (s.length() != 0)
                    listModel.addElement(new TargetDevice(s));
            }
        } catch (Exception e) {
            T.e("e = " + e);
            listModel.addElement(new TargetDevice(e.getMessage()));
        }
    }

    public void setSearchFocus() {
//        m_tfFindWord.requestFocus();
        m_tfSearch.requestFocus();
    }

    public void searchKeyword(String keyword) {
        m_tfSearch.setText(keyword);
    }

    void setDnDListener() {

        new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,
                new DropTargetListener() {
                    public void dropActionChanged(DropTargetDragEvent dtde) {
                    }

                    public void dragOver(DropTargetDragEvent dtde) {
                    }

                    public void dragExit(DropTargetEvent dte) {
                    }

                    public void dragEnter(DropTargetDragEvent event) {
                    }

                    public void drop(DropTargetDropEvent event) {
                        try {
                            event.acceptDrop(DnDConstants.ACTION_COPY);
                            Transferable t = event.getTransferable();
                            List<?> list = (List<?>) (t
                                    .getTransferData(DataFlavor.javaFileListFlavor));
                            Iterator<?> i = list.iterator();
                            if (i.hasNext()) {
                                File file = (File) i.next();
                                setTitle(file.getPath());

                                stopProcess();
                                parseFile(file);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    void setLogLV(int nLogLV, boolean bChecked) {
        if (bChecked)
            m_nFilterLogLV |= nLogLV;
        else
            m_nFilterLogLV &= ~nLogLV;
        m_nChangedFilter = STATUS_CHANGE;
        runFilter();
    }

    void useFilter(JCheckBox checkBox) {
        if (checkBox.equals(m_chkEnableFind))
            getLogTable().setFilterFind(checkBox.isSelected() ? m_tfFindWord
                    .getText() : "");
        if (checkBox.equals(m_chkEnableRemove))
            getLogTable().SetFilterRemove(checkBox.isSelected() ? m_tfRemoveWord
                    .getText() : "");
        if (checkBox.equals(m_chkEnableShowPid))
            getLogTable().SetFilterShowPid(checkBox.isSelected() ? m_tfShowPid
                    .getText() : "");
        if (checkBox.equals(m_chkEnableShowTid))
            getLogTable().SetFilterShowTid(checkBox.isSelected() ? m_tfShowTid
                    .getText() : "");
        if (checkBox.equals(m_chkEnableShowTag))
            getLogTable().SetFilterShowTag(checkBox.isSelected() ? m_tfShowTag
                    .getText() : "");
        if (checkBox.equals(m_chkEnableRemoveTag))
            getLogTable()
                    .SetFilterRemoveTag(checkBox.isSelected() ? m_tfRemoveTag
                            .getText() : "");
        if (checkBox.equals(m_chkEnableBookmarkTag))
            getLogTable().SetFilterBookmarkTag(checkBox.isSelected() ? m_tfBookmarkTag
                    .getText() : "");
        if (checkBox.equals(m_chkEnableHighlight))
            getLogTable().SetHighlight(checkBox.isSelected() ? m_tfHighlight
                    .getText() : "");
        if (checkBox.equals(m_chkEnableTimeTag)) {
            if (checkBox.isSelected()) {
                getLogTable().SetFilterFromTime(m_tfFromTimeTag.getText());
                getLogTable().SetFilterToTime(m_tfToTimeTag.getText());
            } else {
                getLogTable().SetFilterFromTime("");
                getLogTable().SetFilterToTime("");
            }
        }
        m_nChangedFilter = STATUS_CHANGE;
        runFilter();
    }

    void setProcessBtn(boolean bStart) {
        if (bStart) {
            m_btnRun.setEnabled(false);
            m_btnStop.setEnabled(true);
            m_btnClear.setEnabled(true);
            m_tbtnPause.setEnabled(true);
        } else {
            m_btnRun.setEnabled(true);
            m_btnStop.setEnabled(false);
            m_btnClear.setEnabled(false);
            m_tbtnPause.setEnabled(false);
            m_tbtnPause.setSelected(false);
            m_tbtnPause.setText("Pause");
        }
    }

    String getProcessCmd() {
        if (m_lDeviceList.getSelectedIndex() < 0
                || m_strSelectedDevice == null
                || m_strSelectedDevice.length() == 0)
            return ANDROID_DEFAULT_CMD_FIRST + m_comboCmd.getSelectedItem();
        else
            return ANDROID_SELECTED_CMD_FIRST
                    + m_strSelectedDevice
                    + " "
                    + m_comboCmd.getSelectedItem();
    }

    void setStatus(String strText) {
        m_tfStatus.setText(strText);
    }

    public void setTitle(String strTitle) {
        super.setTitle(strTitle);
    }

    void stopProcess() {
        setProcessBtn(false);
        if (m_Process != null)
            m_Process.destroy();
        if (m_thProcess != null)
            m_thProcess.interrupt();
        if (m_thWatchFile != null)
            m_thWatchFile.interrupt();
        m_Process = null;
        m_thProcess = null;
        m_thWatchFile = null;
        m_bPauseADB = false;
    }

    void startFileParse() {
        m_thWatchFile = new Thread(new Runnable() {
            public void run() {
                FileInputStream fstream = null;
                DataInputStream in = null;
                BufferedReader br = null;

                try {
                    fstream = new FileInputStream(m_strLogFileName);
                    in = new DataInputStream(fstream);
                    if (m_comboEncode.getSelectedItem().equals("UTF-8"))
                        br = new BufferedReader(new InputStreamReader(in,
                                "UTF-8"));
                    else
                        br = new BufferedReader(new InputStreamReader(in));

                    String strLine;

                    setTitle(m_strLogFileName);

                    m_arLogInfoAll.clear();

                    boolean bEndLine;
                    int nSelectedIndex;
                    int nAddCount;
                    int nPreRowCount = 0;
                    int nEndLine;

                    while (true) {
                        Thread.sleep(50);

                        if (m_nChangedFilter == STATUS_CHANGE
                                || m_nChangedFilter == STATUS_PARSING)
                            continue;
                        if (m_bPauseADB)
                            continue;

                        bEndLine = false;
                        nSelectedIndex = getLogTable().getSelectedRow();
                        nPreRowCount = getLogTable().getRowCount();
                        nAddCount = 0;

                        if (nSelectedIndex == -1
                                || nSelectedIndex == getLogTable().getRowCount() - 1)
                            bEndLine = true;

                        synchronized (FILE_LOCK) {
                            int nLine = m_arLogInfoAll.size() + 1;
                            while (!m_bPauseADB
                                    && (strLine = br.readLine()) != null) {
                                if (strLine != null
                                        && !"".equals(strLine.trim())) {
                                    LogInfo logInfo = m_iLogParser
                                            .parseLog(strLine);
                                    logInfo.m_intLine = nLine++;
                                    addLogInfo(logInfo);
                                    nAddCount++;
                                }
                            }
                        }
                        if (nAddCount == 0)
                            continue;

                        synchronized (FILTER_LOCK) {
                            if (m_bUserFilter == false) {
                                m_tmLogTableModel.setData(m_arLogInfoAll);
                                m_ipIndicator.setData(m_arLogInfoAll,
                                        m_hmBookmarkAll, m_hmErrorAll);
                            } else {
                                m_tmLogTableModel.setData(m_arLogInfoFiltered);
                                m_ipIndicator
                                        .setData(m_arLogInfoFiltered,
                                                m_hmBookmarkFiltered,
                                                m_hmErrorFiltered);
                            }

                            nEndLine = m_tmLogTableModel.getRowCount();
                            if (nPreRowCount != nEndLine) {
                                if (bEndLine)
                                    updateTable(nEndLine - 1, true);
                                else
                                    updateTable(nSelectedIndex, false);
                            }
                        }
                    }
                } catch (Exception e) {
                    T.e(e);
                }
                try {
                    if (br != null)
                        br.close();
                    if (in != null)
                        in.close();
                    if (fstream != null)
                        fstream.close();
                } catch (Exception e) {
                    T.e(e);
                }
                System.out.println("End m_thWatchFile thread");
            }
        });
        m_thWatchFile.start();
    }

    void runFilter() {
        checkUseFilter();
        while (m_nChangedFilter == STATUS_PARSING)
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        synchronized (FILTER_LOCK) {
            FILTER_LOCK.notify();
        }
    }

    void startFilterParse() {
        m_thFilterParse = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        synchronized (FILTER_LOCK) {
                            m_nChangedFilter = STATUS_READY;
                            FILTER_LOCK.wait();

                            m_nChangedFilter = STATUS_PARSING;

                            m_arLogInfoFiltered.clear();
                            m_hmBookmarkFiltered.clear();
                            m_hmErrorFiltered.clear();
                            getLogTable().clearSelection();

                            if (m_bUserFilter == false) {
                                m_tmLogTableModel.setData(m_arLogInfoAll);
                                m_ipIndicator.setData(m_arLogInfoAll,
                                        m_hmBookmarkAll, m_hmErrorAll);
                                LogInfo latestInfo = getLogTable().getLatestSelectedLogInfo();
                                if (latestInfo != null) {
                                    int i = 0;
                                    for (LogInfo info : m_arLogInfoAll) {
                                        i++;
                                        if (info.m_intLine >= latestInfo.m_intLine) {
                                            break;
                                        }
                                    }
                                    updateTable(i - 1, true);
                                } else {
                                    updateTable(m_arLogInfoFiltered.size() - 1, true);
                                }
                                m_nChangedFilter = STATUS_READY;
                                continue;
                            }
                            m_tmLogTableModel.setData(m_arLogInfoFiltered);
                            m_ipIndicator.setData(m_arLogInfoFiltered,
                                    m_hmBookmarkFiltered, m_hmErrorFiltered);
                            // updateTable(-1);
                            setStatus("Parsing");

                            int nRowCount = m_arLogInfoAll.size();
                            LogInfo logInfo;
                            boolean bAddFilteredArray;

                            for (int nIndex = 0; nIndex < nRowCount; nIndex++) {
                                if (nIndex % 10000 == 0)
                                    Thread.sleep(1);
                                if (m_nChangedFilter == STATUS_CHANGE) {
                                    // com.bt.tool.T.d("m_nChangedFilter == STATUS_CHANGE");
                                    break;
                                }
                                logInfo = m_arLogInfoAll.get(nIndex);

                                if (m_ipIndicator.m_chBookmark.isSelected()
                                        || m_ipIndicator.m_chError.isSelected()) {
                                    bAddFilteredArray = false;
                                    if (logInfo.m_bMarked
                                            && m_ipIndicator.m_chBookmark
                                            .isSelected()) {
                                        bAddFilteredArray = true;
                                        m_hmBookmarkFiltered.put(logInfo.m_intLine - 1,
                                                m_arLogInfoFiltered.size());
                                        if (logInfo.m_strLogLV.equals("E")
                                                || logInfo.m_strLogLV
                                                .equals("ERROR"))
                                            m_hmErrorFiltered.put(logInfo.m_intLine - 1,
                                                    m_arLogInfoFiltered.size());
                                    }
                                    if ((logInfo.m_strLogLV.equals("E") || logInfo.m_strLogLV
                                            .equals("ERROR"))
                                            && m_ipIndicator.m_chError
                                            .isSelected()) {
                                        bAddFilteredArray = true;
                                        m_hmErrorFiltered.put(logInfo.m_intLine - 1,
                                                m_arLogInfoFiltered.size());
                                        if (logInfo.m_bMarked)
                                            m_hmBookmarkFiltered.put(logInfo.m_intLine - 1,
                                                    m_arLogInfoFiltered.size());
                                    }

                                    if (bAddFilteredArray)
                                        m_arLogInfoFiltered.add(logInfo);
                                } else if (checkLogLVFilter(logInfo)
                                        && checkPidFilter(logInfo)
                                        && checkTidFilter(logInfo)
                                        && checkShowTagFilter(logInfo)
                                        && checkRemoveTagFilter(logInfo)
                                        && checkFindFilter(logInfo)
                                        && checkRemoveFilter(logInfo)
                                        && checkFromTimeFilter(logInfo)
                                        && checkToTimeFilter(logInfo)
                                        && checkBookmarkFilter(logInfo)) {
                                    m_arLogInfoFiltered.add(logInfo);
                                    if (logInfo.m_bMarked)
                                        m_hmBookmarkFiltered.put(logInfo.m_intLine - 1,
                                                m_arLogInfoFiltered.size());
                                    if (logInfo.m_strLogLV.equals("E")
                                            || logInfo.m_strLogLV
                                            .equals("ERROR"))
                                        m_hmErrorFiltered.put(logInfo.m_intLine - 1,
                                                m_arLogInfoFiltered.size());
                                }
                            }
                            if (m_nChangedFilter == STATUS_PARSING) {
                                m_nChangedFilter = STATUS_READY;
                                m_tmLogTableModel.setData(m_arLogInfoFiltered);
                                m_ipIndicator
                                        .setData(m_arLogInfoFiltered,
                                                m_hmBookmarkFiltered,
                                                m_hmErrorFiltered);
                                LogInfo latestInfo = getLogTable().getLatestSelectedLogInfo();
                                if (latestInfo != null) {
                                    int i = 0;
                                    for (LogInfo info : m_arLogInfoFiltered) {
                                        i++;
                                        if (info.m_intLine >= latestInfo.m_intLine) {
                                            break;
                                        }
                                    }
                                    updateTable(i - 1, true);
                                } else {
                                    updateTable(m_arLogInfoFiltered.size() - 1, true);
                                }
                                setStatus("Complete");
                            }
                        }
                    }
                } catch (Exception e) {
                    T.e(e);
                }
                System.out.println("End m_thFilterParse thread");
            }
        });
        m_thFilterParse.start();
    }

    void startProcess() {
        clearData();
        getLogTable().clearSelection();
        m_thProcess = new Thread(new Runnable() {
            public void run() {
                try {
                    String s;
                    m_Process = null;

                    T.d("getProcessCmd() = " + getProcessCmd());
                    m_Process = Runtime.getRuntime().exec(getProcessCmd());
                    BufferedReader stdOut = new BufferedReader(
                            new InputStreamReader(m_Process.getInputStream(),
                                    "UTF-8"));

                    // BufferedWriter fileOut = new BufferedWriter(new
                    // FileWriter(m_strLogFileName));
                    Writer fileOut = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(m_strLogFileName), "UTF-8"));

                    startFileParse();

                    while ((s = stdOut.readLine()) != null) {
                        if (s != null && !"".equals(s.trim())) {
                            synchronized (FILE_LOCK) {
                                fileOut.write(s);
                                fileOut.write("\r\n");
                                // fileOut.newLine();
                                fileOut.flush();
                            }
                        }
                    }
                    fileOut.close();
                    // com.bt.tool.T.d("Exit Code: " + m_Process.exitValue());
                } catch (Exception e) {
                    T.e("e = " + e);
                }
                stopProcess();
            }
        });
        m_thProcess.start();
        setProcessBtn(true);
    }

    boolean checkLogLVFilter(LogInfo logInfo) {
        if (m_nFilterLogLV == LogInfo.LOG_LV_ALL)
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_VERBOSE) != 0
                && (logInfo.m_strLogLV.equals("V") || logInfo.m_strLogLV
                .equals("VERBOSE")))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_DEBUG) != 0
                && (logInfo.m_strLogLV.equals("D") || logInfo.m_strLogLV
                .equals("DEBUG")))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_INFO) != 0
                && (logInfo.m_strLogLV.equals("I") || logInfo.m_strLogLV
                .equals("INFO")))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_WARN) != 0
                && (logInfo.m_strLogLV.equals("W") || logInfo.m_strLogLV
                .equals("WARN")))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_ERROR) != 0
                && (logInfo.m_strLogLV.equals("E") || logInfo.m_strLogLV
                .equals("ERROR")))
            return true;
        if ((m_nFilterLogLV & LogInfo.LOG_LV_FATAL) != 0
                && (logInfo.m_strLogLV.equals("F") || logInfo.m_strLogLV
                .equals("FATAL")))
            return true;

        return false;
    }

    boolean checkPidFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterShowPid().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterShowPid(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.m_strPid.toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return true;
        }

        return false;
    }

    boolean checkTidFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterShowTid().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterShowTid(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.m_strThread.toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return true;
        }

        return false;
    }

    boolean checkFindFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterFind().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(getLogTable().GetFilterFind(),
                "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.m_strMessage.toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return true;
        }

        return false;
    }

    boolean checkRemoveFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterRemove().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterRemove(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.m_strMessage.toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return false;
        }

        return true;
    }

    boolean checkShowTagFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterShowTag().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterShowTag(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.m_strTag.toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return true;
        }

        return false;
    }

    boolean checkRemoveTagFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterRemoveTag().length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterRemoveTag(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.m_strTag.toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return false;
        }

        return true;
    }

    boolean checkBookmarkFilter(LogInfo logInfo) {
        if (getLogTable().GetFilterBookmarkTag().length() <= 0 && logInfo.m_strBookmark.length() <= 0)
            return true;

        StringTokenizer stk = new StringTokenizer(
                getLogTable().GetFilterBookmarkTag(), "|", false);

        while (stk.hasMoreElements()) {
            if (logInfo.m_strBookmark.toLowerCase().contains(
                    stk.nextToken().toLowerCase()))
                return true;
        }

        return false;
    }

    boolean checkToTimeFilter(LogInfo logInfo) {
        if (logInfo.m_timestamp == -1)
            return false;
        if (getLogTable().GetFilterToTime() == -1) {
            return true;
        }
        return logInfo.m_timestamp <= getLogTable().GetFilterToTime();
    }

    boolean checkFromTimeFilter(LogInfo logInfo) {
        if (logInfo.m_timestamp == -1)
            return false;
        if (getLogTable().GetFilterFromTime() == -1) {
            return true;
        }
        return logInfo.m_timestamp >= getLogTable().GetFilterFromTime();
    }

    boolean checkUseFilter() {
        if (!m_ipIndicator.m_chBookmark.isSelected()
                && !m_ipIndicator.m_chError.isSelected()
                && checkLogLVFilter(new LogInfo())
                && (getLogTable().GetFilterShowPid().length() == 0 || !m_chkEnableShowPid.isSelected())
                && (getLogTable().GetFilterShowTid().length() == 0 || !m_chkEnableShowTid.isSelected())
                && (getLogTable().GetFilterShowTag().length() == 0 || !m_chkEnableShowTag.isSelected())
                && (getLogTable().GetFilterRemoveTag().length() == 0 || !m_chkEnableRemoveTag.isSelected())
                && (getLogTable().GetFilterBookmarkTag().length() == 0 || !m_chkEnableBookmarkTag.isSelected())
                && ((getLogTable().GetFilterFromTime() == -1l && getLogTable().GetFilterToTime() == -1l)
                || !m_chkEnableTimeTag.isSelected())
                && (getLogTable().GetFilterFind().length() == 0 || !m_chkEnableFind.isSelected())
                && (getLogTable().GetFilterRemove().length() == 0 || !m_chkEnableRemove.isSelected())) {
            m_bUserFilter = false;
        } else
            m_bUserFilter = true;
        return m_bUserFilter;
    }

    ActionListener m_alButtonListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(m_btnDevice))
                setDeviceList();
            else if (e.getSource().equals(m_btnSetFont)) {
                getLogTable().setFontSize(Integer.parseInt(m_tfFontSize
                        .getText()));
                updateTable(-1, false);
            } else if (e.getSource().equals(m_btnRun)) {
                startProcess();
            } else if (e.getSource().equals(m_btnStop)) {
                stopProcess();
            } else if (e.getSource().equals(m_btnClear)) {
                boolean bBackup = m_bPauseADB;
                m_bPauseADB = true;
                clearData();
                updateTable(-1, false);
                m_bPauseADB = bBackup;
            } else if (e.getSource().equals(m_tbtnPause)) {
                pauseProcess();
            }
//            else if (e.getSource().equals(m_jcFontType)) {
//                T.d("font = " + m_tbLogTable.getFont());
//
//                m_tbLogTable.setFont(new Font((String) m_jcFontType
//                        .getSelectedItem(), Font.PLAIN, 12));
//                m_tbLogTable.setFontSize(Integer.parseInt(m_tfFontSize
//                        .getText()));
//            }
        }
    };

    public void notiEvent(EventParam param) {
        switch (param.nEventId) {
            case EVENT_CLICK_BOOKMARK:
            case EVENT_CLICK_ERROR:
                m_nChangedFilter = STATUS_CHANGE;
                runFilter();
                break;
            case EVENT_CHANGE_FILTER_SHOW_TAG:
                m_tfShowTag.setText(getLogTable().GetFilterShowTag());
                break;
            case EVENT_CHANGE_FILTER_REMOVE_TAG:
                m_tfRemoveTag.setText(getLogTable().GetFilterRemoveTag());
                break;
            case EVENT_CHANGE_FILTER_FROM_TIME: {
                String fromTimeStr = (String) param.param1;
                m_tfFromTimeTag.setText(fromTimeStr);
            }
            break;
            case EVENT_CHANGE_FILTER_TO_TIME: {
                String toTimeStr = (String) param.param1;
                m_tfToTimeTag.setText(toTimeStr);
            }
            break;
        }
    }

    void updateTable(int nRow, boolean bMove) {
        // System.out.println("updateTable nRow:" + nRow + " | " + bMove);
        m_tmLogTableModel.fireTableRowsUpdated(0,
                m_tmLogTableModel.getRowCount() - 1);
        m_scrollVBar.validate();
        // if(nRow >= 0)
        // m_tbLogTable.changeSelection(nRow, 0, false, false);
        getLogTable().invalidate();
        getLogTable().repaint();
        if (nRow >= 0)
            getLogTable().changeSelection(nRow, 0, false, false, bMove);
    }

    DocumentListener m_dlFilterListener = new DocumentListener() {
        public void changedUpdate(DocumentEvent arg0) {
            try {
                if (arg0.getDocument().equals(m_tfFindWord.getDocument())
                        && m_chkEnableFind.isSelected())
                    getLogTable().setFilterFind(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument()
                        .equals(m_tfRemoveWord.getDocument())
                        && m_chkEnableRemove.isSelected())
                    getLogTable().SetFilterRemove(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowPid.getDocument())
                        && m_chkEnableShowPid.isSelected())
                    getLogTable().SetFilterShowPid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowTid.getDocument())
                        && m_chkEnableShowTid.isSelected())
                    getLogTable().SetFilterShowTid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowTag.getDocument())
                        && m_chkEnableShowTag.isSelected())
                    getLogTable().SetFilterShowTag(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfRemoveTag.getDocument())
                        && m_chkEnableRemoveTag.isSelected())
                    getLogTable().SetFilterRemoveTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfBookmarkTag.getDocument())
                        && m_chkEnableBookmarkTag.isSelected())
                    getLogTable().SetFilterBookmarkTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfHighlight.getDocument())
                        && m_chkEnableHighlight.isSelected())
                    getLogTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfSearch.getDocument())) {
                    getLogTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getLogTable().gotoNextSearchResult();
                } else if (arg0.getDocument().equals(m_tfFromTimeTag.getDocument())) {
                    getLogTable().SetFilterFromTime(m_tfFromTimeTag.getText());
                } else if (arg0.getDocument().equals(m_tfToTimeTag.getDocument())) {
                    getLogTable().SetFilterToTime(m_tfToTimeTag.getText());
                }
                m_nChangedFilter = STATUS_CHANGE;
                runFilter();
            } catch (Exception e) {
                T.e(e);
            }
        }

        public void insertUpdate(DocumentEvent arg0) {
            try {
                if (arg0.getDocument().equals(m_tfFindWord.getDocument())
                        && m_chkEnableFind.isSelected())
                    getLogTable().setFilterFind(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument()
                        .equals(m_tfRemoveWord.getDocument())
                        && m_chkEnableRemove.isSelected())
                    getLogTable().SetFilterRemove(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowPid.getDocument())
                        && m_chkEnableShowPid.isSelected())
                    getLogTable().SetFilterShowPid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowTid.getDocument())
                        && m_chkEnableShowTid.isSelected())
                    getLogTable().SetFilterShowTid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowTag.getDocument())
                        && m_chkEnableShowTag.isSelected())
                    getLogTable().SetFilterShowTag(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfRemoveTag.getDocument())
                        && m_chkEnableRemoveTag.isSelected())
                    getLogTable().SetFilterRemoveTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfBookmarkTag.getDocument())
                        && m_chkEnableBookmarkTag.isSelected())
                    getLogTable().SetFilterBookmarkTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfHighlight.getDocument())
                        && m_chkEnableHighlight.isSelected())
                    getLogTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfSearch.getDocument())) {
                    getLogTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getLogTable().gotoNextSearchResult();
                } else if (arg0.getDocument().equals(m_tfFromTimeTag.getDocument())) {
                    getLogTable().SetFilterFromTime(m_tfFromTimeTag.getText());
                } else if (arg0.getDocument().equals(m_tfToTimeTag.getDocument())) {
                    getLogTable().SetFilterToTime(m_tfToTimeTag.getText());
                }
                m_nChangedFilter = STATUS_CHANGE;
                runFilter();
            } catch (Exception e) {
                T.e(e);
            }
        }

        public void removeUpdate(DocumentEvent arg0) {
            try {
                if (arg0.getDocument().equals(m_tfFindWord.getDocument())
                        && m_chkEnableFind.isSelected())
                    getLogTable().setFilterFind(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument()
                        .equals(m_tfRemoveWord.getDocument())
                        && m_chkEnableRemove.isSelected())
                    getLogTable().SetFilterRemove(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowPid.getDocument())
                        && m_chkEnableShowPid.isSelected())
                    getLogTable().SetFilterShowPid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowTid.getDocument())
                        && m_chkEnableShowTid.isSelected())
                    getLogTable().SetFilterShowTid(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfShowTag.getDocument())
                        && m_chkEnableShowTag.isSelected())
                    getLogTable().SetFilterShowTag(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfRemoveTag.getDocument())
                        && m_chkEnableRemoveTag.isSelected())
                    getLogTable().SetFilterRemoveTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfBookmarkTag.getDocument())
                        && m_chkEnableBookmarkTag.isSelected())
                    getLogTable().SetFilterBookmarkTag(arg0.getDocument().getText(
                            0, arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfHighlight.getDocument())
                        && m_chkEnableHighlight.isSelected())
                    getLogTable().SetHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                else if (arg0.getDocument().equals(m_tfSearch.getDocument())) {
                    getLogTable().SetSearchHighlight(arg0.getDocument().getText(0,
                            arg0.getDocument().getLength()));
                    getLogTable().gotoNextSearchResult();
                } else if (arg0.getDocument().equals(m_tfFromTimeTag.getDocument())) {
                    getLogTable().SetFilterFromTime(m_tfFromTimeTag.getText());
                } else if (arg0.getDocument().equals(m_tfToTimeTag.getDocument())) {
                    getLogTable().SetFilterToTime(m_tfToTimeTag.getText());
                }
                m_nChangedFilter = STATUS_CHANGE;
                runFilter();
            } catch (Exception e) {
                T.e(e);
            }
        }
    };


    private void loadTableColumnState() {
        for (int nIndex = 0; nIndex < LogFilterTableModel.COMUMN_MAX; nIndex++) {
            LogFilterTableModel.setColumnWidth(nIndex, m_colWidths[nIndex]);
        }
        m_colWidths = LogFilterTableModel.ColWidth;

        getLogTable().showColumn(LogFilterTableModel.COMUMN_BOOKMARK,
                m_chkClmBookmark.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_LINE,
                m_chkClmLine.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_DATE,
                m_chkClmDate.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_TIME,
                m_chkClmTime.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_LOGLV,
                m_chkClmLogLV.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_PID,
                m_chkClmPid.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_THREAD,
                m_chkClmThread.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_TAG,
                m_chkClmTag.isSelected());
        getLogTable().showColumn(LogFilterTableModel.COMUMN_MESSAGE,
                m_chkClmMessage.isSelected());
    }

    ItemListener m_itemListener = new ItemListener() {
        public void itemStateChanged(ItemEvent itemEvent) {
            JCheckBox check = (JCheckBox) itemEvent.getSource();

            if (check.equals(m_chkVerbose)) {
                setLogLV(LogInfo.LOG_LV_VERBOSE, check.isSelected());
            } else if (check.equals(m_chkDebug)) {
                setLogLV(LogInfo.LOG_LV_DEBUG, check.isSelected());
            } else if (check.equals(m_chkInfo)) {
                setLogLV(LogInfo.LOG_LV_INFO, check.isSelected());
            } else if (check.equals(m_chkWarn)) {
                setLogLV(LogInfo.LOG_LV_WARN, check.isSelected());
            } else if (check.equals(m_chkError)) {
                setLogLV(LogInfo.LOG_LV_ERROR, check.isSelected());
            } else if (check.equals(m_chkFatal)) {
                setLogLV(LogInfo.LOG_LV_FATAL, check.isSelected());
            } else if (check.equals(m_chkClmBookmark)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_BOOKMARK,
                        check.isSelected());
            } else if (check.equals(m_chkClmLine)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_LINE,
                        check.isSelected());
            } else if (check.equals(m_chkClmDate)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_DATE,
                        check.isSelected());
            } else if (check.equals(m_chkClmTime)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_TIME,
                        check.isSelected());
            } else if (check.equals(m_chkClmLogLV)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_LOGLV,
                        check.isSelected());
            } else if (check.equals(m_chkClmPid)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_PID,
                        check.isSelected());
            } else if (check.equals(m_chkClmThread)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_THREAD,
                        check.isSelected());
            } else if (check.equals(m_chkClmTag)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_TAG,
                        check.isSelected());
            } else if (check.equals(m_chkClmMessage)) {
                getLogTable().showColumn(LogFilterTableModel.COMUMN_MESSAGE,
                        check.isSelected());
            } else if (check.equals(m_chkEnableFind)
                    || check.equals(m_chkEnableRemove)
                    || check.equals(m_chkEnableShowPid)
                    || check.equals(m_chkEnableShowTid)
                    || check.equals(m_chkEnableShowTag)
                    || check.equals(m_chkEnableRemoveTag)
                    || check.equals(m_chkEnableBookmarkTag)
                    || check.equals(m_chkEnableTimeTag)
                    || check.equals(m_chkEnableHighlight)) {
                useFilter(check);
            }
        }
    };

    private KeyEventDispatcher mKeyEventDispatcher = new KeyEventDispatcher() {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            boolean altPressed = ((e.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK);
            boolean ctrlPressed = ((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK);
            switch (e.getKeyCode()) {
                case KeyEvent.VK_F2:
                    if (e.isControlDown() && e.getID() == KeyEvent.KEY_PRESSED) {
                        int[] arSelectedRow = getLogTable().getSelectedRows();
                        for (int nIndex : arSelectedRow) {
                            LogInfo logInfo = m_tmLogTableModel.getRow(nIndex);
                            logInfo.m_bMarked = !logInfo.m_bMarked;
                            bookmarkItem(nIndex, logInfo.m_intLine - 1, logInfo.m_bMarked);
                        }
                        getLogTable().repaint();
                    } else if (!e.isControlDown() && e.getID() == KeyEvent.KEY_PRESSED)
                        getLogTable().gotoPreBookmark();
                    break;
                case KeyEvent.VK_F3:
                    if (e.getID() == KeyEvent.KEY_PRESSED)
                        getLogTable().gotoNextBookmark();
                    return false;
                case KeyEvent.VK_F:
                    if (e.getID() == KeyEvent.KEY_PRESSED && ctrlPressed) {
                        setSearchFocus();
                    }
                    break;
                case KeyEvent.VK_F5:
                    if (e.getID() == KeyEvent.KEY_PRESSED)
                        getLogTable().gotoNextSearchResult();
                    break;
                case KeyEvent.VK_F4:
                    if (e.getID() == KeyEvent.KEY_PRESSED)
                        getLogTable().gotoPreSearchResult();
                    break;
                case KeyEvent.VK_LEFT:
                    if (e.getID() == KeyEvent.KEY_PRESSED && altPressed)
                        getLogTable().historyBack();
                    break;
                case KeyEvent.VK_RIGHT:
                    if (e.getID() == KeyEvent.KEY_PRESSED && altPressed)
                        getLogTable().historyForward();
                    break;
            }
            return false;
        }
    };

    public void openFileBrowserToLoad(FileType type) {
        FileDialog fd = new FileDialog(this, "File open", FileDialog.LOAD);
        if (type == FileType.LOGFILE) {
            fd.setDirectory(m_strLastDir);
        }
        fd.setVisible(true);
        if (fd.getFile() != null) {
            switch (type) {
                case LOGFILE:
                    parseFile(new File(fd.getDirectory() + fd.getFile()));
                    m_recentMenu.addEntry(fd.getDirectory() + fd.getFile());
                    m_strLastDir = fd.getDirectory();
                    break;
                case MODEFILE:
                    loadModeFile(new File(fd.getDirectory() + fd.getFile()));
                    break;
            }
        }
    }


    private void openFileBrowserToSave(FileType type) {
        FileDialog fd = new FileDialog(this, "File save", FileDialog.SAVE);
        if (type != FileType.MODEFILE) {
            return;
        }
        fd.setVisible(true);
        if (fd.getFile() != null) {
            switch (type) {
                case MODEFILE:
                    saveModeFile(new File(fd.getDirectory() + fd.getFile()));
                    break;
            }
        }
    }

    private void saveModeFile(File file) {
        if (file == null) {
            T.e("mode file == null");
            return;
        }
        mStateSaver.save(file.getAbsolutePath());
    }

    private void loadModeFile(File file) {
        if (file == null) {
            T.e("mode file == null");
            return;
        }
        mStateSaver.load(file.getAbsolutePath());
    }

    public void refreshDiffMenuBar() {
        if (mDiffService.getDiffServiceType() == DiffService.DiffServiceType.AS_SERVER) {
            if (mDiffService.isDiffConnected()) {
                sConnectDiffMenuItem.setEnabled(false);
                sDisconnectDiffMenuItem.setEnabled(true);
            } else {
                sConnectDiffMenuItem.setEnabled(true);
                sDisconnectDiffMenuItem.setEnabled(false);
            }
        }
    }

    public void refreshUIWithDiffState() {
        if (!mDiffService.isDiffConnected()) {
            mSyncScrollCheckBox.setEnabled(false);
            mSyncSelectedCheckBox.setEnabled(false);
            m_tfDiffState.setBackground(null);
            m_tfDiffState.setText("disconnected");
        } else {
            mSyncScrollCheckBox.setEnabled(true);
            mSyncSelectedCheckBox.setEnabled(true);
            m_tfDiffState.setBackground(Color.GREEN);
            switch (mDiffService.getDiffServiceType()) {
                case AS_CLIENT:
                    m_tfDiffState.setText("as client");
                    break;
                case AS_SERVER:
                    m_tfDiffState.setText("as server");
                    break;
            }
        }
    }

    private void initDiffService() {
        int port = 20000 + new Random().nextInt(10000);
        m_tfDiffPort.setText("port: " + port);
        mDiffService = new DiffService(this, port);
    }

    public LogTable getLogTable() {
        return m_tbLogTable;
    }

    public void searchSimilar(String cmd) {
        m_tbLogTable.searchSimilarForward(cmd);
    }

    public void compareWithSelectedRows(String targetRows) {
        String fmtRows = m_tbLogTable.getFormatSelectedRows(LogFilterTableModel.COMUMN_LINE, LogFilterTableModel.COMUMN_TIME);
        if (fmtRows == null || fmtRows.length() == 0) {
            return;
        }

        try {
            File tempFile1 = File.createTempFile("target", ".txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile1));
            bw.write(targetRows);
            bw.close();

            File tempFile2 = File.createTempFile("src", ".txt");
            bw = new BufferedWriter(new FileWriter(tempFile2));
            bw.write(fmtRows);
            bw.close();

            Utils.runCmd(new String[]{DIFF_PROGRAM_PATH, tempFile1.getAbsolutePath(), tempFile2.getAbsolutePath()});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int mLastVBarValue = 0;
    private AdjustmentListener mScrollListener = new AdjustmentListener() {
        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            JScrollBar scrollBar = (JScrollBar) e.getSource();
            if (scrollBar == m_scrollVBar.getHorizontalScrollBar()) {
                T.d("HorizontalScrollBar: " + scrollBar.getValue());
            } else if (scrollBar == m_scrollVBar.getVerticalScrollBar()) {
                mDiffService.writeDiffCommand(
                        DiffService.DiffServiceCmdType.SYNC_SCROLL_V,
                        String.valueOf(scrollBar.getValue() - mLastVBarValue)
                );
                mLastVBarValue = scrollBar.getValue();
            }
        }
    };

    public void enableSyncScroll(boolean enable) {
        mSyncScrollEnable = enable;
        if (mSyncScrollEnable) {
//            m_scrollVBar.getHorizontalScrollBar().addAdjustmentListener(mScrollListener);
            m_scrollVBar.getVerticalScrollBar().addAdjustmentListener(mScrollListener);
        } else {
//            m_scrollVBar.getHorizontalScrollBar().removeAdjustmentListener(mScrollListener);
            m_scrollVBar.getVerticalScrollBar().removeAdjustmentListener(mScrollListener);
        }
    }


    private void enableSyncSelected(boolean enable) {
        mSyncScrollSelected = enable;
    }

    public void handleScrollVSyncEvent(String cmd) {
        JScrollBar scrollBar = m_scrollVBar.getVerticalScrollBar();
        int scrollChanged = Integer.valueOf(cmd);
        int newValue = scrollBar.getValue() + scrollChanged;

        if (newValue >= scrollBar.getMinimum() && newValue <= scrollBar.getMaximum())
            scrollBar.setValue(newValue);
    }

    public void onSelectedRowChanged(int lastRowIndex, int rowIndex, LogInfo logInfo) {
        if (mSyncScrollSelected) {
            if (lastRowIndex > rowIndex) {
                mDiffService.writeDiffCommand(
                        DiffService.DiffServiceCmdType.SYNC_SELECTED_BACKWARD,
                        logInfo.m_strMessage
                );
            } else {
                mDiffService.writeDiffCommand(
                        DiffService.DiffServiceCmdType.SYNC_SELECTED_FORWARD,
                        logInfo.m_strMessage
                );
            }
        }
    }

    public void handleSelectedForwardSyncEvent(String cmd) {
        m_tbLogTable.searchSimilarForward(cmd);
    }

    public void handleSelectedBackwardSyncEvent(String cmd) {
        m_tbLogTable.searchSimilarBackward(cmd);
    }

    private enum FileType {
        LOGFILE, MODEFILE
    }

    public class TargetDevice {
        String code;
        String product;
        String model;
        String device;

        public TargetDevice(String src) {
            src = src.replace("\t", " ");
            int codeIdx = src.indexOf(' ');
            if (codeIdx == -1) {
                this.code = src;
                return;
            }

            this.code = src.substring(0, codeIdx);
            int infoIdx = src.indexOf("product:");
            if (infoIdx != -1) {
                String infoStr = src.substring(infoIdx);
                String[] infos = infoStr.split("\\s+");
                this.product = infos[0].substring("product:".length());
                this.model = infos[1].substring("model:".length());
                this.device = infos[2].substring("device:".length());
            }
        }

        @Override
        public String toString() {
            return "[" + this.model + "]" + this.code;
        }
    }

    public class DumpOfServiceInfo {
        String name = "Dump Of Unknown Service";
        int row;

        public DumpOfServiceInfo(String name, int row) {
            this.name = name;
            this.row = row;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}


