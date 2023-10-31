"use client";
import { setPartialResource } from "@/api/resources";
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
  Sidebar,
  SidebarContent,
  SidebarPanel,
  Split,
  SplitItem,
  Text,
  TextContent,
  TextInput,
  Title,
} from "@/libs/patternfly/react-core";
import { Button } from "@patternfly/react-core";
import { useRouter } from "@/navigation";
import { useState, useTransition } from "react";

export function Step2() {
  const router = useRouter();
  const [principal, setPrincipal] = useState("");
  const [password, setPassword] = useState("");
  const [pending, startTransition] = useTransition();
  return (
    <>
      <PageSection variant={"light"}>
        <Split>
          <SplitItem isFilled>
            <TextContent>
              <Title headingLevel="h1">
                Create a new Resource to access a Cluster
              </Title>
              <Text>
                Brief description of what a Resource is and how it works.
              </Text>
            </TextContent>
          </SplitItem>
        </Split>
      </PageSection>
      <PageSection isFilled>
        <Card>
          <CardBody>
            <Sidebar>
              <SidebarPanel>
                <ProgressStepper isVertical aria-label="Basic progress stepper">
                  <ProgressStep
                    variant="success"
                    id="basic-step1"
                    titleId="basic-step1-title"
                    aria-label="completed step, step with success"
                    isCurrent
                  >
                    Cluster Information
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
              </SidebarPanel>
              <SidebarContent>
                <Form
                  action={async (formData) => {
                    await setPartialResource(formData);
                    startTransition(() => {
                      router.push("/resources/create/kafka/validate");
                    });
                  }}
                >
                  <FormGroup
                    role="radiogroup"
                    isInline
                    fieldId="mechanism"
                    label="SASL Mechanism"
                  >
                    <Radio
                      name="mechanism"
                      label="PLAIN"
                      id="mechanism-01"
                      isChecked
                    />
                    <Radio name="mechanism" label="SCRAM" id="mechanism-02" />
                    <Radio name="mechanism" label="OAUTH" id="mechanism-03" />
                    <Radio name="mechanism" label="GSSAPI" id="mechanism-04" />
                    <Radio name="mechanism" label="Custom" id="mechanism-05" />
                  </FormGroup>
                  <FormGroup label="Principal" isRequired fieldId="principal">
                    <TextInput
                      isRequired
                      type="text"
                      id="principal"
                      name="principal"
                      aria-describedby="principal-helper"
                      value={principal}
                      onChange={(_, value) => setPrincipal(value)}
                    />
                  </FormGroup>
                  <FormGroup label="Password" isRequired fieldId="password">
                    <TextInput
                      isRequired
                      type="password"
                      id="password"
                      name="password"
                      value={password}
                      onChange={(_, value) => setPassword(value)}
                    />
                  </FormGroup>
                  <ActionGroup>
                    <Button
                      variant="primary"
                      type={"submit"}
                      isLoading={pending}
                      isDisabled={
                        pending || principal === "" || password === ""
                      }
                    >
                      Validate
                    </Button>
                  </ActionGroup>
                </Form>
              </SidebarContent>
            </Sidebar>
          </CardBody>
        </Card>
      </PageSection>
    </>
  );
}
