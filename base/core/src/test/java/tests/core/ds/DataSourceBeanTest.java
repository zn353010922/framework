/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tests.core.ds;

import leap.core.annotation.Inject;
import leap.core.ds.DataSourceManager;
import leap.core.junit.AppTestBase;
import org.junit.Test;

import javax.sql.DataSource;

public class DataSourceBeanTest extends AppTestBase {

    private @Inject DataSource        ds;
    private @Inject DataSourceManager dsm;
	
	@Test
	public void testTomcatJdbcDataSourceBean() {
	
		DataSource ds = factory.getBean("tomcatJdbcDataSource");
		
		assertTrue(ds instanceof org.apache.tomcat.jdbc.pool.DataSource);
		
	}

    @Test
    public void testDefaultDataSourceBean() {
        assertNotNull(ds);
        assertSame(ds, dsm.getDefaultDataSource());

    }

}
