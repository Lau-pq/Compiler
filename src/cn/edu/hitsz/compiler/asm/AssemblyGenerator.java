package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    private List<Instruction> instructions = new ArrayList<>();
    private BMap<IRVariable, Register> registerMap = new BMap<>();
    private Map<IRVariable, Integer> lastIndexMap = new HashMap<>();
    private List<String> assemblyCodes = new ArrayList<>(List.of(".text"));

    enum Register {
        t0, t1, t2, t3, t4, t5, t6
    }

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        for (Instruction instruction: originInstructions) {
            InstructionKind instructionKind = instruction.getKind();
            if (instructionKind.isBinary()) {
                IRValue lhs = instruction.getLHS();
                IRValue rhs = instruction.getRHS();
                IRVariable result = instruction.getResult();
                if (instruction.haveTwoImmediate()) {
                    int immediateResult;
                    int lhsVal = ((IRImmediate) lhs).getValue();
                    int rhsVal = ((IRImmediate) rhs).getValue();
                    switch (instructionKind) {
                        case ADD -> immediateResult = lhsVal + rhsVal;
                        case SUB -> immediateResult = lhsVal - rhsVal;
                        case MUL -> immediateResult = lhsVal * rhsVal;
                        default -> throw new RuntimeException("Unsupported instruction kind");
                    }
                    instructions.add(Instruction.createMov(result, IRImmediate.of(immediateResult)));
                } else if (instruction.haveOneImmediate() && instructionKind == InstructionKind.MUL) {
                    IRVariable temp = IRVariable.temp();
                    IRValue immediate = instruction.lhsIsImmediate() ? lhs : rhs;
                    instructions.add(Instruction.createMov(temp, immediate));
                    instructions.add(Instruction.createMul(result, temp, immediate));
                } else if (instruction.lhsIsImmediate() && instructionKind == InstructionKind.SUB) {
                    IRVariable temp = IRVariable.temp();
                    instructions.add(Instruction.createMov(temp, lhs));
                    instructions.add(Instruction.createSub(result, temp, rhs));
                } else if (instruction.haveOneImmediate()) {
                    IRValue immediate;
                    IRValue variable;
                    if (instruction.lhsIsImmediate()) {
                        immediate = lhs;
                        variable = rhs;
                    } else {
                        immediate = rhs;
                        variable = lhs;
                    }
                    switch (instructionKind) {
                        case ADD -> instructions.add(Instruction.createAdd(result, variable, immediate));
                        case SUB -> instructions.add(Instruction.createSub(result, variable, immediate));
                        default -> throw new RuntimeException("Unsupported instruction kind");
                    }
                } else {
                    instructions.add(instruction);
                }
            } else if (instructionKind.isUnary()) {
                instructions.add(instruction);
            } else if (instructionKind.isReturn()) {
                instructions.add(instruction);
                break;
            } else throw new RuntimeException("Unsupported instruction kind");
        }

        int instructionIndex = 0;
        for (Instruction instruction: instructions) {
            var operands = instruction.getOperands();
            for (IRValue operand: operands) {
                if (operand.isIRVariable()) {
                    lastIndexMap.put((IRVariable) operand, instructionIndex);
                }
            }
            instructionIndex ++;
        }

    }

    private void registerAllocation(IRValue irValue, int instructionIndex) {
        if (irValue.isImmediate()) return;
        IRVariable variable = (IRVariable) irValue;
        if (registerMap.containsKey(variable)) return;
        for (Register register: Register.values()) {
            if (!registerMap.containsValue(register)) {
                registerMap.replace(variable, register);
                return;
            }
        }

        for (Register register: Register.values()) {
            if (instructionIndex > lastIndexMap.get(registerMap.getByValue(register))) {
                registerMap.replace(variable, register);
                return;
            }
        }

        throw new RuntimeException("no available registers");

    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        int instructionIndex = 0;
        String assemblyCode = null;
        for (Instruction instruction: instructions) {
            InstructionKind instructionKind = instruction.getKind();
            switch (instructionKind) {
                case ADD -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    IRVariable result = instruction.getResult();
                    registerAllocation(lhs, instructionIndex);
                    registerAllocation(rhs, instructionIndex);
                    registerAllocation(result, instructionIndex);
                    Register lhsReg = registerMap.getByKey((IRVariable) lhs);
                    Register resultReg = registerMap.getByKey(result);
                    if(instruction.rhsIsImmediate()) {
                        assemblyCode = String.format("\taddi %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhs);
                    } else {
                        Register rhsReg = registerMap.getByKey((IRVariable) rhs);
                        assemblyCode = String.format("\tadd %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhsReg.toString());
                    }
                }
                case SUB -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    IRVariable result = instruction.getResult();
                    registerAllocation(lhs, instructionIndex);
                    registerAllocation(rhs, instructionIndex);
                    registerAllocation(result, instructionIndex);
                    Register lhsReg = registerMap.getByKey((IRVariable) lhs);
                    Register resultReg = registerMap.getByKey(result);
                    if(instruction.rhsIsImmediate()) {
                        assemblyCode = String.format("\tsubi %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhs);
                    } else {
                        Register rhsReg = registerMap.getByKey((IRVariable) rhs);
                        assemblyCode = String.format("\tsub %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhsReg.toString());
                    }
                }
                case MUL -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    IRVariable result = instruction.getResult();
                    registerAllocation(lhs, instructionIndex);
                    registerAllocation(rhs, instructionIndex);
                    registerAllocation(result, instructionIndex);
                    Register lhsReg = registerMap.getByKey((IRVariable) lhs);
                    Register rhsReg = registerMap.getByKey((IRVariable) rhs);
                    Register resultReg = registerMap.getByKey(result);
                    assemblyCode = String.format("\tmul %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhsReg.toString());
                }
                case MOV -> {
                    IRVariable result = instruction.getResult();
                    IRValue from = instruction.getFrom();
                    registerAllocation(result, instructionIndex);
                    registerAllocation(from, instructionIndex);
                    Register resultReg = registerMap.getByKey(result);
                    if (from.isImmediate()) {
                        assemblyCode = String.format("\tli %s, %s", resultReg.toString(), from);
                    } else {
                        Register fromReg = registerMap.getByKey((IRVariable) from);
                        assemblyCode = String.format("\tmv %s, %s", resultReg.toString(), fromReg.toString());
                    }
                }
                case RET -> {
                    IRValue returnValue = instruction.getReturnValue();
                    Register returnValueReg = registerMap.getByKey((IRVariable) returnValue);
                    assemblyCode = String.format("\tmv a0, %s", returnValueReg.toString());
                }
            }
            assemblyCode += "\t\t#  %s".formatted(instruction.toString());
            assemblyCodes.add(assemblyCode);
            instructionIndex ++;

            if (instructionKind.isReturn()) {
                break;
            }
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, assemblyCodes);
    }
}

