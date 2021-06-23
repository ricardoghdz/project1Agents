/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project1;

import jade.core.AID;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *
 * @author Equipo
 */
public class ProjectGUI extends JFrame{
    private CustomerAgent myAgent;
    private JTextField productField;
    
    ProjectGUI(CustomerAgent a){
        super(a.getLocalName());
        
        myAgent = a;
        
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(2, 2));
	p.add(new JLabel("Product name:"));
	productField = new JTextField(15);
	p.add(productField);
	getContentPane().add(p, BorderLayout.CENTER);
        
        JButton buyButton = new JButton("Buy");
        buyButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    String title = productField.getText().trim();
                    myAgent.setProductName(title);
                    productField.setText("");
		} catch (Exception e) {
                    JOptionPane.showMessageDialog(ProjectGUI.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
		}
            }
	} );
        
        p = new JPanel();
	p.add(buyButton);
	getContentPane().add(p, BorderLayout.SOUTH);
        
        // Make the agent terminate when the user closes 
	// the GUI using the button on the upper right corner	
	addWindowListener(new	WindowAdapter() {
            public void windowClosing(WindowEvent e) {
		myAgent.doDelete();
            }
	} );
		
	setResizable(false);
    }
    
    public void showGui() {
        pack();
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	int centerX = (int)screenSize.getWidth() / 2;
	int centerY = (int)screenSize.getHeight() / 2;
	setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
	super.setVisible(true);
    }	
}
