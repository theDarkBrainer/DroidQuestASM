package com.droidquest.program;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;

public class DialogEditorASM extends JDialog implements DialogDebuggerASM.IStopDebugger {
	
	private static final short STACK_SIZE	= 100;
	
	private JTextPane textPane;
	private short initialStatus = 0;
	private ProcessorMSP430 processor = new ProcessorMSP430();
	private short[] currentProgram = null;
	private CompilerASM.DebugInfo currentDebugInfo = null;
	private DialogDebuggerASM debugger = null;

	public DialogEditorASM(JFrame parent, String code, short initialStatus) {
		super(parent, "Program Editor", true);
		
		this.initialStatus = initialStatus;
	    
		EditorKit editorKit = new StyledEditorKit() {

            @Override
            public Document createDefaultDocument() {
            	EditorDocument doc = new EditorDocument(keywords);
                doc.setTabs(4);
                doc.addUndoableEditListener(new UndoListener());
                return doc;
            }
        };

    	/*try
    	{
    		BufferedReader br = new BufferedReader(new FileReader(FILENAME));
    		
    		code = null;
    		while ( true )
    		{
    			String line = br.readLine();
    			if ( line == null )
    				break;
    			
    			if ( code == null )
    				code = line;
    			else
    				code += "\n" + line;
    		}
    		
    		br.close();
    	}
    	catch (FileNotFoundException e)
    	{
		}
    	catch (IOException e)
    	{
		}*/
    		
        textPane = new JTextPane();
        textPane.setEditorKitForContentType("text/java", editorKit);
        textPane.setContentType("text/java");
        textPane.setEditable(true);
        
        if ( code == null || code.length() == 0 )
        	this.onFileNew();
        else
           	textPane.setText( code );

	    JPanel messagePane = new JPanel();
	    messagePane.setLayout(new GridLayout());
		
		JScrollPane scroll = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setRowHeaderView(new TextLineNumber(textPane));
	    messagePane.add(scroll);
	    getContentPane().add(messagePane);

	    JButton buttonExecute = new JButton("Close"); 
	    buttonExecute.addActionListener( new ActionListener() {
	        public void actionPerformed(ActionEvent e)
	        {
	            DialogEditorASM.this.onButtonClose();
	            DialogEditorASM.this.dispose();
	        }
	    });
	    
	    JPanel leftPane = new JPanel();
	    JButton buttonHelp = new JButton("Help"); 
	    JButton buttonStep = new JButton("Step");

	    buttonHelp.addActionListener( new ActionListener() {
	        public void actionPerformed(ActionEvent e)
	        {
	            DialogEditorASM.this.onButtonHelp();
	        }
	    });

	    buttonStep.addActionListener( new ActionListener() {
	        public void actionPerformed(ActionEvent e)
	        {
	            DialogEditorASM.this.onButtonStep();
	        }
	    });
	    
	    leftPane.add(buttonHelp);
	    leftPane.add(buttonStep);
	    
	    JPanel buttonPane = new JPanel();
	    buttonPane.setLayout(new BorderLayout ());
	    
	    buttonPane.add(buttonExecute, BorderLayout.EAST);
	    buttonPane.add(leftPane, BorderLayout.WEST);
	    this.getContentPane().add(buttonPane, BorderLayout.SOUTH);
	    
	    JMenuBar menuBar = new JMenuBar();
	    JMenu menuFile = new JMenu("File");
	    menuBar.add( menuFile );
	    JMenuItem menuItemNew = new JMenuItem("New");
	    menuItemNew.addActionListener( new ActionListener() {
	    	public void actionPerformed(ActionEvent e)
	    	{
	    		DialogEditorASM.this.onFileNew();
	    	}
	    });
	    menuFile.add( menuItemNew );
	    JMenuItem menuItemOpen = new JMenuItem("Open...");
	    menuItemOpen.addActionListener( new ActionListener() {
	    	public void actionPerformed(ActionEvent e)
	    	{
	    		DialogEditorASM.this.onFileOperation( false );
	    	}
	    });
	    menuFile.add( menuItemOpen );
	    JMenuItem menuItemSaveAs = new JMenuItem("Save as...");
	    menuItemSaveAs.addActionListener( new ActionListener() {
	    	public void actionPerformed(ActionEvent e)
	    	{
	    		DialogEditorASM.this.onFileOperation( true );
	    	}
	    } );
	    menuFile.add( menuItemSaveAs );
	    this.setJMenuBar( menuBar );
	    
	    ActionListener escListener = new ActionListener() {

	        @Override
	        public void actionPerformed(ActionEvent e) {
	        	DialogEditorASM.this.setVisible(false);
	        }
	    };

	    this.getRootPane().registerKeyboardAction(escListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
	    
	    this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	    this.pack(); 
	}
	
	public String getCode()
	{
		return textPane.getText();
	}

	private void onFileNew()
	{
		String code = 
			"// the bit status of register R15" + "\n" +
			"// access the robot interface" + "\n" +
			"// #0x0001 (#LS) - left sensor" + "\n" +
			"// #0x0002 (#LT) - left thruster" + "\n" +
			"// #0x0004 (#RS) - right sensor" + "\n" +
			"// #0x0008 (#RT) - right thruster" + "\n" +
			"// #0x0010 (#TS) - top sensor" + "\n" +
			"// #0x0020 (#TT) - top thruster" + "\n" +
			"// #0x0040 (#BS) - bottom sensor" + "\n" +
			"// #0x0080 (#BT) - bottom thruster" + "\n" +
			"// #0x0100 (#AI) - antenna in" + "\n" +
			"// #0x0200 (#AO) - antenna out" + "\n" +
			"// #0x0400 (#CI) - claw in" + "\n" +
			"// #0x0800 (#CO) - claw out" + "\n" +
			"\n"+
			"		MOV #0, R4";
			
	    textPane.setText( code );		
	}
	
	private void onFileOperation(boolean save)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter( new FileNameExtensionFilter("Assembly File (*.asm)","asm") );
		int returnVal = save ? chooser.showSaveDialog( this ) : chooser.showOpenDialog( this );
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
            File file = chooser.getSelectedFile();
            if ( save )
            {
            	file = new File(file.toString() + ".asm");
	            try
	            {
	            	PrintWriter out = new PrintWriter( file );
	            	
	        		String[] lines = this.getCode().split("\n");
	        		for(String line : lines)
	        		{
	        			out.println( line );
	        		}
	        		
	        		out.close();
	            }
	            catch (FileNotFoundException e)
	            {
				}
            }
            else
            {
            	try
            	{
                	BufferedReader br = new BufferedReader(new FileReader(file));
            	    StringBuilder sb = new StringBuilder();
            	    String line = br.readLine();

            	    while (line != null) {
            	        sb.append(line);
            	        sb.append("\n");
            	        line = br.readLine();
            	    }

                    textPane.setText( sb.toString() );

            	    br.close();

            	    currentProgram = null;
                    currentDebugInfo = null;
            	}
            	catch (FileNotFoundException e)
            	{
				}
            	catch (IOException e)
            	{
				}
            	finally
            	{
            	}
            }
		}
	}
	
	private void compileCurrent(boolean printUnitTest)
	{
		String code = this.getCode();
		
        try
        {
        	BufferedWriter bw = new BufferedWriter(new FileWriter(FILENAME));
        	bw.write( code );
        	bw.close();
        }
        catch (IOException e)
        {
        }
        
        currentProgram = null;
        currentDebugInfo = null;
        
        try
        {
	        CompilerASM compiler = new CompilerASM(code, STACK_SIZE);
			currentProgram = compiler.Compile();
	        currentDebugInfo = compiler.GetDebugInfo();
	        
	        if ( printUnitTest )
	        	compiler.PrintUnitTestAsserts();
		}
        catch (CompilerASM.CompilerException e)
        {
    		System.out.println("------------------------------------------------------");
    		System.out.println("Compile Error: " + e.getMessage() );
    		System.out.println("Error at:      " + Integer.toString(e.fLine) + "(" + Integer.toString(e.fAt) + ")" );

    		e.printStackTrace();
		}
	}

	private void openDebugDialog()
	{
		debugger = new DialogDebuggerASM( this, this );
		Dimension debugDim = new Dimension( 250, 600 );
	    debugger.setSize( debugDim );
	    Rectangle parentBounts = this.getBounds();
	    debugger.setLocation( parentBounts.x + parentBounts.width - debugDim.width, parentBounts.y );
	    debugger.setVisible( true );
        textPane.setEditable( false );
        debugger.setData( processor );
	}
		
	private void onButtonClose() {			
		UnitTestCompilerProcessor unitTest = new UnitTestCompilerProcessor();
		unitTest.Execute();
	}

	private void onButtonStep() {
		if ( currentProgram == null )
			this.compileCurrent( false );

		if ( currentProgram != null)
		{
			try
			{
				if ( currentDebugInfo == null )
				{
					processor.Reset( currentProgram, STACK_SIZE );
					processor.GetRegisters()[15] = initialStatus;
					processor.Run();
				}
				else if ( debugger == null )
				{
					processor.Reset( currentProgram, STACK_SIZE );
					processor.GetRegisters()[15] = initialStatus;
					this.openDebugDialog();
				}
				else
				{
					processor.Step();
			        debugger.setData( processor );
				}
				
				if ( processor != null && ! processor.IsRunning() )
				{
					currentProgram = processor.GetMemory();
					processor.Reset( currentProgram, STACK_SIZE );
				}

		        int memAddress = processor.GetRegisters()[0];
		        Integer lineNum = currentDebugInfo.memAddress2LineNum.get( memAddress );
		        if ( lineNum == null )
		        	textPane.select( 0, 0 );
		        else
		        {
			        String code = this.getCode();
			        int selectionStart = -1, selectionEnd = code.length();
			        for(int i=0; i<code.length(); i++)
			        {
			        	char ch = code.charAt( i );
			        	if ( ch == '\n' )
			        		lineNum--;
			        	
			        	if ( lineNum == 0 && selectionStart == -1 )
			        		selectionStart = i;
			        	else if ( lineNum == -1 )
			        	{
			        		selectionEnd = i;
			        		break;
			        	}
			        }
			        
			        if ( selectionStart >= 0 && selectionStart < selectionEnd )
			        {
			        	textPane.select(selectionStart, selectionEnd);
			        }
				}
			}
			catch (Exception e)
			{	    	
				e.printStackTrace();				
			}
		}
	}
	
	public void onStopDebugger()
	{
		if ( debugger != null )
		{
			debugger.setVisible(false); 
			debugger.dispose();
			debugger = null;
		}

		textPane.select( 0, 0 );
		textPane.setEditable( true );
        currentProgram = null;
        currentDebugInfo = null;
	}

	private void onButtonHelp() {
	}
	
	private UndoManager mng = new UndoManager();

    public boolean undo() {
        if (mng.canUndo()) {
            mng.undo();
            currentProgram = null;
            currentDebugInfo = null;
       }

        return mng.canUndo();
    }

    public boolean redo() {
        if (mng.canRedo()) {
            mng.redo();
            currentProgram = null;
            currentDebugInfo = null;
        }

        return mng.canRedo();
    }
    
	private class UndoListener implements UndoableEditListener {

	        @Override
	        public void undoableEditHappened(UndoableEditEvent e) {
	            mng.addEdit(e.getEdit());
	        }
	    }
	 
	private static final Map<String, MutableAttributeSet> keywords;
	
	static {
		keywords = new HashMap<String, MutableAttributeSet>(100);

		keywords.put("RRC", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("RRA", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("PUSH", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("SWPB", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("CALL", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("RETI", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("SXT", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("RRC.B", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("RRA.B", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("PUSH.B", EditorDocument.DEFAULT_KEYWORD);

		keywords.put("MOV", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("ADD", EditorDocument.DEFAULT_KEYWORD); 
		keywords.put("ADDC", EditorDocument.DEFAULT_KEYWORD); 
		keywords.put("SUB", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("SUBC", EditorDocument.DEFAULT_KEYWORD); 
		keywords.put("CMP", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("DADD", EditorDocument.DEFAULT_KEYWORD); 
		keywords.put("BIT", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("BIC", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("BIS", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("XOR", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("AND", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("MOV.B", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("ADD.B", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("ADDC.B", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("SUB.B", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("SUBC.B", EditorDocument.DEFAULT_KEYWORD); 
		keywords.put("CMP.B", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("DADD.B", EditorDocument.DEFAULT_KEYWORD); 
		keywords.put("BIT.B", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("BIC.B", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("BIS.B", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("XOR.B", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("AND.B", EditorDocument.DEFAULT_KEYWORD);

		keywords.put("JEQ", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("JZ", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("JNE", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("JNZ", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("JC", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("JNC", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("JN", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("JGE", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("JL", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("JMP", EditorDocument.DEFAULT_KEYWORD);

		keywords.put("R1", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R2", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R3", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R4", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R5", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R6", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R7", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R8", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R9", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R10", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R11", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R12", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R13", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R14", EditorDocument.DEFAULT_KEYWORD);
		keywords.put("R15", EditorDocument.DEFAULT_KEYWORD);
		
		keywords.put(".ls", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".lt", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".rs", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".rt", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".ts", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".tt", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".bs", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".bt", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".ain", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".aout", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".cin", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".cout", EditorDocument.DEFAULT_KEYWORD);
		keywords.put(".bat", EditorDocument.DEFAULT_KEYWORD);
	}
	   
	private static final String FILENAME = "program_code.txt";

    public static void main(String[] args)
    {
        DialogEditorASM editorDialog = new DialogEditorASM(null, null, (short)0);
	    editorDialog.setSize( new Dimension( 600, 600 ) );

	    GraphicsConfiguration gc = editorDialog.getGraphicsConfiguration();
	    Rectangle bounds = gc.getBounds();
	    editorDialog.setLocation(bounds.x + (bounds.width - 568) / 2, bounds.y + (bounds.height - 435) / 2);
	    
	    editorDialog.setVisible(true);
    }
}
