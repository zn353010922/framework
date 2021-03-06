/*
 * Copyright 2014 the original author or authors.
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
package leap.orm.query;

import leap.lang.Emptiable;
import leap.lang.value.Page;

import java.util.List;

public interface PageResult<T> extends Emptiable {
	
	/**
	 * Returns current page info for this query.
	 */
	Page getPage();

    /**
     * Returns current page index.
     */
	PageIndex getPageIndex();

    /**
     * Returns the total count of this query.
     */
	long getTotalCount();

    /**
     * Returns the size of result records.
     */
	int size();

    /**
     * Returns the records.
     */
	List<T> list();

}