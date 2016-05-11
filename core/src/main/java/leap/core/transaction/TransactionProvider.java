/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leap.core.transaction;

import leap.lang.exception.NestedSQLException;
import leap.lang.jdbc.ConnectionCallback;
import leap.lang.jdbc.ConnectionCallbackWithResult;

import java.sql.Connection;

public interface TransactionProvider {

    /**
     * Actives a new transaction with the default definition.
     */
    Transaction activeTransaction();

    /**
     * Actives a new transaction with the given definition.
     */
    Transaction activeTransaction(TransactionDefinition td);

    /**
     * Executes the callback.
     *
     * <p/>
     * If an active transaction is exist, the callback will be executed in the transaction.
     */
    void execute(ConnectionCallback callback);

    /**
     * Executes the callback.
     *
     * <p/>
     * If an active transaction is exists, the callback will be executed in the transaction.
     */
    <T> T executeWithResult(ConnectionCallbackWithResult<T> callback);

	/**
	 * Executes the callback in a currently active transaction or a new one if no active transaction.
	 */
	void doTransaction(TransactionCallback callback);
	
	/**
	 * Executes the callback in a currently active transaction or a new one if no active transaction.
	 */
	<T> T doTransaction(TransactionCallbackWithResult<T> callback);
	
	/**
	 * Executes the callback in a currently active transaction or a new one if no active transaction.
	 * 
	 * <p>
	 * If <code>requiresNew</code> is <code>true</code>, a new transaction will be created.
	 */
	void doTransaction(TransactionCallback callback, boolean requiresNew);
	
	/**
	 * Executes the callback in a currently active transaction or a new one if no active transaction.
	 * 
	 * <p>
	 * If <code>requiresNew</code> is <code>true</code>, a new transaction will be created.
	 */
	<T> T doTransaction(TransactionCallbackWithResult<T> callback, boolean requiresNew);
	
	/**
	 * Executes the callback in a currently active transaction or a new one if no active transaction.
	 */
	void doTransaction(TransactionCallback callback, TransactionDefinition td);
	
	/**
	 * Executes the callback in a currently active transaction or a new one if no active transaction.
	 */
	<T> T doTransaction(TransactionCallbackWithResult<T> callback, TransactionDefinition td);
	
	/**
	 * Obtain a Connection from this {@link TransactionProvider}.
	 */
	Connection getConnection() throws NestedSQLException;
	
	/**
	 * Close the given {@link Connection} which obained from this {@link TransactionProvider}.
	 * 
	 * Returns <code>true</code> if the given connection is closed by this {@link TransactionProvider}.
	 * 
	 * <p>
	 * Returns <code>false</code> if the given connection not closed by this {@link TransactionProvider}.
	 */
	boolean closeConnection(Connection connection);
	
}