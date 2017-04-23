package com.droidquest.program;


public class UnitTestCompilerProcessor
{

	public UnitTestCompilerProcessor()
	{
	}
	
	public void Execute()
	{
		boolean assertOn = false;
		// *assigns* true if assertions are on.
		assert assertOn = true; 
		if ( assertOn )
		{
			try  // Unit Tests
			{
				this.RunTests();
			}
			catch (CompilerASM.CompilerException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void RunTests() throws CompilerASM.CompilerException
	{
		{ // Registers
			CompilerASM compiler = new CompilerASM("		JMP GO\nRS:		.word 0x1234\nLS:		.word 0xFFFF\nARR:	.array 2\nGO:		MOV #0xFEAA, R4\n		MOV R4, R5\n		MOV #RS, R6\n		MOV &RS, R7\n		MOV #ARR, R8\n		MOV @R6+, 0(R8)\n		MOV @R6, 1(R8)\n		MOV 0(R8), R9\n		MOV 1(R8), R10\n		MOV ARR, R11\n		RETI", 5);
			short[] program = compiler.Compile();
			ProcessorMSP430 processor = new ProcessorMSP430();
			processor.Reset(program, (short)5);
			processor.Run();
			short savedReg[] = new short[] {30,5,0,0,-342,-342,7,4660,8,4660,-1,4660,0,0,0,0};
			assert processor.GetRegisters().length == savedReg.length;
			for(int i=0; i<savedReg.length; i++)
				assert processor.GetRegisters()[i] == savedReg[i];
			short savedMem[] = new short[] {0,0,0,0,0,15364,4660,-1,4660,-1,16436,-342,17413,16438,6,16919,6,16440,8,18104,0,18088,1,18457,0,18458,1,16411,-20,4864};
			assert processor.GetMemory().length == savedMem.length;
			for(int i=0; i<savedMem.length; i++)
				assert processor.GetMemory()[i] == savedMem[i];
		};
		{ // CALL
			CompilerASM compiler = new CompilerASM("	JMP GO\nF1:	MOV #1, R4\n	MOV @SP+,PC // RET\nF2:	MOV #2, R5\n	MOV @SP+,PC // RET\nF3:	MOV #3, R6\n	MOV @SP+,PC // RET\nF4:	MOV #4, R7\n	MOV @SP+,PC // RET\nF5:	MOV #5, R8\n	MOV @SP+,PC // RET\nF6:	MOV #6, R9\n	MOV @SP+,PC // RET\nF7:	MOV #7, R10\n	MOV @SP+,PC // RET\nF8:	MOV #8, R11\n	MOV @SP+,PC // RET\nGO:	CALL #F1\n	MOV #F2, R15\n	CALL R15\n	MOV #F3, D3\n	MOV #D3, R14\n	CALL @R14\n	MOV #F4, &D4\n	MOV #F5, &D5\n	MOV #F6, &D6\n	MOV #D4, R13\n	CALL @R13+\n	CALL @R13+\n	CALL @R13\n	MOV #F7, &D7\n	MOV #F8, &D8\n	CALL 1(R13)\n	CALL 2(R13)\n	RETI\nD3:	.word 0\nD4:	.word 0\nD5:	.word 0\nD6:	.word 0\nD7:	.word 0\nD8:	.word 0", 5);
			short[] program = compiler.Compile();
			ProcessorMSP430 processor = new ProcessorMSP430();
			processor.Reset(program, (short)5);
			processor.Run();
			short savedReg[] = new short[] {72,5,0,0,1,2,3,4,5,6,7,8,0,69,66,9};
			assert processor.GetRegisters().length == savedReg.length;
			for(int i=0; i<savedReg.length; i++)
				assert processor.GetRegisters()[i] == savedReg[i];
			short savedMem[] = new short[] {0,0,0,0,65,15384,16436,1,16688,16437,2,16688,16438,3,16688,16439,4,16688,16440,5,16688,16441,6,16688,16442,7,16688,16443,8,16688,4784,6,16447,9,4751,16560,12,29,16446,66,4782,16562,15,67,16562,18,68,16562,21,69,16445,67,4797,4797,4781,16562,24,70,16562,27,71,4765,1,4765,2,4864,12,15,18,21,24,27};
			assert processor.GetMemory().length == savedMem.length;
			for(int i=0; i<savedMem.length; i++)
				assert processor.GetMemory()[i] == savedMem[i];
		};
		{ // Single Commands
			CompilerASM compiler = new CompilerASM("	PUSH #09876h\n	MOV #0b10110, R4	// 0x16\n	PUSH R4\n	MOV @SP+,R10\n	MOV @SP+,R11\n	MOV #05555h, R5\n	RRA R5			// 0x2aaa\n	MOV #0AAAAh, R6\n	RRC R6 			// 0xD555\n	RRC R6 			// 0x6aaa\n	MOV #040BFh, R7\n	SWPB R7			// 0xbf40\n	MOV #0x80, R8\n	SXT R8			// 0xff80\n	MOV #0x08, R9\n	SXT R9			// 0x0080\n	RETI", 5);
			short[] program = compiler.Compile();
			ProcessorMSP430 processor = new ProcessorMSP430();
			processor.Reset(program, (short)5);
			processor.Run();
			short savedReg[] = new short[] {29,5,1,0,22,10922,27306,-16576,-128,8,22,-26506,0,0,0,0};
			assert processor.GetRegisters().length == savedReg.length;
			for(int i=0; i<savedReg.length; i++)
				assert processor.GetRegisters()[i] == savedReg[i];
			short savedMem[] = new short[] {0,0,0,22,-26506,4656,-26506,16436,22,4612,16698,16699,16437,21845,4357,16438,-21846,4102,4102,16439,16575,4231,16440,128,4488,16441,8,4489,4864};
			assert processor.GetMemory().length == savedMem.length;
			for(int i=0; i<savedMem.length; i++)
				assert processor.GetMemory()[i] == savedMem[i];
		};
		
	}
}
