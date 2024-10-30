package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private Stack<Symbol> symbolStack = new Stack<>();
    private final List<Instruction> IRList = new ArrayList<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        Symbol currentSymbol = new Symbol(currentToken);
        if (Objects.equals(currentToken.getKindId(), "IntConst")) {
            currentSymbol.setVal(IRImmediate.of(Integer.parseInt(currentToken.getText())));
        } else {
            currentSymbol.setVal(IRVariable.named(currentToken.getText()));
        }
        symbolStack.push(currentSymbol);
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        Symbol currentNonTerminal = new Symbol(production.head());
        currentNonTerminal.setVal(IRVariable.named(production.head().getTermName()));
        Symbol result, from, lhs, op, rhs;
        switch (production.index()) {
            case 6 -> { // S -> id = E
                from = symbolStack.pop();
                symbolStack.pop();
                result = symbolStack.pop();
                IRList.add(Instruction.createMov((IRVariable) result.getVal(), from.getVal()));
            }
            case 7 -> { // S -> return E
                result = symbolStack.pop();
                symbolStack.pop();
                IRList.add(Instruction.createRet(result.getVal()));
            }
            case 8, 9, 11 -> { // E -> E + A
                rhs = symbolStack.pop();
                op = symbolStack.pop();
                lhs = symbolStack.pop();
                currentNonTerminal.setVal(IRVariable.temp());
                IRVariable nonTerminalVal = (IRVariable) currentNonTerminal.getVal();
                IRValue lhsVal = lhs.getVal();
                IRValue rhsVal = rhs.getVal();
                switch (op.getToken().getKindId()) {
                    case "+" -> IRList.add(Instruction.createAdd(nonTerminalVal, lhsVal, rhsVal));
                    case "-" -> IRList.add(Instruction.createSub(nonTerminalVal, lhsVal, rhsVal));
                    case "*" -> IRList.add(Instruction.createMul(nonTerminalVal, lhsVal, rhsVal));
                }
            }
            case 10, 12, 14, 15 -> { // E -> A, A -> B, B -> id, B -> IntConst
                currentNonTerminal.setVal(symbolStack.pop().getVal());
            }
            case 13 -> { // B -> ( E )
                symbolStack.pop();
                currentNonTerminal.setVal(symbolStack.pop().getVal());
                symbolStack.pop();
            }
            default -> {
                for (Term term: production.body()){
                    symbolStack.pop();
                }
            }
        }
        symbolStack.push(currentNonTerminal);
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        // do nothing
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        // do nothing
    }

    public List<Instruction> getIR() {
        // TODO
        return IRList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

