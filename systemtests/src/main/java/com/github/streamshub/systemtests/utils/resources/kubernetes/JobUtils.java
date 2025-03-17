package com.github.streamshub.systemtests.utils.resources.kubernetes;

import com.github.streamshub.systemtests.constants.ResourceKinds;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.resources.ResourceOperation;
import com.github.streamshub.systemtests.utils.CommonUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;


public class JobUtils {

    private static final Logger LOGGER = LogManager.getLogger(JobUtils.class);

    // -------------
    // Client
    // -------------
    private static MixedOperation<Job, JobList, ScalableResource<Job>> jobClient() {
        return KubeResourceManager.getKubeClient().getClient().batch().v1().jobs();
    }

    // -------------
    // Get
    // -------------
    public static Job getJob(String namespace, String name) {
        return jobClient().inNamespace(namespace).withName(name).get();
    }

    public static JobStatus getJobStatus(String namespaceName, String jobName) {
        Job job = getJob(namespaceName, jobName);
        return job == null ? null : job.getStatus();
    }

    public static boolean checkFailedJobStatus(String namespaceName, String jobName, int expectedFailedPods) {
        JobStatus jobStatus = getJobStatus(namespaceName, jobName);
        return jobStatus != null && jobStatus.getFailed() != null && jobStatus.getFailed().equals(expectedFailedPods);
    }

    // -------------
    // List
    // -------------
    public static List<Job> listJobs(String namespace) {
        return jobClient().inNamespace(namespace).list().getItems();
    }

    // -------------
    // Wait
    // -------------
    public static void waitForJobDeletion(final String namespaceName, String name) {
        LOGGER.debug("Waiting for Job: {}/{} deletion", namespaceName, name);
        CommonUtils.waitFor("deletion of Job: " + namespaceName + "/" + name, TimeConstants.POLL_INTERVAL_FOR_RESOURCE_DELETION, TimeConstants.DELETION_TIMEOUT,
            () -> PodUtils.listPodNamesInSpecificNamespace(namespaceName, "job-name", name).isEmpty());
        LOGGER.debug("Job: {}/{} was deleted", namespaceName, name);
    }

    // -------------
    // Delete
    // -------------
    public static void deleteJob(String namespace, String name) {
        LOGGER.warn("Deleting Job {}", name);
        jobClient().inNamespace(namespace).withName(name).delete();
    }

    public static void deleteJobWithWait(String namespace, String name) {
        deleteJob(namespace, name);
        waitForJobDeletion(namespace, name);
    }

    public static void deleteJobsWithWait(String namespace, String... names) {
        for (String jobName : names) {
            deleteJobWithWait(namespace, jobName);
        }
    }

    public static void waitForJobFailure(String jobName, String namespace, long timeout) {
        LOGGER.info("Waiting for Job: {}/{} to fail", namespace, jobName);
        CommonUtils.waitFor("failure of Job: " + namespace + "/" + jobName, TimeConstants.GLOBAL_POLL_INTERVAL, timeout,
            () -> checkFailedJobStatus(namespace, jobName, 1));
    }

    public static boolean waitForJobRunning(String jobName, String namespace) {
        LOGGER.info("Waiting for Job: {}/{} to be in active state", namespace, jobName);
        CommonUtils.waitFor("Job: " + namespace + "/" + jobName + " to be in active state", TimeConstants.GLOBAL_POLL_INTERVAL, ResourceOperation.getTimeoutForResourceReadiness(ResourceKinds.JOB),
            () -> {
                JobStatus jb = getJobStatus(namespace, jobName);

                if (jb == null || jb.getActive() == null) {
                    return false;
                }

                return jb.getActive() > 0;
            });

        return true;
    }

    // -------------
    // Log
    // -------------
    public static void logCurrentJobStatus(String jobName, String namespace) {
        Job currentJob = getJob(namespace, jobName);

        if (currentJob != null && currentJob.getStatus() != null) {
            List<String> log = new ArrayList<>(asList(ResourceKinds.JOB, " status:\n"));

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

            for (Pod pod : PodUtils.listPodsByPrefixInName(currentJob.getMetadata().getNamespace(), jobName)) {
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
