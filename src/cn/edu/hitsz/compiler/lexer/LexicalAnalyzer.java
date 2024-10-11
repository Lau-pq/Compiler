package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private String sourceCodeBuffer;
    private List<Token> tokens;
    private int position;
    private StringBuilder tokenBuilder;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.tokens = new ArrayList<>();
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        this.sourceCodeBuffer = FileUtils.readFile(path);
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        position = 0;
        State state = State.START;
        tokenBuilder = new StringBuilder();

        while(position < sourceCodeBuffer.length()) {
            char ch = sourceCodeBuffer.charAt(position);

            switch (state) {
                case START -> {
                    if (Character.isWhitespace(ch)) { // 空格
                        position++;
                    } else if (isLetterOrUnderscore(ch)) {
                        state = State.STRING;
                        step(ch);
                    } else if (Character.isDigit(ch)) {
                        state = State.NUMBER;
                        step(ch);
                    } else {
                        state = State.SYMBOL;
                    }
                }
                case STRING -> {
                    if(isLetterOrDigitOrUnderscore(ch)) {
                        step(ch);
                    } else {
                        addString(tokenBuilder.toString());
                        state = State.START;
                        tokenBuilder.setLength(0);
                    }
                }
                case NUMBER -> {
                    if(Character.isDigit(ch)) {
                        step(ch);
                    } else {
                        addNumber(tokenBuilder.toString());
                        state = State.START;
                        tokenBuilder.setLength(0);
                    }
                }
                case SYMBOL -> {
                   addSymbol(ch);
                   state = State.START;
                   position++;
                }
            }
        }

        tokens.add(Token.eof());

    }

    private void step(char ch) {
        tokenBuilder.append(ch);
        position++;
    }

    private void addString(String word) {
        if (TokenKind.isAllowed(word)){
            tokens.add(Token.simple(TokenKind.fromString(word)));
        } else {
            tokens.add(Token.normal(TokenKind.fromString("id"), word));
            if (!symbolTable.has(word)) {
                symbolTable.add(word);
            }
        }
    }

    private void addNumber(String number) {
        tokens.add(Token.normal(TokenKind.fromString("IntConst"), number));
    }

    private void addSymbol(char symbol) {
        if (symbol == ';') {
            tokens.add(Token.simple(TokenKind.fromString("Semicolon")));
        } else {
            tokens.add(Token.simple(TokenKind.fromString(String.valueOf(symbol))));
        }
    }

    private boolean isLetterOrUnderscore(char ch) {
        return Character.isLetter(ch) || ch == '_';
    }

    private boolean isLetterOrDigitOrUnderscore(char ch){
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    private enum State {
        START, STRING, NUMBER, SYMBOL
    }


    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }

}
