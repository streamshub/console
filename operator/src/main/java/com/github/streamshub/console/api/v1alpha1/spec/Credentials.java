package com.github.streamshub.console.api.v1alpha1.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.generator.annotation.ValidationRule;
import io.sundr.builder.annotations.Buildable;

@ValidationRule(value = "has(self.kafkaUser)", message = "kafkaUser is required")
@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Credentials {

    @JsonPropertyDescription("Reference to a Strimzi KafkaUser resource")
    CredentialsKafkaUser kafkaUser;

    public CredentialsKafkaUser getKafkaUser() {
        return kafkaUser;
    }

    public void setKafkaUser(CredentialsKafkaUser user) {
        this.kafkaUser = user;
    }

}
