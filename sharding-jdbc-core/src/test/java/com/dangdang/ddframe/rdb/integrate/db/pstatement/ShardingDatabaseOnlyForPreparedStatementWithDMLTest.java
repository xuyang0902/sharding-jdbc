/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
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
 * </p>
 */

package com.dangdang.ddframe.rdb.integrate.db.pstatement;

import com.dangdang.ddframe.rdb.integrate.db.AbstractShardingDatabaseOnlyDBUnitTest;
import com.dangdang.ddframe.rdb.integrate.sql.DatabaseTestSQL;
import com.dangdang.ddframe.rdb.sharding.constant.SQLType;
import com.dangdang.ddframe.rdb.sharding.jdbc.core.datasource.ShardingDataSource;
import org.dbunit.DatabaseUnitException;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class ShardingDatabaseOnlyForPreparedStatementWithDMLTest extends AbstractShardingDatabaseOnlyDBUnitTest {
    
    private ShardingDataSource shardingDataSource;
    
    private DatabaseTestSQL sql;
    
    @Before
    public void init() throws SQLException {
        shardingDataSource = getShardingDataSource();
        sql = currentDatabaseTestSQL();
    }
    
    @Test
    public void assertInsertWithAllPlaceholders() throws SQLException, DatabaseUnitException {
        for (int i = 1; i <= 10; i++) {
            try (Connection connection = shardingDataSource.getConnection()) {
                PreparedStatement preparedStatement = connection.prepareStatement(sql.getInsertWithAllPlaceholdersSql());
                preparedStatement.setInt(1, i);
                preparedStatement.setInt(2, i);
                preparedStatement.setString(3, "insert");
                preparedStatement.executeUpdate();
            }
        }
        assertDataSet("insert", "insert");
    }
    
    @Test
    public void assertInsertWithAutoIncrementColumn() throws SQLException, DatabaseUnitException {
        try (Connection connection = shardingDataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(String.format(sql.getInsertWithAutoIncrementColumnSql(), "?", "?"))) {
            for (int i = 1; i <= 10; i++) {
                preparedStatement.setInt(1, i);
                preparedStatement.setString(2, "insert");
                preparedStatement.executeUpdate();
            }
        }
        assertDataSet("insert", "insert");
    }
    
    @Test
    public void assertInsertWithoutPlaceholder() throws SQLException, DatabaseUnitException {
        for (int i = 1; i <= 10; i++) {
            try (Connection connection = shardingDataSource.getConnection()) {
                PreparedStatement preparedStatement = connection.prepareStatement(String.format(sql.getInsertWithoutPlaceholderSql(), i, i));
                preparedStatement.executeUpdate();
            }
        }
        assertDataSet("insert", "insert");
    }
    
    @Test
    public void assertInsertWithPartialPlaceholders() throws SQLException, DatabaseUnitException {
        for (int i = 1; i <= 10; i++) {
            try (Connection connection = shardingDataSource.getConnection()) {
                PreparedStatement preparedStatement = connection.prepareStatement(String.format(sql.getInsertWithPartialPlaceholdersSql(), i, i));
                preparedStatement.setString(1, "insert");
                preparedStatement.executeUpdate();
            }
        }
        assertDataSet("insert", "insert");
    }
    
    @Test
    public void assertUpdateWithoutAlias() throws SQLException, DatabaseUnitException {
        for (int i = 10; i < 30; i++) {
            for (int j = 0; j < 2; j++) {
                try (Connection connection = shardingDataSource.getConnection()) {
                    PreparedStatement preparedStatement = connection.prepareStatement(String.format(sql.getUpdateWithoutAliasSql(), "?", "?", "?"));
                    preparedStatement.setString(1, "updated");
                    preparedStatement.setInt(2, i * 100 + j);
                    preparedStatement.setInt(3, i);
                    assertThat(preparedStatement.executeUpdate(), is(1));
                }
            }
        }
        assertDataSet("update", "updated");
    }
    
    @Test
    public void assertUpdateWithAlias() throws SQLException, DatabaseUnitException {
        if (isAliasSupport()) {
            for (int i = 10; i < 30; i++) {
                for (int j = 0; j < 2; j++) {
                    try (Connection connection = shardingDataSource.getConnection()) {
                        PreparedStatement preparedStatement = connection.prepareStatement(sql.getUpdateWithAliasSql());
                        preparedStatement.setString(1, "updated");
                        preparedStatement.setInt(2, i * 100 + j);
                        preparedStatement.setInt(3, i);
                        assertThat(preparedStatement.executeUpdate(), is(1));
                    }
                }
            }
            assertDataSet("update", "updated");
        }
    }
    
    @Test
    public void assertUpdateWithoutShardingValue() throws SQLException, DatabaseUnitException {
        try (Connection connection = shardingDataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(String.format(sql.getUpdateWithoutShardingValueSql(), "?", "?"));
            preparedStatement.setString(1, "updated");
            preparedStatement.setString(2, "init");
            assertThat(preparedStatement.executeUpdate(), is(40));
        }
        assertDataSet("update", "updated");
    }
    
    @Test
    public void assertDeleteWithoutAlias() throws SQLException, DatabaseUnitException {
        for (int i = 10; i < 30; i++) {
            for (int j = 0; j < 2; j++) {
                try (Connection connection = shardingDataSource.getConnection()) {
                    PreparedStatement preparedStatement = connection.prepareStatement(String.format(sql.getDeleteWithoutAliasSql(), "?", "?", "?"));
                    preparedStatement.setInt(1, i * 100 + j);
                    preparedStatement.setInt(2, i);
                    preparedStatement.setString(3, "init");
                    assertThat(preparedStatement.executeUpdate(), is(1));
                }
            }
        }
        assertDataSet("delete", "init");
    }
    
    @Test
    public void assertDeleteWithoutShardingValue() throws SQLException, DatabaseUnitException {
        try (Connection connection = shardingDataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(String.format(sql.getDeleteWithoutShardingValueSql(), "?"));
            preparedStatement.setString(1, "init");
            assertThat(preparedStatement.executeUpdate(), is(40));
        }
        assertDataSet("delete", "init");
    }
    
    private void assertDataSet(final String expectedDataSetPattern, final String status) throws SQLException, DatabaseUnitException {
        for (int i = 0; i < 10; i++) {
            assertDataSet(String.format("integrate/dataset/db/expect/%s/db_%s.xml", expectedDataSetPattern, i), 
                    shardingDataSource.getConnection().getConnection(String.format("dataSource_db_%s", i), SQLType.SELECT), "t_order", String.format(sql.getAssertSelectWithStatusSql(), "?"), status);
        }
    }
}