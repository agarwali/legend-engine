// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.api;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.SqlPlanAbstract;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;
import org.finos.legend.engine.persistence.components.util.ValidationCategory;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public abstract class GeneratorResultAbstract
{
    private static final String SINGLE_QUOTE = "'";

    public abstract SqlPlan preActionsSqlPlan();

    public abstract Optional<SqlPlan> dryRunPreActionsSqlPlan();

    public abstract Optional<SqlPlan> initializeLockSqlPlan();

    public abstract Optional<SqlPlan> acquireLockSqlPlan();

    public abstract Optional<SqlPlan> schemaEvolutionSqlPlan();

    public abstract Optional<Dataset> schemaEvolutionDataset();

    public abstract SqlPlan ingestSqlPlan();

    public abstract Optional<SqlPlan> dryRunSqlPlan();

    public abstract Map<ValidationCategory, List<Pair<Set<FieldValue>, SqlPlan>>> dryRunValidationSqlPlan();

    public abstract Optional<DataSplitRange> ingestDataSplitRange();

    public abstract SqlPlan metadataIngestSqlPlan();

    public abstract Optional<SqlPlan> deduplicationAndVersioningSqlPlan();

    public abstract SqlPlan postActionsSqlPlan();

    public abstract Optional<SqlPlan> postCleanupSqlPlan();

    public abstract Optional<SqlPlan> dryRunPostCleanupSqlPlan();

    public abstract Map<DedupAndVersionErrorSqlType, SqlPlan> deduplicationAndVersioningErrorChecksSqlPlan();

    public abstract Map<StatisticName, SqlPlan> preIngestStatisticsSqlPlan();

    public abstract Map<StatisticName, SqlPlan> postIngestStatisticsSqlPlan();

    public List<String> preActionsSql()
    {
        return preActionsSqlPlan().getSqlList();
    }

    public List<String> dryRunPreActionsSql()
    {
        return dryRunPreActionsSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }

    public List<String> initializeLockSql()
    {
        return initializeLockSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }

    public List<String> acquireLockSql()
    {
        return acquireLockSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }

    public List<String> schemaEvolutionSql()
    {
        return schemaEvolutionSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }

    public List<String> ingestSql()
    {
        return ingestDataSplitRange()
            .map(dataSplitRange -> ingestSqlPlan().getSqlList()
                .stream()
                .map(sql -> enrichSqlWithDataSplits(sql, dataSplitRange))
                .collect(Collectors.toList()))
            .orElseGet(ingestSqlPlan()::getSqlList);
    }

    public List<String> dryRunSql()
    {
        return dryRunSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }

    public Map<ValidationCategory, List<Pair<Set<FieldValue>, String>>> dryRunValidationSql()
    {
        return dryRunValidationSqlPlan().keySet().stream()
            .collect(Collectors.toMap(
               k -> k,
               k -> dryRunValidationSqlPlan().get(k).stream().map(
                   e -> Tuples.pair(e.getOne(), e.getTwo().getSql())
               ).collect(Collectors.toList())
            ));
    }

    public List<String> metadataIngestSql()
    {
        return metadataIngestSqlPlan().getSqlList();
    }

    public List<String> deduplicationAndVersioningSql()
    {
        return deduplicationAndVersioningSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }

    public List<String> postActionsSql()
    {
        return postActionsSqlPlan().getSqlList();
    }

    public List<String> postCleanupSql()
    {
        return postCleanupSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }

    public List<String> dryRunPostCleanupSql()
    {
        return dryRunPostCleanupSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }

    public Map<StatisticName, String> preIngestStatisticsSql()
    {
        return preIngestStatisticsSqlPlan().keySet().stream()
            .collect(Collectors.toMap(
                k -> k,
                k -> preIngestStatisticsSqlPlan().get(k).getSql()));
    }

    public Map<DedupAndVersionErrorSqlType, String> deduplicationAndVersioningErrorChecksSql()
    {
        return deduplicationAndVersioningErrorChecksSqlPlan().keySet().stream()
                .collect(Collectors.toMap(
                        k -> k,
                        k -> deduplicationAndVersioningErrorChecksSqlPlan().get(k).getSql()));
    }

    public Map<StatisticName, String> postIngestStatisticsSql()
    {
        return postIngestStatisticsSqlPlan().keySet().stream()
            .collect(Collectors.toMap(
                k -> k,
                k ->
                {
                    String statsSql = postIngestStatisticsSqlPlan().get(k).getSql();
                    if (ingestDataSplitRange().isPresent())
                    {
                        return enrichSqlWithDataSplits(statsSql, ingestDataSplitRange().get());
                    }
                    else
                    {
                        return statsSql;
                    }
                }));
    }

    private String enrichSqlWithDataSplits(String sql, DataSplitRange dataSplitRange)
    {
        return sql
                .replace(SINGLE_QUOTE + LogicalPlanUtils.DATA_SPLIT_LOWER_BOUND_PLACEHOLDER + SINGLE_QUOTE, String.valueOf(dataSplitRange.lowerBound()))
                .replace(SINGLE_QUOTE + LogicalPlanUtils.DATA_SPLIT_UPPER_BOUND_PLACEHOLDER + SINGLE_QUOTE, String.valueOf(dataSplitRange.upperBound()));
    }
}
