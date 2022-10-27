package com.microsoft.azure.hdinsight.spark.run

import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType
import com.microsoft.azure.hdinsight.spark.run.configuration.ArisSparkConfigurationType
import com.microsoft.azure.toolkit.intellij.hdinsight.spark.run.SparkBatchJobLocalRunConfigurationProducer

class ArisSparkRunConfigurationProducer : SparkBatchJobLocalRunConfigurationProducer(
        ArisSparkConfigurationType,
        SparkApplicationType.ArisSpark
)