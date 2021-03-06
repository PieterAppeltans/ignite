/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.yardstick.ml.knn;

import java.util.Map;
import org.apache.ignite.Ignite;
import org.apache.ignite.ml.knn.models.KNNModel;
import org.apache.ignite.ml.knn.models.KNNStrategy;
import org.apache.ignite.ml.knn.regression.KNNMultipleLinearRegression;
import org.apache.ignite.ml.math.distances.ManhattanDistance;
import org.apache.ignite.ml.structures.LabeledDataset;
import org.apache.ignite.ml.structures.LabeledDatasetTestTrainPair;
import org.apache.ignite.ml.structures.preprocessing.Normalizer;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.thread.IgniteThread;
import org.apache.ignite.yardstick.IgniteAbstractBenchmark;
import org.apache.ignite.yardstick.ml.DataChanger;

/**
 * Ignite benchmark that performs ML Grid operations.
 */
@SuppressWarnings("unused")
public class IgniteKNNRegressionBenchmark extends IgniteAbstractBenchmark {
    /** */
    @IgniteInstanceResource
    private Ignite ignite;

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        // Create IgniteThread, we must work with SparseDistributedMatrix inside IgniteThread
        // because we create ignite cache internally.
        IgniteThread igniteThread = new IgniteThread(ignite.configuration().getIgniteInstanceName(),
            this.getClass().getSimpleName(), new Runnable() {
            /** {@inheritDoc} */
            @Override public void run() {
                // IMPL NOTE originally taken from KNNRegressionExample.
                // Obtain shuffled dataset.
                LabeledDataset dataset = new Datasets().shuffleClearedMachines((int)(DataChanger.next()));

                // Normalize dataset
                Normalizer.normalizeWithMiniMax(dataset);

                // Random splitting of iris data as 80% train and 20% test datasets.
                LabeledDatasetTestTrainPair split = new LabeledDatasetTestTrainPair(dataset, 0.2);

                LabeledDataset test = split.test();
                LabeledDataset train = split.train();

                // Builds weighted kNN-regression with Manhattan Distance.
                KNNModel knnMdl = new KNNMultipleLinearRegression(7, new ManhattanDistance(), KNNStrategy.WEIGHTED, train);

                // Clone labels
                final double[] labels = test.labels();

                // Calculate predicted classes.
                for (int i = 0; i < test.rowSize() - 1; i++)
                    knnMdl.apply(test.getRow(i).features());
            }
        });

        igniteThread.start();

        igniteThread.join();

        return true;
    }
}
