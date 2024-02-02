package org.openclover.idea.autoupdater;

import org.openclover.idea.util.ui.CloverIcons;
import org.openclover.idea.PluginVersionInfo;
import org.openclover.idea.util.l10n.CloverIdeaPluginMessages;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.panels.VerticalBox;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

public class NewVersionDialog extends DialogWrapper {
    public static final int SKIP_VERSION_EXIT_CODE = NEXT_USER_EXIT_CODE;
    
    private JTextPane releaseNotes = new JTextPane();
    private JLabel title = new JLabel();
    private HyperlinkLabel releaseNotesLink = new HyperlinkLabel("Release Notes");

    public NewVersionDialog(final LatestVersionInfo version) {
        super(false);
        setOKButtonText("Yes");
        setCancelButtonText("Not now");
        setTitle(CloverIdeaPluginMessages.getString("autoupdate.updatingplugin"));

        title.setIcon(CloverIcons.CLOVER_BIG);
        title.setText(CloverIdeaPluginMessages.getFormattedString("autoupdate.newversionavailable", version.getNumber(), PluginVersionInfo.RELEASE_NUMBER));
        title.setIconTextGap(10);

        releaseNotes.setContentType("text/html");
        releaseNotes.setText(version.getReleaseNotes());
        releaseNotes.setCaretPosition(0);
        releaseNotes.setEditable(false);
        releaseNotes.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(event.getURL());
            }
        });

        releaseNotesLink.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(version.getReleaseNotesUrl());
            }
        });

        init();
    }

    @Override
    protected JComponent createNorthPanel() {
        return title;
    }

    @SuppressWarnings({"MagicNumber"})
    @Override
    protected JComponent createCenterPanel() {
        final VerticalBox box = new VerticalBox();
        box.add(Box.createVerticalStrut(10));
        box.add(new JScrollPane(releaseNotes));
        box.add(Box.createVerticalStrut(10));


        final JPanel linkBox = new JPanel(new BorderLayout());
        linkBox.add(new JPanel(), BorderLayout.CENTER);
        linkBox.add(releaseNotesLink, BorderLayout.EAST);
        linkBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, linkBox.getPreferredSize().height));

        box.add(linkBox);

        box.setPreferredSize(new Dimension(600, 400));
        return box;
    }

    @Override
    protected Action[] createActions() {
        return new Action[] {
                getOKAction(),
                getCancelAction(),
                new AbstractAction() {
                    {
                        putValue(NAME, "Skip this version");
                    }
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        close(SKIP_VERSION_EXIT_CODE);
                    }
                }
        };
    }

}
