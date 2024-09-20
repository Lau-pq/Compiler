package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.text.CharacterIterator;
import java.util.ArrayList;
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
        int nowState = 0;
        while(position < sourceCodeBuffer.length()) {
            char ch = sourceCodeBuffer.charAt(position);
            switch (nowState) {
                case 0 -> {
                    if (Character.isWhitespace(ch)) {
                        position++;
                    } else if (isLetterOrUnderscore(ch)) {
                        nowState = 1;
                    } else if (Character.isDigit(ch)) {
                        nowState = 2;
                    } else {
                        nowState = 3;
                    }
                }
                case 1 -> {
                    processString();
                    nowState = 0;
                }
                case 2 -> {
                    processNumber();
                    nowState = 0;
                }
                case 3 -> {
                    processSymbol();
                    nowState = 0;
                }
            }
        }
        tokens.add(Token.eof());
    }

    private void processString() {
        StringBuilder wordbuilder = new StringBuilder();
        while(position < sourceCodeBuffer.length()){
            char ch = sourceCodeBuffer.charAt(position);
            if (isLetterOrDigitOrUnderscore(ch)) {
                wordbuilder.append(ch);
                position++;
            } else {
                break;
            }
        }
        String word = wordbuilder.toString();
        if (TokenKind.isAllowed(word)){
            tokens.add(Token.simple(TokenKind.fromString(word)));
        } else {
            tokens.add(Token.normal(TokenKind.fromString("id"), word));
            System.out.println(word);
            if (!symbolTable.has(word)) {
                symbolTable.add(word);
            }
        }
    }

    private void processNumber() {
        StringBuilder numberbuilder = new StringBuilder();
        while(position < sourceCodeBuffer.length()){
            char ch = sourceCodeBuffer.charAt(position);
            if (Character.isDigit(ch)) {
                numberbuilder.append(ch);
                position++;
            } else {
                break;
            }
        }
        tokens.add(Token.normal(TokenKind.fromString("IntConst"), numberbuilder.toString()));
    }

    private void processSymbol() {
        StringBuilder symbolbuilder = new StringBuilder();
        char ch = sourceCodeBuffer.charAt(position);
        if (ch == ';') {
            tokens.add(Token.simple(TokenKind.fromString("Semicolon")));
        } else {
            tokens.add(Token.simple(TokenKind.fromString(String.valueOf(ch))));
        }
        position++;
    }

    private boolean isLetterOrUnderscore(char ch) {
        return Character.isLetter(ch) || ch == '_';
    }

    private boolean isLetterOrDigitOrUnderscore(char ch){
        return Character.isLetterOrDigit(ch) || ch == '_';
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
