package com.droidquest.program;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//
// <index_r>   ::= 'R' <NUM> | 'PC' | 'SP'
// <prm_out>   ::= 'R' <NUM> | <NUM> '(' <index_r> ')' | '&' <ID> | '@' 'R' <NUM>
// <prm_in>    ::= <prm_out> | '@' 'R' <NUM> '+' | '#' <NUM>
// <op_single> ::= <single_cmd> <prm_in>
// <op_double> ::= <double_cmd> <prm_in> <prm_out>
// <op_jump>   ::= <jump_cmd> <ID>
// <op_code>   ::= <op_single> | <op_double> | <op_jump>
// <line_text> ::= [<ID> ':'] <op_code>
// <word_def>  ::= '.word' <NUM>+
// <array_def> ::= '.array' <NUM>
// <io_def>    ::= '.ls' | '.lt' | '.rs' | '.rt' | '.ts' | '.tt' | '.bs' | '.bt' | '.ain' | '.aout' | '.cin' | '.cout' | '.bat'    
// <line_data> ::= [<ID> ':'] ( <word_def> | <array_def> | <io_def> )
//
public class CompilerASM
{	
	public class CompilerException extends Exception
	{
		public CompilerException(int line, int at, String msg)
		{
			super( msg );
			fLine = line + 1;
			fAt = at;
		}
		
		public int fLine;
		public int fAt;
	};
	
	public class DebugInfo
	{
		public Map<Integer, Integer>	memAddress2LineNum = new HashMap<Integer, Integer>();
	}
	
	public CompilerASM(String code, int stackSize)
	{
		fCode = code;
		fStackSize = stackSize;
	}
	
	public short[] Compile() throws CompilerException
	{	
		fDebugInfo.memAddress2LineNum.clear();
		
		try
		{
			fOutput.write( new byte[fStackSize*2] );
		}
		catch (IOException e)
		{
			throw new CompilerException( fCurrentLine, fCurrentAt, "Internal error 'allocating stack size'" );
		}
				
		String[] lines = fCode.split("\n");
		for (int i=0; i<lines.length; i++)
		{
			String line = lines[i];
			int at = line.indexOf('/');
			if ( at != -1 )
				line = line.substring( 0, at );
			
			line = line.trim();
			
			fCurrentLine = i;
			fCurrentAt = 0;
			this.skipWhite( line );
			
			int memAddress = fOutput.size() / 2;
			fDebugInfo.memAddress2LineNum.put( memAddress, fCurrentLine );
			this.skipWhite( line );
		
			if ( fCurrentAt < line.length() )
				this.parse_line( line );
			
			if ( memAddress > 0xFFFF )
				throw new CompilerException( fCurrentLine, 0, "Processor mmeory exceeded. No more memory available." );
		}

		
		// output into a short array
		byte[] bytes = fOutput.toByteArray();
		short[] memory = new short[bytes.length/2];
		ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(memory);
		
		// and process the labels mapping them to the memory
		for(LabelDataJump label : fLabelsLinkJump)
		{
			if ( fLabel2AddressMap.containsKey( label.fLabel) )
			{
				int labelMemLoc = fLabel2AddressMap.get( label.fLabel );
				if ( labelMemLoc < fOutput.size() && (labelMemLoc/2) < 0x03FF)
				{
					int memLoc = label.fMemLocation / 2;

					labelMemLoc /= 2;
					int jumpOffset = (labelMemLoc - memLoc) & 0x03FF;
					jumpOffset--; // compensate for the jump operand
					
					memory[memLoc] |= (short)jumpOffset; 
				}
				else
					throw new CompilerException( label.fLine, label.fAt, "The jump operator cannot jump that far in memory. There is 10-bit limit on the offset." );
			}
			else
				throw new CompilerException( label.fLine, label.fAt, "Label is not defined: " + label.fLabel );
		}
		
		for(LabelDataParam label : fLabelsLinkParam)
		{
			if ( fLabel2AddressMap.containsKey( label.fLabel) )
			{
				int labelMemLoc = fLabel2AddressMap.get( label.fLabel );
				if ( labelMemLoc < fOutput.size() && (labelMemLoc/2) < 0xFFFF)
				{
					int memLoc = label.fMemLocation / 2;
					memLoc += 1;
					if ( label.fSecondWord )
						memLoc += 1;
					labelMemLoc /= 2;
					
					if ( label.fByOffset )
					{
						int memOffset = (labelMemLoc - memLoc);
						//if ( labelMemLoc < memLoc )
						//	memOffset--;
						memory[memLoc] = (short)memOffset;
					}
					else					
						memory[memLoc] = (short)labelMemLoc;
				}
				else
					throw new CompilerException( label.fLine, label.fAt, "internal error LabelDataJump" );
			}
			else
				throw new CompilerException( label.fLine, label.fAt, "Label is not defined: " + label.fLabel );
		}
		
		return memory;
	}
	
	public DebugInfo GetDebugInfo()
	{
		return fDebugInfo;
	}
	
	public void PrintUnitTestAsserts() throws CompilerException
	{
		{
			final short kAssertStackSize = 5;
			
			CompilerASM compiler = new CompilerASM(fCode, kAssertStackSize);
			short[] program = compiler.Compile();
			ProcessorMSP430	processor = new ProcessorMSP430();
			processor.Reset(program, kAssertStackSize);
			processor.Run();
		
			System.out.println("	{ // ");		
			System.out.print  ("		CompilerASM compiler = new CompilerASM(\"");
			System.out.print  (new String(fCode).replace("\n", "\\n").replace("\"","\\\""));
			System.out.print  ("\", ");
			System.out.print  (Integer.toString(kAssertStackSize));
			System.out.println(");");
			System.out.println("		short[] program = compiler.Compile();");
			System.out.println("		ProcessorMSP430 processor = new ProcessorMSP430();");
			System.out.println("		processor.Reset(program, (short)" + Integer.toString(kAssertStackSize) + ");");
			System.out.println("		processor.Run();");
			System.out.print  ("		short savedReg[] = new short[] {");
			for(int i=0; i<processor.GetRegisters().length; i++)
				System.out.print((i!=0?",":"") + Short.toString(processor.GetRegisters()[i]));
			System.out.println("};");
			System.out.println("		assert processor.GetRegisters().length == savedReg.length;");
			System.out.println("		for(int i=0; i<savedReg.length; i++)");
			System.out.println("			assert processor.GetRegisters()[i] == savedReg[i];");
			System.out.print  ("		short savedMem[] = new short[] {");
			for(int i=0; i<processor.GetMemory().length; i++)
				System.out.print((i!=0?",":"") + Short.toString(processor.GetMemory()[i]));
			System.out.println("};");
			System.out.println("		assert processor.GetMemory().length == savedMem.length;");
			System.out.println("		for(int i=0; i<savedMem.length; i++)");
			System.out.println("			assert processor.GetMemory()[i] == savedMem[i];");
			System.out.println("	};");
		}
	}
	
	private void skipWhite(String line)
	{
		while ( fCurrentAt < line.length() && Character.isWhitespace( line.charAt(fCurrentAt) ) )
			fCurrentAt++;
	}
	
	private String getID(String line)
	{
		String result = null;
		char ch = fCurrentAt < line.length() ? line.charAt( fCurrentAt ) : ' ';
		if ( fCurrentAt < line.length() && (ch == '_' || Character.isLetter( ch ) ) )
		{
			result = "";
			do
			{
				result += line.charAt( fCurrentAt );
				fCurrentAt++;
				if ( fCurrentAt >= line.length() )
					break;
				ch = line.charAt( fCurrentAt );
			} while ( ch == '_' || Character.isLetterOrDigit( ch ) );
		}
		
		return result;
	}
	
	private Integer getNUM(String line)
	{
		Integer result = null;
		if ( fCurrentAt < line.length() )
		{
			boolean isHEX = false;
			boolean isBin = false;
			
			char ch = line.charAt( fCurrentAt );
			if ( ch == '0' && (fCurrentAt + 1) < line.length() )
			{
				if ( line.charAt(fCurrentAt+1) == 'x' )
				{
					fCurrentAt++;
					isHEX = true;
				}
				else if ( line.charAt(fCurrentAt+1) == 'b')
				{
					fCurrentAt++;
					isBin = true;
				}
				else
				{
					for(int i=fCurrentAt+2; i<line.length(); i++)
					{
						char chDel = line.charAt(i);
						if ( chDel == 'h' )
						{
							isHEX = true;
							break;
						}
						else if ( ! Character.isDigit( chDel ) && !(chDel >= 'A' && chDel <= 'F') && !(chDel >= 'a' && chDel <= 'f'))
							break;
					}
				}
			}
			
			while (    (! isBin && Character.isDigit( ch ))
					|| (isHEX && ch >= 'A' && ch <= 'F')
					|| (isHEX && ch >= 'a' && ch <= 'f')
					|| (isBin && (ch == '0' || ch == '1'))
					)
			{
				if ( result == null )
					result = 0;
				
				if ( isHEX )
					result *= 16;
				else if ( isBin )
					result *= 2;
				else
					result *= 10;
				result += Character.getNumericValue( ch );
				fCurrentAt++;
				if ( fCurrentAt >= line.length() )
					break;
				
				ch = line.charAt( fCurrentAt );
			}
			
			if ( isHEX && ch == 'h' )
				fCurrentAt++;
		}
		return result;
	}
	
	private void parse_line_add_data(String line, int num) throws CompilerException
	{
		DataOutputStream os = new DataOutputStream( fOutput );
		try
		{
			os.writeShort( num );
		}
		catch (IOException e)
		{
			throw new CompilerException( fCurrentLine, fCurrentAt, "Internal error 'parse_line_data'" );
		}		
	}
	
	private void parse_line(String line) throws CompilerException
	{
		String id = this.getID( line );
		if ( id == null )
			throw new CompilerException( fCurrentLine, fCurrentAt, "expecing operand or label id." );
		
		char ch = fCurrentAt < line.length() ? line.charAt( fCurrentAt ) : ' ';
		if ( ch == ':' )
		{
			Integer currAddress = new Integer( fOutput.size() );
			fLabel2AddressMap.put( id, currAddress );
			
			fCurrentAt++;
			this.skipWhite( line );

			ch = fCurrentAt < line.length() ? line.charAt( fCurrentAt ) : ' ';
			boolean specialKeyword = ( ch == '.');
			if ( specialKeyword )
				fCurrentAt++;
			
			id = this.getID( line );
			
			if ( id != null && specialKeyword )
				id = "." + id;
		}
		
		if ( id.compareToIgnoreCase( ".word" ) == 0 )
		{
			while ( fCurrentAt < line.length() )
			{
				this.skipWhite( line );
				Integer num = this.getNUM( line );
				if ( num == null )
					break;
				this.parse_line_add_data( line, num );				
			}
		}
		else if ( id.compareToIgnoreCase( ".array" ) == 0 )
		{
			this.skipWhite( line );
			Integer num = this.getNUM( line );
			if ( num != null )
			{
				for(int i=0; i<num; i++)
					this.parse_line_add_data( line, 0 );
			}
		}
		else if ( NONE_OPERATORS.containsKey( id ) )
		{
			OperatorData cmd = NONE_OPERATORS.get( id );
			this.parse_none( line, cmd );
		}
		else if ( SINGLE_OPERATORS.containsKey( id ) )
		{
			OperatorData cmd = SINGLE_OPERATORS.get( id );
			this.parse_single( line, cmd );
		}
		else if ( DOUBLE_OPERATORS.containsKey( id ) )
		{
			OperatorData cmd = DOUBLE_OPERATORS.get( id );
			this.parse_double( line, cmd );
		}
		else if ( JUMP_OPERATORS.containsKey( id ) )
		{
			OperatorData cmd = JUMP_OPERATORS.get( id );
			this.parse_jump( line, cmd );
		}
		else
			throw new CompilerException( fCurrentLine, fCurrentAt, "Unknown operator: " + id );
	}
	
	private boolean parse_byteMarker(String line, boolean supportByte) throws CompilerException
	{
		boolean opByte = false;
		
		char ch = fCurrentAt < line.length() ? line.charAt( fCurrentAt ) : ' ';
		if ( ch == '.' )
		{
			if ( supportByte )
			{
				fCurrentAt++;
				ch = line.charAt( fCurrentAt );
				if ( ch != 'B' || ch != 'b' )
					throw new CompilerException( fCurrentLine, fCurrentAt, "This operand supports only .B modifier" );
				fCurrentAt++;
				opByte = true;
			}
			else
				throw new CompilerException( fCurrentLine, fCurrentAt, "This operand doesn't support .B modifier" );
		}
		
		return opByte;
	}
	
	private void parse_none(String line, OperatorData cmd) throws CompilerException
	{
		boolean opByte = this.parse_byteMarker( line, cmd.fSupportByte );

		this.skipWhite( line );

		short opCode = ProcessorMSP430.MakeSingleOperand( 
				cmd.fProcessorCode,
				opByte,
				ProcessorMSP430.EOpMode.Register,
				0
				);
		
		this.add_output( opCode, null, null );
	}
	
	private void parse_single(String line, OperatorData cmd) throws CompilerException
	{
		boolean opByte = this.parse_byteMarker( line, cmd.fSupportByte );

		this.skipWhite( line );
		ParamData src = this.parse_prm( line, true, false );
		
		short opCode = ProcessorMSP430.MakeSingleOperand( 
				cmd.fProcessorCode,
				opByte,
				src.opMode,
				src.Rn
				);
		
		this.add_output( opCode, src.x, null );
	}
	
	private void parse_double(String line, OperatorData cmd) throws CompilerException
	{
		boolean opByte = this.parse_byteMarker( line, cmd.fSupportByte );

		this.skipWhite( line );
		ParamData src = this.parse_prm( line, true, false );
		
		this.skipWhite( line );
		
		char ch = line.charAt( fCurrentAt );
		if ( ch != ',' )
			throw new CompilerException( fCurrentLine, fCurrentAt, "Expecting ',' between parameters' );" );
		
		fCurrentAt++;
		this.skipWhite( line );
		
		ParamData dst = this.parse_prm( line, false, src.x != null );
		
		short opCode = ProcessorMSP430.MakeDualOperand( 
				cmd.fProcessorCode,
				opByte,
				src.opMode,
				src.Rn,
				dst.opMode,
				dst.Rn
				);
		
		this.add_output( opCode, src.x, dst.x );
	}
	
	private void parse_jump(String line, OperatorData cmd) throws CompilerException
	{
		this.skipWhite( line );
		//if ( line.charAt( fCurrentAt ) != '#' )
		//	throw new CompilerException( fCurrentLine, fCurrentAt, "Expecting '#<ID>' of the label after a jump command" );		
		//fCurrentAt++;
		
		String label = this.getID( line );
		if ( label == null )
			throw new CompilerException( fCurrentLine, fCurrentAt, "Expecting '#<ID>' of the label after a jump command" );
		
		int memoryLoc = fOutput.size();
		
		this.add_output( cmd.fProcessorCode, null, null );
		
		LabelDataJump labelData = new LabelDataJump();
		labelData.fLabel = label;
		labelData.fMemLocation = memoryLoc;
		labelData.fLine = fCurrentLine;
		labelData.fAt = fCurrentAt;
		fLabelsLinkJump.add( labelData );
	}
	
	private void add_output(short opCode, Integer x1, Integer x2) throws CompilerException
	{
		DataOutputStream os = new DataOutputStream( fOutput );
		try 
		{
			os.writeShort( opCode );
			if ( x1 != null )
				os.writeShort( x1 );
			if ( x2 != null )
				os.writeShort( x2 );
		}
		catch (IOException e)
		{
			throw new CompilerException( fCurrentLine, fCurrentAt, "Internal error 'parse_single'" );
		}		
	}

	private class ParamData
	{
		public ProcessorMSP430.EOpMode 	opMode;
		public int 						Rn;
		public Integer					x = null;
	};
	
	private ParamData parse_prm(String line, boolean firstParam, boolean isSecondWord) throws CompilerException
	{
		ParamData result = null;
		
		char ch = fCurrentAt < line.length() ? line.charAt( fCurrentAt )  : ' ';
		if ( Character.isDigit( ch ) )
		{
			Integer x = this.getNUM( line );
			if ( x != null )
			{
				ch = line.charAt( fCurrentAt );
				if ( ch != '(' )
					throw new CompilerException( fCurrentLine, fCurrentAt, "Expecting '(' for the indexing operation. X(Rn), X(PC), X(SP) expected.");
				fCurrentAt++;
				ch = line.charAt( fCurrentAt );
				fCurrentAt++;
				if ( ch == 'R' )
				{
					Integer num = this.getNUM( line );
					if ( num == null || ! (num >= 0 && num <= 15) )
						throw new CompilerException( fCurrentLine, fCurrentAt-1, "Expected register number from 0 to 15");
					
					result = new ParamData();
					result.x = new Integer(x);
					result.Rn = num;
					result.opMode = ProcessorMSP430.EOpMode.Indexed;
				}
				else if ( ch == 'P' && (fCurrentAt+1) < line.length() && line.charAt(fCurrentAt+1) == 'C' )
				{
					result = new ParamData();
					result.x = new Integer(x);
					result.Rn = 0;
					result.opMode = ProcessorMSP430.EOpMode.Indexed;
				}
				else if ( ch == 'S' && (fCurrentAt+1) < line.length() && line.charAt(fCurrentAt+1) == 'P' )
				{
					result = new ParamData();
					result.x = new Integer(x);
					result.Rn = 1;
					result.opMode = ProcessorMSP430.EOpMode.Indexed;
				}
				else if ( ch == 'S' && (fCurrentAt+1) < line.length() && line.charAt(fCurrentAt+1) == 'R' )
				{
					result = new ParamData();
					result.x = new Integer(x);
					result.Rn = 2;
					result.opMode = ProcessorMSP430.EOpMode.Indexed;
				}
				else
					throw new CompilerException( fCurrentLine, fCurrentAt-1, "Unknown operand of the indexing operation. X(Rn), X(PC), X(SP) expected.");

				ch = line.charAt( fCurrentAt );
				fCurrentAt++;
				if ( ch != ')' )
					throw new CompilerException( fCurrentLine, fCurrentAt, "Expecting ')' for the indexing operation. X(Rn), X(PC), X(SP) expected.");
			}
		}
		else if ( ch == '&' )
		{
			fCurrentAt++;
			String label = this.getID( line );
			
			LabelDataParam labelData = new LabelDataParam();
			labelData.fLabel = label;
			labelData.fByOffset = false;
			labelData.fSecondWord = isSecondWord;
			labelData.fMemLocation = fOutput.size();
			labelData.fLine = fCurrentLine;		
			labelData.fAt = fCurrentAt;
			fLabelsLinkParam.add( labelData );

			result = new ParamData();
			result.x = new Integer(0);
			result.Rn = 2;
			result.opMode = ProcessorMSP430.EOpMode.Indexed;
		}
		else if ( ch == '@' )
		{
			fCurrentAt++;
			ch = line.charAt( fCurrentAt );
			fCurrentAt++;
			if ( ch == 'R' || ch == 'S' || ch == 'P' )
			{
				int num = 0;
				if ( ch == 'R')
				{
					Integer intNum = this.getNUM( line );
					if ( intNum == null )
						throw new CompilerException( fCurrentLine, fCurrentAt-1, "Expected register number from 0 to 15");
					num = intNum;
				}
				else
				{
					char ch1 = line.charAt( fCurrentAt );
					fCurrentAt++;
					if ( ch == 'S' && ch1 == 'P' )
						num = 1;
					else if ( ch == 'P' || ch1 == 'C' )
						num = 0;
					else if ( ch == 'S' && ch1 == 'R' )
						num = 2;
					else
						throw new CompilerException( fCurrentLine, fCurrentAt-1, "Expected PC or SP as a register name");
				}
				
				if ( ! (num >= 0 && num <= 15) )
					throw new CompilerException( fCurrentLine, fCurrentAt-1, "Expected register number from 0 to 15");
				
				ch = fCurrentAt < line.length() ? line.charAt( fCurrentAt ) : ' ';
				if ( ch == '+' )
				{
					fCurrentAt++;
					if ( firstParam )
					{
						result = new ParamData();
						result.Rn = num;
						result.opMode = ProcessorMSP430.EOpMode.IndirectInc;
					}
					else
					{
						throw new CompilerException( fCurrentLine, fCurrentAt-1, "The Indirect parameter @Rn+ cannot be used as dst of on operation");
					}
				}
				else
				{
					result = new ParamData();
					result.Rn = num;
					result.opMode = ProcessorMSP430.EOpMode.Indirect;
				}
			}
			else
				throw new CompilerException( fCurrentLine, fCurrentAt-1, "Expected Indirect parameter: @Rn or @Rn+");
		}
		else if ( ch == '#' )
		{
			fCurrentAt++;
			if ( firstParam )
			{
				ch = fCurrentAt < line.length() ? line.charAt( fCurrentAt )  : ' ';
				if ( Character.isDigit( ch ) )
				{
					Integer x = this.getNUM( line );
				
					result = new ParamData();
					result.x = x;
					result.Rn = 0;
					result.opMode = ProcessorMSP430.EOpMode.IndirectInc;
				}
				else
				{
					String label = this.getID( line );
					if ( label != null && label.length() > 0 )
					{
						if ( CONSTANTS.containsKey( label ) )
						{
							result = new ParamData();
							result.x = new Integer( CONSTANTS.get( label ) );
							result.Rn = 0;
							result.opMode = ProcessorMSP430.EOpMode.IndirectInc;
						}
						else
						{
							LabelDataParam labelData = new LabelDataParam();
							labelData.fLabel = label;
							labelData.fByOffset = false;
							labelData.fSecondWord= isSecondWord;
							labelData.fMemLocation = fOutput.size();
							labelData.fLine = fCurrentLine;		
							labelData.fAt = fCurrentAt;
							fLabelsLinkParam.add( labelData );
			
							result = new ParamData();
							result.x = new Integer(0);
							result.Rn = 0;
							result.opMode = ProcessorMSP430.EOpMode.IndirectInc;
						}
					}
					else
						throw new CompilerException( fCurrentLine, fCurrentAt, "'#' expects number or label");
				}
			}
			else
				throw new CompilerException( fCurrentLine, fCurrentAt, "'#' is not supported for dst parameter");
		}
		else
		{
			String label = this.getID( line );
			if ( label != null && label.length() > 0 )
			{
				if ( label.charAt(0) == 'R' )
				{
					try
					{
						int len = label.length();
						int num = Integer.parseInt(label.substring(1) );
						if ( (len == 2 && num >= 0 && num <= 9) || (len == 3 && num >= 10 && num <= 15) )
						{
							result = new ParamData();
							result.Rn = num;
							result.opMode = ProcessorMSP430.EOpMode.Register;
						}
					}
					catch(NumberFormatException e)
					{
					}
				}
				else if ( label.equals( "PC" ) )
				{
					result = new ParamData();
					result.Rn = 0;
					result.opMode = ProcessorMSP430.EOpMode.Register;
				}
				else if ( label.equals( "SP" ) )
				{
					result = new ParamData();
					result.Rn = 1;
					result.opMode = ProcessorMSP430.EOpMode.Register;
				}
				else if ( label.equals( "SR" ) )
				{
					result = new ParamData();
					result.Rn = 2;
					result.opMode = ProcessorMSP430.EOpMode.Register;
				}
				
				if ( result == null )
				{
					LabelDataParam labelData = new LabelDataParam();
					labelData.fLabel = label;
					labelData.fByOffset = true;
					labelData.fSecondWord= isSecondWord;
					labelData.fMemLocation = fOutput.size();
					labelData.fLine = fCurrentLine;		
					labelData.fAt = fCurrentAt;
					fLabelsLinkParam.add( labelData );
	
					result = new ParamData();
					result.x = new Integer(0);
					result.Rn = 0;
					result.opMode = ProcessorMSP430.EOpMode.Indexed;
				}
			}
		}

		if ( result == null )
			throw new CompilerException( fCurrentLine, fCurrentAt, "Expecting operand parameter: Rn, X(Rn), LABEL, &LABEL, @Rn, @Rn+, #NUM");

		return result;
	}

	private int fStackSize;
	private String fCode;
	private int fCurrentLine = 0;
	private int fCurrentAt = 0;
	private ByteArrayOutputStream	fOutput = new ByteArrayOutputStream();
	private Map<String, Integer>	fLabel2AddressMap = new HashMap<String, Integer>();
	
	private class LabelDataJump
	{
		public String fLabel;
		public int    fMemLocation;
		public int    fLine;
		public int    fAt;
	};
		
	private class LabelDataParam
	{
		public String 	fLabel;
		public boolean	fByOffset;
		public boolean  fSecondWord;
		public int      fMemLocation;
		public int      fLine;		
		public int      fAt;
	}

	private ArrayList<LabelDataJump>		fLabelsLinkJump  = new ArrayList<LabelDataJump>();
	private ArrayList<LabelDataParam>		fLabelsLinkParam = new ArrayList<LabelDataParam>();
	
	private DebugInfo	fDebugInfo = new DebugInfo();
	
	private class LineData
	{
		public int lineNum;
		public String line;
		public LineData(int lineNum, String line)
		{
			this.lineNum = lineNum;
			this.line = line;
		}
	}
	
	private static class OperatorData
	{
		public boolean 	fSupportByte;
		public short	fProcessorCode;
		public OperatorData(boolean supportByte, short code)
		{
			fSupportByte = supportByte;
			fProcessorCode = code;
		}
	};
		
	private static final Map<String, OperatorData> NONE_OPERATORS;
	private static final Map<String, OperatorData> SINGLE_OPERATORS;
	private static final Map<String, OperatorData> DOUBLE_OPERATORS;
	private static final Map<String, OperatorData> JUMP_OPERATORS;

	private static final Map<String, Short> CONSTANTS;
	
	static {
		NONE_OPERATORS  = new HashMap<String, OperatorData>(20);
		SINGLE_OPERATORS = new HashMap<String, OperatorData>(20);
		DOUBLE_OPERATORS = new HashMap<String, OperatorData>(20);
		JUMP_OPERATORS = new HashMap<String, OperatorData>(20);
		CONSTANTS = new HashMap<String, Short>(20);

		NONE_OPERATORS.put("RETI", new OperatorData( false, ProcessorMSP430.RETI ) );
				
		SINGLE_OPERATORS.put("RRC",  new OperatorData( true , ProcessorMSP430.RRC  ) );
		SINGLE_OPERATORS.put("RRA",  new OperatorData( true , ProcessorMSP430.RRA  ) );
		SINGLE_OPERATORS.put("PUSH", new OperatorData( true , ProcessorMSP430.PUSH ) );
		SINGLE_OPERATORS.put("SWPB", new OperatorData( false, ProcessorMSP430.SWPB ) );
		SINGLE_OPERATORS.put("CALL", new OperatorData( false, ProcessorMSP430.CALL ) );
		SINGLE_OPERATORS.put("SXT",  new OperatorData( false, ProcessorMSP430.SXT  ) );

		DOUBLE_OPERATORS.put("MOV",  new OperatorData( true, ProcessorMSP430.MOV  ) );
		DOUBLE_OPERATORS.put("ADD",  new OperatorData( true, ProcessorMSP430.ADD  ) );
		DOUBLE_OPERATORS.put("ADDC", new OperatorData( true, ProcessorMSP430.ADDC ) );
		DOUBLE_OPERATORS.put("SUB",  new OperatorData( true, ProcessorMSP430.SUB  ) );
		DOUBLE_OPERATORS.put("SUBC", new OperatorData( true, ProcessorMSP430.SUBC ) );
		DOUBLE_OPERATORS.put("CMP",  new OperatorData( true, ProcessorMSP430.CMP  ) );
		DOUBLE_OPERATORS.put("DADD", new OperatorData( true, ProcessorMSP430.DADD ) );
		DOUBLE_OPERATORS.put("BIT",  new OperatorData( true, ProcessorMSP430.BIT  ) );
		DOUBLE_OPERATORS.put("BIC",  new OperatorData( true, ProcessorMSP430.BIC  ) );
		DOUBLE_OPERATORS.put("BIS",  new OperatorData( true, ProcessorMSP430.BIS  ) );
		DOUBLE_OPERATORS.put("XOR",  new OperatorData( true, ProcessorMSP430.XOR  ) );
		DOUBLE_OPERATORS.put("AND",  new OperatorData( true, ProcessorMSP430.AND  ) );

		JUMP_OPERATORS.put("JEQ", new OperatorData( false, ProcessorMSP430.JEQ ) );
		JUMP_OPERATORS.put("JZ",  new OperatorData( false, ProcessorMSP430.JEQ ) );
		JUMP_OPERATORS.put("JNE", new OperatorData( false, ProcessorMSP430.JNE ) );
		JUMP_OPERATORS.put("JNZ", new OperatorData( false, ProcessorMSP430.JNE ) );
		JUMP_OPERATORS.put("JC",  new OperatorData( false, ProcessorMSP430.JC  ) );
		JUMP_OPERATORS.put("JNC", new OperatorData( false, ProcessorMSP430.JNC ) );
		JUMP_OPERATORS.put("JN",  new OperatorData( false, ProcessorMSP430.JN  ) );
		JUMP_OPERATORS.put("JGE", new OperatorData( false, ProcessorMSP430.JGE ) );
		JUMP_OPERATORS.put("JL",  new OperatorData( false, ProcessorMSP430.JL  ) );
		JUMP_OPERATORS.put("JMP", new OperatorData( false, ProcessorMSP430.JMP ) );
		
		CONSTANTS.put( "LS", (short)0x0001 );
		CONSTANTS.put( "LT", (short)0x0002 );
		CONSTANTS.put( "RS", (short)0x0004 );
		CONSTANTS.put( "RT", (short)0x0008 );
		CONSTANTS.put( "TS", (short)0x0010 );
		CONSTANTS.put( "TT", (short)0x0020 );
		CONSTANTS.put( "BS", (short)0x0040 );
		CONSTANTS.put( "BT", (short)0x0080 );
		CONSTANTS.put( "AI", (short)0x0100 );
		CONSTANTS.put( "AO", (short)0x0200 );
		CONSTANTS.put( "CI", (short)0x0400 );
		CONSTANTS.put( "CO", (short)0x0800 );
	}
}
