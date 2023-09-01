"use client";
import { ButtonLink } from "@/components/buttonLink";
import {
  ActionGroup,
  Card,
  CardBody,
  Form,
  FormGroup,
  PageSection,
  ProgressStep,
  ProgressStepper,
  Radio,
  Split,
  SplitItem,
  Text,
  TextContent,
  TextInput,
  Title,
} from "@/libs/patternfly/react-core";
import { useState } from "react";

export default function NewAuthProfilePage({
  params: { cluster },
}: {
  params: { cluster: string };
}) {
  const [username, setUsername] = useState("");
  return (
    <>
      <PageSection variant={"light"}>
        <Split>
          <SplitItem isFilled>
            <TextContent>
              <Title headingLevel="h1">
                Create a new Authorization Profile to access a Cluster
              </Title>
              <Text>
                Brief description of what an Authorization Profile is and works
              </Text>
            </TextContent>
          </SplitItem>
        </Split>
      </PageSection>
      <PageSection>
        <ProgressStepper isCenterAligned aria-label="Basic progress stepper">
          <ProgressStep
            variant="success"
            id="basic-step1"
            titleId="basic-step1-title"
            aria-label="completed step, step with success"
            isCurrent
          >
            Select a Cluster
          </ProgressStep>
          <ProgressStep
            variant="info"
            id="basic-step2"
            titleId="basic-step2-title"
            aria-label="step with info"
          >
            Configure Authentication
          </ProgressStep>
          <ProgressStep
            variant="pending"
            id="basic-step3"
            titleId="basic-step3-title"
            aria-label="pending step"
          >
            Validate Connection
          </ProgressStep>
        </ProgressStepper>
      </PageSection>
      <PageSection isFilled>
        <Card>
          <CardBody>
            <Form>
              <FormGroup
                role="radiogroup"
                isInline
                fieldId="basic-form-radio-group"
                label="SASL Mechanism"
              >
                <Radio
                  name="basic-inline-radio"
                  label="PLAIN"
                  id="basic-inline-radio-01"
                  isChecked
                />
                <Radio
                  name="basic-inline-radio"
                  label="SCRAM"
                  id="basic-inline-radio-02"
                />
                <Radio
                  name="basic-inline-radio"
                  label="OAUTH"
                  id="basic-inline-radio-03"
                />
                <Radio
                  name="basic-inline-radio"
                  label="GSSAPI"
                  id="basic-inline-radio-04"
                />
                <Radio
                  name="basic-inline-radio"
                  label="Custom"
                  id="basic-inline-radio-05"
                />
              </FormGroup>
              <FormGroup
                label="Username"
                isRequired
                fieldId="simple-form-name-01"
              >
                <TextInput
                  isRequired
                  type="text"
                  id="simple-form-name-01"
                  name="simple-form-name-01"
                  aria-describedby="simple-form-name-01-helper"
                  value={username}
                  onChange={(_, value) => setUsername(value)}
                />
              </FormGroup>
              <FormGroup
                label="Password"
                isRequired
                fieldId="simple-form-email-01"
              >
                <TextInput
                  isRequired
                  type="password"
                  id="simple-form-email-01"
                  name="simple-form-email-01"
                />
              </FormGroup>
              <ActionGroup>
                <ButtonLink
                  href={`${cluster}/validate?username=${username}`}
                  variant="primary"
                >
                  Validate
                </ButtonLink>
              </ActionGroup>
            </Form>
          </CardBody>
        </Card>
      </PageSection>
    </>
  );
}
