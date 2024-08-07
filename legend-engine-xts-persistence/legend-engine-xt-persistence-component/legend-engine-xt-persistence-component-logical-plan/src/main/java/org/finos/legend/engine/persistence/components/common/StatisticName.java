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

package org.finos.legend.engine.persistence.components.common;

public enum StatisticName
{
    INCOMING_RECORD_COUNT("incomingRecordCount"),
    ROWS_TERMINATED("rowsTerminated"),
    ROWS_INSERTED("rowsInserted"),
    ROWS_UPDATED("rowsUpdated"),
    ROWS_DELETED("rowsDeleted"),
    FILES_LOADED("filesLoaded"),
    ROWS_WITH_ERRORS("rowsWithErrors"),
    INPUT_FILES_BYTES_SCANNED("inputFilesBytesScanned");

    String value;

    StatisticName(String value)
    {
        this.value = value;
    }

    public String get()
    {
        return value;
    }
}
