"use client";
import { setPartialResource } from "@/api/resources";
import {
  ActionGroup,
  Button,
  Card,
  CardBody,
  Form,
  FormGroup,
  FormHelperText,
  HelperText,
  HelperTextItem,
  PageSection,
  ProgressStep,
  ProgressStepper,
  Sidebar,
  SidebarContent,
  SidebarPanel,
  Split,
  SplitItem,
  Text,
  TextContent,
  TextInput,
  Title,
  Truncate,
} from "@/libs/patternfly/react-core";
import { useRouter } from "@/navigation";
import { useState, useTransition } from "react";

export function Step1({ clusters }: { clusters: string[] }) {
  const router = useRouter();
  const [bootstrapServer, setBootstrapServer] = useState("");
  const [name, setName] = useState("");
  const [pending, startTransition] = useTransition();
  return (
    <>
      <PageSection variant={"light"}>
        <Split>
          <SplitItem isFilled>
            <TextContent>
              <Title headingLevel="h1">
                Create a new Resource to access a Kafka Cluster
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
                    variant="info"
                    id="basic-step1"
                    titleId="basic-step1-title"
                    aria-label="completed step, step with success"
                    isCurrent
                  >
                    Cluster information
                  </ProgressStep>
                  <ProgressStep
                    variant="pending"
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
                      router.push("/resources/create/kafka/authentication");
                    });
                  }}
                >
                  <FormGroup
                    label="Bootstrap Server"
                    isRequired
                    fieldId="bootstrapServer"
                  >
                    <TextInput
                      isRequired
                      type="text"
                      id="bootstrapServer"
                      name="boostrapServer"
                      aria-describedby="bootstrapServer-helper"
                      value={bootstrapServer}
                      onChange={(_, value) => setBootstrapServer(value)}
                    />
                    <FormHelperText>
                      <HelperText>
                        <HelperTextItem>
                          <p>
                            Demo note: provide any value to the bootstrap url to
                            see how the &quot;Validate Connection&quot; works
                            when failing to connect to a cluster.
                          </p>
                          <p>Valid boostrap servers value for the demo are:</p>
                          <ul>
                            {clusters.map((c, idx) => (
                              <li key={idx}>
                                <a onClick={() => setBootstrapServer(c)}>
                                  <Truncate content={c} position={"middle"} />
                                </a>
                              </li>
                            ))}
                          </ul>
                        </HelperTextItem>
                      </HelperText>
                    </FormHelperText>
                  </FormGroup>
                  <FormGroup label="Cluster name" isRequired fieldId="name">
                    <TextInput
                      isRequired
                      type="text"
                      id="name"
                      name="name"
                      value={name}
                      onChange={(_, value) => setName(value)}
                    />
                  </FormGroup>
                  <ActionGroup>
                    <Button
                      type={"submit"}
                      isLoading={pending}
                      isDisabled={
                        pending || bootstrapServer === "" || name == ""
                      }
                    >
                      Next
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
