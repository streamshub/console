package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class JobUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(JobUtils.class);
    private JobUtils() {}

    public static boolean checkSucceededJobStatus(String namespaceName, String jobName, int expectedSucceededPods) {
        Job job = ResourceUtils.getKubeResource(Job.class, namespaceName, jobName);
        if (job == null) {
            return false;
        }
        return job.getStatus() != null && job.getStatus().getSucceeded() != null && job.getStatus().getSucceeded().equals(expectedSucceededPods);
    }

    public static void logCurrentJobStatus(String namespaceName, String jobName) {
        Job currentJob = KubeResourceManager.get().kubeClient().getClient().batch().v1().jobs().inNamespace(namespaceName).withName(jobName).get();

        if (currentJob != null && currentJob.getStatus() != null) {
            List<String> log = new ArrayList<>(asList(HasMetadata.getKind(Job.class), " status:\n"));

            List<JobCondition> conditions = currentJob.getStatus().getConditions();

            log.add("\tActive: " + currentJob.getStatus().getActive());
            log.add("\n\tFailed: " + currentJob.getStatus().getFailed());
            log.add("\n\tReady: " + currentJob.getStatus().getReady());
            log.add("\n\tSucceeded: " + currentJob.getStatus().getSucceeded());

            if (conditions != null) {
                List<String> conditionList = new ArrayList<>();

                for (JobCondition condition : conditions) {
                    if (condition.getMessage() != null) {
                        conditionList.add("\t\tType: " + condition.getType() + "\n");
                        conditionList.add("\t\tMessage: " + condition.getMessage() + "\n");
                    }
                }

                if (!conditionList.isEmpty()) {
                    log.add("\n\tConditions:\n");
                    log.addAll(conditionList);
                }
            }

            log.add("\n\nPods with conditions and messages:\n\n");

            for (Pod pod : KubeResourceManager.get().kubeClient().listPodsByPrefixInName(currentJob.getMetadata().getNamespace(), jobName)) {
                log.add(pod.getMetadata().getName() + ":");
                List<String> podConditions = new ArrayList<>();

                for (PodCondition podCondition : pod.getStatus().getConditions()) {
                    if (podCondition.getMessage() != null) {
                        podConditions.add("\n\tType: " + podCondition.getType() + "\n");
                        podConditions.add("\tMessage: " + podCondition.getMessage() + "\n");
                    }
                }

                if (podConditions.isEmpty()) {
                    log.add("\n\t<EMPTY>");
                } else {
                    log.addAll(podConditions);
                }
                log.add("\n\n");
            }
            LOGGER.info("{}", String.join("", log).strip());
        }
    }
}
