package com.droidquest.program;

public class ProcessorMSP430 {	
	// Single Operand
	public static final short RRC		= (short)0x1000;
	public static final short SWPB		= (short)0x1080;
	public static final short RRA		= (short)0x1100;
	public static final short SXT		= (short)0x1180;
	public static final short PUSH		= (short)0x1200;
	public static final short CALL		= (short)0x1280;
	public static final short RETI		= (short)0x1300;
	
	// Dual Operand
	public static final short MOV		= (short)0x4000;
	public static final short ADD		= (short)0x5000;
	public static final short ADDC		= (short)0x6000;
	public static final short SUB		= (short)0x7000;
	public static final short SUBC		= (short)0x8000;
	public static final short CMP		= (short)0x9000;
	public static final short DADD		= (short)0xA000;
	public static final short BIT		= (short)0xB000;
	public static final short BIC		= (short)0xC000;
	public static final short BIS		= (short)0xD000;
	public static final short XOR		= (short)0xE000;
	public static final short AND		= (short)0xF000;
	
	// Jumps
	public static final short JNE		= (short)0x2000;
	public static final short JEQ		= (short)0x2400;
	public static final short JNC		= (short)0x2800;
	public static final short JC		= (short)0x2C00;
	public static final short JN		= (short)0x3000;
	public static final short JGE		= (short)0x3400;
	public static final short JL		= (short)0x3800;
	public static final short JMP		= (short)0x3C00;
	
	// Tools
	public enum EOpMode { Register, Indexed, Indirect, IndirectInc };
	
	public static short MakeSingleOperand(short op, boolean opByte, EOpMode opMode, int Rn)
	{
		short result = op;
		if ( opByte )
			result &= 0x0040;
		switch ( opMode )
		{
			case Register:		break;
			case Indexed:		result |= 0x0010; break;
			case Indirect:		result |= 0x0020; break;
			case IndirectInc:		result |= 0x0030; break;
		}
		result |= (short)(Rn & 0x0F);
		return result;
	}

	public static short MakeDualOperand(short op, boolean opByte, EOpMode srcMode, int srcRn, EOpMode dstMode, int dstRn)
	{
		short result = op;
		result |= (srcRn & 0x0F) << 8;
		if ( dstMode != EOpMode.Register )
			result |= 0x80;
		if ( opByte )
			result |= 0x40;
		switch ( srcMode )
		{
			case Register:		break;
			case Indexed:		result |= 0x0010; break;
			case Indirect:		result |= 0x0020; break;
			case IndirectInc:	result |= 0x0030; break;
		}
		result |= (dstRn & 0x0F);
		return result;
	}

	// Local functions
	public ProcessorMSP430()
	{
	}
	
	public void Reset(short[] program, short startAddress)
	{
		for(int i=0; i<R.length; i++)
			R[i] = 0;
		
		this.startAddress = startAddress;
		R[0] = startAddress;
		R[1] = startAddress;

		memory = new short[ program.length ];
		System.arraycopy( program, 0, memory, 0, program.length );
	}
	
	public void Run()
	{			
		R[0] = startAddress;
		R[1] = startAddress;
		
		while ( R[0] < memory.length )
		{
			this.StepInternal();
		}
	}
	
	public void Step()
	{
		if ( R[0] < memory.length )
		{
			this.StepInternal();
		}
	}
	
	public short[] GetRegisters()
	{
		return R;
	}

	public boolean IsRunning()
	{
		return R[0] < memory.length;
	}
	
	public short[] GetMemory()
	{
		return memory;
	}
	
	public short GetStartAddress()
	{
		return startAddress;
	}
	
	private void StepInternal()
	{
		short operand = memory[R[0]];
		R[0]++;
		
		// Single Operand
		switch ( (short)(operand & 0xFF80) )
		{
			case RRC:		execute_RRC(operand); break;
			case RRA:		execute_RRA(operand); break;	
			case CALL:		execute_CALL(operand); break;
			case PUSH:		execute_PUSH(operand); break;
			case SWPB:		execute_SWPB(operand); break;
			case SXT:		execute_SXT(operand); break;
			case RETI:		execute_RETI(operand); break;
		}

		// Jump
		switch ( (short)(operand & 0xFC00) )
		{
			case JEQ:		execute_JEQ(operand); break;
			case JNE:		execute_JNE(operand); break;
			case JC:		execute_JC(operand); break;
			case JNC:		execute_JNC(operand); break;
			case JN:		execute_JN(operand); break;
			case JGE:		execute_JGE(operand); break;
			case JL:		execute_JL(operand); break;
			case JMP:		execute_JMP(operand); break;
		}

		// Dual Operand
		switch ( (short)(operand & 0xF000) )
		{
			case MOV:		execute_MOV(operand); break;
			case ADD:		execute_ADD(operand); break;
			case ADDC:		execute_ADDC(operand); break;
			case SUB:		execute_SUB(operand); break;
			case SUBC:		execute_SUBC(operand); break;
			case CMP:		execute_CMP(operand); break;
			case DADD:		execute_DADD(operand); break;
			case BIT:		execute_BIT(operand); break;
			case BIC:		execute_BIC(operand); break;
			case BIS:		execute_BIS(operand); break;
			case XOR:		execute_XOR(operand); break;
			case AND:		execute_AND(operand); break;
		}
	}
	
	public boolean getC() // carry
	{
		return (R[2] & 0x0001) == 0 ? false : true;
	}
	
	private void setC(boolean value) // carry
	{
		if ( value )
			R[2] |= 0x001;
		else
			R[2] &= ~0x001;
	}
	
	public boolean getN() // negative
	{
		return (R[2] & 0x0004) == 0 ? false : true;
	}
	
	private void setN(boolean value) // negative
	{
		if ( value )
			R[2] |= 0x004;
		else
			R[2] &= ~0x004;
	}
	
	public boolean getZ() // zero
	{
		return (R[2] & 0x0002) == 0 ? false : true;
	}
	
	private void setZ(boolean value) // zero
	{
		if ( value )
			R[2] |= 0x002;
		else
			R[2] &= ~0x002;
	}
	
	public boolean getV() // overflow
	{
		return (R[2] & 0x0100) == 0 ? false : true;
	}
	
	private void setV(boolean value) // overflow
	{
		if ( value )
			R[2] |= 0x0100;
		else
			R[2] &= ~0x0100;
	}
	
	private short get_src(short r, short As, boolean isByteOperation, boolean movePC)
	{
		short result = 0;
		
		short x;
		switch ( As )
		{
		case 0:	// Register
			if ( r == 3 )
				result = 0; // take care of the constant register
			else
				result = R[r]; 
			break;
			
		case 1:	// Indexed
			if ( r == 3 )
				result = 1;
			else
			{
				x = memory[R[0]];
				if ( r != 2 )
					x += R[r];
				
				result = memory[x];
				if ( movePC )
					R[0]++;
			}
			break;
			
		case 2:	// Indirect
			if ( r == 2 )
				result = 4;
			else if ( r == 3 )
				result = 2;
			else
			{
				result = memory[R[r]];
				//R[r]++;
			}
			break;
			
		case 3:	// IndirectInc
			if ( r == 2 )
				result = 8;
			else if ( r == 3 )
				result = -1;
			else
			{
				x = memory[R[r]];
				if ( movePC )
					R[r]++;
				result = x;
			}
			break;
		}
		
		if ( isByteOperation )
			result = (short)(result & 0x00FF);
		return result;
	}
	
	private void set_dst(short value, short r, short As, boolean isByteOperation)
	{
		if ( isByteOperation )
			value = (short)(value & 0x00FF);
		
		short x;
		switch ( As )
		{
		case 0:	// Register
			R[r] = value; 
			break;
			
		case 1:	// Indexed
			x = memory[R[0]];
			memory[R[r] + x] = value;
			R[0]++;
			break;
			
		case 2:	// Indirect
			memory[R[r]] = value;
			//R[r]++;
			break;
			
		case 3:	// IndirectInc
			memory[R[r]] = value;
			R[r]++;
		}
	}
	
	// Single Operand
	private short getSingleOperand(short operand, boolean movePC)
	{
		short r = (short)(operand & 0x000F);
		short As = (short)((operand & 0x0030) >> 4);			
		boolean isByteOperation = (operand & 0x0040) != 0;
		
		short result = this.get_src( r, As, isByteOperation, movePC );
		return result;
	}
	
	private void setSingleOperand(short operand, short value)
	{		
		short r = (short)(operand & 0x000F);
		short As = (short)((operand & 0x0030) >> 4);
		boolean isByteOperation = (operand & 0x0040) != 0;
		this.set_dst( value, r, As, isByteOperation );
	}
	
	private void execute_RRC(short operand)
	{
		short dst = this.getSingleOperand( operand, false );
		boolean c = this.getC();
		this.setC( (dst & 0x0001) != 0 );
		dst >>= 1;
		if ( c )
			dst |= 0x8000;
		else
			dst &= ~0x8000;
		this.setSingleOperand( operand, dst );
		this.setN( dst < 0 );
		this.setZ( dst == 0 );
		this.setV( false );
	}
	
	private void execute_RRA(short operand)
	{
		short dst = this.getSingleOperand( operand, false );
		boolean c = (dst & 0x8000) != 0;
		this.setC( (dst & 0x0001) != 0 );
		dst >>= 1;
		if ( c )
			dst |= 0x8000;
		else
			dst &= ~0x8000;
		this.setSingleOperand( operand, dst );
		this.setN( dst < 0 );
		this.setZ( dst == 0 );
		this.setV( false );
	}
	
	private void execute_PUSH(short operand)
	{
		short src = this.getSingleOperand( operand, true );
		R[1]--;
		memory[R[1]] = src;
	}
	
	private void execute_SWPB(short operand)
	{
		short dst = this.getSingleOperand( operand, false );
		byte b1 = (byte)(dst & 0x00FF);
		byte b2 = (byte)(dst >> 8);
		dst = (short)(b2 + (b1 << 8));
		this.setSingleOperand( operand, dst );
	}
	
	private void execute_CALL(short operand)
	{
		short dst = this.getSingleOperand( operand, true );
		R[1]--;
		memory[R[1]] = R[0];
		R[0] = dst;
	}
	
	private void execute_SXT(short operand)
	{
		short dst = this.getSingleOperand( operand, false );
		if ( (dst & 0x0080) != 0 )
		{
			dst = (short)(0xFF00 | (dst & 0x00FF));
		}
		else
		{
			dst = (short)(dst & 0x00FF);
		}
		this.setSingleOperand( operand, dst );
		this.setN( dst < 0 );
		this.setZ( dst == 0 );
		this.setC( dst != 0 );
		this.setV( false );
	}

	private void execute_RETI(short operand)
	{
		// we break program for now with this operand
		R[0] = (short)memory.length;
	}
	
	// Dual Operand
	private short getDoubleOperand_src(short operand)
	{
		short r = (short)((operand >> 8) & 0x000F);
		short As = (short)((operand & 0x0030) >> 4);	
		boolean isByteOperation = (operand & 0x0040) != 0;
		
		short result = this.get_src( r, As, isByteOperation, true );
		return result;
	}
	
	private short getDoubleOperand_dst(short operand, boolean movePC)
	{
		short r = (short)(operand & 0x000F);
		boolean Ad = (operand & 0x0080) != 0;
		boolean isByteOperation = (operand & 0x0040) != 0;
		short As = (short)(Ad ? 1 : 0);
		
		short result = this.get_src( r, As, isByteOperation, movePC );
		return result;
	}
	
	private void setDoubleOperand_dst(short operand, short value)
	{
		short r = (short)(operand & 0x000F);
		boolean Ad = (operand & 0x0080) != 0;
		boolean isByteOperation = (operand & 0x0040) != 0;
		short As = (short)(Ad ? 1 : 0);

		this.set_dst( value, r, As, isByteOperation );		
	}
	
	private void execute_MOV(short operand)
	{
		short src = this.getDoubleOperand_src( operand );
		this.setDoubleOperand_dst( operand, src );
	}
	
	private void execute_ADD(short operand)
	{
		boolean isByteOperation = (operand & 0x0040) != 0;
		short src = this.getDoubleOperand_src( operand );
		short dst = this.getDoubleOperand_dst( operand, false );
		int res = src + dst;
		this.setDoubleOperand_dst( operand, (short)res );
		this.setN( res < 0 );
		this.setZ( res == 0 );
		this.setC( (isByteOperation ? (res & 0x0100) : (res & 0x10000)) != 0 );
		this.setV( isByteOperation ? (res > 0xFF) : (res > 0xFFFF) );
	}
	
	private void execute_ADDC(short operand)
	{
		boolean isByteOperation = (operand & 0x0040) != 0;
		short src = this.getDoubleOperand_src( operand );
		short dst = this.getDoubleOperand_dst( operand, false );
		int c = this.getC() ? 1 : 0;
		int res = src + dst + c;
		this.setDoubleOperand_dst( operand, (short)res );
		this.setN( res < 0 );
		this.setZ( res == 0 );
		this.setC( (isByteOperation ? (res & 0x100) : (res & 0x10000)) != 0 );
		this.setV( isByteOperation ? (res > 0xFF) : (res > 0xFFFF) );
	}
	
	private void execute_SUB(short operand)
	{
		boolean isByteOperation = (operand & 0x0040) != 0;
		short src = this.getDoubleOperand_src( operand );
		short dst = this.getDoubleOperand_dst( operand, false );
		int res = dst - src;
		this.setDoubleOperand_dst( operand, (short)res );
		this.setN( res < 0 );
		this.setZ( res == 0 );
		this.setC( (isByteOperation ? (res & 0x100) : (res & 0x10000)) != 0 );
		this.setV( isByteOperation ? (res > 0xFF) : (res > 0xFFFF) );
	}
	
	private void execute_SUBC(short operand)
	{
		boolean isByteOperation = (operand & 0x0040) != 0;
		short src = this.getDoubleOperand_src( operand );
		short dst = this.getDoubleOperand_dst( operand, false );
		int c = this.getC() ? 1 : 0;
		int res = dst - src - 1 + c;
		this.setDoubleOperand_dst( operand, (short)res );
		this.setN( res < 0 );
		this.setZ( res == 0 );
		this.setC( (isByteOperation ? (res & 0x100) : (res & 0x10000)) != 0 );
		this.setV( isByteOperation ? (res > 0xFF) : (res > 0xFFFF) );
	}
	
	private void execute_CMP(short operand)
	{
		boolean isByteOperation = (operand & 0x0040) != 0;
		short src = this.getDoubleOperand_src( operand );
		short dst = this.getDoubleOperand_dst( operand, true );
		int res = dst - src;
		this.setN( res < 0 );
		this.setZ( res == 0 );
		this.setC( (isByteOperation ? (res & 0x100) : (res & 0x10000)) != 0 );
		this.setV( isByteOperation ? (res > 0xFF) : (res > 0xFFFF) );
	}
	
	private void execute_DADD(short operand)
	{
		boolean isByteOperation = (operand & 0x0040) != 0;
		short src = this.getDoubleOperand_src( operand );
		short dst = this.getDoubleOperand_dst( operand, false );
		short src1 = (short)(src & 0xFF);
		short src2 = (short)(src >> 8);
		short dst1 = (short)(dst & 0xFF);
		short dst2 = (short)(dst >> 8);
		int c = this.getC() ? 1 : 0;
		int res1 = src1 + dst1 + c;
		int res2 = src2 + dst2 + c;
		int res = res1 | (res2 << 8);
		this.setDoubleOperand_dst( operand, (short)res );
		this.setN( res < 0 );
		this.setZ( res == 0 );
		this.setC( (isByteOperation ? (res & 99) : (res & 9999)) != 0 );
		this.setV( isByteOperation ? (res > 0xFF) : (res > 0xFFFF) );
	}
	
	private void execute_BIT(short operand)
	{
		short src = this.getDoubleOperand_src( operand );
		short dst = this.getDoubleOperand_dst( operand, true );
		int res = dst & src;
		this.setN( res < 0 );
		this.setZ( res == 0 );
		this.setC( res != 0 );
		this.setV( false );
	}
	
	private void execute_BIC(short operand)
	{
		short src = this.getDoubleOperand_src( operand );
		short dst = this.getDoubleOperand_dst( operand, false );
		int res = ~src & dst;
		this.setDoubleOperand_dst( operand, (short)res );
	}
	
	private void execute_BIS(short operand)
	{
		short src = this.getDoubleOperand_src( operand );
		short dst = this.getDoubleOperand_dst( operand, false );
		int res = src | dst;
		this.setDoubleOperand_dst( operand, (short)res );
	}
	
	private void execute_XOR(short operand)
	{
		short src = this.getDoubleOperand_src( operand );
		short dst = this.getDoubleOperand_dst( operand, false );
		int res = dst ^ src;
		this.setDoubleOperand_dst( operand, (short)res );
		this.setN( res < 0 );
		this.setZ( res == 0 );
		this.setC( res != 0 );
		this.setV( src < 0 && dst < 0 );
	}
	
	private void execute_AND(short operand)
	{
		short src = this.getDoubleOperand_src( operand );
		short dst = this.getDoubleOperand_dst( operand, false );
		int res = dst & src;
		this.setDoubleOperand_dst( operand, (short)res );
		this.setN( res < 0 );
		this.setZ( res == 0 );
		this.setC( res != 0 );
		this.setV( false );
	}
	
	// Jumps
	private void execute_JEQ(short operand)
	{
		if ( this.getZ() )
		{ 
			short offset = (short)(operand & 0x03FF);
			if ( (offset & 0x0200) != 0) // check sign and calculate negative offset if needed
				offset = (short)(offset - 0x03FF - 1);
			R[0] += offset;
		}
	}
	
	private void execute_JNE(short operand)
	{
		if ( ! this.getZ() )
		{ 
			short offset = (short)(operand & 0x03FF);
			if ( (offset & 0x0200) != 0) // check sign and calculate negative offset if needed
				offset = (short)(offset - 0x03FF - 1);
			R[0] += offset;
		}
	}
	
	private void execute_JC(short operand)
	{
		if ( this.getC() )
		{ 
			short offset = (short)(operand & 0x03FF);
			if ( (offset & 0x0200) != 0) // check sign and calculate negative offset if needed
				offset = (short)(offset - 0x03FF - 1);
			R[0] += offset;
		}
	}
	
	private void execute_JNC(short operand)
	{
		if ( ! this.getC() )
		{ 
			short offset = (short)(operand & 0x03FF);
			if ( (offset & 0x0200) != 0) // check sign and calculate negative offset if needed
				offset = (short)(offset - 0x03FF - 1);
			R[0] += offset;
		}
	}
	
	private void execute_JN(short operand)
	{
		if ( this.getN() )
		{ 
			short offset = (short)(operand & 0x03FF);
			if ( (offset & 0x0200) != 0) // check sign and calculate negative offset if needed
				offset = (short)(offset - 0x03FF - 1);
			R[0] += offset;
		}
	}
	
	private void execute_JGE(short operand)
	{
		if ( ! (this.getN() ^ this.getV()) )
		{ 
			short offset = (short)(operand & 0x03FF);
			if ( (offset & 0x0200) != 0) // check sign and calculate negative offset if needed
				offset = (short)(offset - 0x03FF - 1);
			R[0] += offset;
		}
	}
	
	private void execute_JL(short operand)
	{
		if ( this.getN() ^ this.getV() )
		{ 
			short offset = (short)(operand & 0x03FF);
			if ( (offset & 0x0200) != 0) // check sign and calculate negative offset if needed
				offset = (short)(offset - 0x03FF - 1);
			R[0] += offset;
		}
	}
	
	private void execute_JMP(short operand)
	{
		short offset = (short)(operand & 0x03FF);
		if ( (offset & 0x0200) != 0) // check sign and calculate negative offset if needed
			offset = (short)(offset - 0x03FF - 1);
		R[0] += offset;
	}
	
	private short   startAddress;
	private short[] memory;
	private short[] R = new short[16]; // R0 - PC; R1 - SP; R2 - SR; R3; ...
	
	private static final int DEFAULT_STACK_SIZE	= 100;
}
