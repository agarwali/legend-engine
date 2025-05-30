// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational;

import com.google.common.collect.Iterators;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.finos.legend.engine.plan.execution.result.ResultNormalizer;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResultCSVSerializer;
import org.finos.legend.engine.plan.execution.result.serialization.CsvSerializer;
import org.finos.legend.engine.plan.execution.result.serialization.TemporaryFile;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.IngestionMethod;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommandsVisitor;
import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.TempTableStreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RealizedRelationalResultCSVSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToCSVSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.StreamingTempTableResultCSVSerializer;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StreamResultToTempTableVisitor implements RelationalDatabaseCommandsVisitor<Boolean>
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(StreamResultToTempTableVisitor.class);

    public RelationalExecutionConfiguration config;
    public Connection connection;
    public StreamingResult result;
    public String tableName;
    public String databaseTimeZone;
    public IngestionMethod ingestionMethod;

    public StreamResultToTempTableVisitor(RelationalExecutionConfiguration config, Connection connection, StreamingResult result, String tableName, String databaseTimeZone)
    {
        this.config = config;
        this.connection = connection;
        this.result = result;
        this.tableName = tableName;
        this.databaseTimeZone = databaseTimeZone;
        this.ingestionMethod = null;
    }

    @Override
    public Boolean visit(RelationalDatabaseCommands databaseCommands)
    {
        if (this.ingestionMethod == null)
        {
            this.ingestionMethod = databaseCommands.getDefaultIngestionMethod();
        }

        return this.streamResultToTable(databaseCommands);
    }

    public Boolean streamResultToTable(RelationalDatabaseCommands dbCommands)
    {
        if (ingestionMethod == IngestionMethod.CLIENT_FILE)
        {
            try (TemporaryFile tempFile = new TemporaryFile(config.tempPath))
            {
                CsvSerializer csvSerializer;
                boolean withHeader = dbCommands.supportsHeaderOnCsvFile();
                if (result instanceof RelationalResult)
                {
                    csvSerializer = new RelationalResultToCSVSerializer((RelationalResult) result, withHeader);
                    tempFile.writeFile(csvSerializer);
                    try (Statement statement = connection.createStatement())
                    {
                        statement.execute(dbCommands.dropTempTable(tableName));

                        RelationalResult relationalResult = (RelationalResult) result;

                        if (result.getResultBuilder() instanceof TDSBuilder)
                        {
                            dbCommands.createAndLoadTempTable(tableName, relationalResult.getTdsColumns().stream().map(c -> new Column(c.name, c.relationalType)).collect(Collectors.toList()), tempFile.getTemporaryPathForFile()).forEach(x -> checkedExecute(statement, x));
                        }
                        else
                        {
                            dbCommands.createAndLoadTempTable(tableName, relationalResult.getSQLResultColumns().stream().map(c -> new Column(c.label, c.dataType)).collect(Collectors.toList()), tempFile.getTemporaryPathForFile()).forEach(x -> checkedExecute(statement, x));
                        }
                    }
                }
                else if (result instanceof RealizedRelationalResult)
                {
                    csvSerializer = new RealizedRelationalResultCSVSerializer((RealizedRelationalResult) result, this.databaseTimeZone, withHeader, false);
                    tempFile.writeFile(csvSerializer);
                    try (Statement statement = connection.createStatement())
                    {
                        statement.execute(dbCommands.dropTempTable(tableName));
                        RealizedRelationalResult realizedRelationalResult = (RealizedRelationalResult) result;
                        dbCommands.createAndLoadTempTable(tableName, realizedRelationalResult.columns.stream().map(c -> new Column(c.label, c.dataType)).collect(Collectors.toList()), tempFile.getTemporaryPathForFile()).forEach(x -> checkedExecute(statement, x));
                    }
                }
                else if (result instanceof StreamingObjectResult)
                {
                    csvSerializer = new StreamingObjectResultCSVSerializer((StreamingObjectResult) result, withHeader);
                    tempFile.writeFile(csvSerializer);
                    try (Statement statement = connection.createStatement())
                    {
                        statement.execute(dbCommands.dropTempTable(tableName));
                        dbCommands.createAndLoadTempTable(tableName, csvSerializer.getHeaderColumnsAndTypes().stream().map(c -> new Column(c.getOne(), RelationalExecutor.getRelationalTypeFromDataType(c.getTwo()))).collect(Collectors.toList()), tempFile.getTemporaryPathForFile()).forEach(x -> checkedExecute(statement, x));
                    }
                }
                else if (result instanceof TempTableStreamingResult)
                {
                    csvSerializer = new StreamingTempTableResultCSVSerializer((TempTableStreamingResult) result, withHeader);
                    tempFile.writeFile(csvSerializer);
                    try (Statement statement = connection.createStatement())
                    {
                        statement.execute(dbCommands.dropTempTable(tableName));
                        dbCommands.createAndLoadTempTable(tableName, csvSerializer.getHeaderColumnsAndTypes().stream().map(c -> new Column(c.getOne(), RelationalExecutor.getRelationalTypeFromDataType(c.getTwo()))).collect(Collectors.toList()), tempFile.getTemporaryPathForFile()).forEach(x -> checkedExecute(statement, x));
                    }
                }
                else
                {
                    throw new RuntimeException("Result not supported yet: " + result.getClass().getName());
                }

            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        else if (ingestionMethod == IngestionMethod.BATCH_INSERT)
        {
            streamResultToNewTarget(((RelationalResult) result).resultSet, connection, tableName, 100);
        }
        return true;
    }

    public static boolean checkedExecute(Statement statement, String sql)
    {
        try (Scope ignored = GlobalTracer.get().buildSpan("temp table sql execution").withTag("sql", sql).startActive(true))
        {
            LOGGER.info(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.EXECUTION_RELATIONAL_COMMIT, sql, 0.0d).toString());
            return statement.execute(sql);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void streamResultToNewTarget(ResultSet source, Connection targetConnection, String targetTableName, int batchSize)
    {
        byte[] b_leftParan = "(".getBytes();
        byte[] b_rightParan = ")".getBytes();
        byte[] b_comma = ",".getBytes();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final String insertPrefix = "INSERT INTO " + targetTableName + " VALUES  ";
        boolean finalInsert = false;
        try
        {
            int columnCount = source.getMetaData().getColumnCount();
            try (Statement statement = targetConnection.createStatement())
            {
                while (source.next())
                {

                    for (int i = 1; i <= batchSize; i++)
                    {
                        bos.write(b_leftParan);
                        for (int c = 1; c <= columnCount; c++)
                        {
                            bos.write(parseObjectForInsert(source.getObject(c)).getBytes());
                            if (c < columnCount)
                            {
                                bos.write(b_comma);
                            }
                        }
                        bos.write(b_rightParan);

                        if (!source.isLast() && i != batchSize)
                        {
                            bos.write(b_comma);
                        }

                        if (!source.isLast())
                        {
                            source.next();
                        }
                        else
                        {
                            finalInsert = true;
                        }
                    }
                    statement.execute(insertPrefix + bos.toString());
                    bos.reset();
                }
                if (finalInsert)
                {
                    statement.execute(insertPrefix + bos.toString());
                }
            }
            bos.close();

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private String parseObjectForInsert(Object value)
    {  //TODO:: This probably doesn't work for complex types
        return value instanceof CharSequence ? "'" + value.toString() + "'" : value.toString();
    }

    private void batchInsertRealizedRelationalResultToDB2TempTable(RealizedRelationalResult realizedRelationalResult, Statement statement, String targetTableName, int batchSize) throws SQLException
    {
        final String insertPrefix = "INSERT INTO " + targetTableName + " VALUES  ";
        final Function<Object, String> normalizer = v ->
        {
            if (v == null)
            {
                return "";
            }
            if (v instanceof Number)
            {
                return (String) ResultNormalizer.normalizeToSql(v, this.databaseTimeZone);
            }
            return "'" + ResultNormalizer.normalizeToSql(v, this.databaseTimeZone) + "'";
        };

        Iterator<List<List<Object>>> rowBatchIterator = Iterators.partition(realizedRelationalResult.resultSetRows.iterator(), batchSize);
        while (rowBatchIterator.hasNext())
        {
            String insertSql = rowBatchIterator.next()
                    .stream()
                    .map(row -> row.stream().map(normalizer).collect(Collectors.joining(",", "(", ")")))
                    .collect(Collectors.joining(",", insertPrefix, ""));
            statement.execute(insertSql);
        }
    }


}
