/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;
import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 *
 * @author lcv
 */
public class LARVADialog extends JDialog implements ActionListener {

    Consumer<ActionEvent> myListener;

    public LARVADialog(Consumer<ActionEvent> listener) {
        super();
        myListener = listener;
        this.setTitle("LARVA");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        myListener.accept(e);
    }

    public void closeLARVAFrame() {
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));

    }

}
