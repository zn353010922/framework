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
package leap.orm.command;

import java.util.List;

import leap.lang.Error;

public interface DmoCommand {

	/**
	 * returns <code>true</code> if this command executed successfully.
	 * 
	 * @throws IllegalStateException if this command not executed.
	 */
	boolean success() throws IllegalStateException;
	
	/**
	 * returns <code>true</code> if this command has been executed.
	 */
	boolean executed();
	
	/**
	 * returns a list of execution errors.
	 * 
	 * <p>
	 * every time invoking this method will creates a new {@link List} contains the errors.
	 * 
	 * @throws IllegalStateException if this command not executed.
	 */
	List<? extends Error> errors();
	
	/**
	 * Execute this command.
	 * 
	 * <p>
	 * Return <code>true</code> if no error(s).
	 */
	boolean execute();
}
