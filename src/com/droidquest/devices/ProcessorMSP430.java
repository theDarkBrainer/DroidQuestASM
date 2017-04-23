package com.droidquest.devices;

import com.droidquest.Room;
import com.droidquest.chipstuff.Port;
import com.droidquest.items.GenericRobot;
import com.droidquest.items.Item;
import com.droidquest.program.CompilerASM;
import com.droidquest.program.CompilerASM.CompilerException;
import com.droidquest.program.DialogEditorASM;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ProcessorMSP430 extends Device {
    private Color color;
    private transient GenericRobot robot;
    private String code = "";
    private short[] memory = new short[0];

    public ProcessorMSP430(int X, int Y, Room r, Color col) {
        x = X;
        y = Y;
        width = 28;
        height = 32;
        room = r;
        if (room.portalItem != null) {
            if (room.portalItem.getClass().toString().endsWith("Robot")) {
                robot = (GenericRobot) room.portalItem;
            }
        }
        rotation = 0;
        color = col;
        grabbable = false;
        GenerateIcons();
    }

    public void writeRef(ObjectOutputStream s) throws IOException {
        super.writeRef(s);
        s.writeInt(level.items.indexOf(robot));
        s.writeUTF(code);
        s.writeInt(memory.length);
        for(short d : memory)
        	s.writeShort( d );
    }

    public void readRef(ObjectInputStream s) throws IOException {
        super.readRef(s);
        robot = (GenericRobot) level.FindItem(s.readInt());
        code = s.readUTF();
        int codeBufLen = s.readInt();
        memory = new short[codeBufLen];
        for(int i=0; i<codeBufLen; i++)
        	memory[i] = s.readShort();
    }

    public void GenerateIcons() {
        robot = (GenericRobot) room.portalItem;
        if ( ports == null )
        	ports = new Port[0];
        
        icons = new ImageIcon[2];
        icons[0] = new ImageIcon(new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR));
        icons[1] = new ImageIcon(new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR));
        Graphics g;
        Graphics2D g2;
        Color transparent = new Color(0, 0, 0, 0);

        // 0 = Off
        try {
            g = icons[0].getImage().getGraphics();
        }
        catch (NullPointerException e) {
            System.out.println("Could not get Graphics pointer to " + getClass() + "Image");
            return;
        }
        g2 = (Graphics2D) g;
        g2.setBackground(transparent);
        g2.clearRect(0, 0, width, height);
        g.setColor(Color.white);
        g.fillRect(8, 0, 20, 4);
        g.fillRect(8, 0, 12, 8);
        g.fillRect(0, 10, 4, 4);
        g.fillRect(4, 14, 4, 4);
        g.fillRect(8, 18, 4, 4);
        g.fillRect(12, 22, 4, 4);
        g.fillRect(8, 26, 12, 6);
        g.fillRect(20, 28, 8, 4);

        // 1 = On
        try {
            g = icons[1].getImage().getGraphics();
        }
        catch (NullPointerException e) {
            System.out.println("Could not get Graphics pointer to " + getClass() + "Image");
            return;
        }
        g2 = (Graphics2D) g;
        g2.setBackground(transparent);
        g2.clearRect(0, 0, width, height);
        g.setColor(new Color(255, 128, 0));
        g.fillRect(8, 0, 20, 4);
        g.fillRect(8, 0, 12, 8);
        g.fillRect(12, 8, 4, 18);
        g.fillRect(8, 26, 12, 6);
        g.fillRect(20, 28, 8, 4);
    }

    public void Decorate() {
        super.Decorate();
        currentIcon = icons[0].getImage();
        try {
            g = currentIcon.getGraphics();
        }
        catch (NullPointerException e) {
            System.out.println("Could not get Graphics pointer to " + getClass() + " Image");
            return;
        }
        g.setColor(color);
        switch (rotation) {
            case Port.ROT_UP: // Thrusts Up, moves Down
                g.fillRect(0, 0, 30, 6);
                g.fillRect(4, 6, 22, 4);
                g.fillRect(8, 10, 14, 4);
                g.fillRect(12, 14, 6, 2);
                break;

            case Port.ROT_RIGHT: // Thrusts Right, moves Left
                g.fillRect(44, 0, 10, 20);
                g.fillRect(40, 2, 4, 16);
                g.fillRect(36, 4, 4, 12);
                g.fillRect(32, 6, 4, 8);
                break;

            case Port.ROT_DOWN: // Thrusts Down, moves Up
                g.fillRect(0, 26, 30, 6);
                g.fillRect(4, 22, 22, 4);
                g.fillRect(8, 18, 14, 4);
                g.fillRect(12, 16, 6, 2);
                break;

            case Port.ROT_LEFT: // Thrusts Left, moves Right
                g.fillRect(0, 0, 10, 20);
                g.fillRect(10, 2, 4, 16);
                g.fillRect(14, 4, 4, 12);
                g.fillRect(18, 6, 4, 8);
                break;
        }
    }

    private short getStatus()
    {
    	short status = 0;
    	
    	for(Device device : robot.devices)
    	{
    		if (device instanceof Bumper)
    		{
    			Bumper bump = (Bumper)device;
    			switch ( bump.Rotation() )
    			{
    			case Port.ROT_LEFT:
    				if ( bump.ports[0].value )
    					status |= 0x0001;	// left sensor
    				break;
    			case Port.ROT_RIGHT:
    				if ( bump.ports[0].value )
    					status |= 0x0004;	// right sensor
    				break;
    			case Port.ROT_UP:
    				if ( bump.ports[0].value )
    					status |= 0x0010;	// top sensor
    				break;
    			case Port.ROT_DOWN:
    				if ( bump.ports[0].value )
    					status |= 0x0040;	// bottom sensor
    				break;
    			}
    		}
    	}

		//status &= ~0x0100;	// antenna in
		//status &= ~0x0400;	// claw in
    	
    	return status;
    }
    
    public boolean Function() {
    	if ( memory.length > 0 )
    	{
		    com.droidquest.program.ProcessorMSP430 processor = new com.droidquest.program.ProcessorMSP430();
	    	processor.Reset( memory, (short)100 );
	    	processor.GetRegisters()[15] = this.getStatus();
	    	processor.Run();
	    	
	    	memory = processor.GetMemory();
	    	
	    	short status = processor.GetRegisters()[15];

	    	for(Device device : robot.devices)
	    	{
	    		if (device instanceof Thruster)
	    		{
	    			Thruster thrust = (Thruster)device;
	    			switch ( thrust.Rotation() )
	    			{
	    			case Port.ROT_LEFT:
	    				thrust.ports[0].value = ( (status & 0x0002) != 0 );	// left thruster
	    				break;
	    			case Port.ROT_RIGHT:
	    				thrust.ports[0].value = ( (status & 0x0008) != 0 );	// right thruster
	    				break;
	    			case Port.ROT_UP:
	    				thrust.ports[0].value = ( (status & 0x0020) != 0 );		// top thruster
	    				break;
	    			case Port.ROT_DOWN:
	    				thrust.ports[0].value = ( (status & 0x0080) != 0 );	// bottom thruster
	    				break;
	    			}
	    		}
	    	}
	    	
			//robot.broadcasting = ( (status & 0x0200) != 0 );		// antenna out
			// ( (status & 0x0800) != 0 );	// claw out
    	}

        return false;
    }

    public void Erase() {
        super.Erase();
        robot = null;
    }
    
    public boolean CanBePickedUp(Item i) {
        if (i != level.player) {
            return false;
        }
        
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(level.roomdisplay);
        DialogEditorASM editor = new DialogEditorASM(parentFrame, code, this.getStatus());
        Dimension editorSize = new Dimension( 600, 600 );
        editor.setSize( editorSize );
		Dimension parentSize = parentFrame.getSize(); 
		Point p = parentFrame.getLocation(); 
		editor.setLocation(p.x + parentSize.width / 2 /*- editorSize.width / 2*/, p.y + parentSize.height / 2 + editorSize.height / 2);
	    editor.setVisible(true);

	    String code = editor.getCode();

	    CompilerASM compiler = new CompilerASM( code, 100 );
	    try {
	    	memory = compiler.Compile();
		}
	    catch (CompilerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 

        //activeProgram = !activeProgram;
        //if (activeProgram) {
            currentIcon = icons[1].getImage();
        //}
        //else {
        //    currentIcon = icons[0].getImage();
        //}
        return false;
    }
}
