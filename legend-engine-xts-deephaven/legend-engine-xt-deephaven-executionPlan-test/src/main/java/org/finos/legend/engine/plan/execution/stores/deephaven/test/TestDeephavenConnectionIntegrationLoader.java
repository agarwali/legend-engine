// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.deephaven.test;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.slf4j.Logger;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

public class TestDeephavenConnectionIntegrationLoader
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TestConnectionIntegration.class);
    private static AtomicReference<MutableList<TestConnectionIntegration>> EXTENSIONS = new AtomicReference<>();

    public static void logExtensionList()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(LazyIterate.collect(extensions(), extension -> "- " + extension.getClass().getSimpleName()).makeString("TestConnectionIntegration extension(s) loaded:\n", "\n", ""));
        }
    }

    public static MutableList<TestConnectionIntegration> extensions()
    {
        return EXTENSIONS.updateAndGet(extensions ->
        {
            if (extensions == null)
            {
                MutableList<TestConnectionIntegration> result = Lists.mutable.empty();
                for (TestConnectionIntegration extension : ServiceLoader.load(TestConnectionIntegration.class))
                {
                    try
                    {
                        result.add(extension);
                    }
                    catch (Throwable throwable)
                    {
                        LOGGER.error("Failed to load the Deephaven TestConnectionIntegration extension '" + extension.getClass().getSimpleName() + "'");
                    }
                }
                return result;
            }
            return extensions;
        });
    }
}

