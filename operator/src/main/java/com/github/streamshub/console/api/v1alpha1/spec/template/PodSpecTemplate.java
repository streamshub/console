package com.github.streamshub.console.api.v1alpha1.spec.template;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.TopologySpreadConstraint;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PodSpecTemplate {

    @JsonPropertyDescription("""
            The pod's affinity rules. Allows constraining which nodes the console \
            pod is eligible to be scheduled on, based on node labels or the labels \
            of other pods already running on a node.
            """)
    private Affinity affinity;

    @JsonPropertyDescription("""
            The pod's tolerations. Allows the console pod to be scheduled onto \
            nodes that have matching taints.
            """)
    private List<Toleration> tolerations;

    @JsonPropertyDescription("""
            The pod's topology spread constraints. Controls how the console pod \
            is spread across failure-domains such as regions, zones, or nodes.
            """)
    private List<TopologySpreadConstraint> topologySpreadConstraints;

    @JsonPropertyDescription("""
            A selector which must match a node's labels for the console pod \
            to be scheduled on that node.
            """)
    private Map<String, String> nodeSelector;

    @JsonPropertyDescription("""
            Template for the console container. Allows configuration image, \
            pull policy, resources, and environment variables.
            """)
    private ContainerTemplate serverContainer;

    public Affinity getAffinity() {
        return affinity;
    }

    public void setAffinity(Affinity affinity) {
        this.affinity = affinity;
    }

    public List<Toleration> getTolerations() {
        return tolerations;
    }

    public void setTolerations(List<Toleration> tolerations) {
        this.tolerations = tolerations;
    }

    public List<TopologySpreadConstraint> getTopologySpreadConstraints() {
        return topologySpreadConstraints;
    }

    public void setTopologySpreadConstraints(List<TopologySpreadConstraint> topologySpreadConstraints) {
        this.topologySpreadConstraints = topologySpreadConstraints;
    }

    public Map<String, String> getNodeSelector() {
        return nodeSelector;
    }

    public void setNodeSelector(Map<String, String> nodeSelector) {
        this.nodeSelector = nodeSelector;
    }

    public ContainerTemplate getServerContainer() {
        return serverContainer;
    }

    public void setServerContainer(ContainerTemplate serverContainer) {
        this.serverContainer = serverContainer;
    }
}
