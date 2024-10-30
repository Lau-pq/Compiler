package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Objects;
import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    private SymbolTable symbolTable;
    private Stack<Symbol> symbolStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        // do nothing
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        switch (production.index()) {
            case 4 -> { // S -> D id
                Symbol tokenId = symbolStack.pop();
                Symbol nonTerminalD = symbolStack.pop();
                symbolTable.get(tokenId.getToken().getText()).setType(nonTerminalD.getType());
                Symbol nonTerminalS = new Symbol(production.head());
                symbolStack.push(nonTerminalS);
            }
            case 5 -> { // D -> int
                Symbol tokenInt = symbolStack.pop();
                Symbol nonTerminalD = new Symbol(production.head());
                nonTerminalD.setType(tokenInt.getType());
                symbolStack.push(nonTerminalD);
            }
            default -> {
                for (Term term: production.body()){
                    symbolStack.pop();
                }
                symbolStack.push(new Symbol(production.head()));
            }
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
        Symbol currentSymbol = new Symbol(currentToken);
        if (Objects.equals(currentToken.getKindId(), "int")) {
            currentSymbol.setType(SourceCodeType.Int);
        }
        symbolStack.push(currentSymbol);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        this.symbolTable = table;
    }
}

