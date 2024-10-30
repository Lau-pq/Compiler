package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.NonTerminal;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;

import java.util.Objects;

public class Symbol {
    private Token token;
    private NonTerminal nonTerminal;
    private SourceCodeType type;

    private Symbol(Token token, NonTerminal nonTerminal){
        this.token = token;
        this.nonTerminal = nonTerminal;
        this.type = null;
    }

    public Symbol(Token token){
        this(token, null);
    }

    public Symbol(NonTerminal nonTerminal){
        this(null, nonTerminal);
    }

    public boolean isToken(){
        return this.token != null;
    }

    public boolean isNonterminal(){
        return this.nonTerminal != null;
    }

    public void setType(SourceCodeType type) {
        this.type = type;
    }

    public SourceCodeType getType() {
        return this.type;
    }

    public Token getToken() {
        return this.token;
    }
}
