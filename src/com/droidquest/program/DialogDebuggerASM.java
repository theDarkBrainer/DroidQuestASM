package com.droidquest.program;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

public class DialogDebuggerASM extends JDialog implements ActionListener  {
	private JTextPane textPane;
	private IStopDebugger stopDebuggerCallback;

    JToggleButton 	btnLS;
    JToggleButton 	btnRS;
    JToggleButton 	btnTS;
    JToggleButton 	btnBS;
    JToggleButton 	btnAI;
    JToggleButton 	btnAO;
	
	public interface IStopDebugger
	{
		void onStopDebugger();
	}
	
	public DialogDebuggerASM(Dialog parent, IStopDebugger callback) {
		super(parent, "Registers", false);
		
		stopDebuggerCallback = callback;

        textPane = new JTextPane();
        textPane.setEditable(false);
        
	    JPanel messagePane = new JPanel();
	    messagePane.setLayout(new GridLayout());
		
		JScrollPane scroll = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    messagePane.add(scroll);
	    
	    btnLS = new JToggleButton("LS");
	    btnRS = new JToggleButton("RS");
	    btnTS = new JToggleButton("TS");
	    Container topBtnContainer = new Container();
	    topBtnContainer.setLayout(new BorderLayout());
	    topBtnContainer.add( btnLS, BorderLayout.WEST);
	    topBtnContainer.add( btnRS, BorderLayout.CENTER);
	    topBtnContainer.add( btnTS, BorderLayout.EAST);
	    
	    btnBS = new JToggleButton("BS");
	    btnAI = new JToggleButton("AI");
	    btnAO = new JToggleButton("AO");
	    Container btnBtnContainer = new Container();
	    btnBtnContainer.setLayout(new BorderLayout());
	    btnBtnContainer.add( btnBS, BorderLayout.WEST);
	    btnBtnContainer.add( btnAI, BorderLayout.CENTER);
	    btnBtnContainer.add( btnAO, BorderLayout.EAST);
	    
	    JButton buttonStop = new JButton("Stop Debug");
	    buttonStop.addActionListener( this );
	    
	    Container buttonsContainer = new Container();
	    buttonsContainer.setLayout(new BorderLayout());
	    buttonsContainer.add( topBtnContainer, BorderLayout.NORTH );
	    buttonsContainer.add( btnBtnContainer, BorderLayout.CENTER );
	    buttonsContainer.add( buttonStop, BorderLayout.SOUTH );

	    Container mainContainer = getContentPane();
	    mainContainer.setLayout(new BorderLayout ());
	    mainContainer.add(messagePane);
	    mainContainer.add( buttonsContainer, BorderLayout.SOUTH );

	    
	    ActionListener escListener = new ActionListener() {

	        @Override
	        public void actionPerformed(ActionEvent e) {
	        	DialogDebuggerASM.this.setVisible(false);
	        }
	    };

	    this.getRootPane().registerKeyboardAction(escListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
	    
	    this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	    this.pack(); 
	}
	
	public void actionPerformed(ActionEvent e) {	    
	    if ( stopDebuggerCallback != null )
	    	stopDebuggerCallback.onStopDebugger();
	}
	
	public void setData(ProcessorMSP430 processor)
	{
		this.updateText( processor );
		
		short[] regisers = processor.GetRegisters();

	    for( ActionListener al : btnLS.getActionListeners() )
	    	btnLS.removeActionListener( al );
	    for( ActionListener al : btnRS.getActionListeners() )
	    	btnRS.removeActionListener( al );
	    for( ActionListener al : btnTS.getActionListeners() )
	    	btnTS.removeActionListener( al );
	    for( ActionListener al : btnBS.getActionListeners() )
	    	btnBS.removeActionListener( al );
	    for( ActionListener al : btnAI.getActionListeners() )
	    	btnAI.removeActionListener( al );
	    for( ActionListener al : btnAO.getActionListeners() )
	    	btnAO.removeActionListener( al );
	        
	    btnLS.addActionListener( new SensorButtonActionListener( this, processor, 0x0001 ) );
	    btnRS.addActionListener( new SensorButtonActionListener( this, processor, 0x0004 ) );
	    btnTS.addActionListener( new SensorButtonActionListener( this, processor, 0x0010 ) );
	    btnBS.addActionListener( new SensorButtonActionListener( this, processor, 0x0040 ) );
	    btnAI.addActionListener( new SensorButtonActionListener( this, processor, 0x0100 ) );
	    btnAO.addActionListener( new SensorButtonActionListener( this, processor, 0x0400 ) );
	    
		
		int R15 = regisers[15];
	    btnLS.setSelected( (R15 & 0x0001) != 0 );
	    btnRS.setSelected( (R15 & 0x0004) != 0 );
	    btnTS.setSelected( (R15 & 0x0010) != 0 );
	    btnBS.setSelected( (R15 & 0x0040) != 0 );
	    btnAI.setSelected( (R15 & 0x0100) != 0 );
	    btnAO.setSelected( (R15 & 0x0400) != 0 );
	    
	    btnLS.setText( (R15 & 0x0001) != 0 ? "LS On": "LS Off" );
	    btnRS.setText( (R15 & 0x0004) != 0 ? "RS On": "RS Off" );
	    btnTS.setText( (R15 & 0x0010) != 0 ? "TS On": "TS Off" );
	    btnBS.setText( (R15 & 0x0040) != 0 ? "BS On": "BS Off" );
	    btnAI.setText( (R15 & 0x0100) != 0 ? "AI On": "AI Off" );
	    btnAO.setText( (R15 & 0x0400) != 0 ? "AO On": "AO Off" );
	}
	
	private void updateText(ProcessorMSP430 processor)
	{
		short[] regisers = processor.GetRegisters();
		
		String 	str = "";
		for(int i=0; i<regisers.length;i++)
		{
			if ( i == 2 )
			{
				str += "Status[";
				str += processor.getC() ? "C-On" : "C-Off";
				str += " ";
				str += processor.getN() ? "N-On" : "N-Off"; 
				str += " ";
				str += processor.getZ() ? "Z-On" : "Z-Off"; 
				str += " ";
				str += processor.getC() ? "V-On" : "V-Off"; 
				str += "]";
			}
			else
			{
				int value = regisers[i] & 0xFFFF;
				str += "R[";
				if ( i < 10 )
					str += "0";
				str += Integer.toString(i) + "] = ";
				str += Integer.toString(value);
				str += " (0x";
				str += Integer.toHexString(value);
				str += ")";
			}
			str += "\n";
		}
		
		List<Integer>	mapSpecialAddressesFlag = new ArrayList<Integer>();
		List<String>	mapSpecialAddressesName = new ArrayList<String>();
		mapSpecialAddressesFlag.add( 0x0002 );
		mapSpecialAddressesName.add( "R15[LT]" );
		mapSpecialAddressesFlag.add( 0x0008 );
		mapSpecialAddressesName.add( "R15[RT]" );
		mapSpecialAddressesFlag.add( 0x0020 );
		mapSpecialAddressesName.add( "R15[TT]" );
		mapSpecialAddressesFlag.add( 0x0080 );
		mapSpecialAddressesName.add( "R15[BT]" );
		mapSpecialAddressesFlag.add( 0x0200 );
		mapSpecialAddressesName.add( "R15[AO]" );
		mapSpecialAddressesFlag.add( 0x0800 );
		mapSpecialAddressesName.add( "R15[CO]" );

		int R15 = regisers[15];
		for(int i=0; i<mapSpecialAddressesFlag.size(); i++)
		{
			int flag = mapSpecialAddressesFlag.get(i);
			String name = mapSpecialAddressesName.get(i);
			
			boolean value = (R15 & flag) != 0;
			
			str += name + " = " + (value ? "True" : "False");
			str += "\n";
		}
		
		short[] memory = processor.GetMemory();
		
		int stackIndex = processor.GetStartAddress() - 1;
		for(int i=0; i<100; i++)
		{
			if ( stackIndex < 0 )
				break;
			
			str += "Stack[";
			if ( i < 10 )
				str += "0";
			str += Integer.toString(stackIndex) + "] = ";
			
			int value = memory[stackIndex] & 0xFFFF;
			str += Integer.toString(value);
			str += " (0x";
			str += Integer.toHexString(value);
			str += ")";
			str += "\n";
			
			stackIndex--;
		}
		
		textPane.setText( str );
		textPane.setCaretPosition(0);
	}
	
	private class SensorButtonActionListener implements ActionListener
	{
		private DialogDebuggerASM parent;
		private ProcessorMSP430 processor;
		private int flag;
		
		public SensorButtonActionListener(DialogDebuggerASM parent, ProcessorMSP430 processor, int flag)
		{
			this.parent = parent;
			this.processor = processor;
			this.flag = flag;
		}
		
        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
            boolean selected = abstractButton.getModel().isSelected();
            //((JToggleButton)abstractButton).setSelected(selected);
            
            short[] regisers = processor.GetRegisters();
            int R15 = regisers[15];
            if ( selected )
            	R15 |= flag;
            else
            	R15 &= ~flag;
            regisers[15] = (short) R15;
            parent.updateText( processor );
            
    	    btnLS.setText( (R15 & 0x0001) != 0 ? "LS On": "LS Off" );
    	    btnRS.setText( (R15 & 0x0004) != 0 ? "RS On": "RS Off" );
    	    btnTS.setText( (R15 & 0x0010) != 0 ? "TS On": "TS Off" );
    	    btnBS.setText( (R15 & 0x0040) != 0 ? "BS On": "BS Off" );
    	    btnAI.setText( (R15 & 0x0100) != 0 ? "AI On": "AI Off" );
    	    btnAO.setText( (R15 & 0x0400) != 0 ? "AO On": "AO Off" );
      }
    };
}
