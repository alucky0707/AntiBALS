package info.alucky0707.antibals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

@SuppressWarnings("serial")
public class AuthDialog extends JDialog {
    Twitter twitter;
    RequestToken requestToken;
    AccessToken accessToken;

    JTextField url;
    JTextField pin = new JTextField();

    public AuthDialog(Frame frame, Twitter twitter) {
        super(frame, null, false);
        this.twitter = twitter;
        this.twitter.setOAuthAccessToken(null);
        this.accessToken = null;
        initPane();
        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setSize(400, 230);
        this.setTitle("OAuth認証");
    }

    public AuthDialog() {
        this(null, TwitterFactory.getSingleton());
    }

    public AuthDialog(Frame frame) {
        this(frame, TwitterFactory.getSingleton());
    }

    private void initPane() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        url = new JTextField();
        url.setText(this.getRequestUrl());
        url.setEditable(false);
        url.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
        url.setPreferredSize(new Dimension(200, 30));
        url.addMouseListener(new PopupMenu(url));
        gbc.gridx = 0;
        gbc.gridy = 0;
        p.add(url, gbc);
        JButton btn = new JButton("copy");
        final AuthDialog self = this;
        btn.addActionListener(new ActionListener() {
            private JTextField url = self.url;

            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(null, "URLをクリップボードにコピーします",
                        "確認", DISPOSE_ON_CLOSE) == JOptionPane.OK_OPTION) {
                    url.selectAll();
                    url.copy();
                }
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 0;
        p.add(btn, gbc);
        pin = new JTextField();
        pin.setPreferredSize(new Dimension(200, 30));
        pin.addMouseListener(new PopupMenu(pin));

        gbc.gridx = 0;
        gbc.gridy = 1;
        p.add(pin, gbc);
        JButton pinBtn = new JButton("PIN");
        pinBtn.addActionListener(new ActionListener() {
            JTextField pin = self.pin;

            public void actionPerformed(ActionEvent e) {
                if (!pin.getText().equals("") && self.auth(pin.getText())) {
                    self.setVisible(false);
                }
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 1;
        p.add(pinBtn, gbc);
        getContentPane().add(p, BorderLayout.CENTER);
    }

    class PopupMenu extends MouseAdapter {
        JTextComponent component;
        JPopupMenu popup;

        public PopupMenu(JTextComponent _component) {
            component = _component;
            popup = initPopupMenu();
        }

        private JPopupMenu initPopupMenu() {
            JMenuItem cut = new JMenuItem("切り取り");
            cut.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    component.cut();
                }
            });
            JMenuItem copy = new JMenuItem("コピー");
            copy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    component.copy();
                }
            });
            JMenuItem paste = new JMenuItem("貼り付け");
            paste.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    component.paste();
                }
            });
            JMenuItem all = new JMenuItem("すべて選択");
            all.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    component.selectAll();
                }
            });
            JPopupMenu popup = new JPopupMenu("popup");
            if (component.isEditable())
                popup.add(cut);
            popup.add(copy);
            if (component.isEditable())
                popup.add(paste);
            popup.add(all);
            return popup;
        }

        private void mousePopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                JComponent c = (JComponent) e.getSource();
                popup.show(c, e.getX(), e.getY());
                e.consume();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            mousePopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mousePopup(e);
        }
    }

    protected boolean auth(String text) {
        try {
            accessToken = twitter.getOAuthAccessToken(requestToken, text);
        } catch (TwitterException e) {
            JOptionPane.showMessageDialog(this, "リクエストトークンの取得失敗しました");
            e.printStackTrace();
        }
        return accessToken == null ? false : true;
    }

    private String getRequestUrl() {
        String url = null;
        try {
            requestToken = twitter.getOAuthRequestToken();
            url = requestToken.getAuthorizationURL();
        } catch (TwitterException te) {
            JOptionPane.showMessageDialog(this, "何かがおかしいよ");
            te.printStackTrace();
        }
        return url;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public static void main(String... args) {
        new AuthDialog().setVisible(true);
    }

}
