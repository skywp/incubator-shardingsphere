/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.parse.old.lexer;

import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.constant.DatabaseType;
import org.apache.shardingsphere.core.parse.antlr.sql.statement.SQLStatement;
import org.apache.shardingsphere.core.parse.old.lexer.dialect.h2.H2Lexer;
import org.apache.shardingsphere.core.parse.old.lexer.dialect.mysql.MySQLLexer;
import org.apache.shardingsphere.core.parse.old.lexer.dialect.oracle.OracleLexer;
import org.apache.shardingsphere.core.parse.old.lexer.dialect.postgresql.PostgreSQLLexer;
import org.apache.shardingsphere.core.parse.old.lexer.dialect.sqlserver.SQLServerLexer;
import org.apache.shardingsphere.core.parse.old.lexer.token.Assist;
import org.apache.shardingsphere.core.parse.old.lexer.token.Symbol;
import org.apache.shardingsphere.core.parse.old.lexer.token.Token;
import org.apache.shardingsphere.core.parse.old.lexer.token.TokenType;
import org.apache.shardingsphere.core.parse.old.parser.exception.SQLParsingException;
import org.apache.shardingsphere.core.parse.old.parser.exception.SQLParsingUnsupportedException;

import java.util.Set;

/**
 * Lexical analysis engine.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class LexerEngine {
    
    private final Lexer lexer;
    
    /**
     * Get input string.
     * 
     * @return inputted string
     */
    public String getInput() {
        return lexer.getInput();
    }
    
    /**
     * Analyse next token.
     */
    public void nextToken() {
        lexer.nextToken();
    }
    
    /**
     * Is end or not.
     *
     * @return current token is end token or not.
     */
    public boolean isEnd() {
        return Assist.END == lexer.getCurrentToken().getType();
    }
    
    /**
     * Get current token.
     * 
     * @return current token
     */
    public Token getCurrentToken() {
        return lexer.getCurrentToken();
    }
    
    /**
     * skip all tokens that inside parentheses.
     *
     * @param sqlStatement SQL statement
     * @return skipped string
     */
    public String skipParentheses(final SQLStatement sqlStatement) {
        StringBuilder result = new StringBuilder("");
        int count = 0;
        if (Symbol.LEFT_PAREN == lexer.getCurrentToken().getType()) {
            final int beginPosition = lexer.getCurrentToken().getEndPosition();
            result.append(Symbol.LEFT_PAREN.getLiterals());
            lexer.nextToken();
            while (true) {
                if (equalAny(Symbol.QUESTION)) {
                    sqlStatement.setParametersIndex(sqlStatement.getParametersIndex() + 1);
                }
                if (Assist.END == lexer.getCurrentToken().getType() || (Symbol.RIGHT_PAREN == lexer.getCurrentToken().getType() && 0 == count)) {
                    break;
                }
                if (Symbol.LEFT_PAREN == lexer.getCurrentToken().getType()) {
                    count++;
                } else if (Symbol.RIGHT_PAREN == lexer.getCurrentToken().getType()) {
                    count--;
                }
                lexer.nextToken();
            }
            result.append(lexer.getInput().substring(beginPosition, lexer.getCurrentToken().getEndPosition()));
            lexer.nextToken();
        }
        return result.toString();
    }
    
    /**
     * Assert current token type should equals input token and go to next token type.
     *
     * @param tokenType token type
     */
    public void accept(final TokenType tokenType) {
        if (lexer.getCurrentToken().getType() != tokenType) {
            throw new SQLParsingException(lexer, tokenType);
        }
        lexer.nextToken();
    }
    
    /**
     * Judge current token equals one of input tokens or not.
     *
     * @param tokenTypes to be judged token types
     * @return current token equals one of input tokens or not
     */
    public boolean equalAny(final TokenType... tokenTypes) {
        for (TokenType each : tokenTypes) {
            if (each == lexer.getCurrentToken().getType()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Skip current token if equals one of input tokens.
     *
     * @param tokenTypes to be judged token types
     * @return skipped current token or not
     */
    public boolean skipIfEqual(final TokenType... tokenTypes) {
        if (equalAny(tokenTypes)) {
            lexer.nextToken();
            return true;
        }
        return false;
    }
    
    /**
     * Skip all input tokens.
     *
     * @param tokenTypes to be skipped token types
     */
    public void skipAll(final TokenType... tokenTypes) {
        Set<TokenType> tokenTypeSet = Sets.newHashSet(tokenTypes);
        while (tokenTypeSet.contains(lexer.getCurrentToken().getType())) {
            lexer.nextToken();
        }
    }
    
    /**
     * Skip until one of input tokens.
     *
     * @param tokenTypes to be skipped untiled token types
     */
    public void skipUntil(final TokenType... tokenTypes) {
        Set<TokenType> tokenTypeSet = Sets.newHashSet(tokenTypes);
        tokenTypeSet.add(Assist.END);
        while (!tokenTypeSet.contains(lexer.getCurrentToken().getType())) {
            lexer.nextToken();
        }
    }
    
    /**
     * Throw unsupported exception if current token equals one of input tokens.
     * 
     * @param tokenTypes to be judged token types
     */
    public void unsupportedIfEqual(final TokenType... tokenTypes) {
        if (equalAny(tokenTypes)) {
            throw new SQLParsingUnsupportedException(lexer.getCurrentToken().getType());
        }
    }
    
    /**
     * Throw unsupported exception if current token not equals one of input tokens.
     *
     * @param tokenTypes to be judged token types
     */
    public void unsupportedIfNotSkip(final TokenType... tokenTypes) {
        if (!skipIfEqual(tokenTypes)) {
            throw new SQLParsingUnsupportedException(lexer.getCurrentToken().getType());
        }
    }
    
    /**
     * Get database type.
     * 
     * @return database type
     */
    public DatabaseType getDatabaseType() {
        if (lexer instanceof H2Lexer) {
            return DatabaseType.H2;
        }
        if (lexer instanceof MySQLLexer) {
            return DatabaseType.MySQL;
        }
        if (lexer instanceof OracleLexer) {
            return DatabaseType.Oracle;
        }
        if (lexer instanceof SQLServerLexer) {
            return DatabaseType.SQLServer;
        }
        if (lexer instanceof PostgreSQLLexer) {
            return DatabaseType.PostgreSQL;
        }
        throw new UnsupportedOperationException(String.format("Cannot support lexer class: %s", lexer.getClass().getCanonicalName()));
    }
}
