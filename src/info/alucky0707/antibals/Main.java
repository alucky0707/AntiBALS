package info.alucky0707.antibals;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.UserStreamAdapter;
import twitter4j.auth.AccessToken;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class Main {
    static final String version = "0.2.0";
    static final String title = "ロボットのようなもの";
    static final String keywordFileName = "keyword.dat";
    static SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss");
    static final String[] modes = { "何もしない", "リムーブ", "ブロック", "スパム報告" };

    static Pattern[] keywords;

    static Twitter twitter;
    static TwitterStream stream;
    static String screenName;

    static int banNum = 0;
    static JLabel banNumLbl;

    static JFrame frame;
    static JComboBox<String> modeCombo;
    static JTextArea streamArea;

    public static void main(String... args) {
        if (!loadKeyword())
            return;
        JOptionPane.showMessageDialog(null, "起動しました。\n" + "バージョンは " + version
                + "です");
        twitter = TwitterFactory.getSingleton();
        stream = TwitterStreamFactory.getSingleton();
        frame = initFrame();
        frame.setVisible(true);
    }

    static JFrame initFrame() {
        final JFrame frame = new JFrame(title);
        frame.setLocationByPlatform(true);
        frame.setSize(new Dimension(300, 400));
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int flag = JOptionPane.showConfirmDialog(frame,
                                "ウインドウを閉じ終了してもよろしいですか？",
                                "確認", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                if (flag == JOptionPane.YES_OPTION) {
                    stream.cleanUp();
                    System.exit(0);
                }
            }
        });
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        JPanel authPanel = new JPanel();
        authPanel.setLayout(new BoxLayout(authPanel, BoxLayout.X_AXIS));
        authPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        JButton authBtn = new JButton("認証");
        final JLabel authName = new JLabel("              ");
        banNumLbl = new JLabel(zeroPadding("0"));
        authBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AuthDialog dialog = new AuthDialog(frame, twitter);
                dialog.setVisible(true);
                AccessToken access = dialog.getAccessToken();
                if (access != null) {
                    twitter.setOAuthAccessToken(access);
                    stream.setOAuthAccessToken(access);
                    screenName = access.getScreenName();
                    authName.setText("@" + screenName);
                    streamArea.setText("");
                    setBanNum(0);
                    connectStream();
                } else {
                    JOptionPane.showMessageDialog(null, "認証に失敗しました");
                    frame.setTitle(title);
                }
            }
        });
        modeCombo = new JComboBox<String>(modes);
        modeCombo.setMaximumSize(new Dimension(100, 36));
        authPanel.add(authBtn);
        authPanel.add(authName);
        authPanel.add(modeCombo);
        authPanel.add(banNumLbl);

        streamArea = new JTextArea();
        streamArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(streamArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        mainPanel.add(authPanel);
        mainPanel.add(scroll);

        frame.getContentPane().add(mainPanel);
        return frame;
    }

    static boolean loadKeyword() {
        List<String> list;
        try {
            list = Files.readLines(new File(keywordFileName), Charsets.UTF_8);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, keywordFileName
                    + "が読み込めません\n終了します");
            e.printStackTrace();
            return false;
        }
        keywords = new Pattern[list.size()];
        for (int i = 0; i < list.size(); i++) {
            String keyword = list.get(i);
            keywords[i] = Pattern.compile(keyword, Pattern.UNICODE_CASE
                    | Pattern.COMMENTS | Pattern.CASE_INSENSITIVE);
        }
        return true;
    }

    static void connectStream() {
        frame.setTitle(title + "[監視中]");
        stream.cleanUp();
        stream.addListener(new UserStreamAdapter() {
            @Override
            public void onStatus(Status status) {
                onBals(status);
            }
        });
        stream.user();
    }

    static void onBals(final Status status) {
        final String text = status.getText().replaceAll(
                "(RT )?@[a-zA-Z0-9_]+:?|\\s+", "");
        System.out.println(text);
        if (status.getUser().getScreenName().equals(screenName))
            return;
        boolean flag = false;
        for (int i = 0; i < keywords.length; i++) {
            if (keywords[i].matcher(text).find()) {
                flag = true;
                break;
            }
        }
        if (flag) {
            try {
                long userId = status.getUser().getId();
                final String mode = modes[modeCombo.getSelectedIndex()];
                if (mode.equals("何もしない")) {
                    // nop
                } else if (mode.equals("リムーブ")) {
                    twitter.destroyFriendship(userId);
                } else if (mode.equals("ブロック")) {
                    twitter.createBlock(userId);
                } else if (mode.equals("スパム報告")) {
                    twitter.reportSpam(userId);
                } else {
                    JOptionPane.showMessageDialog(frame, "何かがおかしいよ");
                    return;
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        streamArea.setText(streamArea.getText() + "["
                                + dateFormat.format(status.getCreatedAt())
                                + "]\n" + "@"
                                + status.getUser().getScreenName() + "を「"
                                + text + "」が原因で" + mode + "しました\n");
                        incrBanNum();
                        System.out.println(banNum);
                    }
                });
            } catch (TwitterException e) {
                JOptionPane.showMessageDialog(frame, "何かがおかしいよ");
                e.printStackTrace();
            }
        }
    }

    static void setBanNum(int n) {
        banNum = n;
        banNumLbl.setText(zeroPadding(Integer.toString(n)));
    }

    static void incrBanNum() {
        setBanNum(banNum + 1);
    }

    static String zeroPadding(String n) {
        n = ("00000" + n);
        return n.substring(n.length() - 5);
    }

}
